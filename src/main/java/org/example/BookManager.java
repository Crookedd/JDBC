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
                        Books1.getSortedBookList(connection);
                        break;
                    case 2:
                        Books2.getBooksBeforeYear2000(connection);
                        break;
                    case 3:
                        Books3.addPersonalInfo(connection);
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
            System.out.println("Таблицы 'visitors' и 'books' удалены.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        ResultSet resultSet = metaData.getTables(null, null, tableName, null);
        return !resultSet.next();
    }
}


