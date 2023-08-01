public class MyThread1 extends Thread {
    BookThreaterSeat b;
    int seats;

    MyThread1(BookThreaterSeat b, int seats) {
        this.b = b;
        this.seats = seats;
    }

    public void run() {
        b.bookseat(seats);
    }
}
