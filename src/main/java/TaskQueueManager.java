import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskQueueManager {
    private final List<BlockingQueue<Runnable>> queues;
    private final int numberOfQueues;
    private final AtomicInteger nextIndex = new AtomicInteger(0);

    public TaskQueueManager(int numberOfQueues, int queueSize) {
        this.numberOfQueues = numberOfQueues;
        this.queues = new CopyOnWriteArrayList<>();
        for (int i = 0; i < numberOfQueues; i++) {
            queues.add(new LinkedBlockingQueue<>(queueSize));
        }
    }

    public BlockingQueue<Runnable> getNextQueue() {
        int index = nextIndex.getAndUpdate(i -> (i + 1) % numberOfQueues);
        return queues.get(index);
    }

    public List<BlockingQueue<Runnable>> getAllQueues() {
        return queues;
    }
}