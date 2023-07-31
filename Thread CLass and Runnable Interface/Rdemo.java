public class Rdemo implements Runnable {

    @Override
    public void run() {
        System.out.println("Thread Task 2");
    }

    public static void main(String[] args) {
        Rdemo t = new Rdemo();
        Thread th = new Thread(t);
        th.start();
    }
}
