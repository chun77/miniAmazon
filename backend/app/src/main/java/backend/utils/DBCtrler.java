package backend.utils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import backend.*;
import backend.Package;
import backend.protocol.WorldAmazon.AProduct;

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
            sql = "CREATE TABLE IF NOT EXISTS warehouses (\n"
                    + " id serial PRIMARY KEY,\n"
                    + " x_coordinate INT NOT NULL,\n"
                    + " y_coordinate INT NOT NULL\n"
                    + ");";
            stmt.execute(sql);
            sql = "CREATE TABLE IF NOT EXISTS products (\n"
                    + " id serial PRIMARY KEY,\n"
                    + " description VARCHAR (50) UNIQUE NOT NULL,\n"
                    + " price FLOAT NOT NULL\n"
                    + ");";
            stmt.execute(sql);
            
            sql = "CREATE TABLE IF NOT EXISTS orders (\n"
                    + " id serial PRIMARY KEY,\n"
                    + " user_id INT NOT NULL,\n"
                    + " tracking_id INT64 NOT NULL,\n"
                    + " product_id INT NOT NULL,\n"
                    + " quantity INT NOT NULL,\n"
                    + " x_coordinate INT NOT NULL,\n"
                    + " y_coordinate INT NOT NULL,\n"
                    + " FOREIGN KEY (user_id) REFERENCES users (id),\n"
                    + " FOREIGN KEY (product_id) REFERENCES products (id)\n"
                    + ");";
            stmt.execute(sql);

            sql = "CREATE TABLE IF NOT EXISTS order_items (\n"
                    + " package_id INT NOT NULL,\n"
                    + " product_id INT NOT NULL,\n"
                    + " amt INT NOT NULL\n"
                    + " FOREIGN KEY (package_id) REFERENCES orders (id),\n"
                    + " FOREIGN KEY (product_id) REFERENCES products (id)\n"
                    + ");";
            stmt.execute(sql);

            sql = "CREATE TABLE IF NOT EXISTS packages (\n"
                    + " package_id PRIMARY KEY,\n"
                    + " status VARCHAR(255) NOT NULL,\n"
                    + " whnum INT NOT NULL,\n"
                    + " FOREIGN KEY (package_id) REFERENCES orders (id),\n"
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

    public Package getPackageByID(long packageID) {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            String sql = "SELECT * FROM orders WHERE id = " + packageID + ";";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                // get x_coordinate and y_coordinate and tracking id
                int dest_x = rs.getInt("x_coordinate");
                int dest_y = rs.getInt("y_coordinate");
                long trackingID = rs.getLong("tracking_id");
                // loop through warehouses to find the closest one
                sql = "SELECT * FROM warehouses;";
                ResultSet rs2 = stmt.executeQuery(sql);
                int wh_x, WH_x = Integer.MAX_VALUE;
                int wh_y, WH_y = Integer.MAX_VALUE; 
                int minDistance = Integer.MAX_VALUE;
                int warehouseID = -1;
                while (rs2.next()) {
                    wh_x = rs2.getInt("x_coordinate");
                    wh_y = rs2.getInt("y_coordinate");
                    int distance = (dest_x - wh_x) * (dest_x - wh_x) + (dest_y - wh_y) * (dest_y - wh_y);
                    if (distance < minDistance) {
                        minDistance = distance;
                        warehouseID = rs2.getInt("id");
                        WH_x = wh_x;
                        WH_y = wh_y;
                    }
                }
                // get products in the package
                List<AProduct> products = new ArrayList<>();
                sql = "SELECT * FROM order_items WHERE package_id = " + packageID + ";";
                ResultSet rs3 = stmt.executeQuery(sql);
                while (rs3.next()) {
                    int productID = rs3.getInt("product_id");
                    int amt = rs3.getInt("amt");
                    // get product name
                    sql = "SELECT * FROM products WHERE id = " + productID + ";";
                    ResultSet rs4 = stmt.executeQuery(sql);
                    String description = rs4.getString("description");
                    AProduct product = AProduct.newBuilder().setId(productID).setDescription(description).setCount(amt).build();
                    products.add(product);
                }
                // store package into database
                sql = "INSERT INTO packages (package_id, status, whnum) VALUES (" + packageID + ", 'PURCHASING', " + warehouseID + ");";
                stmt.execute(sql);
                // generate a new package
                Package newPackage = new Package(packageID, trackingID, -1, new Location(dest_x, dest_y), products, new WareHouse(warehouseID, new Location(WH_x, WH_y)), "PURCHASING");
                return newPackage;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

}
