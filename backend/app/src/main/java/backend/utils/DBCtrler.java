package backend.utils;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backend.*;
import backend.Package;
import backend.protocol.AmazonUps.Product;
import backend.protocol.WorldAmazon.AProduct;

public class DBCtrler {
    //private static String url = "jdbc:postgresql://127.0.0.1:5432/ece568";
    private static String url = "jdbc:postgresql://vcm-39848.vm.duke.edu:5432/amazondb";
    private static String user = "postgres";
    private static String password = "postgres";
    //private static String password = "psql";

    public DBCtrler() {
    }

    public static void createTables() {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS amazonuser (\n"
                    + " id SERIAL PRIMARY KEY,\n"
                    + " username VARCHAR (50) UNIQUE NOT NULL,\n"
                    + " password VARCHAR (50) NOT NULL,\n"
                    + " UpsAccount INT\n"
                    + ");";
            stmt.execute(sql);
            sql = "CREATE TABLE IF NOT EXISTS warehouse (\n"
                    + " id SERIAL PRIMARY KEY,\n"
                    + " wh_x INT NOT NULL,\n"
                    + " wh_y INT NOT NULL\n"
                    + ");";
            stmt.execute(sql);
            sql = "CREATE TABLE IF NOT EXISTS product (\n"
                    + " id BIGSERIAL PRIMARY KEY,\n"
                    + " name VARCHAR (50) NOT NULL,\n"
                    + " description VARCHAR (50) UNIQUE NOT NULL,\n"
                    + " price FLOAT NOT NULL\n"
                    + ");";
            stmt.execute(sql);
            sql = "CREATE TABLE IF NOT EXISTS Orders (\n"
                    + " package_id BIGSERIAL PRIMARY KEY,\n"
                    + " amazonAccount INT NOT NULL,\n"
                    + " tracking_id BIGINT NOT NULL,\n"
                    + " dest_x INT NOT NULL,\n"
                    + " dest_y INT NOT NULL,\n"
                    + " FOREIGN KEY (amazonAccount) REFERENCES amazonuser (id) ON DELETE CASCADE\n"
                    + ");";
            stmt.execute(sql);

            sql = "CREATE TABLE IF NOT EXISTS packageProduct (\n"
                    + " package_id BIGINT NOT NULL,\n"
                    + " product_id INT NOT NULL,\n"
                    + " quantity INT NOT NULL,\n"
                    + " FOREIGN KEY (package_id) REFERENCES Orders (package_id) ON DELETE CASCADE,\n"
                    + " FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE CASCADE\n"
                    + ");";
            stmt.execute(sql);

            sql = "CREATE TABLE IF NOT EXISTS packageStatus (\n"
                    + " package_id BIGINT PRIMARY KEY,\n"
                    + " status VARCHAR(255) NOT NULL,\n"
                    + " wh_id INT NOT NULL,\n"
                    + " FOREIGN KEY (package_id) REFERENCES Orders (package_id) ON DELETE CASCADE,\n"
                    + " FOREIGN KEY (wh_id) REFERENCES warehouse (id) ON DELETE CASCADE\n"
                    + ");";
            stmt.execute(sql);
            
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void initializeTables() {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            String sql = "INSERT INTO amazonUser (username, password, UpsAccount) VALUES ('admin', 'admin', 0);";
            stmt.execute(sql);
            sql = "INSERT INTO warehouse (wh_x, wh_y) VALUES (0, 0);";
            stmt.execute(sql);
            sql = "INSERT INTO warehouse (wh_x, wh_y) VALUES (1, 1);";
            stmt.execute(sql);
            sql = "INSERT INTO warehouse (wh_x, wh_y) VALUES (2, 2);";
            stmt.execute(sql);
            sql = "INSERT INTO warehouse (wh_x, wh_y) VALUES (3, 3);";
            stmt.execute(sql);
            sql = "INSERT INTO warehouse (wh_x, wh_y) VALUES (4, 4);";
            stmt.execute(sql);
            sql = "INSERT INTO warehouse (wh_x, wh_y) VALUES (5, 5);";
            stmt.execute(sql);
            sql = "INSERT INTO product (name, description, price) VALUES ('product1', 'product1', 0);";
            stmt.execute(sql);
            sql = "INSERT INTO product (name, description, price) VALUES ('product2', 'product2', 0);";
            stmt.execute(sql);
            sql = "INSERT INTO product (name, description, price) VALUES ('product3', 'product3', 0);";
            stmt.execute(sql);
            sql = "INSERT INTO product (name, description, price) VALUES ('product4', 'product4', 0);";
            stmt.execute(sql);
            sql = "INSERT INTO product (name, description, price) VALUES ('product5', 'product5', 0);";
            stmt.execute(sql);
            sql = "INSERT INTO Orders (amazonAccount, tracking_id, dest_x, dest_y) VALUES (1, 16161, 3, 2);";
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void dropTables() {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            String sql = "DROP TABLE IF EXISTS Orders CASCADE;";
            stmt.execute(sql);
            sql = "DROP TABLE IF EXISTS product CASCADE;";
            stmt.execute(sql);
            sql = "DROP TABLE IF EXISTS amazonUser CASCADE;";
            stmt.execute(sql);
            sql = "DROP TABLE IF EXISTS warehouse CASCADE;";
            stmt.execute(sql);
            sql = "DROP TABLE IF EXISTS packageProduct CASCADE;";
            stmt.execute(sql);
            sql = "DROP TABLE IF EXISTS packageStatus CASCADE;";
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public Package getPackageByID(long packageID) {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            String sql = "SELECT * FROM shop_order WHERE package_id = " + packageID + ";";
            ResultSet rs = stmt.executeQuery(sql);
            if (rs.next()) {
                // get x_coordinate and y_coordinate and tracking id
                int dest_x = rs.getInt("dest_x");
                int dest_y = rs.getInt("dest_y");
                String trackingID = rs.getString("tracking_id");
                // loop through warehouses to find the closest one
                sql = "SELECT * FROM shop_warehouse;";
                ResultSet rs2 = stmt.executeQuery(sql);
                int wh_x, WH_x = Integer.MAX_VALUE;
                int wh_y, WH_y = Integer.MAX_VALUE; 
                int minDistance = Integer.MAX_VALUE;
                int warehouseID = -1;
                while (rs2.next()) {
                    wh_x = rs2.getInt("wh_x");
                    wh_y = rs2.getInt("wh_y");
                    int distance = (dest_x - wh_x) * (dest_x - wh_x) + (dest_y - wh_y) * (dest_y - wh_y);
                    if (distance < minDistance) {
                        minDistance = distance;
                        warehouseID = rs2.getInt("wh_id");
                        WH_x = wh_x;
                        WH_y = wh_y;
                    }
                }
                // get products in the package
                List<Product> products = new ArrayList<>();
                Map<Integer, Integer> productMap = new HashMap<>();
                sql = "SELECT * FROM shop_packageproduct WHERE package_id = " + packageID + ";";
                ResultSet rs3 = stmt.executeQuery(sql);
                while (rs3.next()) {
                    int productID = rs3.getInt("product_id");
                    int amt = rs3.getInt("quantity");
                    productMap.put(productID, amt);
                }
                // get product name
                for (Map.Entry<Integer, Integer> entry : productMap.entrySet()) {
                    int productID = entry.getKey();
                    int amt = entry.getValue();
                    sql = "SELECT * FROM shop_product WHERE product_id = " + productID + ";";
                    ResultSet rs4 = stmt.executeQuery(sql);
                    rs4.next();
                    String description = rs4.getString("description");
                    Product product = Product.newBuilder().setId(productID).setDescription(description).setCount(amt).build();
                    products.add(product);
                }
                // store package into database
                sql = "INSERT INTO shop_packagestatus (status, package_id, wh_id) VALUES ('PURCHASING', " + packageID + ", " + warehouseID + ");";
                stmt.execute(sql);
                // generate a new package
                Package newPackage = new Package(packageID, trackingID, -1, new Location(dest_x, dest_y), products, new WareHouse(warehouseID, new Location(WH_x, WH_y)), "PURCHASING");
                System.out.println(newPackage);
                return newPackage;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public void updatePackageStatus(long packageID, String status) {
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            String sql = "UPDATE shop_packagestatus SET status = '" + status + "' WHERE package_id = " + packageID + ";";
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


}
