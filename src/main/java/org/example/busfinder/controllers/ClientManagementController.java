package org.example.busfinder.controllers;

import Util.DatabaseHelper;
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
import javafx.scene.control.TextField;
import org.example.busfinder.datamodels.ClientRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClientManagementController {
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField loyaltyField;

    @FXML private TextField ClientIdField;
    @FXML private TextField updateEmailField;
    @FXML private TextField updatePhoneField;
    @FXML private TextField updateLoyaltyField;

    @FXML private TableView<ClientRow> clientsTable;
    @FXML private TableColumn<ClientRow, String> colId;
    @FXML private TableColumn<ClientRow, String> colName;
    @FXML private TableColumn<ClientRow, String> colEmail;
    @FXML private TableColumn<ClientRow, String> colPhone;
    @FXML private TableColumn<ClientRow, String> colLoyalty;


    @FXML
    void initialize() {
        colId.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().clientId()));
        colName.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().clientName()));
        colEmail.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().clientEmail()));
        colPhone.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().clientPhone()));
        colLoyalty.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().loyaltyPoints()));

        loadTableData();
    }

    @FXML
    public void handleAddClient(ActionEvent actionEvent) {
        String Fname = firstNameField.getText();
        String Lname = lastNameField.getText();
        String Email = emailField.getText();
        String Phone = phoneField.getText();
        String Loyalty = loyaltyField.getText();

        if (Fname.isEmpty() || Lname.isEmpty()  || Phone.isEmpty() ) {
            System.out.println("Please fill in all required fields (*)");
            return;
        }

        String username = Fname.trim() + Lname.trim();
        String genericPassword = "Client123!";

        int LoyaltyPoints = Integer.parseInt(Loyalty);

        Thread addClientThread = new Thread(() -> {
            String Query1 = "INSERT INTO Client (Fname, Lname, Email, Loyalty_points) VALUES (?,?,?,?)";
            String Query2 = "INSERT INTO Client_phone_numbers (Client_ID, Phone_number) VALUES (?, ?)";
            String Query3 = "INSERT INTO AppUser (Username, PasswordHash, Role, Client_ID) VALUES (?, ?, ?, ?)";
            try (Connection conn = DatabaseHelper.getConnection()) {
                conn.setAutoCommit(false); // Start transaction

                try (PreparedStatement ps1 = conn.prepareStatement(Query1, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    ps1.setString(1, Fname);
                    ps1.setString(2, Lname);
                    ps1.setString(3, Email);
                    ps1.setInt(4, LoyaltyPoints);
                    ps1.executeUpdate();

                    try (ResultSet generatedKeys = ps1.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int newClientId = generatedKeys.getInt(1);

                            try (PreparedStatement ps2 = conn.prepareStatement(Query2)) {
                                ps2.setInt(1, newClientId);
                                ps2.setString(2, Phone);
                                ps2.executeUpdate();
                            }

                            try (PreparedStatement ps3 = conn.prepareStatement(Query3)) {
                                ps3.setString(1, username);
                                ps3.setString(2, genericPassword);
                                ps3.setString(3, "Client");
                                ps3.setInt(4, newClientId);
                                ps3.executeUpdate();
                            }

                        } else {
                            throw new SQLException("Creating client failed, no ID obtained.");
                        }
                    }
                }
                conn.commit();

                Platform.runLater(() -> {
                    firstNameField.clear();
                    lastNameField.clear();
                    emailField.clear();
                    phoneField.clear();
                    loyaltyField.clear();
                    loadTableData();
                });

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        addClientThread.start();
    }

    @FXML
    public void handleUpdateClient(ActionEvent actionEvent) {
        String clientIdStr = ClientIdField.getText();
        if (clientIdStr == null || clientIdStr.isEmpty()) {
            System.out.println("Please provide a Client ID to update.");
            return;
        }

        Thread updateThread = new Thread(() -> {
            int clientId = Integer.parseInt(clientIdStr);
            String newEmail = updateEmailField.getText();
            String newPhone = updatePhoneField.getText();
            String newLoyalty = updateLoyaltyField.getText();

            String clientQuery1 = "UPDATE Client SET Email = ? WHERE Client_ID = ?";
            String clientQuery2 = "UPDATE Client SET Loyalty_points = ? WHERE Client_ID = ?";

            try (Connection conn = DatabaseHelper.getConnection()) {
                conn.setAutoCommit(false); // Start transaction

                // Update Email if provided
                if (newEmail != null && !newEmail.isEmpty()) {
                    try (PreparedStatement ps = conn.prepareStatement(clientQuery1)) {
                        ps.setString(1, newEmail);
                        ps.setInt(2, clientId);
                        ps.executeUpdate();
                    }
                }

                // Update Loyalty Points if provided
                if (newLoyalty != null && !newLoyalty.isEmpty()) {
                    try (PreparedStatement ps = conn.prepareStatement(clientQuery2)) {
                        ps.setInt(1, Integer.parseInt(newLoyalty));
                        ps.setInt(2, clientId);
                        ps.executeUpdate();
                    }
                }

                // Update Phone if provided
                if (newPhone != null && !newPhone.isEmpty()) {
                    String updatePhoneSql = "UPDATE Client_phone_numbers SET Phone_number = ? WHERE Client_ID = ?";
                    try (PreparedStatement psUpdate = conn.prepareStatement(updatePhoneSql)) {
                        psUpdate.setString(1, newPhone);
                        psUpdate.setInt(2, clientId);

                        // Check how many rows were actually updated
                        int rowsAffected = psUpdate.executeUpdate();

                        // If 0 rows were updated, the client doesn't have a phone record yet
                        // We must INSERT a new row instead.
                        if (rowsAffected == 0) {
                            String insertPhoneSql = "INSERT INTO Client_phone_numbers (Client_ID, Phone_number) VALUES (?, ?)";
                            try (PreparedStatement psInsert = conn.prepareStatement(insertPhoneSql)) {
                                psInsert.setInt(1, clientId);
                                psInsert.setString(2, newPhone);
                                psInsert.executeUpdate();
                            }
                        }
                    }
                }

                conn.commit();

                // Clear fields and refresh table
                Platform.runLater(() -> {
                    updateEmailField.clear();
                    updatePhoneField.clear();
                    updateLoyaltyField.clear();
                    loadTableData();
                });

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        updateThread.start();
    }

    @FXML
    public void handleRemoveClient(ActionEvent event) {
        String clientIdStr = ClientIdField.getText();
        if (clientIdStr == null || clientIdStr.isEmpty()) return;

        Thread removeClientThread = new Thread(() -> {
            try (Connection conn = DatabaseHelper.getConnection()) {
                conn.setAutoCommit(false); // Start transaction
                int clientId = Integer.parseInt(clientIdStr);

                String deletePhoneSql = "DELETE FROM Client_phone_numbers WHERE Client_ID = ?";
                try (PreparedStatement ps0 = conn.prepareStatement(deletePhoneSql)) {
                    ps0.setInt(1, clientId);
                    ps0.executeUpdate();
                }
                String deleteClientSql = "DELETE FROM Client WHERE Client_Id = ?";
                try (PreparedStatement ps1 = conn.prepareStatement(deleteClientSql)) {
                    ps1.setInt(1, clientId);
                    ps1.executeUpdate();
                }
                conn.commit();

                Platform.runLater(() -> {
                    ClientIdField.clear();
                    loadTableData();
                });

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        removeClientThread.start();
    }

    private void loadTableData() {
        Thread dbThread = new Thread(() -> {
            ObservableList<ClientRow> clientRows = FXCollections.observableArrayList();

            String Query = "SELECT " +
                    "C.Client_ID, " +
                    "C.Fname + ' ' + C.Lname AS Name, " +
                    "C.Email, " +
                    "C.Loyalty_points, " +
                    "CP.Phone_number " +
                    "FROM Client AS C " +
                    "LEFT JOIN Client_phone_numbers AS CP ON CP.Client_ID = C.Client_ID";

            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement ps = conn.prepareStatement(Query)) {
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {
                    String clientId = String.valueOf(rs.getInt("Client_ID"));
                    String clientName = rs.getString("Name");
                    String clientEmail = rs.getString("Email");
                    String clientPhone = rs.getString("Phone_number");
                    String loyaltyPoints = String.valueOf(rs.getInt("Loyalty_points"));

                    clientRows.add(new ClientRow(clientId, clientName, clientEmail, clientPhone, loyaltyPoints));
                }
                Platform.runLater(() -> {
                    clientsTable.setItems(clientRows);
                });

            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
        dbThread.start();
    }


    // NAVIGATION
    @FXML
    public void handleDashboard(ActionEvent actionEvent) {
        SceneSwitcher sceneSwitcher = new SceneSwitcher();
        sceneSwitcher.switchPage(actionEvent, "/org/example/busfinder/Admin-View.fxml");
    }

    @FXML
    public void handleAddTrip(ActionEvent event) {
        SceneSwitcher sceneSwitcher = new SceneSwitcher();
        sceneSwitcher.switchPage(event,"/org/example/busfinder/ManageTrips-View.fxml");
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        Util.UserSession.cleanUserSession();
        SceneSwitcher sceneSwitcher = new SceneSwitcher();
        sceneSwitcher.switchPage(event, "/org/example/busfinder/Login-View.fxml");
    }


}