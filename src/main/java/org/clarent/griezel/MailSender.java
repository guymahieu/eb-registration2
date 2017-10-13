package org.clarent.griezel;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.clarent.griezel.model.Reservation;
import org.clarent.griezel.model.TimeBlock;
import org.clarent.griezel.servlet.ReservationServlet;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.text.StringEscapeUtils.escapeHtml4;

/**
 * @author Guy Mahieu
 * @since 2017-09-30
 */
public class MailSender {

    private static final String WORKINGDAYS_FOR_PAYMENT = "3";

    public void sendMail(ReservationServlet.Bill bill) throws EmailException, MalformedURLException, AddressException {
        Reservation reservation = bill.getReservation();

        HtmlEmail email = new HtmlEmail();
        setConnectionInfo(email);

        email.setReplyTo(getReplyToAddress());
        email.setFrom(getFromAddress(), "Halloweentocht Boxberg");
        email.addTo(reservation.getEmailAddress(), reservation.getFirstname() + " " + reservation.getLastname());
        email.addBcc("guy.mahieu+griezeltocht@gmail.com");
        email.setSubject("Jouw reservatie voor de Halloweentocht 2017");

        // embed the image and get the content id
        URL url = new URL("http://www.boxberggriezelt.be/ui/img/logo.jpg");
        String logoCid = email.embed(url, "Ouderraad logo");

        // set the html message
        email.setHtmlMsg("" +
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
                " <head>\n" +
                "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />\n" +
                "  <title>Reservatie-informatie Halloweentocht 2017</title>\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n" +
                "</head>\n" +
                "<body>\n" +
                "   <table width=\"100%\"><tr><td>" +
                "   <p>" +
                "       Beste " + escapeHtml4(reservation.getFirstname()) + "," +
                "       <br/>" +
                "       <br/>" +
                "       We hebben jouw reservatie op naam van " + escapeHtml4(reservation.getFirstname()) + " " + reservation.getLastname() + " correct ontvangen " +
                "       en verwachten je op <strong>zondag 29 oktober</strong> om <strong>"+reservation.getTimeBlock().getDisplay()+"</strong>." +
                "       <br/>" +
                "       Zorg ervoor dat je minstens 10 minuten op voorhand aanwezig bent op school zodat je zeker op tijd kan vertrekken!" +
                "       <br/>" +
                "       <br/>" +
                "       <strong>Let op:</strong> je plaatsen worden pas definitief voor jou gereserveerd na betaling van " +
                "       <strong>&euro;&nbsp;" + bill.getTotalAmountDue().intValue() + ".00</strong> op rekening " +
                "       <strong>BE46&nbsp;0014&nbsp;1821&nbsp;1536</strong> van de ouderraad met vermelding van " +
                "       <strong>" + escapeHtml4(bill.getReference()) + "</strong>" +
                "       <br/>" +
                "       <br/>" +
                "       Bij niet-betaling binnen de " + escapeHtml4(WORKINGDAYS_FOR_PAYMENT) + " werkdagen worden deze plaatsen terug beschikbaar gesteld voor anderen." +
                "       <br/>" +
                "       <br/>" +
                "       <p><u>Details van jouw reservatie</u>:</p>" +
                "       <table>" +
                (bill.getLines().stream().map(billingLine -> "" +
                        "<tr>" +
                        "   <td>" + escapeHtml4(billingLine.getDescription()) + ": </td>" +
                        "   <td>" + billingLine.getNrOfPeople() + "</td>" +
                        "   <td>x</td>" +
                        "   <td>&euro;&nbsp;" + billingLine.getPricePerPerson().intValue() + ".00</td>" +
                        "   <td>=</td>" +
                        "   <td>&euro;&nbsp;" + billingLine.getTotalPrice().intValue() + ".00</td>" +
                        "   <td>&nbsp;</td>" +
                        "   <td style='text-wrap: avoid'>" + escapeHtml4(billingLine.getExtraInfo()) + "</td>" +
                        "</tr>"
                ).collect(Collectors.joining())) +
                "          <tr style=\"font-weight: bolder\">" +
                "           <td colspan=\"3\">&nbsp;</td>" +
                "           <td>Totaal: </td>" +
                "           <td>=</td>" +
                "           <td>&euro;&nbsp;" + bill.getTotalAmountDue().intValue() + ".00</td>" +
                "       </tr>" +
                "       </table>" +
                "       " +
                "       <p>" +
                "       Vriendelijke groeten," +
                "       <br/>" +
                "       <br/>" +
                "       De ouderraad van G.V. Basisschool Boxbergheide." +
                "       <br/>" +
                "       <a href=\"mailto:orboxbergheide@gmail.com\">orboxbergheide@gmail.com</a><br/>" +
                "       <br/>" +
                "       <br/>" +
                "       <img src=\"cid:" + logoCid + "\">" +
                "       </p>" +
                "   </td></tr></table>" +
                "</body>" +
                "</html>");

        // set the alternative message
        email.setTextMsg("" +
                "Uw e-mail client ondersteunt helaas geen HTML mails.\n\n\n" +
                "Indien u de details van uw reservatie wenst, stuur dan een mail naar guy.mahieu@gmail.com." +
                "");

        // send the email
        email.send();

    }

    private String getFromAddress() {
        String fromAddress = System.getenv("SMTP_FROM");
        if (fromAddress == null || fromAddress.trim().length() == 0) {
            fromAddress = "no-reply@mail.boxberggriezelt.be";
        }
        return fromAddress;
    }

    private List<InternetAddress> getReplyToAddress() throws AddressException {
        String replyToAddress = System.getenv("SMTP_REPLY_TO");
        if (replyToAddress == null || replyToAddress.trim().length() == 0) {
            replyToAddress = "guy.mahieu@gmail.com";
        }
        InternetAddress[] replyToAddresses = InternetAddress.parse(replyToAddress);
        return Arrays.asList(replyToAddresses);
    }

    private void setConnectionInfo(HtmlEmail email) {
        email.setHostName(System.getenv("SMTP_HOST"));
        email.setSmtpPort(Integer.parseInt(System.getenv("SMTP_PORT")));
        email.setSSLOnConnect(true);
        email.setAuthentication(
                System.getenv("SMTP_USER"),
                System.getenv("SMTP_PASSWORD")
        );
    }

    public static void main(String[] args) throws AddressException, EmailException, MalformedURLException {
        Reservation reservation = new Reservation();
        reservation.setId(1L);

        reservation.setReference("Halloween 1-Bortels");
        reservation.setFirstname("Sofie");
        reservation.setLastname("Bortels");
        reservation.setEmailAddress("guy.mahieu@gmail.com");
        reservation.setTimeBlock(TimeBlock.START_AT_17_50);


        ArrayList<ReservationServlet.BillingLine> lines = new ArrayList<>();
        lines.add(new ReservationServlet.BillingLine(
                "volwassenen", 2, new BigDecimal("7"), new BigDecimal("14"), "(1 vegetarisch, 1 halal)"
        ));
        lines.add(new ReservationServlet.BillingLine(
                "kinderen", 3, new BigDecimal("5"), new BigDecimal("15"), "(1 vegetarisch)"
        ));

        ReservationServlet.Bill bill = new ReservationServlet.Bill(reservation, "18:00", new BigDecimal("29.00"), lines);
        bill.setReference(reservation.getReference());
        new MailSender().sendMail(bill);
    }

}
