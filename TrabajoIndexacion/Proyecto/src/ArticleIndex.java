import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.Arrays;

final class ArticleIndex {
    static final int MAGIC = 0x4C534149; // LSAI
    static final DiskIndex.KeySource KEY = new DiskIndex.KeySource() {
        @Override
        public int column(DataFile.Schema schema) {
            return schema.table().equals("articles") ? Arrays.asList(schema.columns()).indexOf("article_id") : -1;
        }

        @Override
        public long key(DataFile.Row row, int column) throws IOException {
            try {
                return parse(row.field(column));
            } catch (IllegalArgumentException e) {
                throw new IOException("article_id inválido en la clave interna " + row.key, e);
            }
        }
    };

    static long parse(String value) {
        if (value.length() != 10) throw invalidId();
        long key = 0;
        for (int i = 0; i < value.length(); i++) {
            int digit = value.charAt(i) - '0';
            if (digit < 0 || digit > 9) throw invalidId();
            key = key * 10 + digit;
        }
        return key;
    }

    private static IllegalArgumentException invalidId() {
        return new IllegalArgumentException("article_id debe tener 10 dígitos, incluidos los ceros iniciales");
    }

    static boolean present(Store store, Store.Manifest manifest) throws IOException {
        // El importador H&M empieza con las particiones de articles.
        try (var input = new DataFile.Reader(store.data(manifest, 0))) {
            if (!input.schema.table().equals("articles")) return false;
            if (KEY.column(input.schema) < 0) throw new IOException("Falta article_id en la tabla articles");
            return true;
        }
    }

    static DataFile.Found search(Store store, Store.Manifest manifest, long id) throws IOException {
        var entry = DiskIndex.find(store.articleIndex(manifest), manifest.version(), id, MAGIC);
        if (entry == null) return null;
        Path path = store.data(manifest, entry.file());
        try (var in = new RandomAccessFile(path.toFile(), "r")) {
            var found = DataFile.readAt(in, path, entry.offset());
            if (KEY.key(found.row(), KEY.column(found.schema())) != id) {
                throw new IOException("Referencia incorrecta para article_id " + id);
            }
            return found.row().state == DataFile.ACTIVE ? found : null;
        }
    }

    static void verify(Store store, Store.Manifest manifest, long expectedActive) throws IOException {
        try (var entries = new DiskIndex.Cursor(store.articleIndex(manifest), DiskIndex.HEADER);
             var files = new FilesById(manifest.fileCount())) {
            DiskIndex.header(entries.in, entries.remaining, manifest.version(), MAGIC);
            long active = 0;
            long previous = -1;
            var row = new DataFile.Row();
            while (entries.next()) {
                if (entries.key <= previous) throw new IOException("article_id duplicado o índice desordenado: " + entries.key);
                previous = entries.key;
                Path path = store.data(manifest, entries.file);
                ArticleFile file = files.get(entries.file, path);
                DataFile.readAt(file.in, path, entries.offset, file.first, file.size, row);
                if (KEY.key(row, file.column) != entries.key) throw row.invalid(path, "article_id incorrecto en el índice");
                if (row.state == DataFile.ACTIVE) active++;
            }
            if (active != expectedActive) throw new IOException("El índice secundario no cubre todos los artículos activos");
        }
    }

    private record ArticleFile(RandomAccessFile in, long first, long size, int column) {}

    private static final class FilesById implements Closeable {
        private final ArticleFile[] files;

        FilesById(int count) {
            files = new ArticleFile[count];
        }

        ArticleFile get(int id, Path path) throws IOException {
            if (files[id] == null) {
                var in = new RandomAccessFile(path.toFile(), "r");
                try {
                    int column = KEY.column(DataFile.readSchema(in));
                    if (column < 0) throw new IOException("El índice de artículos apunta a " + path);
                    files[id] = new ArticleFile(in, in.getFilePointer(), in.length(), column);
                } catch (IOException e) {
                    in.close();
                    throw e;
                }
            }
            return files[id];
        }

        @Override
        public void close() throws IOException {
            IOException failure = null;
            for (ArticleFile file : files) {
                if (file == null) continue;
                try {
                    file.in.close();
                } catch (IOException e) {
                    if (failure == null) failure = e;
                    else failure.addSuppressed(e);
                }
            }
            if (failure != null) throw failure;
        }
    }

    private ArticleIndex() {}
}
