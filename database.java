package mealplanner;

import java.sql.*;

public class database {
    public static Connection connectToDatabase() throws SQLException {

        String DB_URL = "jdbc:postgresql:meals_db";
        String USER = "postgres";
        String PASS = "1111";

        Connection con = DriverManager.getConnection(DB_URL, USER, PASS);
        con.setAutoCommit(true);
        return con;
    }

    public static void createTables(Connection con) throws SQLException {
        // Check if the table "ingredients" exists
        String checkTableQuery = "SELECT EXISTS (SELECT 1 FROM pg_tables WHERE tablename = ?)";
        PreparedStatement checkTableStmt = con.prepareStatement(checkTableQuery);
        checkTableStmt.setString(1, "ingredients");

        ResultSet resultSet = checkTableStmt.executeQuery();
        resultSet.next();
        boolean tableExists = resultSet.getBoolean(1);

        if (!tableExists) {
            // Create the table "ingredients" if it doesn't exist
            String createTableQuery = "CREATE TABLE ingredients (ingredient VARCHAR(35), ingredient_id INT, meal_id INT)";
            Statement createTableStmt = con.createStatement();
            createTableStmt.executeUpdate(createTableQuery);
            createTableStmt.close();
        }

        checkTableStmt.close();
        resultSet.close();

        // Check if the table "meals" exists
        checkTableQuery = "SELECT EXISTS (SELECT 1 FROM pg_tables WHERE tablename = ?)";
        checkTableStmt = con.prepareStatement(checkTableQuery);
        checkTableStmt.setString(1, "meals");

        resultSet = checkTableStmt.executeQuery();
        resultSet.next();
        tableExists = resultSet.getBoolean(1);

        if(!tableExists) {
            // Create the table "meals" if it doesn't exist
            String createTableQuery = "CREATE TABLE meals ( meal_id INT, category VARCHAR(35), meal VARCHAR(35) )";
            Statement createTableStmt = con.createStatement();
            createTableStmt.executeUpdate(createTableQuery);
            createTableStmt.close();
        }

        checkTableStmt.close();
        resultSet.close();
    }
}

