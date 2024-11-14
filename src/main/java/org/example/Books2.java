package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Books2 {
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
}
