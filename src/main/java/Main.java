import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        CustomThreadPool pool = new CustomThreadPool(
                2, 6, 5, TimeUnit.SECONDS, 3, 1, 3
        );

        for (int i = 0; i < 20; i++) {
            int finalI = i;
            pool.execute(() -> {
                System.out.println("[Task] Running task " + finalI);
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException ignored) {}
                System.out.println("[Task] Finished task " + finalI);
            });
        }

        Thread.sleep(15000);
        pool.shutdown();
    }
}