//1.Performing single task from single thread

public class Test1 {
    public static void main(String[] args) {
        MyThread th = new MyThread();
        th.start();
    }
}
