public class ThreadNameDemo1 extends Thread {

    public void run() {
        // Thread.currentThread().setName("Hussu");
        System.out.println("Thread Task is executed by : " + Thread.currentThread().getName());
    }

    public static void main(String[] args) {
        System.out.println("hello is printed by : " + Thread.currentThread().getName());

        ThreadNameDemo1 t1 = new ThreadNameDemo1();
        t1.start();

        ThreadNameDemo1 t2 = new ThreadNameDemo1();
        t2.setName("Tussu");
        t2.start();

        ThreadNameDemo1 t3 = new ThreadNameDemo1();
        t3.setName("Mussu");
        t3.start();
    }
}
