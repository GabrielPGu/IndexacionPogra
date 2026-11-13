import java.io.IOException;
import java.io.InterruptedIOException;

final class Parallel {
    static final int MAX_THREADS = 8;

    @FunctionalInterface
    interface Task {
        void run(int file) throws IOException;
    }

    static void run(int files, int threads, Task task) throws IOException {
        if (threads < 1 || threads > MAX_THREADS) throw new IllegalArgumentException("Hilos fuera de rango");
        int count = Math.min(files, threads);
        if (count <= 1) {
            for (int file = 0; file < files; file++) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Operación interrumpida");
                task.run(file);
            }
            return;
        }

        Thread[] workers = new Thread[count];
        Throwable[] errors = new Throwable[count];
        int started = 0;
        boolean interrupted = false;
        try {
            for (int i = 0; i < count; i++) {
                int worker = i;
                workers[i] = new Thread(() -> {
                    try {
                        for (int file = worker; file < files; file += count) task.run(file);
                    } catch (Throwable e) {
                        errors[worker] = e;
                    }
                }, "lsdfms-" + i);
                workers[i].start();
                started++;
            }
        } finally {
            for (int i = 0; i < started; i++) {
                while (true) {
                    try {
                        workers[i].join();
                        break;
                    } catch (InterruptedException e) {
                        // Los trabajadores aún podrían tener temporales abiertos.
                        interrupted = true;
                    }
                }
            }
            if (interrupted) Thread.currentThread().interrupt();
        }
        if (interrupted) throw new InterruptedIOException("Operación interrumpida");
        for (Throwable error : errors) {
            if (error != null) throw new IOException("Falló un trabajador: " + error.getMessage(), error);
        }
    }

    private Parallel() {}
}
