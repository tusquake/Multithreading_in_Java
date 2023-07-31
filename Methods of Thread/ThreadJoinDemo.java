public class ThreadJoinDemo extends Thread {
    public void run() {
        for (int i = 0; i <= 5; i++) {
            try {
                Thread.sleep(1000);// sleep method is ststic method
                System.out.println("child thread : " + i);
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        ThreadJoinDemo th = new ThreadJoinDemo();
        th.start();

        th.join();// main thread waits until child threads completes execution

        for (int i = 0; i <= 5; i++) {
            try {
                Thread.sleep(1000);// sleep method is ststic method
                System.out.println("main thread : " + i);
            } catch (Exception e) {
                System.out.println(e);
            }
        }

    }
}
