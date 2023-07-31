public class Test extends Thread {
    public void run() {
        for (int i = 0; i <= 5; i++) {
            try {
                Thread.sleep(500);// sleep method is ststic method
                System.out.println(i);
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public static void main(String[] args) {
        Test th = new Test();
        th.start();
        System.out.println(th.getName());

        Test th1 = new Test();
        th1.start();
        System.out.println(th1.getName());

    }
}
