import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

final class Store implements Closeable {
    static final int MAX_FILES = 64;
    private static final int MANIFEST_MAGIC = 0x4C534D46; // LSMF
    private static final int DELETE_MAGIC = 0x4C53444A; // LSDJ
    private static final int MANIFEST_VERSION = 2;
    private final Path root;
    private final FileChannel lockChannel;
    private final FileLock lock;
    private final PackCheckpoint checkpoint;
    private long tempBytes;

    record Manifest(long version, int fileCount, long total, long deleted, String index, boolean articles) {
        Manifest(long version, int fileCount, long total, long deleted, String index) {
            this(version, fileCount, total, deleted, index, false);
        }

        long active() { return total - deleted; }

        Manifest indexed(String name, boolean withArticles) {
            return new Manifest(version, fileCount, total, deleted, name, withArticles);
        }
    }

    enum PackStage { DATA_WRITTEN, BEFORE_PUBLISH, AFTER_PUBLISH }

    @FunctionalInterface
    interface PackCheckpoint {
        void reached(PackStage stage);
    }

    Store(Path root) throws IOException {
        this(root, stage -> {});
    }

    Store(Path root, PackCheckpoint checkpoint) throws IOException {
        this.checkpoint = checkpoint;
        this.root = Files.createDirectories(root);
        lockChannel = FileChannel.open(root.resolve("store.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        try {
            lock = lockChannel.tryLock();
            if (lock == null) throw new IOException("Almacén en uso: " + root);
            recoverDelete();
            cleanup();
        } catch (OverlappingFileLockException e) {
            lockChannel.close();
            throw new IOException("Almacén en uso: " + root, e);
        } catch (IOException | RuntimeException e) {
            lockChannel.close();
            throw e;
        }
    }

    Manifest current() throws IOException {
        Path path = root.resolve("active.meta");
        if (!Files.isRegularFile(path)) throw new IOException("Almacén sin importar o generar: " + root);
        try (var in = new DataInputStream(Files.newInputStream(path))) {
            int magic = in.readInt();
            int format = in.readInt();
            if (magic != MANIFEST_MAGIC || (format != 1 && format != MANIFEST_VERSION)) {
                throw new IOException("Manifiesto inválido: " + path);
            }
            Manifest manifest = new Manifest(in.readLong(), in.readInt(), in.readLong(), in.readLong(),
                    in.readUTF(), format == MANIFEST_VERSION && in.readBoolean());
            if (manifest.version <= 0 || manifest.fileCount < 1 || manifest.fileCount > MAX_FILES
                    || manifest.total < 0 || manifest.deleted < 0 || manifest.deleted > manifest.total
                    || (!manifest.index.isEmpty() && !manifest.index.matches("index-[0-9a-f-]{36}\\.idx"))
                    || (manifest.articles && manifest.index.isEmpty())
                    || in.read() != -1) {
                throw new IOException("Metadatos inválidos: " + path);
            }
            for (int file = 0; file < manifest.fileCount; file++) {
                if (!Files.isRegularFile(data(manifest, file))) throw new IOException("Falta " + data(manifest, file));
            }
            if (!manifest.index.isEmpty() && !Files.isRegularFile(index(manifest))) {
                throw new IOException("Falta el índice publicado");
            }
            if (manifest.articles && !Files.isRegularFile(articleIndex(manifest))) {
                throw new IOException("Falta el índice publicado de artículos");
            }
            return manifest;
        }
    }

    void requireEmpty() throws IOException {
        if (Files.exists(root.resolve("active.meta"))) throw new IOException("El almacén ya contiene datos: " + root);
    }

    Path version(long version) {
        return root.resolve("data-" + version);
    }

    Path data(Manifest manifest, int file) throws IOException {
        if (file < 0 || file >= manifest.fileCount) throw new IOException("ID de archivo inválido: " + file);
        return version(manifest.version).resolve("part-" + file + ".dat");
    }

    Path index(Manifest manifest) throws IOException {
        if (manifest.index.isEmpty()) throw new IOException("Ejecuta index para construir el índice");
        return root.resolve(manifest.index);
    }

    Path articleIndex(Manifest manifest) {
        return root.resolve(manifest.index.replace(".idx", ".articles.idx"));
    }

    Path work() throws IOException {
        return Files.createTempDirectory(root, ".work-");
    }

    String newIndex() {
        return "index-" + UUID.randomUUID() + ".idx";
    }

    private void publish(Manifest manifest) throws IOException {
        writeAtomic("active.meta", out -> writeManifest(out, manifest));
    }

    private static void writeManifest(DataOutputStream out, Manifest manifest) throws IOException {
        out.writeInt(MANIFEST_MAGIC);
        out.writeInt(MANIFEST_VERSION);
        out.writeLong(manifest.version);
        out.writeInt(manifest.fileCount);
        out.writeLong(manifest.total);
        out.writeLong(manifest.deleted);
        out.writeUTF(manifest.index);
        out.writeBoolean(manifest.articles);
    }

    private interface Encode {
        void write(DataOutputStream out) throws IOException;
    }

    private void writeAtomic(String name, Encode encode) throws IOException {
        writeAtomic(name, encode, () -> {});
    }

    private void writeAtomic(String name, Encode encode, Runnable beforeMove) throws IOException {
        Path temp = Files.createTempFile(root, ".meta-", ".tmp");
        try {
            try (var file = new FileOutputStream(temp.toFile()); var out = new DataOutputStream(file)) {
                encode.write(out);
                out.flush();
                file.getFD().sync();
            }
            beforeMove.run();
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Publicación interrumpida");
            Files.move(temp, root.resolve(name), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            throw new IOException("El almacén requiere reemplazo atómico del manifiesto", e);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    boolean delete(long key) throws IOException {
        Manifest manifest = current();
        DiskIndex.Entry entry = DiskIndex.find(index(manifest), manifest.version, key);
        if (entry == null || read(manifest, entry).row().state == DataFile.DELETED) return false;
        writeAtomic("delete.meta", out -> {
            out.writeInt(DELETE_MAGIC);
            out.writeLong(manifest.version);
            out.writeLong(manifest.deleted);
            out.writeInt(entry.file());
            out.writeLong(entry.offset());
            out.writeLong(key);
        });
        recoverDelete();
        return true;
    }

    private void recoverDelete() throws IOException {
        Path journal = root.resolve("delete.meta");
        if (!Files.exists(journal)) return;
        Manifest manifest = current();
        try (var in = new DataInputStream(Files.newInputStream(journal))) {
            if (in.readInt() != DELETE_MAGIC || in.readLong() != manifest.version) {
                throw new IOException("Diario de borrado inválido");
            }
            long before = in.readLong();
            int file = in.readInt();
            long offset = in.readLong();
            long key = in.readLong();
            if (before < 0 || before >= manifest.total || in.read() != -1
                    || (manifest.deleted != before && manifest.deleted != before + 1)) {
                throw new IOException("Diario de borrado inconsistente");
            }
            Path path = data(manifest, file);
            try (var records = new RandomAccessFile(path.toFile(), "rw")) {
                DataFile.Found found = DataFile.readAt(records, path, offset, key);
                if (manifest.deleted == before + 1 && found.row().state != DataFile.DELETED) {
                    throw new IOException("Estado de borrado inconsistente");
                }
                records.seek(offset + Integer.BYTES);
                records.writeByte(DataFile.DELETED);
                records.getFD().sync();
            }
            if (manifest.deleted == before) {
                publish(new Manifest(manifest.version, manifest.fileCount, manifest.total,
                        before + 1, manifest.index, manifest.articles));
            }
        }
        Files.delete(journal);
    }

    DataFile.Found search(long key) throws IOException {
        Manifest manifest = current();
        DiskIndex.Entry entry = DiskIndex.find(index(manifest), manifest.version, key);
        if (entry == null) return null;
        DataFile.Found found = read(manifest, entry);
        return found.row().state == DataFile.ACTIVE ? found : null;
    }

    DataFile.Found searchArticle(String id) throws IOException {
        long key = ArticleIndex.parse(id);
        Manifest manifest = current();
        if (!manifest.articles) throw new IOException("Ejecuta index sobre un almacén H&M para buscar por article_id");
        return ArticleIndex.search(this, manifest, key);
    }

    private DataFile.Found read(Manifest manifest, DiskIndex.Entry entry) throws IOException {
        Path path = data(manifest, entry.file());
        try (var records = new RandomAccessFile(path.toFile(), "r")) {
            return DataFile.readAt(records, path, entry.offset(), entry.key());
        }
    }

    Manifest initialize(Manifest manifest) throws IOException {
        verify(manifest);
        publish(manifest);
        return manifest;
    }

    Manifest index(int threads) throws IOException {
        Manifest previous = current();
        Manifest next = previous.indexed(newIndex(), ArticleIndex.present(this, previous));
        try {
            buildIndex(next, threads);
            publish(next);
        } catch (IOException | RuntimeException e) {
            cleanupAfterFailure(e);
            throw e;
        }
        cleanup();
        return next;
    }

    Manifest pack(int threads) throws IOException {
        Manifest previous = current();
        Manifest next = new Manifest(previous.version + 1, previous.fileCount, previous.active(), 0,
                newIndex(), ArticleIndex.present(this, previous));
        try {
            copyActive(previous, next, threads);
            checkpoint.reached(PackStage.DATA_WRITTEN);
            buildIndex(next, threads);
            publishPack(next);
        } catch (IOException | RuntimeException e) {
            cleanupAfterFailure(e);
            throw e;
        }
        checkpoint.reached(PackStage.AFTER_PUBLISH);
        cleanup();
        return next;
    }

    private void copyActive(Manifest previous, Manifest next, int threads) throws IOException {
        // Si una limpieza anterior quedó pendiente, esta versión sigue sin publicarse.
        removeTree(version(next.version));
        Files.createDirectory(version(next.version));
        long[] copied = new long[next.fileCount];
        long[] discarded = new long[next.fileCount];
        Parallel.run(next.fileCount, threads, file -> {
            try (var source = new DataFile.Reader(data(previous, file));
                 var target = new DataFile.Writer(data(next, file), source.schema)) {
                while (source.next()) {
                    if (source.row.state == DataFile.ACTIVE) {
                        target.copy(source.row);
                        copied[file]++;
                    } else {
                        discarded[file]++;
                    }
                }
            }
        });
        long active = 0;
        long deleted = 0;
        for (int file = 0; file < next.fileCount; file++) {
            active += copied[file];
            deleted += discarded[file];
        }
        if (active != previous.active() || deleted != previous.deleted) {
            throw new IOException("Los conteos de PACK no coinciden con el manifiesto");
        }
    }

    private void publishPack(Manifest manifest) throws IOException {
        writeAtomic("active.meta", out -> writeManifest(out, manifest),
                () -> checkpoint.reached(PackStage.BEFORE_PUBLISH));
    }

    private void cleanupAfterFailure(Exception failure) {
        // Parallel.run espera a todos los trabajadores. El manifiesto decide qué archivos conservar.
        try {
            cleanup();
        } catch (IOException e) {
            failure.addSuppressed(e);
        }
    }

    private void buildIndex(Manifest next, int threads) throws IOException {
        Path work = work();
        try {
            long entries = DiskIndex.build(this, next, work, index(next), threads);
            tempBytes = Files.size(work.resolve("sorted"));
            if (entries != next.active()) throw new IOException("Conteo de entradas incorrecto");
            if (next.articles) {
                Path articleWork = Files.createDirectory(work.resolve("articles"));
                DiskIndex.build(this, next, articleWork, articleIndex(next), threads,
                        ArticleIndex.MAGIC, ArticleIndex.KEY);
                tempBytes += Files.size(articleWork.resolve("sorted"));
            }
            verify(next);
        } finally {
            removeTree(work);
        }
    }

    void verify(Manifest manifest) throws IOException {
        if (manifest.index.isEmpty()) {
            verifyRows(manifest, null);
        } else {
            try (var entries = new DiskIndex.Cursor(index(manifest), DiskIndex.HEADER)) {
                DiskIndex.header(entries.in, entries.remaining, manifest.version);
                long articles = verifyRows(manifest, entries);
                if (manifest.articles) ArticleIndex.verify(this, manifest, articles);
            }
        }
    }

    // Los importadores asignan claves crecientes dentro de cada archivo.
    // La validación cruza el índice con todos los registros en una pasada, sin búsquedas por registro.
    private long verifyRows(Manifest manifest, DiskIndex.Cursor entries) throws IOException {
        long[] counts = new long[3];
        try (var rows = new Readers(manifest.fileCount)) {
            for (int file = 0; file < manifest.fileCount; file++) {
                rows.open(file, data(manifest, file));
            }
            if (entries != null) {
                long previous = 0;
                boolean first = true;
                while (entries.next()) {
                    if (!first && entries.key <= previous) throw new IOException("Clave duplicada o índice desordenado: " + entries.key);
                    first = false;
                    previous = entries.key;
                    if (entries.file < 0 || entries.file >= manifest.fileCount) throw new IOException("Archivo inválido en el índice");
                    DataFile.Reader reader = rows.files[entries.file];
                    boolean match = false;
                    while (advance(rows, entries.file, counts)) {
                        if (reader.row.offset == entries.offset && reader.row.key == entries.key) {
                            match = true;
                            break;
                        }
                        if (reader.row.state == DataFile.ACTIVE || reader.row.offset >= entries.offset) break;
                    }
                    if (!match) throw new IOException("Índice inconsistente en clave " + entries.key);
                }
            }
            for (int file = 0; file < manifest.fileCount; file++) {
                while (advance(rows, file, counts)) {
                    if (entries != null && rows.files[file].row.state == DataFile.ACTIVE) {
                        throw new IOException("Falta una clave activa en el índice: " + rows.files[file].row.key);
                    }
                }
            }
        }
        if (counts[0] != manifest.total || counts[1] != manifest.deleted) {
            throw new IOException("Conteos de datos distintos del manifiesto");
        }
        return counts[2];
    }

    private static boolean advance(Readers rows, int file, long[] counts) throws IOException {
        DataFile.Reader reader = rows.files[file];
        if (!reader.next()) return false;
        if (rows.seen[file] && reader.row.key <= rows.last[file]) {
            throw new IOException("Claves repetidas o desordenadas en archivo " + file);
        }
        rows.seen[file] = true;
        rows.last[file] = reader.row.key;
        counts[0]++;
        if (reader.row.state == DataFile.DELETED) counts[1]++;
        else if (rows.articleFile[file]) counts[2]++;
        return true;
    }

    private static final class Readers implements Closeable {
        final DataFile.Reader[] files;
        // verifyRows consume cada fila antes de avanzar cualquier lector.
        private final DataFile.Row sharedRow = new DataFile.Row();
        final long[] last;
        final boolean[] seen;
        final boolean[] articleFile;

        Readers(int count) {
            files = new DataFile.Reader[count];
            last = new long[count];
            seen = new boolean[count];
            articleFile = new boolean[count];
        }

        void open(int file, Path path) throws IOException {
            files[file] = new DataFile.Reader(path, sharedRow);
            articleFile[file] = ArticleIndex.KEY.column(files[file].schema) >= 0;
        }

        @Override
        public void close() throws IOException {
            IOException failure = null;
            for (DataFile.Reader reader : files) {
                if (reader == null) continue;
                try {
                    reader.close();
                } catch (IOException e) {
                    if (failure == null) failure = e;
                    else failure.addSuppressed(e);
                }
            }
            if (failure != null) throw failure;
        }
    }

    long dataBytes(Manifest manifest) throws IOException {
        long bytes = 0;
        for (int file = 0; file < manifest.fileCount; file++) bytes += Files.size(data(manifest, file));
        return bytes;
    }

    long indexBytes(Manifest manifest) throws IOException {
        if (manifest.index.isEmpty()) return 0;
        return Files.size(index(manifest)) + (manifest.articles ? Files.size(articleIndex(manifest)) : 0);
    }

    long tempBytes() {
        return tempBytes;
    }

    private void cleanup() throws IOException {
        Manifest active = Files.exists(root.resolve("active.meta")) ? current() : null;
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(root)) {
            for (Path path : paths) {
                String name = path.getFileName().toString();
                if (name.startsWith(".work-") || name.matches("\\.meta-\\d+\\.tmp")
                        || (name.matches("data-\\d+") && (active == null || !path.equals(version(active.version))))
                        || (name.matches("index-[0-9a-f-]{36}(\\.articles)?\\.idx") && !isActiveIndex(active, path))) {
                    try {
                        removeTree(path);
                    } catch (IOException e) {
                        System.err.println("Limpieza pendiente: " + path + " (" + e.getMessage() + ")");
                    }
                }
            }
        }
    }

    private boolean isActiveIndex(Manifest active, Path path) {
        return active != null && (path.getFileName().toString().equals(active.index)
                || (active.articles && path.equals(articleIndex(active))));
    }

    static void removeTree(Path path) throws IOException {
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return;
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path directory, IOException error) throws IOException {
                if (error != null) throw error;
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    @Override
    public void close() throws IOException {
        try (lockChannel; lock) {
            // El bloqueo dura toda la operación, incluidas las lecturas.
        }
    }
}
