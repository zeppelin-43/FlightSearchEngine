package is.hi.Core;

import java.util.ArrayList;

/**
 * Class for a booking
 *
 * @author Agnar Pétursson, Háskóli Íslands, agp11@hi.is
 */
public class Booking {

    private String bookingNumber; // The booking number
    private ArrayList<Ticket> tickets = new ArrayList<Ticket>(); // The ticket that the booking contains

    /**
     * Creates a new booking
     *
     * @param bookingNumber The booking number
     */
    public Booking(String bookingNumber) {
        this.bookingNumber = bookingNumber;
    }

    /**
     * To string method
     *
     * @return
     */
    @Override
    public String toString() {
        return "Booking{" + "bookingNumber=" + bookingNumber + ", tickets=" + tickets + '}';
    }

    /**
     * Returns the booking number
     *
     * @return bookingNumber
     */
    public String getBookingNumber() {
        return bookingNumber;
    }

    /**
     * Returns the tickets
     *
     * @return tickets
     */
    public ArrayList<Ticket> getTickets() {
        return tickets;
    }

    /**
     * Sets the tickets of the booking
     *
     * @param tickets
     */
    void setTickets(ArrayList<Ticket> tickets) {
        this.tickets = tickets;
    }

}
