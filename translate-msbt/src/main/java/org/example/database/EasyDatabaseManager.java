package org.example.database;

import org.example.utils.ThrowingInterfaces;
import org.example.utils.ThrowingInterfaces.ThrowingSQLConsumer;
import org.example.utils.ThrowingInterfaces.ThrowingSQLFunction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EasyDatabaseManager {
    String jdbcUrl = "jdbc:postgresql://localhost:5432/postgres";
    String username = "postgres";
    String password = "postgres";

    public static Connection getConnection(String jdbcUrl, String username, String password) {
        try {
            return DriverManager.getConnection(jdbcUrl, username, password);
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T singleResult(String query, ThrowingSQLFunction<ResultSet, T> fn) {
        try (Connection connection = getConnection(jdbcUrl, username, password)) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    return fn.apply(resultSet);
                }
                else {
                    return null;
                }
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> List<T> queryList(String query, ThrowingSQLFunction<ResultSet, T> fn) {
        try (Connection connection = getConnection(jdbcUrl, username, password)) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                ResultSet resultSet = statement.executeQuery();

                List<T> results = new ArrayList<>();

                while (resultSet.next()) {
                    results.add(fn.apply(resultSet));
                }

                return results;
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int update(String query, ThrowingSQLConsumer<PreparedStatement> fn) {
        try (Connection connection = getConnection(jdbcUrl, username, password)) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                fn.accept(statement);
                return statement.executeUpdate();
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int batchUpdate(String query, ThrowingSQLConsumer<PreparedStatement> fn) {
        try (Connection connection = getConnection(jdbcUrl, username, password)) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                fn.accept(statement);
                return statement.executeBatch().length;
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> List<T> queryList(String query, ThrowingSQLConsumer<PreparedStatement> fn, ThrowingSQLFunction<ResultSet, T> fn2) {
        try (Connection connection = getConnection(jdbcUrl, username, password)) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                fn.accept(statement);
                ResultSet resultSet = statement.executeQuery();

                List<T> results = new ArrayList<>();

                while (resultSet.next()) {
                    results.add(fn2.apply(resultSet));
                }

                return results;
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
}
