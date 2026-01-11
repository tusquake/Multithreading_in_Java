import java.util.concurrent.CompletableFuture;

public class CompletableFutureExample {

    public static void main(String[] args) {

        CompletableFuture<Integer> future =
                CompletableFuture.supplyAsync(() -> {
                    System.out.println("Task started by " + Thread.currentThread().getName());
                    sleep(8000);
                    return 200;
                });

        future
                .thenApply(result -> result * 2)
                .thenAccept(finalResult ->
                        System.out.println("Final result from CompletableFuture: " + finalResult)
                );

        System.out.println("Main thread is free to do other work");

        // Wait so JVM doesn't exit early
        future.join();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
