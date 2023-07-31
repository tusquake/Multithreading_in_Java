public class Tdemo extends Thread {

    @Override
    public void run() {
        System.out.println("Thread Task");
    }

    public static void main(String[] args) {
        Tdemo t = new Tdemo();
        t.start();
        // ->t.start(); //Exception in thread "main"
        // java.lang.IllegalThreadStateException
    }
}