public class ThreadInterrupedDemo extends Thread {
    public void run() {
        System.out.println("A : " + Thread.interrupted()); // true ->false
        // System.out.println(Thread.currentThread().isInterrupted()); // true -> true
        try {
            for (int i = 0; i <= 5; i++) {
                System.out.println(i);
                Thread.sleep(1000);
                // System.out.println("B : " + Thread.interrupted());
            }
        } catch (Exception e) {
            System.out.println("Thread Interrupted : " + e);
        }
    }

    public static void main(String[] args) {
        ThreadInterrupedDemo t = new ThreadInterrupedDemo();
        t.start();
        t.interrupt();
    }
}
