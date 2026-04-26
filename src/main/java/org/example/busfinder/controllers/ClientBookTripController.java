package org.example.busfinder.controllers;

import Util.DatabaseHelper;
import Util.SceneSwitcher;
import Util.UserSession;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClientBookTripController {

    @FXML private Label sidebarName;
    @FXML private Label sidebarAvatar;

    @FXML private ComboBox<String> fromCityCombo;
    @FXML private ComboBox<String> toCityCombo;
    @FXML private DatePicker searchDatePicker;
    @FXML private Label statusLabel;

    // The container where we will dynamically load the Trip Cards
    @FXML private VBox tripsContainer;

    @FXML
    public void initialize() {
        loadCities();
        loadAvailableTrips(null, null, null);
    }

    private void loadCities() {
        Thread citiesThread = new Thread(()-> {
           String sql = "select distinct City from Stop";

           ObservableList<String> cities = FXCollections.observableArrayList();

           try(Connection conn = DatabaseHelper.getConnection();
           PreparedStatement ps = conn.prepareStatement(sql)) {
               ResultSet rs = ps.executeQuery();

               while(rs.next()) {
                   cities.add(rs.getString("City"));
               }

           } catch (SQLException e) {
               e.printStackTrace();
           }

           Platform.runLater(()->{
              fromCityCombo.setItems(cities);
              toCityCombo.setItems(cities);
           });
        });
        citiesThread.start();
    }

    private void loadAvailableTrips(String from, String to, java.time.LocalDate date) {
        Platform.runLater(() -> tripsContainer.getChildren().clear());

        Thread loadThread = new Thread(() -> {
            // Build dynamic SQL query using a CTE
            StringBuilder sql = new StringBuilder(
                    "WITH TripDetails AS (" +
                            "   SELECT T.Trip_ID, T.Date, T.Departure, T.Arrival, T.Base_Fare, " +
                            "          B.Total_Capacity, " +
                            "          (SELECT COUNT(*) FROM Reserve_Trip R WHERE R.Trip_ID = T.Trip_ID) AS BookedSeats, " +
                            "          (SELECT TOP 1 S.City FROM Route_Stop RS JOIN Stop S ON RS.Stop_ID = S.Stop_ID WHERE RS.Route_ID = T.Route_ID ORDER BY RS.[order] ASC) AS StartCity, " +
                            "          (SELECT TOP 1 S.City FROM Route_Stop RS JOIN Stop S ON RS.Stop_ID = S.Stop_ID WHERE RS.Route_ID = T.Route_ID ORDER BY RS.[order] DESC) AS EndCity " +
                            "   FROM Trip T " +
                            "   JOIN Bus B ON T.Bus_ID = B.Bus_ID " +
                            "   WHERE T.Trip_Status = 'Scheduled'" +
                            ") " +
                            "SELECT * FROM TripDetails WHERE 1=1"
            );
            List<Object> parameters = new ArrayList<>();

            // Dynamically append filters if they are not null
            if (from != null && !from.isEmpty()) {
                sql.append(" AND StartCity = ?");
                parameters.add(from);
            }
            if (to != null && !to.isEmpty()) {
                sql.append(" AND EndCity = ?");
                parameters.add(to);
            }
            if (date != null) {
                sql.append(" AND Date = ?");
                parameters.add(java.sql.Date.valueOf(date));
            }

            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql.toString())) {

                for (int i = 0; i < parameters.size(); i++) {
                    ps.setObject(i + 1, parameters.get(i));
                }

                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    int tripId = rs.getInt("Trip_ID");
                    String startCity = rs.getString("StartCity");
                    String endCity = rs.getString("EndCity");
                    String tripDate = rs.getDate("Date").toString();

                    // Format times to HH:MM
                    String depTime = rs.getTime("Departure").toString().substring(0, 5);
                    String arrTime = rs.getTime("Arrival").toString().substring(0, 5);

                    double fare = rs.getDouble("Base_Fare");

                    int capacity = rs.getInt("Total_Capacity");
                    int booked = rs.getInt("BookedSeats");
                    int seatsLeft = capacity - booked;

                    Platform.runLater(() -> {
                        VBox tripCard = createTripCard(tripId, startCity, endCity, tripDate, depTime, arrTime, fare, seatsLeft);
                        tripsContainer.getChildren().add(tripCard);
                    });
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        loadThread.start();
    }

    private VBox createTripCard(int tripId, String start, String end, String date, String dep, String arr, double fare, int seatsLeft) {
        // Main Container
        VBox card = new VBox(15.0);
        card.getStyleClass().add("table-container");
        card.setPadding(new Insets(20, 25, 20, 25));

        // TOP ROW
        HBox topRow = new HBox(15.0);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label routeLabel = new Label(start + " → " + end);
        routeLabel.setTextFill(javafx.scene.paint.Color.web("#2d3436"));
        routeLabel.setFont(Font.font("System", FontWeight.BOLD, 20.0));

        Label statusBadge = new Label("Scheduled");
        statusBadge.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-padding: 3 12 3 12; -fx-background-radius: 12;");

        topRow.getChildren().addAll(routeLabel, statusBadge);

        // BOTTOM ROW
        HBox bottomRow = new HBox(25.0);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        Label dateLabel = new Label("📅 " + date);
        dateLabel.setTextFill(javafx.scene.paint.Color.web("#636e72"));
        dateLabel.setFont(new Font(14.0));

        Label timeLabel = new Label("🕒 " + dep + " - " + arr);
        timeLabel.setTextFill(javafx.scene.paint.Color.web("#636e72"));
        timeLabel.setFont(new Font(14.0));

        Label seatsLabel = new Label("👥 " + seatsLeft + " seats left");
        seatsLabel.setTextFill(javafx.scene.paint.Color.web("#636e72"));
        seatsLabel.setFont(new Font(14.0));

        // Pushes the price and button to the right side
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label priceLabel = new Label("$" + fare);
        priceLabel.setTextFill(javafx.scene.paint.Color.web("#2d3436"));
        priceLabel.setFont(Font.font("System", FontWeight.BOLD, 22.0));

        Button bookBtn = new Button("→ Book Now");
        bookBtn.getStyleClass().add("primary-btn");
        bookBtn.setPrefHeight(35.0);
        bookBtn.setPrefWidth(120.0);
        bookBtn.setCursor(Cursor.HAND);

        // Disable booking if full!
        if (seatsLeft <= 0) {
            bookBtn.setDisable(true);
            bookBtn.setText("Sold Out");
        }

        // Attach the booking action
        // Pass the bookBtn object into the method
        bookBtn.setOnAction(e -> handleBookTrip(tripId, bookBtn));

        bottomRow.getChildren().addAll(dateLabel, timeLabel, seatsLabel, spacer, priceLabel, bookBtn);

        // Assemble Card
        card.getChildren().addAll(topRow, bottomRow);

        return card;
    }

    @FXML
    public void handleSearch(ActionEvent event) {
        String fromCity = fromCityCombo.getValue();
        String toCity = toCityCombo.getValue();
        java.time.LocalDate date = searchDatePicker.getValue();
        loadAvailableTrips(fromCity, toCity, date);
    }

    private void handleBookTrip(int tripId, Button clickedButton) {
        // Instantly disable the button so they can't double-click
        clickedButton.setDisable(true);
        clickedButton.setText("Booking...");

        int clientId = UserSession.getCurrentClientId();

        Thread bookingThread = new Thread(() -> {
            String query =
                    "IF (SELECT B.Total_Capacity - (SELECT COUNT(*) FROM Reserve_Trip R WHERE R.Trip_ID = T.Trip_ID) " +
                            "    FROM Trip T JOIN Bus B ON T.Bus_ID = B.Bus_ID WHERE T.Trip_ID = ?) > 0 " +
                            "BEGIN " +
                            "   INSERT INTO Reserve_Trip (Client_ID, Trip_ID) VALUES (?, ?); " +
                            "   SELECT 1 AS Result; " +
                            "END " +
                            "ELSE " +
                            "BEGIN " +
                            "   SELECT 0 AS Result; " +
                            "END";

            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setInt(1, tripId);
                pstmt.setInt(2, clientId);
                pstmt.setInt(3, tripId);

                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    int result = rs.getInt("Result");

                    Platform.runLater(() -> {
                        if (result == 1) {
                            showStatus("Success! Your trip has been booked.", false);
                            handleSearch(null); // This will redraw the cards anyway!
                        } else {
                            showStatus("Sold Out! Sorry, someone just booked the last seat.", true);
                            clickedButton.setText("Sold Out");
                        }
                    });
                }

            } catch (SQLException e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showStatus("You already booked this trip OR Internal Error", true);
                    // Re-enable the button so they can try again if it was a network error
                    clickedButton.setDisable(false);
                    clickedButton.setText("→ Book Now");
                });
            }
        });
        bookingThread.start();
    }


    // NAVIGATION
    @FXML
    public void handleProfile(ActionEvent event) {
        SceneSwitcher sceneSwitcher = new SceneSwitcher();
        sceneSwitcher.switchPage(event, "/org/example/busfinder/Client-View.fxml");
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        Util.UserSession.cleanUserSession();
        SceneSwitcher sceneSwitcher = new SceneSwitcher();
        sceneSwitcher.switchPage(event, "/org/example/busfinder/Login-View.fxml");
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #2ecc71;");
        statusLabel.setVisible(true);
    }
}