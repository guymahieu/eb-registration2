package org.clarent.griezel.servlet;

import org.clarent.griezel.Application;
import org.clarent.griezel.model.TimeBlock;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.apache.commons.text.StringEscapeUtils.unescapeHtml4;
import static org.clarent.griezel.servlet.BackOfficeServlet.isLoginSuccessful;
import static org.clarent.griezel.servlet.ReservationServlet.setCorsHeaders;
import static org.clarent.griezel.servlet.ReservationServlet.setNoCacheHeaders;

/**
 * @author Guy Mahieu
 * @since 2017-10-13
 */
public class StatusServlet extends HttpServlet {

    private static final String DASHBOARD_USER = System.getenv("DASHBOARD_USER");
    private static final String DASHBOARD_PASSWORD = System.getenv("DASHBOARD_PASSWORD");

    private final SessionFactory sessionFactory;

    public StatusServlet() {
        this.sessionFactory = Application.getSessionFactory();
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doOptions(req, resp);
        setNoCacheHeaders(resp);
        setCorsHeaders(resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        setNoCacheHeaders(resp);
        resp.setContentType("text/html");
        if (!isLoginSuccessful(req, resp, DASHBOARD_USER, DASHBOARD_PASSWORD)) {
            return;
        }

        PrintWriter writer = resp.getWriter();
        writer.print("<html><head>" +
                "    <link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\"\n" +
                "          integrity=\"sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u\" crossorigin=\"anonymous\">\n" +
                "    <!-- Optional theme -->\n" +
                "    <link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css\"\n" +
                "          integrity=\"sha384-rHyoN1iRsVXV4nD0JutlnGaslCJuC7uwjduW9SVrLvRYooPp2bWYgmgJQIXwl/Sp\" crossorigin=\"anonymous\">\n" +
                "</head><body><div class=\"container\">");

        try (Session session = sessionFactory.openSession()) {

            Object[] singleResult = (Object[]) session.createNativeQuery("" +
                    "SELECT sum(nr_of_adults + nr_of_children) AS personen_totaal,\n" +
                    "       sum(nr_of_adults) AS volwassenen_totaal,\n" +
                    "       sum(nr_of_children) AS kinderen_totaal,\n" +
                    "       sum(nr_of_adults_halal) AS volwassenen_halal,\n" +
                    "       sum(nr_of_children_halal) AS kinderen_halal,\n" +
                    "       sum(nr_of_adults_veggie) AS volwassenen_veggie,\n" +
                    "       sum(nr_of_children_veggie) AS kinderen_veggie\n" +
                    "  FROM reservations" +
                    "  WHERE deleted is FALSE  " +
                    "").getSingleResult();

            int personenTotaalNumeric = ((Number) singleResult[0]).intValue();
            int volwassenenTotaalNumeric = ((Number) singleResult[1]).intValue();
            int kinderenTotaalNumeric = ((Number) singleResult[2]).intValue();
            int volwassenenHalalNumeric = ((Number) singleResult[3]).intValue();
            int kinderenHalalNumeric = ((Number) singleResult[4]).intValue();
            int volwassenenVeggieNumeric = ((Number) singleResult[5]).intValue();
            int kinderenVeggieNumeric = ((Number) singleResult[6]).intValue();

            String personenTotaal = unescapeHtml4("" + personenTotaalNumeric);
            String volwassenenTotaal = unescapeHtml4("" + volwassenenTotaalNumeric);
            String kinderenTotaal = unescapeHtml4("" + kinderenTotaalNumeric);
            String volwassenenHalal = unescapeHtml4("" + volwassenenHalalNumeric);
            String kinderenHalal = unescapeHtml4("" + kinderenHalalNumeric);
            String volwassenenVeggie = unescapeHtml4("" + volwassenenVeggieNumeric);
            String kinderenVeggie = unescapeHtml4("" + kinderenVeggieNumeric);


            writer.print("<div>" +
                    "<h2>Personen</h2>" +
                    "<table class=\"table\">" +
                    "<thead>" +
                    "<tr>" +
                    "<th>&nbsp;</th>" +
                    "<th>Totaal</th>" +
                    "<th>Halal</th>" +
                    "<th>Vegetarisch</th>" +
                    "</tr>" +
                    "</thead>" +
                    "<tbody>" +
                    "<tr>" +
                    "<th>Volwassenen</th>" +
                    "<td>" + volwassenenTotaal + "</td>" +
                    "<td>" + volwassenenHalal + "</td>" +
                    "<td>" + volwassenenVeggie + "</td>" +
                    "</tr>" +
                    "<tr>" +
                    "<th>Kinderen</th>" +
                    "<td>" + kinderenTotaal + "</td>" +
                    "<td>" + kinderenHalal + "</td>" +
                    "<td>" + kinderenVeggie + "</td>" +
                    "</tr>" +
                    "<tr>" +
                    "<th>Totaal</th>" +
                    "<th>" + (volwassenenTotaalNumeric + kinderenTotaalNumeric) + "</th>" +
                    "<th>" + (volwassenenHalalNumeric + kinderenHalalNumeric) + "</th>" +
                    "<th>" + (volwassenenVeggieNumeric + kinderenVeggieNumeric) + "</th>" +
                    "</tr>" +
                    "</tbody>" +
                    "</table>" +
                    "</div>");

            writer.print("<div>" +
                    "<h2>Tijdsblokken</h2>" +
                    "<table class=\"table table-hover table-condensed\">" +
                    "<thead>" +
                    "<tr>" +
                    "<th>Starttijd</th>" +
                    "<th>Aantal</th>" +
                    "<th>Maximum</th>" +
                    "<th>Vrij</th>" +
                    "</tr>" +
                    "</thead>" +
                    "<tbody>" +
                    "");

            TimeBlockFillingServlet.getTimeBlockItems(session).forEach(timeBlockItem -> {
                TimeBlock timeBlock = TimeBlock.valueOf(timeBlockItem.getKey());
                writer.print("" +
                        "<tr>" +
                        "<td>" + timeBlockItem.getDisplayValue() + "</td>" +
                        "<td>" + (timeBlock.getMaxPeople() - timeBlockItem.getFreePlaces()) + "</td>" +
                        "<td>" + timeBlock.getMaxPeople() + "</td>" +
                        "<td>" + timeBlockItem.getFreePlaces() + "</td>" +
                        "</tr>");
            });
            writer.print("</tbody></table></div>");

        }
        writer.print("</div></body></html><head>");

    }


}
