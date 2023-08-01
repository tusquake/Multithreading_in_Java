public class BookThreaterSeat {
    int total_seats = 10;

    void bookseat(int seats) {
        if (total_seats >= seats) {
            System.out.println("seats booked successfully!");
            total_seats = total_seats - seats;
            System.out.println("total seats left : " + total_seats);
        } else {
            System.out.println("seats cannot be booked!");
            System.out.println("total seats left : " + total_seats);
        }
    }
}
