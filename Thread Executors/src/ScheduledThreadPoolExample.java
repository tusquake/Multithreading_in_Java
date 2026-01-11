import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledThreadPoolExample {

    public static void main(String[] args){

        ScheduledExecutorService scheduler =
                Executors.newScheduledThreadPool(2);

        scheduler.schedule(
                () -> System.out.println("Executed after 3 seconds"),
                3,
                TimeUnit.SECONDS
        );

        scheduler.scheduleAtFixedRate(
                () -> System.out.println("Running every 2 seconds"),
                1,
                2,
                TimeUnit.SECONDS
        );

        try {
            Thread.sleep(10000);
            scheduler.shutdown();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
