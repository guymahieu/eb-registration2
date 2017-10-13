package org.clarent.griezel.servlet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.clarent.griezel.Application;
import org.clarent.griezel.model.TimeBlock;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.clarent.griezel.servlet.ReservationServlet.setCorsHeaders;
import static org.clarent.griezel.servlet.ReservationServlet.setNoCacheHeaders;

/**
 * @author Guy Mahieu
 * @since 2017-09-26
 */
public class TimeBlockFillingServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger("APPLICATION");

    private final SessionFactory sessionFactory;

    public TimeBlockFillingServlet() {
        this.sessionFactory = Application.getSessionFactory();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        setCorsHeaders(response);
        setNoCacheHeaders(response);

        try {
            try (Session session = sessionFactory.openSession()) {

                List<TimeBlockItem> result = getTimeBlockItems(session);


                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                response.getWriter().print(gson.toJson(result));
                response.setStatus(HttpServletResponse.SC_OK);
            }
        } catch (Exception e) {
            response.getWriter().print("{\"error;\", \""+e.toString()+"\"}");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<TimeBlockItem> getTimeBlockItems(Session session) {
        EnumMap<TimeBlock, Integer> fillings = new EnumMap<>(TimeBlock.class);
        ((List<Object[]>) session.createNativeQuery("" +
                " SELECT r.time_block, sum(r.nr_of_children + r.nr_of_adults) " +
                "   FROM reservations r " +
                "  WHERE deleted is FALSE  " +
                "  GROUP BY r.time_block " +
                "  ORDER BY r.time_block ")
                .list())
                .forEach(objects -> {
                    TimeBlock timeBlock = TimeBlock.valueOf((String) objects[0]);
                    int filling = ((BigInteger) objects[1]).intValue();
                    fillings.put(timeBlock, Math.max(timeBlock.getMaxPeople() - filling, 0));
                });

        return Arrays.stream(TimeBlock.values())
                .map(timeBlock -> {
                    if (fillings.containsKey(timeBlock)) {
                        return new TimeBlockItem(timeBlock.name(), timeBlock.getDisplay(), fillings.get(timeBlock));
                    } else {
                        return new TimeBlockItem(timeBlock.name(), timeBlock.getDisplay(), timeBlock.getMaxPeople());
                    }
                })
                .collect(Collectors.toList());
    }

    public static class TimeBlockItem {

        private String key;
        private String displayValue;
        private int freePlaces;

        public TimeBlockItem(String key, String displayValue, int freePlaces) {
            this.key = key;
            this.displayValue = displayValue;
            this.freePlaces = freePlaces;
        }

        public String getKey() {
            return key;
        }

        public String getDisplayValue() {
            return displayValue;
        }

        public int getFreePlaces() {
            return freePlaces;
        }
    }


}
