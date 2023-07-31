//Implementation of Thread Classes

public class Test extends Thread {

    @Override
    public void run() {
        System.out.println();
    }

    public static void main(String[] args) {
        Test t1 = new Test();
        t1.start();
    }

}