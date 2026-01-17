import java.util.concurrent.Semaphore;

public class SemaphoreExample {

    // Only 3 permits = 3 threads allowed at a time
    private static final Semaphore semaphore = new Semaphore(3);

    public static void main(String[] args) {

        // Create 7 threads (more than permits)
        for (int i = 1; i <= 7; i++) {
            int taskId = i;

            new Thread(() -> {
                try {
                    System.out.println("Task " + taskId + " waiting for permit");

                    semaphore.acquire(); // take permit

                    System.out.println("Task " + taskId + " acquired permit");
                    Thread.sleep(5000); // simulate work

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    System.out.println("Task " + taskId + " releasing permit");
                    semaphore.release(); // give back permit
                }
            }).start();
        }
    }
}
