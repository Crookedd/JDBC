package org.example;
import java.sql.*;

public class MusicDatabase {
    private static final String URL = "jdbc:postgresql://localhost:5432/java";
    private static final String USER = "postgres";
    private static final String PASSWORD = "123456";

    public static void main(String[] args) {
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            String selectQuery = "SELECT name FROM javamus WHERE name NOT ILIKE '%m%' AND name NOT ILIKE '%t%'";
            try (PreparedStatement selectStatement = connection.prepareStatement(selectQuery);
                 ResultSet resultSet = selectStatement.executeQuery()) {

                while (resultSet.next()) {
                    String name = resultSet.getString("name");
                    System.out.printf("Name: %s%n", name);
                }
            }
            // 2. Добавить любимую композицию
            String checkQuery = "SELECT COUNT(*) FROM javamus WHERE name = ?";
            try (PreparedStatement checkStatement = connection.prepareStatement(checkQuery)) {
                checkStatement.setString(1, "CRY BABY");
                try (ResultSet checkResult = checkStatement.executeQuery()) {
                    if (checkResult.next() && checkResult.getInt(1) == 0) {
                        // Получить максимальный id из таблицы
                        String maxIdQuery = "SELECT MAX(id) FROM javamus";
                        int newId;
                        try (Statement maxIdStatement = connection.createStatement();
                             ResultSet maxIdResult = maxIdStatement.executeQuery(maxIdQuery)) {
                            if (maxIdResult.next()) {
                                newId = maxIdResult.getInt(1) + 1;
                            } else {
                                newId = 1;
                            }
                        }

                        // Вставить новую композицию
                        String insertQuery = "INSERT INTO javamus (id, name) VALUES (?, ?)";
                        try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                            insertStatement.setInt(1, newId);
                            insertStatement.setString(2, "CRY BABY");
                            System.out.printf("Композиция добавлена с id: %d%n", newId);
                        }
                    } else {
                        System.out.println("Композиция 'CRY BABY' уже существует в базе данных.");
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.printf("Ошибка при добавлении композиции: %s%n", e.getMessage());
        }
    }
}
