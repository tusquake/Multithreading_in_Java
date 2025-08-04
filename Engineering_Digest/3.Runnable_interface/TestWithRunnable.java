public class TestWithRunnable {
    public static void main(String[] args) {
        WorldRunnable world = new WorldRunnable();
        Thread thread = new Thread(world);
        thread.start();
        for (;;) {
            System.out.println("Hello");
        }
    }
}