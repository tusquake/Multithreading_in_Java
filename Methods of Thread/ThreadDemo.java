public class ThreadDemo extends Thread {
    public void run() {
        // Thread.currentThread().setName("Hussu");
        System.out.println("Thread Task is executed by : " + Thread.currentThread().getName());
    }

    public static void main(String[] args) {
        System.out.println("hello is printed by : " + Thread.currentThread().getName());

        ThreadDemo t1 = new ThreadDemo();
        t1.start();

        System.out.println(Thread.currentThread().getName());
        System.out.println(Thread.currentThread().isAlive());
        System.out.println(t1.isAlive());
    }
}
