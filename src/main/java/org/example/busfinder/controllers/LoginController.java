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



public class LoginController {

    @FXML private Button createAccountButton;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;


    @FXML
    void handleLogin(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both username/email and password.");
            errorLabel.setVisible(true);
            return;
        }

        loginButton.setDisable(true);
        loginButton.setText("Logging in...");
        errorLabel.setVisible(false);

        Thread dbThread = new Thread(() -> {
            String query = "SELECT a.Role, a.Client_ID " +
                    "FROM AppUser a " +
                    "LEFT JOIN Client c ON a.Client_ID = c.Client_ID " +
                    "WHERE (a.Username = ? OR c.Email = ?) AND a.PasswordHash = ?";

            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(query)) {

                pstmt.setString(1, username);
                pstmt.setString(2, username);
                pstmt.setString(3, password);

                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    String userRole = rs.getString("Role");
                    int clientId = rs.getInt("Client_ID");
                    String newWindow;

                    if(userRole.equals("Admin")) {
                        newWindow = "/org/example/busfinder/Admin-View.fxml";
                    }
                    else if(userRole.equals("Client")) {
                        newWindow = "/org/example/busfinder/Client-View.fxml";
                        Util.UserSession.setCurrentClientId(clientId);
                    } else {
                        newWindow = "";
                    }
                    Platform.runLater(() -> {
                        System.out.println("Login Successful! Welcome, " + userRole);
                        SceneSwitcher sceneSwitcher = new SceneSwitcher();

                        sceneSwitcher.switchPage(event ,newWindow);
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

    @FXML
    public void handleCreateAccount(ActionEvent actionEvent) {
        SceneSwitcher sceneSwitcher = new SceneSwitcher();
        sceneSwitcher.switchPage(actionEvent , "/org/example/busfinder/CreateAccount-View.fxml");
    }

    @FXML
    public void handleForgotPassword(ActionEvent actionEvent) {
        SceneSwitcher sceneSwitcher = new SceneSwitcher();
        sceneSwitcher.switchPage(actionEvent , "/org/example/busfinder/ForgotPassword-View.fxml");
    }
}
