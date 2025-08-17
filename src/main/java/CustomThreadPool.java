import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CustomThreadPool implements CustomExecutor {
    private final int corePoolSize;
    private final int maxPoolSize;
    private final long keepAliveTime;
    private final TimeUnit timeUnit;
    private final int minSpareThreads;

    private final TaskQueueManager queueManager;
    private final ThreadFactory threadFactory;
    private final Set<Worker> workers = ConcurrentHashMap.newKeySet();
    private final AtomicInteger activeThreads = new AtomicInteger(0);
    private volatile boolean isShutdown = false;

    public CustomThreadPool(int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit timeUnit,
                            int queueSize, int minSpareThreads, int numberOfQueues) {
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.minSpareThreads = minSpareThreads;
        this.threadFactory = new LoggingThreadFactory("MyPool");
        this.queueManager = new TaskQueueManager(numberOfQueues, queueSize);

        // Start initial core pool workers
        for (BlockingQueue<Runnable> queue : queueManager.getAllQueues()) {
            addWorker(queue);
        }
    }

    @Override
    public void execute(Runnable command) {
        if (isShutdown) {
            reject(command);
            return;
        }

        BlockingQueue<Runnable> queue = queueManager.getNextQueue();

        if (!queue.offer(command)) {
            if (activeThreads.get() < maxPoolSize) {
                addWorker(queue);
                if (!queue.offer(command)) {
                    reject(command);
                } else {
                    System.out.println("[Pool] Task accepted into queue: " + command);
                }
            } else {
                reject(command);
            }
        } else {
            System.out.println("[Pool] Task accepted into queue: " + command);
            maintainMinSpareThreads();
        }
    }

    private void reject(Runnable command) {
        System.out.println("[Rejected] Task " + command + " was rejected due to overload!");
    }

    private void addWorker(BlockingQueue<Runnable> queue) {
        Worker worker = new Worker(this, queue, keepAliveTime, timeUnit);
        workers.add(worker);
        activeThreads.incrementAndGet();
        Thread t = threadFactory.newThread(worker);
        t.start();
    }

    public void removeWorker(Worker worker) {
        workers.remove(worker);
        activeThreads.decrementAndGet();
    }

    public boolean shouldTerminate(Worker worker) {
        return activeThreads.get() > corePoolSize;
    }

    private void maintainMinSpareThreads() {
        long idle = workers.stream().filter(t -> t.getState() == Thread.State.WAITING).count();
        if (idle < minSpareThreads && activeThreads.get() < maxPoolSize) {
            for (BlockingQueue<Runnable> q : queueManager.getAllQueues()) {
                addWorker(q);
                break;
            }
        }
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        FutureTask<T> ft = new FutureTask<>(task);
        execute(ft);
        return ft;
    }

    @Override
    public void shutdown() {
        isShutdown = true;
        System.out.println("[Pool] Shutdown initiated.");
    }

    @Override
    public void shutdownNow() {
        isShutdown = true;
        for (Worker w : workers) {
            w.interrupt();
        }
        System.out.println("[Pool] Immediate shutdown initiated.");
    }

    public boolean isShutdown() {
        return isShutdown;
    }
}