package org.clarent.griezel;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.entities.Subsegment;
import com.amazonaws.xray.handlers.TracingHandler;
import com.amazonaws.xray.javax.servlet.AWSXRayServletFilter;
import com.amazonaws.xray.plugins.ElasticBeanstalkPlugin;
import org.clarent.griezel.model.Reservation;
import org.clarent.griezel.servlet.BackOfficeServlet;
import org.clarent.griezel.servlet.ReservationServlet;
import org.clarent.griezel.servlet.StatusServlet;
import org.clarent.griezel.servlet.TimeBlockFillingServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHandler;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Application {

    private static final String OBJECT_KEY = "eb-registrations.zip";
    private static final String BUCKET_NAME;
    private static final AmazonS3 s3Client;

    private static final Logger logger = Logger.getLogger("APPLICATION");

    // Initialize the global AWS XRay Recorder with the Elastic Beanstalk plugin to trace environment metadata

    private static SessionFactory sessionFactory;

    static {
        if (isLocalMode()) {
            BUCKET_NAME = "local";
            s3Client = null;
        } else {
            BUCKET_NAME = String.format("eb-registrations-%s", Regions.getCurrentRegion().getName());
            AWSXRayRecorderBuilder builder = AWSXRayRecorderBuilder.standard().withPlugin(new ElasticBeanstalkPlugin());
            AWSXRay.setGlobalRecorder(builder.build());
            s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(Regions.getCurrentRegion().getName())
                    .withRequestHandlers(new TracingHandler())
                    .build();

        }
    }

    public static boolean isLocalMode() {
        return "TRUE".equals(System.getenv("LOCAL_MODE"));
    }

    // Create AWS clients instrumented with AWS XRay tracing handler


    private static int getPort() {
        return Integer.parseInt(System.getenv().get("PORT"));
    }

    private static boolean isXRayEnabled() {
        return Boolean.valueOf(System.getenv("XRAY_ENABLED"));
    }

    public static void main(String[] args) throws Exception {
        Configuration configuration = configurePeristence();
        logger.log(Level.INFO, "!!!! HIBERNATE CONFIGURED");
        sessionFactory = configuration.buildSessionFactory();
        logger.log(Level.INFO, "!!!! SESSIONFACTORY");

        Server server = new Server(getPort());
        ServletHandler handler = new ServletHandler();


        handler.addServletWithMapping(TraceServlet.class, "/trace");
        handler.addServletWithMapping(ReservationServlet.class, "/api/reservation");
        handler.addServletWithMapping(TimeBlockFillingServlet.class, "/api/timeblocks");
        handler.addServletWithMapping(BackOfficeServlet.class, "/api/backoffice");
        handler.addServletWithMapping(StatusServlet.class, "/werkgroep");
        handler.addServletWithMapping(HomeServlet.class, "/*");

//        handler.addServletWithMapping(CronServlet.class, "/crontask");
        if (isXRayEnabled()) {
            FilterHolder filterHolder = handler.addFilterWithMapping(AWSXRayServletFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
            filterHolder.setInitParameter("dynamicNamingFallbackName", "ElasticBeanstalkSample");
            filterHolder.setInitParameter("dynamicNamingRecognizedHosts", "*");
        }
        server.setHandler(handler);
        server.start();
        logger.log(Level.INFO, "!!!! JETTY RUNNING");
        server.join();
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    private static Configuration configurePeristence() {
        String dbName = System.getenv("RDS_DB_NAME");
        String userName = System.getenv("RDS_USERNAME");
        String password = System.getenv("RDS_PASSWORD");
        String hostname = System.getenv("RDS_HOSTNAME");
        String port = System.getenv("RDS_PORT");
        String jdbcUrl = "jdbc:postgresql://" + hostname + ":" + port + "/" + dbName + "?user=" + userName + "&password=" + password;
        return new Configuration()
                .addAnnotatedClass(Reservation.class)
                .setProperty("hibernate.show_sql", "true")
                .setProperty("show_sql", "true")
                .setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQL95Dialect")
                .setProperty("hibernate.connection.driver_class", "org.postgresql.Driver")
                .setProperty("hibernate.connection.url", jdbcUrl)
                .setProperty("hibernate.connection.pool_size", "10") //maximum number of pooled connections
                .setProperty("hibernate.order_updates", "true");
    }

    public static class HomeServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
            response.setHeader("Location", "/ui/index.html");
        }
    }

    public static class CronServlet extends HttpServlet {
        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().println("Process Task Here.");
        }
    }

    public static class TraceServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            if (isXRayEnabled() && !isLocalMode()) {
                traceS3();
            }
        }

        private void traceS3() {
            // Add subsegment to current request to track call to S3
            Subsegment subsegment = AWSXRay.beginSubsegment("## Getting object metadata");
            try {
                // Gets metadata about this sample app object in S3
                s3Client.getObjectMetadata(BUCKET_NAME, OBJECT_KEY);
            } catch (Exception ex) {
                subsegment.addException(ex);
            } finally {
                AWSXRay.endSubsegment();
            }
        }
    }
}