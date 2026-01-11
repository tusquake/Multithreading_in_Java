import java.util.concurrent.*;

public class ThreadPoolExecutorExample {

    public static void main(String[] args) {

        ExecutorService executor =
                new ThreadPoolExecutor(
                        2,                          // corePoolSize
                        4,                          // maxPoolSize
                        30,                         // keepAliveTime
                        TimeUnit.SECONDS,           // unit
                        new ArrayBlockingQueue<>(5),// bounded queue
                        new ThreadPoolExecutor.CallerRunsPolicy()
                );

        for (int i = 1; i <= 10; i++) {
            int taskId = i;
            executor.submit(() -> {
                System.out.println(
                        "Task " + taskId +
                                " executed by " +
                                Thread.currentThread().getName()
                );
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executor.shutdown();
    }
}
