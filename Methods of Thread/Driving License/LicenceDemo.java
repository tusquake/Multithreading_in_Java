public class LicenceDemo {
    public static void main(String[] args) throws InterruptedException {
        Medical medical = new Medical();
        medical.start();

        medical.join();

        TestDriver td = new TestDriver();
        td.start();

        td.join();

        OfficerSign os = new OfficerSign();
        os.start();
    }
}
