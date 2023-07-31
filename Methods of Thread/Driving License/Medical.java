public class Medical extends Thread {
    public void run() {
        try {
            System.out.println("Medical Starts.....");
            Thread.sleep(3000);
            System.out.println("Medical Ends.....");
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}