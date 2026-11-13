import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Locale;

final class Benchmark {
    private static final String[] OPERATIONS = {"index", "pack"};

    static void run(String[] args) throws IOException {
        Path csv = Path.of(args[1]).toAbsolutePath();
        long records = Long.parseLong(args[2]);
        int files = Integer.parseInt(args[3]);
        int deletedPercent = Integer.parseInt(args[4]);
        int repetitions = Integer.parseInt(args[5]);
        int[] threads = new int[args.length - 5];
        threads[0] = 1;
        for (int i = 1; i < threads.length; i++) {
            threads[i] = Integer.parseInt(args[i + 5]);
            if (threads[i] < 2 || threads[i] > Math.min(files, Parallel.MAX_THREADS)) {
                throw new IllegalArgumentException("La configuración paralela excede los hilos o archivos disponibles");
            }
            for (int j = 0; j < i; j++) {
                if (threads[j] == threads[i]) throw new IllegalArgumentException("Configuración de hilos repetida");
            }
        }
        if (repetitions < 3 || repetitions > 30) throw new IllegalArgumentException("Usa entre 3 y 30 repeticiones");
        Files.createDirectories(csv.getParent());
        Path work = Files.createTempDirectory(csv.getParent(), "benchmark-");
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(csv, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))) {
            out.println("operacion,tipo,hilos,archivos,registros,porcentaje_objetivo,eliminados_iniciales,"
                    + "repeticion,ms,min_ms,max_ms,speedup,eficiencia,registros_por_segundo,datos_antes_bytes,"
                    + "datos_despues_bytes,indice_antes_bytes,indice_despues_bytes,recuperados_bytes,recuperados_pct,"
                    + "corrida_temporal_final_bytes,heap_limite_bytes,heap_pico_bytes");
            Path source = work.resolve("base");
            Store.Manifest initial;
            try (Store store = new Store(source)) {
                initial = Datasets.generate(store, records, files, deletedPercent);
            }
            for (String operation : OPERATIONS) {
                var samples = new Sample[threads.length][repetitions];
                // Primera ronda de calentamiento; el orden de las rondas medidas rota.
                for (int trial = -1; trial < repetitions; trial++) {
                    for (int i = 0; i < threads.length; i++) {
                        int mode = (i + Math.max(0, trial)) % threads.length;
                        Sample sample = measure(source, work.resolve("medicion"), initial, operation, threads[mode]);
                        if (trial >= 0) samples[mode][trial] = sample;
                    }
                }
                double serial = median(samples[0]);
                for (int mode = 0; mode < threads.length; mode++) {
                    Sample[] runs = samples[mode];
                    for (int i = 0; i < repetitions; i++) {
                        write(out, operation, "medicion", i + 1, threads[mode], initial, deletedPercent,
                                runs[i], Double.NaN, Double.NaN, Double.NaN);
                    }
                    double median = median(runs);
                    double min = Arrays.stream(runs).mapToDouble(Sample::ms).min().orElseThrow();
                    double max = Arrays.stream(runs).mapToDouble(Sample::ms).max().orElseThrow();
                    long peak = Arrays.stream(runs).mapToLong(Sample::heapPeak).max().orElseThrow();
                    Sample first = runs[0];
                    write(out, operation, "mediana", 0, threads[mode], initial, deletedPercent,
                            new Sample(median, first.dataBefore, first.dataAfter, first.indexBefore,
                                    first.indexAfter, first.tempBytes, peak), min, max, serial / median);
                }
                out.flush();
                if (out.checkError()) throw new IOException("No se pudo escribir " + csv);
            }
        } finally {
            Store.removeTree(work);
        }
        System.out.println("Resultados: " + csv);
    }

    private record Sample(double ms, long dataBefore, long dataAfter, long indexBefore, long indexAfter,
                          long tempBytes, long heapPeak) {}

    private static Sample measure(Path source, Path path, Store.Manifest initial, String operation, int threads)
            throws IOException {
        try {
            Files.createDirectory(path);
            Files.createDirectory(path.resolve("data-1"));
            Files.copy(source.resolve("active.meta"), path.resolve("active.meta"));
            for (int file = 0; file < initial.fileCount(); file++) {
                Path part = Path.of("data-1", "part-" + file + ".dat");
                Files.copy(source.resolve(part), path.resolve(part));
            }
            try (Store store = new Store(path)) {
                long dataBefore = store.dataBytes(initial);
                long indexBefore = store.indexBytes(initial);
                var pools = ManagementFactory.getMemoryPoolMXBeans().stream()
                        .filter(pool -> pool.getType() == java.lang.management.MemoryType.HEAP).toList();
                for (var pool : pools) pool.resetPeakUsage();
                long start = System.nanoTime();
                Store.Manifest next = operation.equals("index") ? store.index(threads) : store.pack(threads);
                double ms = (System.nanoTime() - start) / 1_000_000.0;
                long peak = pools.stream().mapToLong(pool -> pool.getPeakUsage().getUsed()).sum();
                return new Sample(ms, dataBefore, store.dataBytes(next), indexBefore,
                        store.indexBytes(next), store.tempBytes(), peak);
            }
        } finally {
            Store.removeTree(path);
        }
    }

    private static double median(Sample[] samples) {
        double[] times = Arrays.stream(samples).mapToDouble(Sample::ms).sorted().toArray();
        return (times[(times.length - 1) / 2] + times[times.length / 2]) / 2;
    }

    private static void write(PrintWriter out, String operation, String type, int trial, int threads,
                              Store.Manifest initial, int percent, Sample sample, double min, double max, double speedup) {
        long recovered = sample.dataBefore - sample.dataAfter;
        out.printf(Locale.ROOT, "%s,%s,%d,%d,%d,%d,%d,%d,%.3f,%s,%s,%s,%s,%.2f,%d,%d,%d,%d,%d,%.4f,%d,%d,%d%n",
                operation, type, threads, initial.fileCount(), initial.total(), percent, initial.deleted(),
                trial, sample.ms, optional(min), optional(max), optional(speedup), optional(speedup / threads),
                initial.total() * 1000.0 / sample.ms, sample.dataBefore, sample.dataAfter,
                sample.indexBefore, sample.indexAfter, recovered, recovered * 100.0 / sample.dataBefore,
                sample.tempBytes, Runtime.getRuntime().maxMemory(), sample.heapPeak);
    }

    private static String optional(double value) {
        return Double.isNaN(value) ? "" : String.format(Locale.ROOT, "%.3f", value);
    }

    private Benchmark() {}
}
