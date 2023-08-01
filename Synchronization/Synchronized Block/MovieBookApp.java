public class MovieBookApp extends Thread {

    static BookThreaterSeat b;
    int seats;

    public void run() {
        b.bookseat(seats);
    }

    public static void main(String[] args) {
        b = new BookThreaterSeat();

        MovieBookApp tushar = new MovieBookApp();
        tushar.seats = 7;
        tushar.start();
        // tushar.run();

        MovieBookApp seth = new MovieBookApp();
        seth.seats = 6;
        seth.start();
        // seth.run();
    }
}