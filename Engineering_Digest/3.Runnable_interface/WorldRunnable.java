public class WorldRunnable implements Runnable {
    @Override
    public void run() {
        for (;;) {
            System.out.println("World");
        }
    }
}