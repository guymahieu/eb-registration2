package org.clarent.griezel.servlet;

import com.google.gson.Gson;
import org.clarent.griezel.MailSender;
import org.clarent.griezel.model.Prices;
import org.clarent.griezel.model.Reservation;
import org.clarent.griezel.Application;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Guy Mahieu
 * @since 2017-09-26
 */
public class ReservationServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger("APPLICATION");

    private final SessionFactory sessionFactory;

    public ReservationServlet() {
        this.sessionFactory = Application.getSessionFactory();
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doOptions(req, resp);
        setCorsHeaders(resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        setCorsHeaders(response);
        setNoCacheHeaders(response);

        try {
            Gson gson = new Gson();
            Reservation reservation = gson.fromJson(req.getReader(), Reservation.class);

            reservation.setId(null);
            reservation.setExternalId(UUID.randomUUID().toString());
            reservation.setCreatedAt(new Date());

            Bill bill = calculatePrice(reservation);
            reservation.setReference("Halloween " + reservation.getLastname());
            reservation.setTotalAmount(bill.getTotalAmountDue());


            logger.info("Received reservation; " + reservation.toString());

            try (Session session = sessionFactory.openSession()) {

                List<TimeBlockFillingServlet.TimeBlockItem> timeBlockItems = TimeBlockFillingServlet.getTimeBlockItems(session);
                TimeBlockFillingServlet.TimeBlockItem timeBlockItem1 = timeBlockItems.stream()
                        .filter(timeBlockItem -> Objects.equals(timeBlockItem.getKey(), reservation.getTimeBlock().name()))
                        .findFirst()
                        .orElseThrow(() -> new NullPointerException("No timeblock found with key " + reservation.getTimeBlock().name()));

                int nrOfPeopleComing = reservation.getNrOfAdults() + reservation.getNrOfChildren();
                if (timeBlockItem1.getFreePlaces() < nrOfPeopleComing) {
                    logger.warning("Reservation for " + reservation.getLastname() + " " + reservation.getEmailAddress() + " can't be made, another timeblock needs to be selected");

                    TimeBlockFullError error = new TimeBlockFullError("TIMEBLOCK_FULL", "Het door u gekozen tijstip is intussen niet meer beschikbaar, kies een ander tijdstip voor vertrek.", timeBlockItems);

                    gson.toJson(error, response.getWriter());
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                } else {
                    Transaction transaction = session.beginTransaction();
                    try {
                        session.saveOrUpdate(reservation);
                        logger.info("Saved reservation with id " + reservation.getId());
                        reservation.setReference("Halloween " + reservation.getId() + "-" + reservation.getLastname());
                        bill.setReference(reservation.getReference());
                        logger.info("Added reference " + reservation.getReference());
                        session.flush();
                        transaction.commit();
                    } catch (Exception e) {
                        transaction.rollback();
                        throw e;
                    }
                    try {
                        new MailSender().sendMail(bill);
                        bill.mailSent = true;
                    } catch (Exception e) {
                        logger.severe("Error sending email to " + reservation.getEmailAddress());
                        e.printStackTrace();
                    }

                    gson.toJson(bill, response.getWriter());

                    response.setStatus(HttpServletResponse.SC_OK);

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    static void setNoCacheHeaders(HttpServletResponse response) {
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
    }

    static void setCorsHeaders(HttpServletResponse resp) {
        if (Application.isLocalMode()) {
            resp.setHeader("Access-Control-Allow-Origin", "*");
            resp.setHeader("Access-Control-Allow-Method", "GET,POST,OPTIONS");
            resp.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
            resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");
        }
    }

    private Bill calculatePrice(Reservation reservation) {
        List<BillingLine> lines = new ArrayList<>();
        List<String> adultsExtraInfos = new ArrayList<>();
        if (reservation.getNrOfAdultsHalal() > 0) {
            adultsExtraInfos.add(reservation.getNrOfAdultsHalal() + " halal");
        }
        if (reservation.getNrOfAdultsVeggie() > 0) {
            adultsExtraInfos.add(reservation.getNrOfAdultsVeggie() + " vegetarisch");
        }
        lines.add(
                new BillingLine(
                        "volwassenen",
                        reservation.getNrOfAdults(),
                        Prices.ADULT.getPrice(),
                        Prices.ADULT.getPrice(reservation.getNrOfAdults()),
                        adultsExtraInfos.isEmpty() ? "" : adultsExtraInfos.stream().collect(Collectors.joining(", ", "(", ")"))
                ));
        List<String> childrenExtraInfos = new ArrayList<>();
        if (reservation.getNrOfChildrenHalal() > 0) {
            childrenExtraInfos.add(reservation.getNrOfChildrenHalal() + " halal");
        }
        if (reservation.getNrOfChildrenVeggie() > 0) {
            childrenExtraInfos.add(reservation.getNrOfChildrenVeggie() + " vegetarisch");
        }
        lines.add(
                new BillingLine(
                        "kinderen",
                        reservation.getNrOfChildren(),
                        Prices.CHILD.getPrice(),
                        Prices.CHILD.getPrice(reservation.getNrOfChildren()),
                        childrenExtraInfos.isEmpty() ? "" : childrenExtraInfos.stream().collect(Collectors.joining(", ", "(", ")"))
                ));
        return new Bill(
                reservation,
                reservation.getTimeBlock().getDisplay(),
                lines.stream()
                        .map(billingLine -> billingLine.totalPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                lines
        );
    }

    public static class Bill {

        private Reservation reservation;
        private final String startTime;
        private String reference;
        private BigDecimal totalAmountDue;
        private List<BillingLine> lines;
        private boolean mailSent;

        public Bill(Reservation reservation, String startTime, BigDecimal totalAmountDue, List<BillingLine> lines) {
            this.reservation = reservation;
            this.startTime = startTime;
            this.totalAmountDue = totalAmountDue;
            this.lines = lines;
        }

        public String getStartTime() {
            return startTime;
        }

        public Reservation getReservation() {
            return reservation;
        }

        public void setReference(String reference) {
            this.reference = reference;
        }

        public String getReference() {
            return reference;
        }

        public BigDecimal getTotalAmountDue() {
            return totalAmountDue;
        }

        public List<BillingLine> getLines() {
            return lines;
        }

        public boolean isMailSent() {
            return mailSent;
        }

        public void setMailSent(boolean mailSent) {
            this.mailSent = mailSent;
        }
    }

    public static class BillingLine {
        private String description;
        private int nrOfPeople;
        private BigDecimal pricePerPerson;
        private BigDecimal totalPrice;
        private String extraInfo;

        public BillingLine(String description, int nrOfPeople, BigDecimal pricePerPerson, BigDecimal totalPrice, String extraInfo) {
            this.description = description;
            this.nrOfPeople = nrOfPeople;
            this.pricePerPerson = pricePerPerson;
            this.totalPrice = totalPrice;
            this.extraInfo = extraInfo;
        }

        public String getExtraInfo() {
            return extraInfo;
        }

        public String getDescription() {
            return description;
        }

        public int getNrOfPeople() {
            return nrOfPeople;
        }

        public BigDecimal getPricePerPerson() {
            return pricePerPerson;
        }

        public BigDecimal getTotalPrice() {
            return totalPrice;
        }
    }

    public static class TimeBlockFullError {

        private String errorCode;
        private String errorMessage;
        private List<TimeBlockFillingServlet.TimeBlockItem> refreshedTimeBlocks;

        public TimeBlockFullError(String errorCode, String errorMessage, List<TimeBlockFillingServlet.TimeBlockItem> refreshedTimeBlocks) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.refreshedTimeBlocks = refreshedTimeBlocks;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public List<TimeBlockFillingServlet.TimeBlockItem> getRefreshedTimeBlocks() {
            return refreshedTimeBlocks;
        }
    }


}
