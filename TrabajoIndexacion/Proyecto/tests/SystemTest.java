import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;

public final class SystemTest {
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("lsdfms-test-");
        try {
            for (int percent : new int[]{0, 30, 100}) equivalence(root.resolve("p" + percent), 2000, 4, percent);
            equivalence(root.resolve("empty"), 0, 1, 0);
            equivalence(root.resolve("single"), 1, 1, 0);
            equivalence(root.resolve("runs"), 270_000, 2, 10);
            csv(root.resolve("muestra.csv"));
            limits(root.resolve("limite.dat"));
            faults(root.resolve("fallo"));
            if (args.length == 1 && args[0].equals("--offset-grande")) largeOffset(root.resolve("grande.dat"));
            System.out.println("Pruebas correctas");
        } finally {
            Store.removeTree(root);
        }
    }

    private static void equivalence(Path path, long count, int files, int percent) throws Exception {
        Path serial = path.resolve("serial");
        Path parallel = path.resolve("parallel");
        long before;
        long deleted;
        try (Store a = new Store(serial); Store b = new Store(parallel)) {
            var initial = Datasets.generate(a, count, files, percent);
            Datasets.generate(b, count, files, percent);
            var indexA = a.index(1);
            var indexB = b.index(4);
            check(Files.mismatch(a.index(indexA), b.index(indexB)) == -1, "INDEX serial/paralelo");
            check(a.search(0) == null && a.search(count + 1) == null, "claves inexistentes");
            before = a.dataBytes(initial);
            deleted = initial.deleted();
            if (count > 0 && percent == 0) {
                check(a.search(1).row().fields()[0].equals("Artículo 1"), "primera clave UTF-8");
                check(a.search(count).row().key == count, "última clave");
                check(a.delete(1) && b.delete(1), "borrado lógico");
                check(a.search(1) == null && !a.delete(1), "borrado idempotente");
                a.verify(a.current());
                deleted++;
            }
        }
        try (Store a = new Store(serial); Store b = new Store(parallel)) {
            var compactA = a.pack(1);
            var compactB = b.pack(4);
            check(compactA.total() == count - deleted && compactA.deleted() == 0, "conteo tras PACK");
            check(deleted == 0 ? a.dataBytes(compactA) == before : a.dataBytes(compactA) < before,
                    "espacio recuperado");
            check(Files.mismatch(a.index(compactA), b.index(compactB)) == -1, "índice de PACK serial/paralelo");
            for (int file = 0; file < files; file++) {
                check(Files.mismatch(a.data(compactA, file), b.data(compactB, file)) == -1,
                        "registros de PACK serial/paralelo");
                try (var rows = new DataFile.Reader(a.data(compactA, file))) {
                    while (rows.next()) {
                        check(rows.row.state == DataFile.ACTIVE, "PACK conservó una marca");
                        check(rows.row.fields()[0].equals("Artículo " + rows.row.key), "PACK alteró un valor");
                        check(rows.row.key != 1 || percent > 0, "PACK conservó el registro borrado");
                    }
                }
            }
            var again = a.pack(4);
            check(again.total() == compactA.total() && a.dataBytes(again) == b.dataBytes(compactB), "PACK repetido");
            if (percent == 100 || count == 0) check(a.search(1) == null, "índice vacío");
            if (count > 1 && percent == 0) {
                check(a.search(count).row().key == count && a.search(1) == null, "consulta tras PACK");
            }
        }
    }

    private static void csv(Path path) throws Exception {
        Files.writeString(path, "\uFEFFid,detalle,vacio\r\n1,\"línea,\r\ncomillas \"\"dobles\"\"\",\r\n",
                StandardCharsets.UTF_8);
        try (var reader = new CsvSource(path)) {
            check(reader.next()[0].equals("id"), "BOM");
            check(Arrays.equals(reader.next(), new String[]{"1", "línea,\r\ncomillas \"dobles\"", ""}), "CSV escapado");
            check(reader.next() == null, "EOF");
        }
        Files.writeString(path, "\"abierta");
        try (var reader = new CsvSource(path)) {
            expectFailure(reader::next);
        }
    }

    private static void limits(Path path) throws Exception {
        var schema = new DataFile.Schema("test", new String[]{"campo"});
        int size = DataFile.MAX_RECORD - DataFile.PREFIX - Integer.BYTES;
        try (var out = new DataFile.Writer(path, schema)) {
            out.write(1, false, new String[]{"a".repeat(size)});
            expectFailure(() -> out.write(2, false, new String[]{"a".repeat(size + 1)}));
        }
        try (var in = new DataFile.Reader(path)) {
            check(in.next() && in.row.fields()[0].length() == size && !in.next(), "límite de longitud");
        }
        long bytes = Files.size(path);
        try (var file = new RandomAccessFile(path.toFile(), "rw")) {
            file.setLength(bytes - 1);
        }
        try (var in = new DataFile.Reader(path)) {
            expectFailure(in::next);
        }
    }

    private static void faults(Path path) throws Exception {
        try (Store store = new Store(path)) {
            Datasets.generate(store, 24, 4, 0);
            Store.Manifest active = store.index(4);
            byte[] manifest = Files.readAllBytes(path.resolve("active.meta"));
            expectFailure(() -> {
                try (var ignored = new Store(path)) {
                    throw new AssertionError("Se adquirió un segundo bloqueo");
                }
            });
            var entry = DiskIndex.find(store.index(active), active.version(), 1);
            try (var journal = new DataOutputStream(Files.newOutputStream(path.resolve("delete.meta")))) {
                journal.writeInt(0x4C53444A);
                journal.writeLong(active.version());
                journal.writeLong(0);
                journal.writeInt(entry.file());
                journal.writeLong(entry.offset());
                journal.writeLong(entry.key());
            }
            Path part = store.data(active, 1);
            try (var records = new RandomAccessFile(part.toFile(), "rw")) {
                records.writeInt(0);
                expectFailure(() -> store.pack(4));
                check(Arrays.equals(manifest, Files.readAllBytes(path.resolve("active.meta"))),
                        "Un fallo de trabajador publicó una versión");
                check(store.search(1) != null, "Se perdió la versión anterior");
                records.seek(0);
                records.writeInt(DataFile.MAGIC);
            }
        }
        try (Store reopened = new Store(path)) {
            check(reopened.current().deleted() == 1 && reopened.search(1) == null, "Reanudación del borrado");
            check(!Files.exists(path.resolve("data-2")), "No se limpió una versión sin publicar");
            var active = reopened.current();
            Path pathToPart = reopened.data(active, 0);
            var duplicate = DiskIndex.find(reopened.index(active), active.version(), 5);
            try (var part = new RandomAccessFile(pathToPart.toFile(), "rw")) {
                part.seek(duplicate.offset() + Integer.BYTES + 1);
                part.writeLong(2);
                expectFailure(() -> reopened.index(4));
                check(reopened.current().index().equals(active.index()), "Un índice duplicado se publicó");
                part.seek(duplicate.offset() + Integer.BYTES + 1);
                part.writeLong(5);
            }
            Thread.currentThread().interrupt();
            try {
                expectFailure(() -> reopened.pack(4));
            } finally {
                Thread.interrupted();
            }
            check(reopened.current().version() == active.version(), "Se publicó una operación interrumpida");
            reopened.verify(active);
        }
    }

    private static void largeOffset(Path path) throws Exception {
        var schema = new DataFile.Schema("grande", new String[]{"detalle"});
        long offset = (1L << 31) + 4096;
        try (var out = new DataFile.Writer(path, schema)) {
            out.write(1, false, new String[]{"texto"});
        }
        try (var file = new RandomAccessFile(path.toFile(), "rw")) {
            DataFile.readSchema(file);
            byte[] row = new byte[(int) (file.length() - file.getFilePointer())];
            file.readFully(row);
            file.seek(offset);
            file.write(row);
            check(DataFile.readAt(file, path, offset, 1).row().fields()[0].equals("texto"), "seek mayor a 2 GiB");
        }
    }

    @FunctionalInterface
    private interface Action {
        void run() throws Exception;
    }

    private static void expectFailure(Action action) throws Exception {
        try {
            action.run();
        } catch (IOException expected) {
            return;
        }
        throw new AssertionError("Se esperaba un error de E/S");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
