import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CachedThreadPoolExample {

    public static void main(String[] args) {
        ExecutorService executor = Executors.newCachedThreadPool();

        for (int i = 1; i <= 10; i++) {
            int taskId = i;
            executor.submit(() -> {
                System.out.println(
                        "Task " + taskId +
                                " executed by " +
                                Thread.currentThread().getName()
                );
            });
        }

        executor.shutdown();
    }
}
