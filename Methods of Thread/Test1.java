public class Test1 extends Thread {
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
        Test1 th = new Test1();
        // Now run() has become a normal method and it is not overriding from thread
        // class
        // so the thread will execute one by one
        th.run();
        System.out.println(th.getName());

        Test1 th1 = new Test1();
        th1.run();
        System.out.println(th1.getName());

    }
}
