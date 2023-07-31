public class ThreadSleepDemo {
    public static void main(String[] args) {
        for (int i = 0; i <= 5; i++) {
            try {
                Thread.sleep(1000 * i);
                System.out.println(i);
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }
}
