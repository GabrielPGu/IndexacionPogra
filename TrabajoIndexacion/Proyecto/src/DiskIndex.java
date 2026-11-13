import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.PriorityQueue;

final class DiskIndex {
    static final int MAGIC = 0x4C534958; // LSIX
    static final int HEADER = 24;
    static final int ENTRY = 20;
    private static final int RUN_SIZE = 65_536;
    private static final int FAN_IN = 16;

    record Entry(long key, int file, long offset) {}

    interface KeySource {
        int column(DataFile.Schema schema);
        long key(DataFile.Row row, int column) throws IOException;
    }

    static long build(Store store, Store.Manifest data, Path work, Path index, int threads) throws IOException {
        return build(store, data, work, index, threads, MAGIC, null);
    }

    static long build(Store store, Store.Manifest data, Path work, Path index, int threads,
                      int magic, KeySource source) throws IOException {
        int files = data.fileCount();
        Parallel.run(files, threads, file -> {
            Path directory = Files.createDirectory(work.resolve("file-" + file));
            try (DataFile.Reader reader = new DataFile.Reader(store.data(data, file))) {
                int column = source == null ? 0 : source.column(reader.schema);
                if (column < 0) {
                    Files.createFile(run(work, 0, file));
                    return;
                }
                long[] keys = new long[RUN_SIZE];
                long[] offsets = new long[RUN_SIZE];
                long[] keyScratch = new long[RUN_SIZE];
                long[] offsetScratch = new long[RUN_SIZE];
                long runs = 0;
                int size = 0;
                while (reader.next()) {
                    if (reader.row.state == DataFile.DELETED) continue;
                    keys[size] = source == null ? reader.row.key : source.key(reader.row, column);
                    offsets[size++] = reader.row.offset;
                    if (size == RUN_SIZE) {
                        writeRun(run(directory, 0, runs++), file, size, keys, offsets, keyScratch, offsetScratch);
                        size = 0;
                    }
                }
                if (size != 0) {
                    writeRun(run(directory, 0, runs++), file, size, keys, offsets, keyScratch, offsetScratch);
                }
                reduce(directory, runs, run(work, 0, file));
            }
        });
        Path sorted = work.resolve("sorted");
        reduce(work, files, sorted);
        long count = Files.size(sorted) / ENTRY;
        try (FileOutputStream file = new FileOutputStream(index.toFile());
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(file, DataFile.BUFFER));
             InputStream in = Files.newInputStream(sorted)) {
            out.writeInt(magic);
            out.writeInt(DataFile.VERSION);
            out.writeLong(data.version());
            out.writeLong(count);
            in.transferTo(out);
            out.flush();
            file.getFD().sync();
        }
        return count;
    }

    private static Path run(Path directory, int pass, long number) {
        return directory.resolve(pass + "-" + number + ".run");
    }

    private static void writeRun(Path path, int file, int size, long[] keys, long[] offsets,
                                 long[] keyScratch, long[] offsetScratch) throws IOException {
        sort(keys, offsets, keyScratch, offsetScratch, 0, size);
        try (DataOutputStream out = output(path)) {
            for (int i = 0; i < size; i++) {
                out.writeLong(keys[i]);
                out.writeInt(file);
                out.writeLong(offsets[i]);
            }
        }
    }

    private static void sort(long[] keys, long[] offsets, long[] keyScratch, long[] offsetScratch,
                             int start, int end) {
        if (end - start < 2) return;
        int middle = (start + end) / 2;
        sort(keys, offsets, keyScratch, offsetScratch, start, middle);
        sort(keys, offsets, keyScratch, offsetScratch, middle, end);
        if (keys[middle - 1] <= keys[middle]) return;
        int left = start;
        int right = middle;
        for (int target = start; target < end; target++) {
            int next = right == end || (left < middle && keys[left] <= keys[right]) ? left++ : right++;
            keyScratch[target] = keys[next];
            offsetScratch[target] = offsets[next];
        }
        System.arraycopy(keyScratch, start, keys, start, end - start);
        System.arraycopy(offsetScratch, start, offsets, start, end - start);
    }

    private static void reduce(Path directory, long runs, Path result) throws IOException {
        if (runs == 0) {
            Files.createFile(result);
            return;
        }
        int pass = 0;
        while (runs > 1) {
            long groups = 0;
            for (long start = 0; start < runs; start += FAN_IN) {
                int count = (int) Math.min(FAN_IN, runs - start);
                merge(directory, pass, start, count, run(directory, pass + 1, groups++));
                for (int i = 0; i < count; i++) Files.delete(run(directory, pass, start + i));
            }
            runs = groups;
            pass++;
        }
        Files.move(run(directory, pass, 0), result);
    }

    private static void merge(Path directory, int pass, long start, int count, Path target) throws IOException {
        PriorityQueue<Cursor> queue = new PriorityQueue<>(count, (a, b) -> Long.compare(a.key, b.key));
        try (Inputs inputs = new Inputs(count); DataOutputStream out = output(target)) {
            for (int i = 0; i < count; i++) {
                Cursor cursor = inputs.open(i, run(directory, pass, start + i));
                if (cursor.next()) queue.add(cursor);
            }
            while (!queue.isEmpty()) {
                Cursor cursor = queue.remove();
                out.writeLong(cursor.key);
                out.writeInt(cursor.file);
                out.writeLong(cursor.offset);
                if (cursor.next()) queue.add(cursor);
            }
        }
    }

    private static DataOutputStream output(Path path) throws IOException {
        return new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path), DataFile.BUFFER));
    }

    private static final class Inputs implements Closeable {
        private final Cursor[] cursors;

        Inputs(int count) {
            cursors = new Cursor[count];
        }

        Cursor open(int i, Path path) throws IOException {
            cursors[i] = new Cursor(path, 0);
            return cursors[i];
        }

        @Override
        public void close() throws IOException {
            IOException failure = null;
            for (Cursor cursor : cursors) {
                if (cursor == null) continue;
                try {
                    cursor.close();
                } catch (IOException e) {
                    if (failure == null) failure = e;
                    else failure.addSuppressed(e);
                }
            }
            if (failure != null) throw failure;
        }
    }

    static final class Cursor implements Closeable {
        final DataInputStream in;
        long remaining;
        long key;
        int file;
        long offset;

        Cursor(Path path, int header) throws IOException {
            long size = Files.size(path) - header;
            if (size < 0 || size % ENTRY != 0) throw new IOException("Índice truncado: " + path);
            remaining = size / ENTRY;
            in = new DataInputStream(new BufferedInputStream(Files.newInputStream(path), DataFile.BUFFER));
        }

        boolean next() throws IOException {
            if (remaining == 0) return false;
            key = in.readLong();
            file = in.readInt();
            offset = in.readLong();
            remaining--;
            return true;
        }

        @Override
        public void close() throws IOException {
            in.close();
        }
    }

    static long header(DataInput in, long entries, long version) throws IOException {
        return header(in, entries, version, MAGIC);
    }

    static long header(DataInput in, long entries, long version, int magic) throws IOException {
        if (in.readInt() != magic || in.readInt() != DataFile.VERSION || in.readLong() != version) {
            throw new IOException("Cabecera de índice inválida o versión incompatible");
        }
        long count = in.readLong();
        if (count != entries) throw new IOException("Cantidad de entradas inválida");
        return count;
    }

    static Entry find(Path path, long version, long key) throws IOException {
        return find(path, version, key, MAGIC);
    }

    static Entry find(Path path, long version, long key, int magic) throws IOException {
        try (RandomAccessFile in = new RandomAccessFile(path.toFile(), "r")) {
            long size = in.length() - HEADER;
            if (size < 0 || size % ENTRY != 0) throw new IOException("Índice truncado: " + path);
            long low = 0;
            long high = header(in, size / ENTRY, version, magic) - 1;
            while (low <= high) {
                long middle = low + (high - low) / 2;
                in.seek(HEADER + middle * ENTRY);
                long candidate = in.readLong();
                if (candidate == key) return new Entry(candidate, in.readInt(), in.readLong());
                if (candidate < key) low = middle + 1;
                else high = middle - 1;
            }
            return null;
        }
    }

    private DiskIndex() {}
}
