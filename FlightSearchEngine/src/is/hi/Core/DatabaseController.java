package is.hi.Core;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

/**
 * Connects to the database and handles all request.
 *
 * @author Agnar Pétursson, Háskóli Íslands, agp11@hi.is
 */
public class DatabaseController {
    
    //Connection for database connection
    private Connection conn;
    //Prepared statement
    private PreparedStatement pstmt;

    //Url to the database.
    private final String SQL_URL = "jdbc:mysql://sql2.freemysqlhosting.net:3306/sql2276561";
    //Username for the database.
    private final String SQL_USER = "sql2276561";
    //Password for the database.
    private final String SQL_PASSWORD = "qF3!zE5!";

    //SQL query to get flights, for getFlights method.
    private final String SQL_GETFLIGHTS = "SELECT * FROM Flight WHERE fFrom = ? and fTo = ? and date = ? ";
    //SQL query to get seats on a flight.
    private final String SQL_GETFLIGHTSEAT = "SELECT seats FROM Flight WHERE  fnumber = ? and fFrom = ? and time = ? and date = ? ";

    //SQL query to get booking with booking number.
    private final String SQL_GETBOOKING = "SELECT * FROM Booking WHERE bookingNumber = ?";
    //SQL query to get everything for a booking.
    private final String SQL_GETTICKETPASSFLIGHT = "SELECT * FROM Ticket NATURAL JOIN Passenger NATURAL JOIN Flight WHERE bookingNumber = ?";

    //SQL query to insert passenger to the database. Ignore is to be able to have passenger for 2 or more flights.
    private final String SQL_INSERTPASSENGER = "INSERT IGNORE INTO Passenger (kt, name, numberOfTickets) VALUES (?, ?, ?)";
    //SQL query to insert ticket to the database.
    private final String SQL_INSERTTICKET = "INSERT INTO Ticket (bookingNumber, kt, seat, fnumber, date, time, fFrom) VALUES (?, ?, ?, ?, ?, ?, ?)";
    //SQL query to insert booking to the database.
    private final String SQL_INSERTBOOKING = "INSERT INTO Booking (bookingNumber, tickets) VALUES (?, ?)";
    //SQL query to increment number of tickets for a passenger.
    private final String SQL_INCREMENTTICKETCOUNT = "UPDATE Passenger SET numberOfTickets = numberOfTickets + 1 WHERE kt = ?";
    //SQL query to decrement number of tickets for a passenger.
    private final String SQL_DECREMENTTICKETCOUNT = "UPDATE Passenger SET numberOfTickets = numberOfTickets - 1 WHERE kt = ?";
    //SQL query to update seats for a flight.
    private final String SQL_UPDATESEATS = "UPDATE Flight SET seats = ? where fNumber = ? and fFrom = ? and date = ? and time = ?";

    //SQL query to delete booking from the database.
    private final String SQL_DELETEBOOKING = "DELETE FROM Booking WHERE bookingNumber = ?";
    //SQL query to delete passenger from the database.
    private final String SQL_DELETEPASSENGER = "DELETE FROM Passenger WHERE kt = ? and numberOFTickets = 0";

    /**
     * Establish a connection to the database
     *
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public DatabaseController() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.jdbc.Driver");
        conn = DriverManager.getConnection(SQL_URL, SQL_USER, SQL_PASSWORD);
        conn.setAutoCommit(false);

    }

    /**
     * Gets all flights from the database with a specific 
     * Departure location, destination location and date.
     *
     * @param from location flight will be flying from.
     * @param to location flight will be flying to.
     * @param date of flight
     * 
     * @return List of all flights with a given input.
     */
    public ArrayList<Flight> getFlights(String from, String to, LocalDate date) throws SQLException {
        ArrayList<Flight> list = new ArrayList<Flight>();
        Flight flight;

        pstmt = conn.prepareStatement(SQL_GETFLIGHTS);

        pstmt.clearParameters();
        pstmt.setString(1, from);
        pstmt.setString(2, to);
        pstmt.setString(3, date.toString());

        ResultSet rs = pstmt.executeQuery();
        conn.commit();
        while (rs.next()) {
            String fNumber = rs.getString(1);
            String airline = rs.getString(2);
            String ffrom = rs.getString(3);
            String fto = rs.getString(4);
            LocalDate fdate = LocalDate.parse(rs.getString(5));
            LocalTime time = LocalTime.parse(rs.getString(6));
            int price = Integer.parseInt(rs.getString(7));
            String seats = rs.getString(8);

            flight = new Flight(fNumber, airline, ffrom, fto, fdate, time, price, seats);
            list.add(flight);
        }
        rs.close();

        return list;
    }

