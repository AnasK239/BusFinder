package org.example.busfinder.controllers;

import Util.DatabaseHelper; // Assuming your DatabaseHelper is in the Util package
import Util.SceneSwitcher;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import org.example.busfinder.datamodels.TripRow;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminDashboardController {

    // 1. Notice the <TripRow, String> types added here! This is required.
    @FXML public TableView<TripRow> tripsTable;
    @FXML public TableColumn<TripRow, String> colId;
    @FXML public TableColumn<TripRow, String> colRoute;
    @FXML public TableColumn<TripRow, String> colTime;
    @FXML public TableColumn<TripRow, String> colStatus;
    @FXML public TableColumn<TripRow, String> colSeats;
    public Label revenueLabel;
    public Label reservationsLabel;
    public Label activeTripsLabel;
    public Label totalClientsLabel;


    @FXML
    public void initialize() {
        // Map the columns to the exact variable names inside the TripRow class below
        colId.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().tripId()));
        colRoute.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().route()));
        colTime.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().time()));
        colStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().status()));
        colSeats.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().seats()));

        // Load the data from the database
        loadTableData();
        loadStats();
    }
    @FXML
    public void handleLogout(ActionEvent event) {
        SceneSwitcher sceneSwitcher = new SceneSwitcher();
        sceneSwitcher.switchPage(event,"/org/example/busfinder/Login-View.fxml");
    }

    @FXML
    public void handleAddClient(ActionEvent event) {
        SceneSwitcher sceneSwitcher = new SceneSwitcher();
        sceneSwitcher.switchPage(event,"/org/example/busfinder/ManageClient-View.fxml");
    }

    private void loadStats(){
        Thread statsThread = new Thread(() -> {
            String Query = "SELECT " +
                    "(SELECT COUNT(*) FROM Client) AS TotalClients, " +
                    "(SELECT COUNT(*) FROM Trip WHERE Trip_Status IN ('Scheduled', 'Active')) AS ActiveTrips, " +
                    "(SELECT COUNT(*) FROM Reserve_Trip) AS TotalReservations, " +
            "(SELECT ISNULL(SUM(T.Base_Fare), 0) FROM Reserve_Trip RT JOIN Trip T ON RT.Trip_ID = T.Trip_ID) AS TotalRevenue";

           try(Connection conn = DatabaseHelper.getConnection();
               PreparedStatement pstmt = conn.prepareStatement(Query);) {
               ResultSet rs = pstmt.executeQuery();

               if(rs.next()){
                   String clients = String.valueOf(rs.getInt("TotalClients"));
                   String trips = String.valueOf(rs.getInt("ActiveTrips"));
                   String reservations = String.valueOf(rs.getInt("TotalReservations"));
                   String revenue = "$" + String.format("%,.2f", rs.getDouble("TotalRevenue"));

                   Platform.runLater(() -> {
                       totalClientsLabel.setText(clients);
                       activeTripsLabel.setText(trips);
                       reservationsLabel.setText(reservations);
                       revenueLabel.setText(revenue);
                   });
               }
           } catch (SQLException e) {
                throw new RuntimeException(e);
           }
        });
        statsThread.start();
    }
    private void loadTableData() {
        // Run database query on a background thread to prevent the UI from freezing
        Thread dbThread = new Thread(() -> {
            ObservableList<TripRow> tripList = FXCollections.observableArrayList();

            // 3. The SQL Query: This does a lot of heavy lifting to match your UI perfectly!
            String query = "SELECT " +
                    "T.Trip_ID, " +
            "(SELECT TOP 1 S.City FROM Route_Stop RS JOIN Stop S ON RS.Stop_ID = S.Stop_ID WHERE RS.Route_ID = T.Route_ID ORDER BY RS.[order] ASC) + ' -> ' + " +
            "(SELECT TOP 1 S.City FROM Route_Stop RS JOIN Stop S ON RS.Stop_ID = S.Stop_ID WHERE RS.Route_ID = T.Route_ID ORDER BY RS.[order] DESC) AS RoutePath, " +
            "CONVERT(varchar(5), T.Departure) AS DepartureTime, " +
                    "T.Trip_Status, " +
            "CAST((SELECT COUNT(*) FROM Reserve_Trip RT WHERE RT.Trip_ID = T.Trip_ID) AS VARCHAR) + '/' + CAST(B.Total_Capacity AS VARCHAR) AS SeatInfo " +
                    "FROM Trip T " +
                    "JOIN Bus B ON T.Bus_ID = B.Bus_ID";

            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query);
                 ResultSet rs = pstmt.executeQuery()) {

                // Loop through every row the database gives us
                while (rs.next()) {
                    // Format the ID so '1' becomes 'T001' to match your design
                    String id = String.valueOf(rs.getInt("Trip_ID"));
                    String route = rs.getString("RoutePath");
                    String time = rs.getString("DepartureTime");
                    String status = rs.getString("Trip_Status");
                    String seats = rs.getString("SeatInfo");

                    // Create a new TripRow object and add it to our list
                    tripList.add(new TripRow(id, route, time, status, seats));
                }

                // Push the data to the TableView safely on the UI Thread
                Platform.runLater(() -> tripsTable.setItems(tripList));

            } catch (SQLException e) {
                System.out.println("Error loading table data!");
                e.printStackTrace();
            }
        });

        dbThread.start();
    }


}