public class TestDriver extends Thread {
    public void run() {
        try {
            System.out.println("Test Drive Starts.....");
            Thread.sleep(5000);
            System.out.println("Test Drive Ends.....");
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
