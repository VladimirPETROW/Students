package doc.test2;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Handler {

    static Logger log = Logger.getLogger(Handler.class.getName());

    class Response {
        public int code;
        public Object message;

        Response(int c, Object m) {
            code = c;
            message = m;
        }
    }

    interface Worker {
        Response process(HttpExchange exchange) throws IOException, SQLException;
    }

    Response createResponse(int code) {
        String message = "";
        switch (code) {
            case 400:
                message = "Bad Request";
                break;
            case 405:
                message = "Method Not Allowed";
                break;
        }
        return new Response(code, message);
    }

    Response asJson(Response response) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            if (response.message instanceof String) {
                response.message = mapper.writeValueAsString(response);
            } else {
                mapper.registerModule(new JavaTimeModule());
                response.message = mapper.writeValueAsString(response.message);
            }
        } catch (JsonProcessingException e) {
            String message = "Ошибка сериализации.";
            log.log(Level.SEVERE, message, e);
            int code = 500;
            return new Response(code, String.format("{\"code\":%d,\"message\":\"%s\"}", code, message));
        }
        return response;
    }

    Response asText(Response response) {
        return response;
    }

    void sendText(int code, String text, HttpExchange exchange) throws IOException {
        byte[] bytes = text.getBytes();
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream out = exchange.getResponseBody();
        out.write(bytes);
        out.close();
    }

    public void handle(Server.Method method, Worker worker, HttpExchange exchange, Function<Response, Response> formatter) throws IOException {
        Response response;
        if (!exchange.getRequestMethod().equals(method.toString())) {
            response = createResponse(405);
        }
        else {
            try {
                response = worker.process(exchange);
            } catch (Exception e) {
                log.log(Level.SEVERE, "", e);
                response = createResponse(400);
            }
        }
        response = formatter.apply(response);
        sendText(response.code, response.message.toString(), exchange);
    }

    Response root(HttpExchange exchange) {
        return new Response(200, "Ok");
    }

    Response create(HttpExchange exchange) throws IOException, SQLException {
        InputStreamReader input = new InputStreamReader(exchange.getRequestBody());
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        Student student = mapper.readValue(input, Student.class);
        StringBuffer error = new StringBuffer();
        if (student.name == null) {
            error.append("Не указано имя.");
        }
        if (student.bdate == null) {
            if (error.length() > 0) {
                error.append(" ");
            }
            error.append("Не указана дата рождения.");
        }
        if (error.length() > 0) {
            return new Response(400, error.toString());
        }
        try (PreparedStatement statement = Server.database.prepareStatement("INSERT INTO student (name, fam, otch, bdate, team) VALUES (?, ?, ?, ?, ?) RETURNING id")) {
            statement.setString(1, student.name);
            statement.setString(2, student.fam);
            statement.setString(3, student.otch);
            statement.setDate(4, Date.valueOf(student.bdate));
            statement.setString(5, student.team);
            ResultSet rs = statement.executeQuery();
            rs.next();
            Long id = rs.getLong(1);
            String message = String.format("Student %d added.", id);
            log.info(message);
            return new Response(200, message);
        }
    }

    Response read(HttpExchange exchange) throws SQLException {
        try (Statement statement = Server.database.createStatement()) {
            ArrayList<Student> students = new ArrayList<>();
            ResultSet rs = statement.executeQuery("SELECT id, name, fam, otch, bdate, team FROM student");
            while (rs.next()) {
                Student student = new Student();
                student.id = rs.getLong(1);
                student.name = rs.getString(2);
                student.fam = rs.getString(3);
                student.otch = rs.getString(4);
                student.bdate = rs.getDate(5).toLocalDate();
                student.team = rs.getString(6);
                students.add(student);
            }
            return new Response(200, students.toArray());
        }
    }

    Response delete(HttpExchange exchange) throws SQLException {
        String start = exchange.getHttpContext().getPath();
        String request = exchange.getRequestURI().getPath();
        if (!request.startsWith(start)) {
            String message = "Неверный путь.";
            log.severe(message);
            return new Response(500, message);
        }
        String last = request.substring(start.length());
        Long id = Long.parseLong(last);
        try (PreparedStatement statement = Server.database.prepareStatement("DELETE FROM student WHERE id=?")) {
            statement.setLong(1, id);
            String message;
            if (statement.executeUpdate() > 0) {
                message = String.format("Студент %d удален.", id);
            }
            else {
                message = String.format("Студент %d не найден.", id);
            }
            log.info(message);
            return new Response(200, message);
        }
    }

}