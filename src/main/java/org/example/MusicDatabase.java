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
                    System.out.println("Name: " + name);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Ошибка при добавлении композиции: " + e.getMessage());
        }
    }
}
