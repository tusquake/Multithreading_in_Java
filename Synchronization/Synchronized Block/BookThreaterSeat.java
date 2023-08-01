public class BookThreaterSeat {
    int total_seats = 10;

    void bookseat(int seats) {
        System.out.println("Tussu1 : " + Thread.currentThread().getName());
        System.out.println("Tussu2 : " + Thread.currentThread().getName());
        System.out.println("Tussu3 : " + Thread.currentThread().getName());
        System.out.println("Tussu4 : " + Thread.currentThread().getName());

        // synchronized block
        synchronized (this) {
            if (total_seats >= seats) {
                System.out.println("seats booked successfully!");
                total_seats = total_seats - seats;
                System.out.println("total seats left : " + total_seats);
            } else {
                System.out.println("seats cannot be booked!");
                System.out.println("total seats left : " + total_seats);
            }
        }

        System.out.println("Tussu5 : " + Thread.currentThread().getName());
        System.out.println("Tussu6 : " + Thread.currentThread().getName());
        System.out.println("Tussu7 : " + Thread.currentThread().getName());
        System.out.println("Tussu8 : " + Thread.currentThread().getName());
    }
}
