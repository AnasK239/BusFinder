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
import org.example.busfinder.datamodels.TripRow;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminDashboardController {

    @FXML private TableView<TripRow> tripsTable;
    @FXML private TableColumn<TripRow, String> colId;
    @FXML private TableColumn<TripRow, String> colRoute;
    @FXML private TableColumn<TripRow, String> colTime;
    @FXML private TableColumn<TripRow, String> colStatus;
    @FXML private TableColumn<TripRow, String> colSeats;
    @FXML private Label revenueLabel;
    @FXML private Label reservationsLabel;
    @FXML private Label activeTripsLabel;
    @FXML private Label totalClientsLabel;


    @FXML
    public void initialize() {
        colId.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().tripId()));
        colRoute.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().route()));
        colTime.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().time()));
        colStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().status()));
        colSeats.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().seats()));

        loadTableData();
        loadStats();
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
        Thread dbThread = new Thread(() -> {
            ObservableList<TripRow> tripList = FXCollections.observableArrayList();

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

                while (rs.next()) {
                    String id = String.valueOf(rs.getInt("Trip_ID"));
                    String route = rs.getString("RoutePath");
                    String time = rs.getString("DepartureTime");
                    String status = rs.getString("Trip_Status");
                    String seats = rs.getString("SeatInfo");

                    tripList.add(new TripRow(id, route, time, status, seats));
                }
                Platform.runLater(() -> tripsTable.setItems(tripList));

            } catch (SQLException e) {
                System.out.println("Error loading table data!");
                e.printStackTrace();
            }
        });
        dbThread.start();
    }


    // NAVIGATION
    @FXML
    public void handleLogout(ActionEvent event) {
        Util.UserSession.cleanUserSession();
        SceneSwitcher sceneSwitcher = new SceneSwitcher();
        sceneSwitcher.switchPage(event,"/org/example/busfinder/Login-View.fxml");
    }

    @FXML
    public void handleAddClient(ActionEvent event) {
        SceneSwitcher sceneSwitcher = new SceneSwitcher();
        sceneSwitcher.switchPage(event,"/org/example/busfinder/ManageClient-View.fxml");
    }

    @FXML
    public void handleAddTrip(ActionEvent event) {
        SceneSwitcher sceneSwitcher = new SceneSwitcher();
        sceneSwitcher.switchPage(event,"/org/example/busfinder/ManageTrips-View.fxml");
    }



}