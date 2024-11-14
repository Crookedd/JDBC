package org.example;
import java.sql.*;
import java.util.Scanner;

import static org.example.BooksCreate.*;

public class BookManager {
    private static final String URL = "jdbc:postgresql://localhost:5432/java";
    private static final String USER = "postgres";
    private static final String PASSWORD = "123456";

    public static void main(String[] args) {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {

            Scanner scanner = new Scanner(System.in);
            int choice;
            do {
                System.out.println("Выберите действие:");
                System.out.println("1. Получить отсортированный список книг по году издания");
                System.out.println("2. Вывести книги младше 2000 года");
                System.out.println("3. Вывести информацию о себе и любимые книги");
                System.out.println("4. Удалить таблицы");
                System.out.println("5. Создать таблицы");
                System.out.println("6. Выход");
                choice = scanner.nextInt();
                scanner.nextLine(); // Очистка буфера

                switch (choice) {
                    case 1:
                        getSortedBookList(connection);
                        break;
                    case 2:
                        getBooksBeforeYear2000(connection);
                        break;
                    case 3:
                        addPersonalInfo(connection);
                        break;
                    case 4:
                        dropTable(connection);
                        break;
                    case 5:
                        // Проверяем, существуют ли таблицы
                        if (tableExists(connection, "visitors")) {
                            createVisitorsTable(connection);
                        }
                        if (tableExists(connection, "books")) {
                            createBooksTable(connection);
                        }
                        if (tableExists(connection, "visitor_books")) {
                            createVisitorBooksTable(connection);
                        }

                        // Добавляем данные из JSON-файла
                        addVisitorsAndBooks(connection);
                        break;
                    case 6:
                        System.out.println("Выход из программы.");
                        break;
                    default:
                        System.out.println("Неверный выбор. Попробуйте еще раз.");
                        break;
                }
            } while (choice != 6);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void dropTable(Connection connection) {
        String dropBVTable = "DROP TABLE visitor_books";
        String dropVisitorsTable = "DROP TABLE visitors";
        String dropBooksTable = "DROP TABLE books";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(dropBVTable);
            statement.executeUpdate(dropVisitorsTable);
            statement.executeUpdate(dropBooksTable);
            System.out.println("Tables 'visitors' and 'books' have been dropped.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet resultSet = metaData.getTables(null, null, tableName, null);
        return !resultSet.next();
    }

    static void  getSortedBookList(Connection connection) {
        String selectBooksQuery = "SELECT * FROM books ORDER BY publishingYear ASC";
        try (PreparedStatement selectStatement = connection.prepareStatement(selectBooksQuery);
             ResultSet resultSet = selectStatement.executeQuery()) {
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                String author = resultSet.getString("author");
                int publishingYear = resultSet.getInt("publishingYear");
                String isbn = resultSet.getString("isbn");
                String publisher = resultSet.getString("publisher");
                System.out.printf("ID: %d, Name: %s, Author: %s, Publishing Year: %d, ISBN: %s, Publisher: %s%n", id, name, author, publishingYear, isbn, publisher);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void getBooksBeforeYear2000(Connection connection) {
        String selectBooksQuery = "SELECT * FROM books WHERE publishingYear < 2000";
        try (PreparedStatement selectStatement = connection.prepareStatement(selectBooksQuery);
             ResultSet resultSet = selectStatement.executeQuery()) {
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                String author = resultSet.getString("author");
                int publishingYear = resultSet.getInt("publishingYear");
                String isbn = resultSet.getString("isbn");
                String publisher = resultSet.getString("publisher");
                System.out.printf("ID: %d, Name: %s, Author: %s, Publishing Year: %d, ISBN: %s, Publisher: %s%n", id, name, author, publishingYear, isbn, publisher);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

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
        scanner.nextLine(); // Очистка буфера

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

        for (int i = 1; i <= 2; i++) {
            System.out.printf("Введите информацию о любимой книге #%d:%n", i);

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
            scanner.nextLine(); // Очистка буфера

            // Вставка новой книги и получение её ID
            int bookId = addNewBook(connection, title, author, publishingYear, isbn, publisher);

            // Связываем книгу с пользователем
            linkBookToVisitor(connection, visitorId, bookId);
        }
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


