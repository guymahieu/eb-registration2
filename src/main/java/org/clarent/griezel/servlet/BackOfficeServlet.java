package org.clarent.griezel.servlet;

import com.google.gson.Gson;
import org.apache.commons.codec.binary.Base64;
import org.clarent.griezel.Application;
import org.clarent.griezel.model.Reservation;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import javax.persistence.NoResultException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.clarent.griezel.servlet.ReservationServlet.setCorsHeaders;
import static org.clarent.griezel.servlet.ReservationServlet.setNoCacheHeaders;

/**
 * @author Guy Mahieu
 * @since 2017-10-04
 */
public class BackOfficeServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger("APPLICATION");

    private static final String BACKOFFICE_USER = System.getenv("BACKOFFICE_USER");
    private static final String BACKOFFICE_PASSWORD = System.getenv("BACKOFFICE_PASSWORD");

    private final SessionFactory sessionFactory;

    public BackOfficeServlet() {
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
        if (!isLoginSuccessful(req, resp, BACKOFFICE_USER, BACKOFFICE_PASSWORD)) {
            return;
        }

        resp.setContentType("application/json");
        try (Session session = sessionFactory.openSession()) {
            @SuppressWarnings("unchecked")
            List<Reservation> reservations = (List<Reservation>) session.createQuery(" " +
                            "select r " +
                            "  from Reservation r " +
//                  " where r.deleted = false " +
                            " order by r.id "
            ).list();

            Gson gson = new Gson();
            gson.toJson(reservations, resp.getWriter());
            resp.setStatus(HttpServletResponse.SC_OK);
        }
    }


    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        setCorsHeaders(resp);
        setNoCacheHeaders(resp);
        resp.setContentType("application/json");
        if (!isLoginSuccessful(req, resp, BACKOFFICE_USER, BACKOFFICE_PASSWORD)) {
            return;
        }
//        if (req.getRequestURI().endsWith("/registerPayment")) {
        try (Session session = sessionFactory.openSession()) {
//                PaymentRegistration paymentRegistration = new Gson().fromJson(req.getReader(), PaymentRegistration.class);

            String externalId = req.getParameter("externalId");
            boolean paid = "true".equalsIgnoreCase(req.getParameter("paid"));
            if (isBlank(externalId)) {
                throw new RuntimeException("No external id in post data.");
            }
            Transaction transaction = session.beginTransaction();
            try {
                Reservation reservation = session.createQuery(" " +
                                " select r from Reservation r " +
                                "  where r.externalId = :externalId "
//                                    +"    and r.deleted = false "
                        , Reservation.class)
                        .setParameter("externalId", externalId)
                        .getSingleResult();
                logger.info("Payment registered with value " + paid + " for reservation " + reservation);
                reservation.setPaid(paid);
                session.flush();
                Gson gson = new Gson();
                gson.toJson(reservation, resp.getWriter());
                resp.setStatus(HttpServletResponse.SC_OK);
            } catch (NoResultException e) {
                e.printStackTrace();
                transaction.rollback();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (Exception e) {
                e.printStackTrace();
                transaction.rollback();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            transaction.commit();
        }
//        }
    }

    public static boolean isLoginSuccessful(HttpServletRequest req, HttpServletResponse resp, String backofficeUser, String backofficePassword) {
        Boolean authenticated = login(req, backofficeUser, backofficePassword);
        if (authenticated == null) {
            //WWW-Authenticate: Basic realm="Access to the staging site"
            resp.setHeader("WWW-Authenticate", "Basic realm=\"Toegang tot backoffice griezeltocht.\"");
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        if (!authenticated) {
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return false;
        }
        return true;
    }

    public static Boolean login(HttpServletRequest req, String expectedUser, String expectedPassword) {
        if (isBlank(BACKOFFICE_USER) || isBlank(BACKOFFICE_PASSWORD)) {
            return false;
        }
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null) {
            return null;
        }
        StringTokenizer st = new StringTokenizer(authHeader);
        if (st.hasMoreTokens()) {
            String basic = st.nextToken();
            if (basic.equalsIgnoreCase("Basic")) {
                try {
                    String credentials = new String(Base64.decodeBase64(st.nextToken()), "UTF-8");
                    int p = credentials.indexOf(":");
                    if (p != -1) {
                        String login = credentials.substring(0, p).trim();
                        String password = credentials.substring(p + 1).trim();
                        return expectedUser.equalsIgnoreCase(login) && expectedPassword.equals(password);
                    }
                } catch (UnsupportedEncodingException e) {
                }
            }
        }
        return false;
    }

    private static class PaymentRegistration {
        String externalId;
    }

}