    /**
     * Finds and return booking from database with a specific booking number.
     *
     * @param bookingNumber Booking number to look up in the database.
     * @return booking with bookingNumber. Returns null if booking with bookingNumber does not exist.
     */
    public Booking getBooking(String bookingNumber) {
        Booking booking = null;
        Passenger passenger;
        Ticket ticket;
        Flight flight;
        ResultSet rs;

        //Ticket and booking
        String bookingN;
        String seat;

        //Flight
        String fNumber;
        LocalDate date;
        LocalTime time;
        String fFrom;
        String airline;
        String fTo;
        int price;
        String seats;

        //Passenger
        String name;
        String kt;

        try {

            pstmt = conn.prepareStatement(SQL_GETBOOKING);

            pstmt.clearParameters();
            pstmt.setString(1, bookingNumber);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                bookingN = rs.getString(1);
                booking = new Booking(bookingNumber);
            }

            pstmt = conn.prepareStatement(SQL_GETTICKETPASSFLIGHT);
            pstmt.clearParameters();
            pstmt.setString(1, bookingNumber);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                fNumber = rs.getString(1);
                date = LocalDate.parse(rs.getString(2));
                time = LocalTime.parse(rs.getString(3));
                fFrom = rs.getString(4);
                kt = rs.getString(5);
                bookingN = rs.getString(6);
                seat = rs.getString(7);
                name = rs.getString(8);
                airline = rs.getString(10);
                fTo = rs.getString(11);
                price = Integer.parseInt(rs.getString(12));
                seats = rs.getString(13);

                passenger = new Passenger(name, kt);
                flight = new Flight(fNumber, airline, fFrom, fTo, date, time, price, seats);
                ticket = new Ticket(bookingNumber, seat, passenger, flight);
                ArrayList<Ticket> list = booking.getTickets();
                list.add(ticket);
                booking.setTickets(list);
            }

