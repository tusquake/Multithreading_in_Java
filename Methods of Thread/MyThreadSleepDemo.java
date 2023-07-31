public class MyThreadSleepDemo extends Thread {
    public void run() {
        for (int i = 0; i <= 5; i++) {
            try {
                Thread.sleep(1000);// sleep method is ststic method
                System.out.println(i);
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public static void main(String[] args) {
        MyThreadSleepDemo th = new MyThreadSleepDemo();
        th.start();
        System.out.println(th.getName());

    }
}
