import java.util.concurrent.CyclicBarrier;

public class CyclicBarrierExample {

    private static final int PLAYERS = 3;

    public static void main(String[] args) {

        CyclicBarrier barrier = new CyclicBarrier(
                PLAYERS,
                () -> System.out.println("All players ready. Game starts!\n")
        );

        for (int i = 1; i <= PLAYERS; i++) {
            int id = i;
            new Thread(() -> {
                try {
                    System.out.println("Player " + id + " is ready");
                    Thread.sleep(2000 + id * 500); // different arrival times
                    barrier.await(); // wait at barrier

                    System.out.println("Player " + id + " started playing");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
