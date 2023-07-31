public class ThreadYieldDemo extends Thread {
    public void run() {
        Thread.yield();// If you want child thread to stop and provide chance to other threads for
                       // execution
        for (int i = 0; i <= 5; i++) {
            System.out.println(Thread.currentThread().getName() + " - " + i);
        }
    }

    public static void main(String[] args) {
        ThreadYieldDemo th = new ThreadYieldDemo();
        th.start();

        Thread.yield(); // If you want main thread to stop and provide chance to other threads for
                        // execution

        for (int i = 1; i <= 5; i++) {
            System.out.println(Thread.currentThread().getName() + " -" + i);
        }

    }
}
