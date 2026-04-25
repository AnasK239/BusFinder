package org.example.busfinder.controllers;

import Util.DatabaseHelper;
import Util.SceneSwitcher;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ForgotPasswordController {

    @FXML private Button updateButton; // REMEMBER: Add fx:id="updateButton" to your FXML!
    @FXML private TextField usernameField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label statusLabel;

    @FXML
    public void handleBackToLogin(ActionEvent event) {
        SceneSwitcher sceneSwitcher = new SceneSwitcher();
        sceneSwitcher.switchPage(event, "/org/example/busfinder/Login-View.fxml");
    }

    @FXML
    public void handleChangePassword(ActionEvent event) {
        String username = usernameField.getText();
        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // 1. Basic Validation
        if (username.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            showStatus("Please fill in all required fields (*)", true);
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showStatus("Passwords do not match!", true);
            return; // Exit before disabling the button
        }

        // Disable button visually while loading
        updateButton.setDisable(true);
        updateButton.setText("Updating...");
        showStatus("Updating password...", false);

        // 2. Database Update Thread
        Thread updateThread = new Thread(() -> {
            String query = "UPDATE AppUser SET PasswordHash = ? WHERE Username = ?";

            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setString(1, newPassword);
                pstmt.setString(2, username);

                int rowsAffected = pstmt.executeUpdate();

                Platform.runLater(() -> {
                    if (rowsAffected > 0) {
                        // Success!
                        showStatus("Password updated successfully! You can now log in.", false);
                        usernameField.clear();
                        newPasswordField.clear();
                        confirmPasswordField.clear();
                        SceneSwitcher sceneSwitcher = new SceneSwitcher();
                        sceneSwitcher.switchPage(event, "/org/example/busfinder/Login-View.fxml");
                    } else {
                        // Fail
                        showStatus("Username not found. Please try again.", true);
                    }

                    // FIXED: Always re-enable the button when finished!
                    updateButton.setDisable(false);
                    updateButton.setText("Update Password");
                });

            } catch (SQLException e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    showStatus("Database error. Please try again later.", true);
                    // FIXED: Always re-enable the button on error!
                    updateButton.setDisable(false);
                    updateButton.setText("Update Password");
                });
            }
        });

        updateThread.start();
    }

    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle(isError ? "-fx-text-fill: #e74c3c;" : "-fx-text-fill: #2ecc71;");
        statusLabel.setVisible(true);
    }
}