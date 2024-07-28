package doc.test2;

import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.Properties;
import java.util.function.Function;

public class Server {

    HttpServer httpServer;
    Handler handler;

    static Connection database;

    enum Method {
        POST,
        GET,
        DELETE
    }

    public static void main(String[] args) {
        String config = "config.properties";
        Properties properties = new Properties();
        try (FileReader reader = new FileReader(config)) {
            properties.load(reader);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Properties connProps = new Properties();
        String url = properties.getProperty("db.url");
        connProps.put("user", properties.getProperty("db.user"));
        connProps.put("password", properties.getProperty("db.password"));
        try {
            database = DriverManager.getConnection(url, connProps);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                database.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }));

        try (Statement statement = database.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS student (id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, name TEXT, fam TEXT, otch TEXT, bdate DATE, team TEXT)");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        Server server = new Server();
        server.createContexts();
        server.start();
    }

    public Server() {
        try {
            httpServer = HttpServer.create(new InetSocketAddress("localhost", 80), 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        handler = new Handler();
    }

    void createContexts() {
        addContext("/", Method.GET, handler::root, handler::asText);
        addContext("/api/create/", Method.POST, handler::create, handler::asJson);
        addContext("/api/read/", Method.GET, handler::read, handler::asJson);
        addContext("/api/delete/", Method.DELETE, handler::delete, handler::asJson);
    }

    void addContext(String path, Method method, Handler.Worker worker, Function<Handler.Response, Handler.Response> formatter) {
        httpServer.createContext(path, exchange -> handler.handle(method, worker, exchange, formatter));
    }

    void start() {
        httpServer.start();
    }

}
