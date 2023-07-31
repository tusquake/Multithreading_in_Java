public class ThreadDeamonDemo extends Thread {
    public void run() {
        if (Thread.currentThread().isDaemon()) {
            System.out.println("daemon thread");
        } else {
            System.out.println("child thread");
        }
    }

    public static void main(String[] args) {

        Thread.currentThread().setDaemon(true);
        // Exception in thread "main" java.lang.IllegalThreadStateException

        System.out.println("main thread");
        ThreadDeamonDemo t = new ThreadDeamonDemo();
        t.setDaemon(true);
        t.start();
    }
}
