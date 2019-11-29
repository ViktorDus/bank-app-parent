package ru.vdusanyuk.bank;


import ru.vdusanyuk.bank.rest.EntryPoint;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

/**
 * The application to run banking service based on embedded jetty http server
 */

public class Application {

    private static final String DEFAULT_PORT = "8090";
    private static final String DEFAULT_MAX_THREADS = "100";
    private static final String DEFAULT_MIN_THREADS = "5";
    private static final String DEFAULT_IDLE_TIMEOUT = "120";

    private static Server server;

    public static void main(String[] args) throws Exception {

        String port = args != null && args.length > 0 ? args[0] : DEFAULT_PORT;

        Properties properties = loadApplicationProperties();
        server = configureServer(Integer.valueOf(port),
                                 properties);
        try {
            server.start();
            server.join();
        } finally {
            server.destroy();
        }
    }

    private static Server configureServer(int port, Properties properties) {

        QueuedThreadPool threadPool = configureThreadPool(properties);

        Server server = new Server(threadPool);
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        server.setConnectors(new Connector[]{connector});

        ServletContextHandler servletContextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletContextHandler.setContextPath("/");
        server.setHandler(servletContextHandler);

        ServletHolder jerseyServlet = servletContextHandler.addServlet(
                org.glassfish.jersey.servlet.ServletContainer.class, "/*");
        jerseyServlet.setInitOrder(0);

        jerseyServlet.setInitParameter(
                "jersey.config.server.provider.classnames",
                EntryPoint.class.getCanonicalName());
        return server;
    }

    private static QueuedThreadPool configureThreadPool(Properties properties) {
        int maxThreads = Integer.valueOf(properties.getProperty("server.maxThreads", DEFAULT_MAX_THREADS));
        int minThreads = Integer.valueOf(properties.getProperty("server.minThreads", DEFAULT_MIN_THREADS));
        int idleTimeout = Integer.valueOf(properties.getProperty("server.idleTimeout", DEFAULT_IDLE_TIMEOUT));
        return new QueuedThreadPool(maxThreads, minThreads, idleTimeout);
    }

    private static Properties loadApplicationProperties() throws IOException {

        Properties properties = new Properties();
        try (InputStream input = Application.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.out.println("Unable to find application.properties");
            }

            //load a properties file from class path, inside static method
            properties.load(input);
        }
        return properties;
    }

    static boolean waitForStarted() throws InterruptedException {
        long startStamp = System.currentTimeMillis();
        while (server != null && server.isStarting() && startStamp + 10000 > System.currentTimeMillis() ) {
            Thread.sleep(500);
        }
        return server != null && server.isStarted();
    }

    static boolean isRunning() {
        return server != null && server.isRunning();
    }

    static URI getURI() {
        return server != null && server.isStarted() ? server.getURI() : URI.create("");
    }

    static void stop()  {
        if (server != null && server.isStarted()) {
            try {
                server.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }



}
