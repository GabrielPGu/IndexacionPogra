import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class HmFlowTest {
    private static final String DELETED_ID = "0108775015";
    private static final String KEPT_ID = "0110065002";
    private static final long FULL_COUNT = 33_265_846L;

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Uso: HmFlowTest <carpeta DatasetHyM> <almacén nuevo>");
        }
        Path storage = Path.of(args[1]);
        Files.createDirectory(storage);
        Store.Manifest initial;
        try (var store = new Store(storage)) {
            initial = Datasets.importHAndM(store, Path.of(args[0]), 4, Long.MAX_VALUE);
            check(initial.total() == FULL_COUNT && initial.active() == FULL_COUNT && initial.fileCount() == 12,
                    "Conteo de importación completa");
            report("importar", store, initial);
        }
        long deletedKey;
        long keptKey;
        long removedBytes;
        long beforeBytes;
        long beforeIndexBytes;
        String[] keptFields;
        try (var store = new Store(storage)) {
            var indexed = store.index(4);
            var removed = store.searchArticle(DELETED_ID);
            var kept = store.searchArticle(KEPT_ID);
            check(removed != null && kept != null, "Consulta por ID de catálogo");
            deletedKey = removed.row().key;
            keptKey = kept.row().key;
            check(deletedKey == 1 && keptKey == 5, "Los artículos deben compartir partición");
            removedBytes = Integer.BYTES + removed.row().length;
            keptFields = kept.row().fields();
            check(keptFields[0].equals(KEPT_ID), "El índice secundario apunta al artículo esperado");
            beforeBytes = store.dataBytes(indexed);
            beforeIndexBytes = store.indexBytes(indexed);
            report("index", store, indexed);
        }
        try (var store = new Store(storage)) {
            check(store.delete(deletedKey), "Borrado lógico");
            var marked = store.current();
            check(marked.total() == initial.total() && marked.deleted() == 1 && store.dataBytes(marked) == beforeBytes,
                    "El borrado lógico alteró el tamaño o los conteos");
            check(store.search(deletedKey) == null && store.searchArticle(DELETED_ID) == null,
                    "El borrado debe ocultar ambas claves");
            report("eliminar", store, marked);
        }
        try (var store = new Store(storage)) {
            var packed = store.pack(4);
            check(packed.version() == initial.version() + 1 && packed.active() == initial.active() - 1
                    && packed.deleted() == 0, "Conteos de PACK");
            check(store.dataBytes(packed) == beforeBytes - removedBytes, "Bytes del registro retirado");
            check(store.indexBytes(packed) == beforeIndexBytes - 2L * DiskIndex.ENTRY, "Una entrada menos en cada índice");
            report("pack", store, packed);
        }
        try (var store = new Store(storage)) {
            var packed = store.current();
            store.verify(packed);
            check(store.search(deletedKey) == null && store.searchArticle(DELETED_ID) == null,
                    "El artículo eliminado reapareció al reabrir");
            var kept = store.searchArticle(KEPT_ID);
            check(kept != null && kept.row().key == keptKey && Arrays.equals(keptFields, kept.row().fields()),
                    "Cambió el artículo conservado");
            var primary = store.search(keptKey);
            check(primary != null && Arrays.equals(keptFields, primary.row().fields()),
                    "Los índices discrepan tras cambiar el offset");
            report("verificar", store, packed);
        }
        System.out.printf("H&M verificado: ID eliminado=%s, ID conservado=%s, recuperados=%d bytes, heap máximo=%d bytes%n",
                DELETED_ID, KEPT_ID, removedBytes, Runtime.getRuntime().maxMemory());
    }

    private static void report(String operation, Store store, Store.Manifest manifest) throws Exception {
        System.out.printf("%s version=%d archivos=%d total=%d activos=%d eliminados=%d datosBytes=%d indiceBytes=%d%n",
                operation, manifest.version(), manifest.fileCount(), manifest.total(), manifest.active(),
                manifest.deleted(), store.dataBytes(manifest), store.indexBytes(manifest));
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
