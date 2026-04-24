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
import java.sql.ResultSet;
import java.sql.SQLException;
// ... your other imports ...



public class LoginController {

    @FXML
    public Button createAccountButton;
    @FXML
    public TextField usernameField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public Label errorLabel;
    @FXML
    public Button loginButton;


    @FXML
    void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both username and password.");
            errorLabel.setVisible(true);
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Logging in...");
        errorLabel.setVisible(false);

        Thread dbThread = new Thread(() -> {
            String query = "SELECT Role FROM AppUser WHERE Username = ? AND PasswordHash = ?";
            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setString(1, username);
                pstmt.setString(2, password);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    String userRole = rs.getString("Role");

                    Platform.runLater(() -> {
                        System.out.println("Login Successful! Welcome, " + userRole);
                        SceneSwitcher sceneSwitcher = new SceneSwitcher();
                        sceneSwitcher.switchPage(event ,"/org/example/busfinder/Admin-View.fxml");
                        loginButton.setDisable(false);
                        loginButton.setText("Login");
                    });

                } else {
                    Platform.runLater(() -> {
                        errorLabel.setText("Invalid username or password.");
                        errorLabel.setVisible(true);
                        loginButton.setDisable(false);
                        loginButton.setText("Login");
                    });
                }

            } catch (SQLException e) {
                e.printStackTrace();
                //DB ERROR
                Platform.runLater(() -> {
                    errorLabel.setText("Cannot connect to the database. Try again later.");
                    errorLabel.setVisible(true);
                    loginButton.setDisable(false);
                    loginButton.setText("Login");
                });
            }
        });

        dbThread.start();
    }


    public void handleCreateAccount(ActionEvent actionEvent) {

    }
}
