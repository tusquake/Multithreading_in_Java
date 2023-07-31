//In real world we dont create the main class as thread so we make another class and make the thread of that particular class

public class MyThread extends Thread {
    public void run() {
        System.out.println("task 1");
    }
}
