import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class CsvSource implements Closeable {
    private final PushbackReader in;
    private final Path path;
    private long completedRows;

    CsvSource(Path path) throws IOException {
        this.path = path;
        in = new PushbackReader(new BufferedReader(
                new InputStreamReader(open(path), StandardCharsets.UTF_8.newDecoder()), DataFile.BUFFER), 1);
        try {
            int first = in.read();
            if (first != -1 && first != '\uFEFF') in.unread(first);
        } catch (IOException e) {
            in.close();
            throw e;
        }
    }

    private static InputStream open(Path path) throws IOException {
        BufferedInputStream input = new BufferedInputStream(Files.newInputStream(path), DataFile.BUFFER);
        try {
            input.mark(4);
            boolean zip = input.read() == 'P' && input.read() == 'K'
                    && input.read() == 3 && input.read() == 4;
            input.reset();
            if (!zip) return input;

            ZipInputStream archive = new ZipInputStream(input);
            ZipEntry entry;
            while ((entry = archive.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().equals(path.getFileName().toString())) return archive;
            }
            throw new IOException("El ZIP no contiene " + path.getFileName());
        } catch (IOException e) {
            input.close();
            throw e;
        }
    }

    String[] next() throws IOException {
        String[] fields = new String[8];
        int count = 0;
        int chars = 0;
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean afterQuote = false;
        boolean atStart = true;

        while (true) {
            int ch = in.read();
            if (ch == -1 && chars == 0) return null;
            if (ch != -1 && ++chars > DataFile.MAX_RECORD) throw error("fila demasiado larga");
            if (quoted) {
                if (ch == -1) throw error("comillas sin cerrar");
                if (ch == '"') {
                    quoted = false;
                    afterQuote = true;
                } else {
                    field.append((char) ch);
                }
                continue;
            }
            if (afterQuote && ch == '"') {
                field.append('"');
                quoted = true;
                afterQuote = false;
                continue;
            }
            if (ch == ',' || ch == '\r' || ch == '\n' || ch == -1) {
                if (count == DataFile.MAX_FIELDS) throw error("demasiadas columnas");
                if (count == fields.length) fields = Arrays.copyOf(fields, fields.length * 2);
                fields[count++] = field.toString();
                if (ch != ',') {
                    if (ch == '\r') {
                        int next = in.read();
                        if (next != -1 && next != '\n') in.unread(next);
                    }
                    completedRows++;
                    return Arrays.copyOf(fields, count);
                }
                field.setLength(0);
                atStart = true;
                afterQuote = false;
                continue;
            }
            if (afterQuote) throw error("carácter después del cierre de comillas");
            if (ch == '"') {
                if (!atStart) throw error("comillas en un valor sin delimitar");
                quoted = true;
            } else {
                field.append((char) ch);
            }
            atStart = false;
        }
    }

    IOException error(String detail) {
        return errorAt(completedRows + 1, detail);
    }

    IOException invalidRow(String detail) {
        return errorAt(completedRows, detail);
    }

    private IOException errorAt(long row, String detail) {
        return new IOException(path + ", fila CSV " + row + ": " + detail);
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
