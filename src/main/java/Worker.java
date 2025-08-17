import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

class Worker implements Runnable {
    private final BlockingQueue<Runnable> queue;
    private final CustomThreadPool pool;
    private final long keepAliveTime;
    private final TimeUnit unit;
    private Thread thread;

    public Worker(CustomThreadPool pool, BlockingQueue<Runnable> queue, long keepAliveTime, TimeUnit unit) {
        this.queue = queue;
        this.pool = pool;
        this.keepAliveTime = keepAliveTime;
        this.unit = unit;
    }

    public void interrupt() {
        if (thread != null) {
            thread.interrupt();
        }
    }

    public Thread.State getState() {
        return thread != null ? thread.getState() : Thread.State.TERMINATED;
    }

    @Override
    public void run() {
        thread = Thread.currentThread();  // Store the current thread
        try {
            while (!pool.isShutdown()) {
                Runnable task = queue.poll(keepAliveTime, unit);
                if (task != null) {
                    System.out.println("[Worker] " + thread.getName() + " executes " + task);
                    try {
                        task.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    if (pool.shouldTerminate(this)) {
                        System.out.println("[Worker] " + thread.getName() + " idle timeout, stopping.");
                        break;
                    }
                }
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();  // Restore interrupt flag
        } finally {
            System.out.println("[Worker] " + thread.getName() + " terminated.");
            pool.removeWorker(this);
        }
    }
}