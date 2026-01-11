import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FutureExample {

    public static void main(String[] args) throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(1);

        Callable<Integer> task = () -> {
            System.out.println("Task started by " + Thread.currentThread().getName());
            Thread.sleep(2000);
            return 100;
        };

        Future<Integer> future = executor.submit(task);

        System.out.println("Main thread doing other work...");

        // BLOCKING call
        Integer result = future.get();

        System.out.println("Result from Future: " + result);

        executor.shutdown();
    }
}
