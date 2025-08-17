import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class LoggingThreadFactory implements ThreadFactory {
    private final String poolName;
    private final AtomicInteger count = new AtomicInteger(1);

    public LoggingThreadFactory(String poolName) {
        this.poolName = poolName;
    }

    @Override
    public Thread newThread(Runnable r) {
        String threadName = poolName + "-worker-" + count.getAndIncrement();
        System.out.println("[ThreadFactory] Creating new thread: " + threadName);
        return new Thread(r, threadName);
    }
}