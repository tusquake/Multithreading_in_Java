public class BookThreaterSeat {
    static int total_seats = 20;

    // by just adding one keyword synchronized our program starts to run correctly
    synchronized static void bookseat(int seats) {
        if (total_seats >= seats) {
            System.out.println(seats + " : seats booked successfully!");
            total_seats = total_seats - seats;
            System.out.println("total seats left : " + total_seats);
        } else {
            System.out.println("seats cannot be booked!");
            System.out.println("total seats left : " + total_seats);
        }
    }
}
