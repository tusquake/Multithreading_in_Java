//4.Performing multiple task from multiple thread

public class Test4 {
    public static void main(String[] args) {
        MyThread thread1 = new MyThread();
        thread1.start();

        MyThread1 thread2 = new MyThread1();
        thread2.start();
    }
}

// Everthing got executed at the same time
