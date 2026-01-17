import java.util.concurrent.CountDownLatch;

public class CountDownLatchExample {

    private static final int WORKERS = 3;
    private static final CountDownLatch latch = new CountDownLatch(WORKERS);

    public static void main(String[] args) throws InterruptedException {

        for (int i = 1; i <= WORKERS; i++) {
            int id = i;
            new Thread(() -> {
                try {
                    System.out.println("Worker " + id + " started");
                    Thread.sleep(2000); // simulate work
                    System.out.println("Worker " + id + " finished");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown(); // signal completion
                }
            }).start();
        }

        System.out.println("Main thread waiting for workers...");
        latch.await(); // wait until count reaches zero

        System.out.println("All workers finished. Main thread continues.");
    }
}
