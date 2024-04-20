package backend.utils;

import java.sql.*;

public class DBCtrler {
    private static String url = "jdbc:postgresql://localhost:5432/ece568";
    private static String user = "postgres";
    private static String password = "psql";

    public DBCtrler() {
    }

    public static void createTables() {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS users (\n"
                    + " id serial PRIMARY KEY,\n"
                    + " username VARCHAR (50) UNIQUE NOT NULL,\n"
                    + " password VARCHAR (50) NOT NULL\n"
                    + ");";
            stmt.execute(sql);
            sql = "CREATE TABLE IF NOT EXISTS products (\n"
                    + " id serial PRIMARY KEY,\n"
                    + " name VARCHAR (50) UNIQUE NOT NULL,\n"
                    + " price FLOAT NOT NULL\n"
                    + ");";
            stmt.execute(sql);
            sql = "CREATE TABLE IF NOT EXISTS orders (\n"
                    + " id serial PRIMARY KEY,\n"
                    + " user_id INT NOT NULL,\n"
                    + " tracking_id VARCHAR(255) NOT NULL,\n"
                    + " package_id INT NOT NULL,\n"
                    + " product_id INT NOT NULL,\n"
                    + " quantity INT NOT NULL,\n"
                    + " x_coordinate FLOAT NOT NULL,\n"
                    + " y_coordinate FLOAT NOT NULL,\n"
                    + " FOREIGN KEY (user_id) REFERENCES users (id),\n"
                    + " FOREIGN KEY (product_id) REFERENCES products (id)\n"
                    + ");";
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void initializeTables() {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            String sql = "INSERT INTO users (username, password) VALUES ('user1', 'password1');";
            stmt.execute(sql);
            sql = "INSERT INTO users (username, password) VALUES ('user2', 'password2');";
            stmt.execute(sql);
            sql = "INSERT INTO products (name, price) VALUES ('product1', 10.0);";
            stmt.execute(sql);
            sql = "INSERT INTO products (name, price) VALUES ('product2', 20.0);";
            stmt.execute(sql);
            sql = "INSERT INTO orders (user_id, tracking_id, package_id, product_id, "+
                "quantity, x_coordinate, y_coordinate) VALUES (2, 'tracking1', 1, 2, 5, 8.0, 9.0);";
            stmt.execute(sql);
            sql = "INSERT INTO orders (user_id, tracking_id, package_id, product_id, "+
                "quantity, x_coordinate, y_coordinate) VALUES (1, 'tracking2', 2, 1, 10, 32.0, 51.0);";
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void dropTables() {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            String sql = "DROP TABLE IF EXISTS orders;";
            stmt.execute(sql);
            sql = "DROP TABLE IF EXISTS products;";
            stmt.execute(sql);
            sql = "DROP TABLE IF EXISTS users;";
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    

}
