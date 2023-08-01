public class MovieBookApp {
    public static void main(String[] args) {
        TotalEarnings te = new TotalEarnings();
        te.start();

        // System.out.println("Total earnings : " + te.total + " Rs");
        try {
            synchronized (te) {
                te.wait();
                System.out.println("Total earnings : " + te.total + " Rs");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
