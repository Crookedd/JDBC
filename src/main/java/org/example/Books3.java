package org.example;

import java.sql.*;
import java.util.Scanner;

public class Books3 {

    static void addPersonalInfo(Connection connection) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("Введите имя: ");
        String name = scanner.nextLine();

        System.out.print("Введите фамилию: ");
        String surname = scanner.nextLine();

        System.out.print("Введите телефон: ");
        String phone = scanner.nextLine();

        System.out.print("Подписаться на рассылку (true/false): ");
        boolean subscribed = scanner.nextBoolean();
        scanner.nextLine();

        String insertVisitorQuery = "INSERT INTO visitors (name, surname, phone, subscribed) VALUES (?, ?, ?, ?)";
        try (PreparedStatement insertStatement = connection.prepareStatement(insertVisitorQuery, Statement.RETURN_GENERATED_KEYS)) {
            insertStatement.setString(1, name);
            insertStatement.setString(2, surname);
            insertStatement.setString(3, phone);
            insertStatement.setBoolean(4, subscribed);
            insertStatement.executeUpdate();

            try (ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int visitorId = generatedKeys.getInt(1);
                    System.out.printf("Visitor ID: %d%n", visitorId);

                    // Добавление любимых книг
                    System.out.print("Введите информацию о любимой книге");
                    addFavoriteBooks(connection, visitorId);

                    // Вывод информации о пользователе и его любимых книгах
                    displayVisitorInfo(connection, visitorId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void addFavoriteBooks(Connection connection, int visitorId) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Название книги: ");
        String title = scanner.nextLine();

        System.out.print("Автор книги: ");
        String author = scanner.nextLine();

        System.out.print("Год издания: ");
        int publishingYear = scanner.nextInt();

        System.out.print("ISBN: ");
        String isbn = scanner.next();

        System.out.print("Издатель: ");
        String publisher = scanner.next();
        scanner.nextLine();

        // Вставка новой книги и получение её ID
        int bookId = addNewBook(connection, title, author, publishingYear, isbn, publisher);

        // Связываем книгу с пользователем
        linkBookToVisitor(connection, visitorId, bookId);

    }

    static int addNewBook(Connection connection, String title, String author, int publishingYear, String isbn, String publisher) {
        String insertBookQuery = "INSERT INTO books (name, author, publishingYear, isbn, publisher) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement insertBookStatement = connection.prepareStatement(insertBookQuery, Statement.RETURN_GENERATED_KEYS)) {
            insertBookStatement.setString(1, title);
            insertBookStatement.setString(2, author);
            insertBookStatement.setInt(3, publishingYear);
            insertBookStatement.setString(4, isbn);
            insertBookStatement.setString(5, publisher);
            insertBookStatement.executeUpdate();

            // Получаем сгенерированный ID книги
            try (ResultSet generatedKeys = insertBookStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Если не удалось получить ID
    }

    static void linkBookToVisitor(Connection connection, int visitorId, int bookId) {
        String insertBookIds = "INSERT INTO visitor_books (visitor_id, book_id) VALUES (?, ?)";
        try (PreparedStatement insertBookIdsStatement = connection.prepareStatement(insertBookIds)) {
            insertBookIdsStatement.setInt(1, visitorId);
            insertBookIdsStatement.setInt(2, bookId);
            insertBookIdsStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void displayVisitorInfo(Connection connection, int visitorId) {
        String query = "SELECT v.name, v.surname, v.phone, b.name AS title FROM visitors v " +
                "JOIN visitor_books vb ON v.id = vb.visitor_id " +
                "JOIN books b ON vb.book_id = b.id " +
                "WHERE v.id = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, visitorId);
            ResultSet resultSet = statement.executeQuery();

            System.out.println("Информация о пользователе:");
            if (resultSet.next()) {
                System.out.printf("Имя: %s%n", resultSet.getString("name"));
                System.out.printf("Фамилия: %s%n", resultSet.getString("surname"));
                System.out.printf("Телефон: %s%n", resultSet.getString("phone"));
                System.out.println("Любимые книги:");
                do {
                    System.out.printf("- %s%n", resultSet.getString("title"));
                } while (resultSet.next());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
