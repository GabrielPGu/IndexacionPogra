import java.io.IOException;
import java.nio.file.Path;

public final class Main {
    public static void main(String[] args) {
        try {
            execute(args);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void execute(String[] args) throws IOException {
        if (args.length == 0 || args[0].equals("ayuda")) {
            System.out.println("""
                LSDFMS
                  importar <almacen> <carpeta-csv> <particiones-por-tabla> [limite-por-tabla]
                  generar <almacen> <registros> <archivos> <porcentaje-eliminado>
                  index|pack <almacen> <hilos>
                  buscar|eliminar <almacen> <clave>
                  verificar <almacen>
                  benchmark <csv-resultados> <registros> <archivos> <porcentaje> <repeticiones> <hilos>...
                """);
            return;
        }
        String operation = args[0];
        switch (operation) {
            case "importar" -> require(args, 4, 5);
            case "generar" -> require(args, 5, 5);
            case "index", "pack", "buscar", "eliminar" -> require(args, 3, 3);
            case "verificar" -> require(args, 2, 2);
            case "benchmark" -> {
                require(args, 7, 6 + Parallel.MAX_THREADS - 1);
                Benchmark.run(args);
                return;
            }
            default -> throw new IllegalArgumentException("Operación desconocida. Usa ayuda.");
        }
        long start = System.nanoTime();
        try (Store store = new Store(Path.of(args[1]))) {
            Store.Manifest manifest;
            int threads = 1;
            switch (operation) {
                case "importar" -> manifest = Datasets.importHAndM(store, Path.of(args[2]),
                        Integer.parseInt(args[3]), args.length == 5 ? Long.parseLong(args[4]) : Long.MAX_VALUE);
                case "generar" -> manifest = Datasets.generate(store, Long.parseLong(args[2]),
                        Integer.parseInt(args[3]), Integer.parseInt(args[4]));
                case "index", "pack" -> {
                    threads = Integer.parseInt(args[2]);
                    manifest = operation.equals("index") ? store.index(threads) : store.pack(threads);
                }
                case "buscar" -> {
                    long key = Long.parseLong(args[2]);
                    DataFile.Found found = store.search(key);
                    if (found == null) {
                        System.out.println("Clave ausente: " + key);
                        return;
                    }
                    System.out.println(found.schema().table() + " clave=" + key);
                    String[] fields = found.row().fields();
                    String[] columns = found.schema().columns();
                    for (int i = 0; i < fields.length; i++) {
                        String label = i < columns.length ? columns[i] : "campo " + (i + 1);
                        System.out.println(label + " = " + fields[i]);
                    }
                    return;
                }
                case "eliminar" -> {
                    boolean marked = store.delete(Long.parseLong(args[2]));
                    System.out.println(marked ? "Registro marcado" : "Clave ausente");
                    manifest = store.current();
                }
                case "verificar" -> {
                    manifest = store.current();
                    store.verify(manifest);
                }
                default -> throw new IllegalStateException(operation);
            }
            System.out.printf(java.util.Locale.ROOT,
                    "%s versión=%d archivos=%d hilos=%d total=%d activos=%d eliminados=%d datosBytes=%d índiceBytes=%d ms=%.3f%n",
                    operation, manifest.version(), manifest.fileCount(), Math.min(threads, manifest.fileCount()),
                    manifest.total(), manifest.active(), manifest.deleted(), store.dataBytes(manifest),
                    store.indexBytes(manifest), (System.nanoTime() - start) / 1_000_000.0);
        }
    }

    private static void require(String[] args, int min, int max) {
        if (args.length < min || args.length > max) throw new IllegalArgumentException("Argumentos inválidos. Usa ayuda.");
    }
}