            rs.close();
        } catch (Exception e) {
            booking = null;
        }

        return booking;
    }

    /**
     * Removes booking from the database with a specific booking number. Returns true if the booking was in
     * database and deletion was done successfully else returns false.
     *
     * @param bookingNumber of booking to be deleted.
     * @return true if deletion was done successfully.
     * @throws SQLException
     */
    boolean deleteBooking(String bookingNumber) throws SQLException {
        //Passenger
        String kt;
        String seat;
        String fNumber;
        String fFrom;
        LocalDate date;
        LocalTime time;

        String seats;

        SeatCoder seatCoder = new SeatCoder();
        ResultSet rs;

        Booking booking = getBooking(bookingNumber);

        for (Ticket i : booking.getTickets()) {
            //Get information from booking
            kt = i.getPassenger().getKt();
            fNumber = i.getFlight().getfNumber();
            seat = i.getSeat();
            date = i.getFlight().getDate();
            time = i.getFlight().getTime();
            fFrom = i.getFlight().getFrom();

            //Decrement numberOfTickets by 1
            pstmt = conn.prepareStatement(SQL_DECREMENTTICKETCOUNT);
            pstmt.clearParameters();
            pstmt.setString(1, kt);
            pstmt.execute();

            //Delete Passenger if numberOfTickets is 0
            pstmt = conn.prepareStatement(SQL_DELETEPASSENGER);
            pstmt.clearParameters();
            pstmt.setString(1, kt);
            pstmt.execute();

            //Get newest seats from database
            pstmt = conn.prepareStatement(SQL_GETFLIGHTSEAT);
            pstmt.clearParameters();
            pstmt.setString(1, fNumber);
            pstmt.setString(2, fFrom);
            pstmt.setString(3, time.toString());
            pstmt.setString(4, date.toString());
            rs = pstmt.executeQuery();
            rs.next();
            seats = rs.getString(1);
            rs.close();
            i.getFlight().setSeats(seats);

            //Update seats for flight
            seatCoder.setFlight(i.getFlight());
            seatCoder.cancelReservedSeat(seat);
            pstmt = conn.prepareStatement(SQL_UPDATESEATS);
            pstmt.clearParameters();
            pstmt.setString(1, i.getFlight().getSeats());
            pstmt.setString(2, fNumber);
            pstmt.setString(3, fFrom);
            pstmt.setString(4, date.toString());
            pstmt.setString(5, time.toString());
            pstmt.execute();
        }
        pstmt = conn.prepareStatement(SQL_DELETEBOOKING);
        pstmt.clearParameters();
        pstmt.setString(1, bookingNumber);
        pstmt.execute();

        conn.commit();
        return true;

    }

    /**
     * Insert booking into the database.
     *
     * @param booking booking to put into the database.
     * @return true if insertion was successful.
     */
    boolean bookBooking(Booking booking) throws SQLException {
        //For booking
        String bookingNumber; // Booking and Ticket
        int tickets;

        //For Passenger
        String name;
        String kt; //Passenger and Ticket

        //For Ticket
        int id;
        String seat;
        String fnumber;
        LocalDate date;
        LocalTime time;
        String fFrom;

        SeatCoder seatCoder = new SeatCoder();

        //Inserting into Booking
        pstmt = conn.prepareStatement(SQL_INSERTBOOKING);
        bookingNumber = booking.getBookingNumber();
        tickets = booking.getTickets().size();
        pstmt.clearParameters();
        pstmt.setString(1, bookingNumber);
        pstmt.setString(2, Integer.toString(tickets));
        pstmt.execute();

        //Inserting into Passenger and ticket
        for (Ticket i : booking.getTickets()) {

            //Inserting into passenger if the passenger does not exists
            pstmt = conn.prepareStatement(SQL_INSERTPASSENGER);
            name = i.getPassenger().name;
            kt = i.getPassenger().kt;
            pstmt.clearParameters();
            pstmt.setString(1, kt);
            pstmt.setString(2, name);
            pstmt.setString(3, Integer.toString(0));
            pstmt.execute();

            //Increment numberOfTickets by 1
            pstmt = conn.prepareStatement(SQL_INCREMENTTICKETCOUNT);
            pstmt.clearParameters();
            pstmt.setString(1, kt);
            pstmt.execute();

            //Inserting Ticket for passenger
            pstmt = conn.prepareStatement(SQL_INSERTTICKET);
            seat = i.getSeat();
            fnumber = i.getFlight().getfNumber();
            date = i.getFlight().getDate();
            time = i.getFlight().getTime();
            fFrom = i.getFlight().getFrom();

            pstmt.clearParameters();
            pstmt.setString(1, bookingNumber);
            pstmt.setString(2, kt);
            pstmt.setString(3, seat);
            pstmt.setString(4, fnumber);
            pstmt.setString(5, date.toString());
            pstmt.setString(6, time.toString());
            pstmt.setString(7, fFrom);
            pstmt.execute();

            //Insert reserved seats into flights
            pstmt = conn.prepareStatement(SQL_UPDATESEATS);
            pstmt.clearParameters();
            pstmt.setString(1, i.getFlight().getSeats());
            pstmt.setString(2, fnumber);
            pstmt.setString(3, fFrom);
            pstmt.setString(4, date.toString());
            pstmt.setString(5, time.toString());
            pstmt.execute();
        }
        conn.commit();
        return true;

    }

    /**
     * Closes the connection to the database.
     */
    void closeConnection() {
        try {
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws SQLException {
        //  DatabaseController DB = new DatabaseController();

//        ArrayList<Flight> list = DB.getFlights("Reykjavík", "Akureyri" ,LocalDate.of(2019, 01, 01));
//        System.out.println(list);
//        //System.out.println(booking);
//        Flight flight1 = list.get(2);
//        Booking booking = new Booking("4321");
//        ArrayList<Ticket> listi = booking.getTickets();
//        Ticket e = new Ticket("1234", "2a", new Passenger("Agnar Petursson", "3004972929"), flight1);
//        Ticket e1 = new Ticket("1234", "3a", new Passenger("Jon Jonsson", "2008972929"), flight1);
//        listi.add(e);
//        listi.add(e1);
//        booking.setTickets(listi);
//        System.out.println(DB.getBooking("4321"));
//        DB.bookBooking(booking);
//        System.out.println(DB.getBooking("4321"));
//        DB.deleteBooking("4321");
//        System.out.println(DB.getBooking("4321"));
//        
        //testBooking();
        // System.out.println("Hello");
        // DB.closeConnection();
    }

}
