package org.example;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class BooksCreate {

    static void createVisitorsTable(Connection connection) throws SQLException {
        String createTableQuery = "CREATE TABLE visitors (" +
                "id SERIAL PRIMARY KEY," +
                "name VARCHAR(255) NOT NULL," +
                "surname VARCHAR(255) NOT NULL," +
                "phone VARCHAR(20) NOT NULL," +
                "subscribed BOOLEAN NOT NULL)";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(createTableQuery);
            System.out.println("Таблица visitors создана.");
        }
    }

    static void createBooksTable(Connection connection) throws SQLException {
        String createTableQuery = "CREATE TABLE books (" +
                "id SERIAL PRIMARY KEY," +
                "name VARCHAR(255) NOT NULL," +
                "author VARCHAR(255) NOT NULL," +
                "publishingYear INTEGER NOT NULL," +
                "isbn VARCHAR(20) NOT NULL," +
                "publisher VARCHAR(255) NOT NULL)";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(createTableQuery);
            System.out.println("Таблица books создана.");
        }
    }

    static void createVisitorBooksTable(Connection connection) throws SQLException {
        String createTableQuery = "CREATE TABLE visitor_books (" +
                "visitor_id INTEGER NOT NULL," +
                "book_id INTEGER NOT NULL," +
                "FOREIGN KEY (visitor_id) REFERENCES visitors(id)," +
                "FOREIGN KEY (book_id) REFERENCES books(id)," +
                "PRIMARY KEY (visitor_id, book_id))";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(createTableQuery);
            System.out.println("Таблица visitor_books создана.");
        }
    }

    static void addVisitorsAndBooks(Connection connection) throws SQLException {
        String jsonData = readJsonFromFile();
        if (jsonData == null) {
            System.out.println("Error reading JSON file.");
            return;
        }

        Gson gson = new Gson();
        JsonArray jsonArray = gson.fromJson(jsonData, JsonArray.class);

        Set<Integer> uniqueVisitorIds = new HashSet<>();
        Set<Integer> uniqueBookIds = new HashSet<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject visitor = jsonArray.get(i).getAsJsonObject();

            // Добавляем посетителя, если он уникален
            int visitorId = addUniqueVisitor(connection, visitor);
            if (visitorId != -1) {
                uniqueVisitorIds.add(visitorId);
            }

            // Добавляем книги посетителя, если они уникальны
            JsonArray favoriteBooks = visitor.getAsJsonArray("favoriteBooks");
            for (int j = 0; j < favoriteBooks.size(); j++) {
                JsonObject book = favoriteBooks.get(j).getAsJsonObject();
                int bookId = addUniqueBook(connection, book);
                if (bookId != -1) {
                    uniqueBookIds.add(bookId);
                    addVisitorBook(connection, visitorId, bookId);
                }
            }
        }

        System.out.printf("Уникальные посетители: %d%n", uniqueVisitorIds.size());
        System.out.printf("Уникальные книги: %d%n", uniqueBookIds.size());
    }

    private static String readJsonFromFile() {
        StringBuilder jsonContent = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(BookManager.class.getResourceAsStream("/books.json")), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return jsonContent.toString();
    }

    private static int addUniqueVisitor(Connection connection, JsonObject visitor) throws SQLException {
        String name = visitor.get("name").getAsString();
        String surname = visitor.get("surname").getAsString();
        String phone = visitor.get("phone").getAsString();
        boolean subscribed = visitor.get("subscribed").getAsBoolean();

        String checkVisitorQuery = "SELECT id FROM visitors WHERE name = ? AND surname = ? AND phone = ?";
        try (PreparedStatement checkStatement = connection.prepareStatement(checkVisitorQuery)) {
            checkStatement.setString(1, name);
            checkStatement.setString(2, surname);
            checkStatement.setString(3, phone);
            try (ResultSet resultSet = checkStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }
        }

        String insertVisitorQuery = "INSERT INTO visitors (name, surname, phone, subscribed) VALUES (?, ?, ?, ?)";
        try (PreparedStatement insertStatement = connection.prepareStatement(insertVisitorQuery, Statement.RETURN_GENERATED_KEYS)) {
            insertStatement.setString(1, name);
            insertStatement.setString(2, surname);
            insertStatement.setString(3, phone);
            insertStatement.setBoolean(4, subscribed);
            int rowsAffected = insertStatement.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        }
        return -1;
    }

    private static int addUniqueBook(Connection connection, JsonObject book) throws SQLException {
        String name = book.get("name").getAsString();
        String author = book.get("author").getAsString();
        int publishingYear = book.get("publishingYear").getAsInt();
        String isbn = book.get("isbn").getAsString();
        String publisher = book.get("publisher").getAsString();

        String checkBookQuery = "SELECT id FROM books WHERE name = ? AND author = ? AND publishingYear = ? AND isbn = ? AND publisher = ?";
        try (PreparedStatement checkStatement = connection.prepareStatement(checkBookQuery)) {
            checkStatement.setString(1, name);
            checkStatement.setString(2, author);
            checkStatement.setInt(3, publishingYear);
            checkStatement.setString(4, isbn);
            checkStatement.setString(5, publisher);
            try (ResultSet resultSet = checkStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }
        }

        String insertBookQuery = "INSERT INTO books (name, author, publishingYear, isbn, publisher) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement insertStatement = connection.prepareStatement(insertBookQuery, Statement.RETURN_GENERATED_KEYS)) {
            insertStatement.setString(1, name);
            insertStatement.setString(2, author);
            insertStatement.setInt(3, publishingYear);
            insertStatement.setString(4, isbn);
            insertStatement.setString(5, publisher);
            int rowsAffected = insertStatement.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        }
        return -1;
    }

    private static void addVisitorBook(Connection connection, int visitorId, int bookId) throws SQLException {
        String insertVisitorBookQuery = "INSERT INTO visitor_books (visitor_id, book_id) VALUES (?, ?)";
        try (PreparedStatement insertStatement = connection.prepareStatement(insertVisitorBookQuery)) {
            insertStatement.setInt(1, visitorId);
            insertStatement.setInt(2, bookId);
            insertStatement.executeUpdate();
        }
    }
}
