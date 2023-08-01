public class MovieBookApp {

    static BookThreaterSeat b;
    int seats;

    public void run() {
        b.bookseat(seats);
    }

    public static void main(String[] args) {
        BookThreaterSeat b1 = new BookThreaterSeat();

        MyThread1 t1 = new MyThread1(b1, 7);
        t1.start();

        MyThread2 t2 = new MyThread2(b1, 6);
        t2.start();

        // _________________________________________________

        BookThreaterSeat b2 = new BookThreaterSeat();

        MyThread1 t3 = new MyThread1(b2, 5);
        t3.start();

        MyThread2 t4 = new MyThread2(b2, 9);
        t4.start();

    }
}