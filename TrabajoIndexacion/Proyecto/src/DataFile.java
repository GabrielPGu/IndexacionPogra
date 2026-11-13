import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

final class DataFile {
    static final int MAGIC = 0x4C534446; // LSDF
    static final int VERSION = 1;
    static final int BUFFER = 64 * 1024;
    static final int MAX_RECORD = Integer.getInteger("lsdfms.maxRecord", 1024 * 1024);
    static final int MAX_FIELDS = 256;
    static final int PREFIX = 11;
    static final byte ACTIVE = 0;
    static final byte DELETED = 1;

    record Schema(String table, String[] columns) {
        void write(DataOutput out) throws IOException {
            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.writeUTF(table);
            out.writeShort(columns.length);
            for (String column : columns) out.writeUTF(column);
        }
    }

    static Schema readSchema(DataInput in) throws IOException {
        if (in.readInt() != MAGIC || in.readInt() != VERSION) {
            throw new IOException("Cabecera de datos inválida");
        }
        String table = in.readUTF();
        int count = in.readUnsignedShort();
        if (count == 0 || count > MAX_FIELDS) throw new IOException("Esquema inválido");
        String[] columns = new String[count];
        for (int i = 0; i < count; i++) columns[i] = in.readUTF();
        return new Schema(table, columns);
    }

    static final class Writer implements Closeable {
        private final FileOutputStream file;
        private final DataOutputStream out;

        Writer(Path path, Schema schema) throws IOException {
            file = new FileOutputStream(path.toFile());
            out = new DataOutputStream(new BufferedOutputStream(file, BUFFER));
            try {
                schema.write(out);
            } catch (IOException e) {
                out.close();
                throw e;
            }
        }

        void write(long key, boolean deleted, String[] fields) throws IOException {
            if (fields.length > MAX_FIELDS) throw new IOException("Demasiados campos en clave " + key);
            byte[][] values = new byte[fields.length][];
            long length = PREFIX + (long) Integer.BYTES * fields.length;
            for (int i = 0; i < fields.length; i++) {
                values[i] = fields[i].getBytes(StandardCharsets.UTF_8);
                length += values[i].length;
                if (length > MAX_RECORD) throw new IOException("Registro excesivo en clave " + key);
            }
            out.writeInt((int) length);
            out.writeByte(deleted ? DELETED : ACTIVE);
            out.writeLong(key);
            out.writeShort(fields.length);
            for (byte[] value : values) {
                out.writeInt(value.length);
                out.write(value);
            }
        }

        void copy(Row row) throws IOException {
            out.writeInt(row.length);
            out.write(row.buffer.array(), 0, row.length);
        }

        @Override
        public void close() throws IOException {
            try (out) {
                out.flush();
                file.getFD().sync();
            }
        }
    }

    // Cada lector reutiliza un solo buffer para el cuerpo del registro.
    static final class Row {
        private ByteBuffer buffer = ByteBuffer.allocate(Math.min(MAX_RECORD, 4096));
        long offset;
        int length;
        byte state;
        long key;
        int fieldCount;

        void read(DataInput in, Path path, long start, long remaining) throws IOException {
            offset = start;
            length = in.readInt();
            if (length < PREFIX || length > MAX_RECORD || length > remaining - Integer.BYTES) {
                throw invalid(path, "longitud de registro " + length);
            }
            if (buffer.capacity() < length) {
                buffer = ByteBuffer.allocate(Math.min(MAX_RECORD, Math.max(length, buffer.capacity() * 2)));
            }
            in.readFully(buffer.array(), 0, length);
            buffer.clear().limit(length);
            state = buffer.get();
            key = buffer.getLong();
            fieldCount = Short.toUnsignedInt(buffer.getShort());
            if ((state != ACTIVE && state != DELETED) || fieldCount > MAX_FIELDS) {
                throw invalid(path, "estado o número de campos inválido");
            }
            for (int i = 0; i < fieldCount; i++) {
                if (buffer.remaining() < Integer.BYTES) throw invalid(path, "campo truncado");
                int size = buffer.getInt();
                if (size < 0 || size > buffer.remaining()) throw invalid(path, "longitud de campo inválida");
                buffer.position(buffer.position() + size);
            }
            if (buffer.hasRemaining()) throw invalid(path, "bytes sobrantes");
        }

        String[] fields() {
            String[] fields = new String[fieldCount];
            buffer.position(PREFIX);
            for (int i = 0; i < fieldCount; i++) {
                int size = buffer.getInt();
                fields[i] = new String(buffer.array(), buffer.position(), size, StandardCharsets.UTF_8);
                buffer.position(buffer.position() + size);
            }
            return fields;
        }

        String field(int column) throws IOException {
            if (column < 0 || column >= fieldCount) throw new IOException("Falta el campo " + column + " en clave " + key);
            buffer.position(PREFIX);
            for (int i = 0; i < column; i++) {
                int size = buffer.getInt();
                buffer.position(buffer.position() + size);
            }
            int size = buffer.getInt();
            return new String(buffer.array(), buffer.position(), size, StandardCharsets.UTF_8);
        }

        long end() {
            return offset + Integer.BYTES + length;
        }

        IOException invalid(Path path, String detail) {
            return new IOException(path + " en offset " + offset + ": " + detail);
        }
    }

    static final class Reader implements Closeable {
        private final DataInputStream in;
        private final Path path;
        private final long size;
        private long offset;
        final Schema schema;
        final Row row = new Row();

        Reader(Path path) throws IOException {
            this.path = path;
            size = Files.size(path);
            in = new DataInputStream(new BufferedInputStream(Files.newInputStream(path), BUFFER));
            try {
                schema = readSchema(in);
                // Se obtiene el tamaño exacto de la cabecera con el mismo codec.
                try (var bytes = new ByteArrayOutputStream(); var header = new DataOutputStream(bytes)) {
                    schema.write(header);
                    offset = bytes.size();
                }
            } catch (IOException e) {
                in.close();
                throw new IOException("Cabecera inválida: " + path, e);
            }
        }

        boolean next() throws IOException {
            if (offset == size) return false;
            if (size - offset < Integer.BYTES) throw row.invalid(path, "final truncado en " + offset);
            row.read(in, path, offset, size - offset);
            offset = row.end();
            return true;
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }

    record Found(Schema schema, Row row) {}

    static Found readAt(RandomAccessFile in, Path path, long offset, long key) throws IOException {
        Found found = readAt(in, path, offset);
        if (found.row.key != key) throw found.row.invalid(path, "clave del índice incorrecta");
        return found;
    }

    static Found readAt(RandomAccessFile in, Path path, long offset) throws IOException {
        in.seek(0);
        Schema schema = readSchema(in);
        Row row = new Row();
        readAt(in, path, offset, in.getFilePointer(), in.length(), row);
        return new Found(schema, row);
    }

    static void readAt(RandomAccessFile in, Path path, long offset, long first, long size, Row row) throws IOException {
        if (offset < first || offset > size - Integer.BYTES) {
            throw new IOException("Offset fuera de rango: " + path + "@" + offset);
        }
        in.seek(offset);
        row.read(in, path, offset, size - offset);
    }

    private DataFile() {}
}
