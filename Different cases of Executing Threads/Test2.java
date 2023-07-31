//2.Performing single task from multiple thread

public class Test2 extends Thread {
    public static void main(String[] args) {
        MyThread thread1 = new MyThread();
        thread1.start();

        MyThread thread2 = new MyThread();
        thread2.start();

        MyThread thread3 = new MyThread();
        thread3.start();

        MyThread thread4 = new MyThread();
        thread4.start();
    }
}
