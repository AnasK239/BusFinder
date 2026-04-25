package org.example.busfinder.controllers;

import Util.DatabaseHelper;
import Util.SceneSwitcher;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClientProfileController {
    @FXML private Label sidebarName;
    @FXML private Label sidebarAvatar;

    @FXML private TextField profileFirstName;
    @FXML private TextField profileLastName;
    @FXML private TextField profileEmail;
    @FXML private TextField profilePhone;
    @FXML private Label sideBarEmail;
    @FXML private Label profileLoyalty;
    @FXML private TextField profileUsername;

    @FXML
    public void initialize() {
        loadProfile();
    }

    @FXML
    public void handleEditProfile(ActionEvent event) {
        // Logic to make TextFields editable,
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        SceneSwitcher sceneSwitcher = new SceneSwitcher();
        sceneSwitcher.switchPage(event, "/org/example/busfinder/Login-View.fxml");
        Util.UserSession.cleanUserSession();
    }

    private void loadProfile() {
        Thread profileThread = new Thread(() -> {
            String Query = "SELECT\n" +
                    "\tC.Fname,\n" +
                    "\tC.Lname,\n" +
                    "\tC.Email,\n" +
                    "\tC.Loyalty_points,\n" +
                    "\tCP.Phone_number,\n" +
                    "\tAP.Username\n" +
                    "FROM Client AS C\n" +
                    "LEFT JOIN Client_phone_numbers AS CP ON C.Client_ID = CP.Client_ID\n" +
                    "LEFT JOIN AppUser AS AP ON AP.Client_ID = C.Client_ID\n" +
                    "WHERE\n" +
                    "\tC.Client_ID = ?";

            // Added pstmt to the try-with-resources block so it auto-closes
            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(Query)) {

                int userID = Util.UserSession.getCurrentClientId();
                pstmt.setInt(1, userID);

                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String firstName = rs.getString("Fname");
                        String lastName = rs.getString("Lname");
                        String email = rs.getString("Email");
                        String phone = rs.getString("Phone_number");
                        String loyaltyPoints = String.valueOf(rs.getInt("Loyalty_points"));
                        String username = rs.getString("Username");

                        Platform.runLater(() -> {
                            sidebarName.setText(firstName + " " + lastName);

                            if (firstName != null && !firstName.isEmpty()) {
                                sidebarAvatar.setText(firstName.substring(0, 1).toUpperCase() + lastName.substring(0, 1).toUpperCase());
                            }

                            profileFirstName.setText(firstName);
                            profileLastName.setText(lastName);
                            profileEmail.setText(email);
                            sideBarEmail.setText(email);

                            // In case they don't have a phone number in the DB yet
                            if (phone != null) {
                                profilePhone.setText(phone);
                            } else {
                                profilePhone.setText("Not Provided");
                            }

                            profileLoyalty.setText(loyaltyPoints);
                            profileUsername.setText(username);
                        });
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        profileThread.start();
    }




}