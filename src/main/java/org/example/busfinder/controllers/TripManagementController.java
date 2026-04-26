package org.example.busfinder.controllers;

import Util.DatabaseHelper;
import Util.SceneSwitcher;
import Util.UserSession;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.example.busfinder.datamodels.TripRowManage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TripManagementController {

    @FXML private Button createButton;
    @FXML private DatePicker tripDatePicker;
    @FXML private TextField baseFareField;
    @FXML private TextField departureTimeField;
    @FXML private TextField arrivalTimeField;

    @FXML private ComboBox<String> driverComboBox;
    @FXML private ComboBox<String> busComboBox;
    @FXML private ComboBox<String> routeComboBox;

    @FXML private TableView<TripRowManage> tripsTable;
    @FXML private TableColumn<TripRowManage, String> colTripId;
    @FXML private TableColumn<TripRowManage, String> colDate;
    @FXML private TableColumn<TripRowManage, String> colTime;
    @FXML private TableColumn<TripRowManage, String> colDriver;
    @FXML private TableColumn<TripRowManage, String> colBus;
    @FXML private TableColumn<TripRowManage, String> colRoute;
    @FXML private TableColumn<TripRowManage, String> colFare;
    @FXML private TableColumn<TripRowManage, String> colStatus;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        // 1. Tell the Table Columns where to get their data from the TripRow record
        statusLabel.setVisible(false);
        colTripId.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().tripId()));
        colDate.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().date()));
        colTime.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().time()));
        colDriver.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().driver()));
        colBus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().bus()));
        colRoute.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().route()));
        colFare.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().fare()));
        colStatus.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().status()));

        loadTableData();
        loadComboBoxData();
    }


    @FXML
    public void handleCreateTrip(ActionEvent event) {
        // Grab all the inputs from the UI
        java.time.LocalDate date = tripDatePicker.getValue();
        String depTimeStr = departureTimeField.getText();
        String arrTimeStr = arrivalTimeField.getText();
        String fareStr = baseFareField.getText();
        String driverSelection = driverComboBox.getValue();
        String busSelection = busComboBox.getValue();
        String routeSelection = routeComboBox.getValue();

        // Validate that nothing is empty
        if (date == null || depTimeStr.isEmpty() || arrTimeStr.isEmpty() || fareStr.isEmpty() ||
                driverSelection == null || busSelection == null || routeSelection == null) {
            System.out.println("Validation Error: All fields are required!");
            showStatus("Validation Error: All fields are required!" , true);
            return;
        }
        createButton.setDisable(true);
        createButton.setText("Creating...");

        // 3. Extract the Database IDs and format data
        int driverId;
        int busId;
        int routeId;
        double baseFare;

        try {
            driverId = Integer.parseInt(driverSelection);

            // Split "1 - Cairo -> Alexandria" to get [1]
            routeId = Integer.parseInt(routeSelection.split(" - ")[0]);

            // Replace "BUS-101" to get [101]
            busId = Integer.parseInt(busSelection.replace("BUS-", ""));

            baseFare = Double.parseDouble(fareStr);

            // SQL Server TIME needs seconds like "08:30:00"
            if (depTimeStr.length() == 5) depTimeStr += ":00";
            if (arrTimeStr.length() == 5) arrTimeStr += ":00";

        } catch (Exception e) {
            System.out.println("Validation Error: Please ensure fare is a number and times are formatted correctly.");
            return;
        }

        String finalDepTimeStr = depTimeStr;
        String finalArrTimeStr = arrTimeStr;
        Thread insertThread = new Thread(() -> {
            String query = "INSERT INTO Trip (Date, Departure, Arrival, Base_Fare, Driver_ID, Bus_ID, Route_ID) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setDate(1, java.sql.Date.valueOf(date));
                pstmt.setTime(2, java.sql.Time.valueOf(finalDepTimeStr));
                pstmt.setTime(3, java.sql.Time.valueOf(finalArrTimeStr));
                pstmt.setDouble(4, baseFare);
                pstmt.setInt(5, driverId);
                pstmt.setInt(6, busId);
                pstmt.setInt(7, routeId);

                pstmt.executeUpdate();

                Platform.runLater(() -> {
                    System.out.println("Trip successfully created!");
                    createButton.setDisable(false);
                    createButton.setText("Create");
                    statusLabel.setVisible(false);
                    tripDatePicker.setValue(null);
                    departureTimeField.clear();
                    arrivalTimeField.clear();
                    baseFareField.clear();
                    driverComboBox.setValue(null);
                    busComboBox.setValue(null);
                    routeComboBox.setValue(null);

                    loadTableData();
                });

            } catch (SQLException e) {
                e.printStackTrace();
                Platform.runLater(() -> System.out.println("Database Error: Could not create trip."));
            }
        });

        insertThread.start();
    }

    private void loadTableData() {
        Thread dbThread = new Thread(() -> {
            String Query ="SELECT\n" +
                    "    T.Trip_ID,\n" +
                    "    T.Date,\n" +
                    "    CONCAT(CONVERT(VARCHAR(5), T.Departure), ' - ', CONVERT(VARCHAR(5), T.Arrival)) AS Time," +
                    "    T.Base_Fare AS Fare,\n" +
                    "    T.Trip_Status AS Status,\n" +
                    "\n" +
                    "    CONCAT('ID: ', D.Driver_ID, ' (', D.License_Number, ')') AS Driver,\n" +
                    "    \n" +
                    "    CONCAT('BUS-', T.Bus_ID) AS Bus, \n" +
                    "\n" +
                    "    CONCAT(\n" +
                    "        (SELECT TOP 1 S.City FROM Route_Stop RS JOIN Stop S ON RS.Stop_ID = S.Stop_ID WHERE RS.Route_ID = T.Route_ID ORDER BY RS.[order] ASC),\n" +
                    "        ' -> ',\n" +
                    "        (SELECT TOP 1 S.City FROM Route_Stop RS JOIN Stop S ON RS.Stop_ID = S.Stop_ID WHERE RS.Route_ID = T.Route_ID ORDER BY RS.[order] DESC)\n" +
                    "    ) AS Route\n" +
                    "\n" +
                    "FROM Trip AS T\n" +
                    "LEFT JOIN Driver AS D ON D.Driver_ID = T.Driver_ID\n" +
                    "LEFT JOIN Route AS R ON R.Route_ID = T.Route_ID\n" +
                    "LEFT JOIN Bus AS B ON B.Bus_ID = T.Bus_ID";

            ObservableList<TripRowManage> tripList = FXCollections.observableArrayList();

            try(Connection conn = DatabaseHelper.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(Query)) {
                ResultSet rs = pstmt.executeQuery();

                while(rs.next()) {
                    String Driver = rs.getString("Driver");
                    String TripID = rs.getString("Trip_ID");
                    String Fare = String.valueOf(rs.getInt("Fare"));
                    String Status = rs.getString("Status");
                    String Date = rs.getString("Date");
                    String Time = rs.getString("Time");
                    String Route = rs.getString("Route");
                    String Bus = rs.getString("Bus");

                    tripList.add(new TripRowManage(TripID, Date , Time , Driver , Bus , Route , Fare, Status));
                }
                Platform.runLater(() -> tripsTable.setItems(tripList));
            }
            catch (SQLException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            };
        });
        dbThread.start();
    }

    private void loadComboBoxData() {
        Thread dbThread = new Thread(() -> {

            ObservableList<String> drivers = FXCollections.observableArrayList();
            ObservableList<String> buses = FXCollections.observableArrayList();
            ObservableList<String> routes = FXCollections.observableArrayList();

            String driverQuery = "SELECT Driver_ID FROM Driver";
            String busQuery = "SELECT Bus_ID FROM Bus";

            String routeQuery = "SELECT Route_ID, " +
                    "(SELECT TOP 1 S.City FROM Route_Stop RS JOIN Stop S ON RS.Stop_ID = S.Stop_ID WHERE RS.Route_ID = R.Route_ID ORDER BY RS.[order] ASC) AS StartCity, " +
                    "(SELECT TOP 1 S.City FROM Route_Stop RS JOIN Stop S ON RS.Stop_ID = S.Stop_ID WHERE RS.Route_ID = R.Route_ID ORDER BY RS.[order] DESC) AS EndCity " +
                    "FROM Route R";

            try (Connection conn = DatabaseHelper.getConnection()) {
                try (PreparedStatement ps = conn.prepareStatement(driverQuery);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        drivers.add(String.valueOf(rs.getInt("Driver_ID")));
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(busQuery);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        // Format: "BUS-1"
                        buses.add("BUS-" + rs.getInt("Bus_ID"));
                    }
                }
                try (PreparedStatement ps = conn.prepareStatement(routeQuery);
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        // Format: "1 - Cairo -> Alexandria"
                        routes.add(rs.getInt("Route_ID") + " - " + rs.getString("StartCity") + " → " + rs.getString("EndCity"));
                    }
                }
                Platform.runLater(() -> {
                    driverComboBox.setItems(drivers);
                    busComboBox.setItems(buses);
                    routeComboBox.setItems(routes);
                });

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        dbThread.start();
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #2ecc71;");
        statusLabel.setVisible(true);
    }

    // NAVIGATION
    @FXML
    public void handleDashboard(ActionEvent event) {
        SceneSwitcher sceneSwitcher = new SceneSwitcher();
        sceneSwitcher.switchPage(event, "/org/example/busfinder/Admin-View.fxml");
    }

    @FXML
    public void handleClients(ActionEvent event) {
        SceneSwitcher sceneSwitcher = new SceneSwitcher();
        sceneSwitcher.switchPage(event, "/org/example/busfinder/ManageClient-View.fxml");
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        Util.UserSession.cleanUserSession();
        SceneSwitcher sceneSwitcher = new SceneSwitcher();
        sceneSwitcher.switchPage(event, "/org/example/busfinder/Login-View.fxml");
    }
}