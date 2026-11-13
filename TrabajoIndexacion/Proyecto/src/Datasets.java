import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

final class Datasets {
    private static final String[] TABLES = {"articles", "customers", "transactions_train"};
    private static final long TABLE_STRIDE = 1_000_000_000_000L;

    static Store.Manifest importHAndM(Store store, Path input, int partitions, long limit) throws IOException {
        if (partitions < 1 || partitions > Store.MAX_FILES / TABLES.length || limit < 0) {
            throw new IllegalArgumentException("Particiones o límite inválidos");
        }
        store.requireEmpty();
        var draft = new Store.Manifest(1, TABLES.length * partitions, 0, 0, "");
        Files.createDirectory(store.version(draft.version()));
        long total = 0;
        for (int table = 0; table < TABLES.length; table++) {
            String name = TABLES[table];
            try (var csv = new CsvSource(input.resolve(name + ".csv")); var files = new Writers(partitions)) {
                String[] columns = csv.next();
                if (columns == null) throw csv.error("falta la cabecera");
                var schema = new DataFile.Schema(name, columns);
                for (int part = 0; part < partitions; part++) {
                    files.open(part, store.data(draft, table * partitions + part), schema);
                }
                long row = 0;
                while (row < limit) {
                    String[] values = csv.next();
                    if (values == null) break;
                    if (values.length != columns.length) {
                        throw csv.invalidRow("se esperaban " + columns.length + " campos y se leyeron " + values.length);
                    }
                    if (++row >= TABLE_STRIDE) throw csv.invalidRow("se agotaron las claves de la tabla");
                    int part = (int) ((row - 1) % partitions);
                    files.parts[part].write(table * TABLE_STRIDE + row, false, values);
                }
                total += row;
                System.out.printf("%s filas=%d%n", name, row);
            }
        }
        return store.initialize(new Store.Manifest(1, draft.fileCount(), total, 0, ""));
    }

    static Store.Manifest generate(Store store, long count, int files, int deletePercent) throws IOException {
        if (count < 0 || count == Long.MAX_VALUE || files < 1 || files > Store.MAX_FILES
                || deletePercent < 0 || deletePercent > 100) {
            throw new IllegalArgumentException("Parámetros de generación inválidos");
        }
        store.requireEmpty();
        var draft = new Store.Manifest(1, files, count, 0, "");
        Files.createDirectory(store.version(draft.version()));
        var schema = new DataFile.Schema("sintetico", new String[]{"producto", "detalle", "nota"});
        long deleted = 0;
        for (int file = 0; file < files; file++) {
            try (var out = new DataFile.Writer(store.data(draft, file), schema)) {
                for (long key = file + 1L; key <= count; ) {
                    boolean remove = (key % 100 * 37) % 100 < deletePercent;
                    if (remove) deleted++;
                    String name = "Artículo " + key;
                    String[] fields = key % 3 == 0
                            ? new String[]{name, "Talla " + key % 12, ""}
                            : new String[]{name, "Color azul"};
                    out.write(key, remove, fields);
                    if (count - key < files) break;
                    key += files;
                }
            }
        }
        return store.initialize(new Store.Manifest(1, files, count, deleted, ""));
    }

    private static final class Writers implements AutoCloseable {
        final DataFile.Writer[] parts;

        Writers(int count) {
            parts = new DataFile.Writer[count];
        }

        void open(int part, Path path, DataFile.Schema schema) throws IOException {
            parts[part] = new DataFile.Writer(path, schema);
        }

        @Override
        public void close() throws IOException {
            IOException failure = null;
            for (var part : parts) {
                if (part == null) continue;
                try {
                    part.close();
                } catch (IOException e) {
                    if (failure == null) failure = e;
                    else failure.addSuppressed(e);
                }
            }
            if (failure != null) throw failure;
        }
    }

    private Datasets() {}
}
