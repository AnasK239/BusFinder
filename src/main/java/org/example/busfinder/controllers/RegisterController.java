package org.example.busfinder.controllers;

import Util.DatabaseHelper;
import Util.SceneSwitcher;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RegisterController {

    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;

    @FXML
    public void handleRegister(ActionEvent event) {
        String fname = firstNameField.getText();
        String lname = lastNameField.getText();
        String email = emailField.getText();
        String phone = phoneField.getText();
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (fname.isEmpty() || lname.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showStatus("Please fill in all required fields (*)", true);
            return;
        }

        if (!password.equals(confirmPass)) {
            showStatus("Passwords do not match!", true);
            return;
        }

        Thread registerThread = new Thread(() -> {
            String clientQuery = "INSERT INTO Client (Fname, Lname, Email, Loyalty_points) VALUES (?,?,?,?)";
            String phoneQuery = "INSERT INTO Client_phone_numbers (Client_ID, Phone_number) VALUES (?, ?)";
            String userQuery = "INSERT INTO AppUser (Username, PasswordHash, Role, Client_ID) VALUES (?, ?, ?, ?)";

            try (Connection conn = DatabaseHelper.getConnection()) {
                conn.setAutoCommit(false);

                try (PreparedStatement psClient = conn.prepareStatement(clientQuery, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    psClient.setString(1, fname);
                    psClient.setString(2, lname);
                    psClient.setString(3, email);
                    psClient.setInt(4, 0);
                    psClient.executeUpdate();

                    try (ResultSet keys = psClient.getGeneratedKeys()) {
                        if (keys.next()) {
                            int newClientId = keys.getInt(1);

                            if (!phone.isEmpty()) {
                                try (PreparedStatement psPhone = conn.prepareStatement(phoneQuery)) {
                                    psPhone.setInt(1, newClientId);
                                    psPhone.setString(2, phone);
                                    psPhone.executeUpdate();
                                }
                            }

                            try (PreparedStatement psUser = conn.prepareStatement(userQuery)) {
                                psUser.setString(1, username);
                                psUser.setString(2, password);
                                psUser.setString(3, "Client");
                                psUser.setInt(4, newClientId);
                                psUser.executeUpdate();
                            }
                        }
                    }
                }

                conn.commit();

                Platform.runLater(() -> {
                    showStatus("Account created successfully! Redirecting...", false);
                    SceneSwitcher switcher = new SceneSwitcher();
                    switcher.switchPage(event, "/org/example/busfinder/Login-View.fxml");
                });

            } catch (SQLException e) {
                e.printStackTrace();
                Platform.runLater(() -> showStatus("Error connecting to database or Username already exists.", true));
            }
        });
        registerThread.start();
    }

    @FXML
    public void handleBackToLogin(ActionEvent event) {
        SceneSwitcher switcher = new SceneSwitcher();
        switcher.switchPage(event, "/org/example/busfinder/Login-View.fxml");
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #2ecc71;");
        statusLabel.setVisible(true);
    }
}