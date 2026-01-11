import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WorkStealingPoolExample {

    public static void main(String[] args) {

        ExecutorService executor =
                Executors.newWorkStealingPool();

        for (int i = 1; i <= 8; i++) {
            int taskId = i;
            executor.submit(() -> {
                System.out.println(
                        "Task " + taskId +
                                " executed by " +
                                Thread.currentThread().getName()
                );
            });
        }
    }
}
