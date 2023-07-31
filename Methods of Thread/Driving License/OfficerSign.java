public class OfficerSign extends Thread {
    public void run() {
        try {
            System.out.println("Officer file checking Starts.....");
            Thread.sleep(8000);
            System.out.println("Officer file checking Ends.....");
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
