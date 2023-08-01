public class MyThread2 extends Thread {
    BookThreaterSeat b;
    int seats;

    MyThread2(BookThreaterSeat b, int seats) {
        this.b = b;
        this.seats = seats;
    }

    public void run() {
        b.bookseat(seats);
    }
}
