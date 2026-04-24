package Util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseHelper {

    // Update these three lines with your actual SQL Server details!
    // The "trustServerCertificate=true" is crucial for the modern MS SQL JDBC driver.
    private static final String DATABASE_URL = "jdbc:sqlserver://localhost:1433;databaseName=BusTripsReservation;encrypt=true;trustServerCertificate=true;";
    private static final String DATABASE_USER = "Java_User"; // Replace with your SQL Server username (often "sa")
    private static final String DATABASE_PASSWORD = "1234"; // Replace with your SQL Server password

    /**
     * Establishes and returns a connection to the database.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
    }
}