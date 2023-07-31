public class ThreadNameDemo {
    public static void main(String[] args) {
        System.out.println("Hello");
        System.out.println(Thread.currentThread().getName());
        // System.out.println(10 / 0);// Exception in thread "main"
        Thread.currentThread().setName("Tussu");
        // System.out.println(10 / 0);//Exception in thread "Tussu"
        System.out.println("New name of Thread : " + Thread.currentThread().getName());
    }
}