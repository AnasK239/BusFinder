package org.example.busfinder.controllers;

import Util.DatabaseHelper;
import Util.SceneSwitcher;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.example.busfinder.datamodels.ClientRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class ClientManagementController {

    // Form Fields
    @FXML public TextField firstNameField;
    @FXML public TextField lastNameField;
    @FXML public TextField emailField;
    @FXML public TextField phoneField;

    // Table Elements
    @FXML public TableView<ClientRow> clientsTable;
    @FXML public TableColumn<ClientRow, String> colId;
    @FXML public TableColumn<ClientRow, String> colName;
    @FXML public TableColumn<ClientRow, String> colEmail;
    @FXML public TableColumn<ClientRow, String> colPhone;
    @FXML public TableColumn<ClientRow, String> colLoyalty;
    @FXML public TableColumn<ClientRow, String> colActions;

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
        String Loyalty = colLoyalty.getText();

        if(Fname.isEmpty() || Lname.isEmpty() || Email.isEmpty() || Phone.isEmpty()) {

        }
        Thread addClientThread = new Thread(() -> {
            String Query1 = "INSERT INTO Client (Fname, Lname, Email) VALUES (?,?,?)";
            String Query2 = "INSERT INTO Client_phone_numbers (Client_ID, Phone_number) VALUES (?, ?)";


            try(Connection conn = DatabaseHelper.getConnection();
                PreparedStatement ps1 = conn.prepareStatement(Query1, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps1.setString(1, Fname);
                ps1.setString(2, Lname);
                ps1.setString(3, Email);
                ps1.executeUpdate();

                try (ResultSet generatedKeys = ps1.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int newClientId = generatedKeys.getInt(1);

                        // 3. Now prepare Query 2 using the new ID
                        try (PreparedStatement ps2 = conn.prepareStatement(Query2)) {
                            ps2.setInt(1, newClientId); // Pass the ID from Query 1
                            ps2.setString(2, Phone); // Pass the phone number
                            ps2.executeUpdate();
                        }
                    } else {
                        throw new SQLException("Creating client failed, no ID obtained.");
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            loadTableData();
        });
        addClientThread.start();
    }

    @FXML
    public void handleDashboard(ActionEvent actionEvent) {
        SceneSwitcher sceneSwitcher = new SceneSwitcher();
        sceneSwitcher.switchPage(actionEvent,"/org/example/busfinder/Admin-View.fxml");
    }
    @FXML
    public void handleLogout(ActionEvent event) {
        SceneSwitcher sceneSwitcher = new SceneSwitcher();
        sceneSwitcher.switchPage(event,"/org/example/busfinder/Login-View.fxml");
    }

    private void loadTableData(){
        Thread dbThread = new Thread(() -> {
            ObservableList<ClientRow> clientRows = FXCollections.observableArrayList();

            String Query= "Select \n" +
                    "\tC.Client_ID,\n" +
                    "\tC.Fname +' '+ C.Lname AS Name,\n" +
                    "\tC.Email,\n" +
                    "\tC.Loyalty_points,\n" +
                    "\tCP.Phone_number\n" +
                    "From Client AS C\n" +
                    "LEFT JOIN Client_phone_numbers AS CP ON CP.Client_ID = C.Client_ID";

           try(Connection conn = DatabaseHelper.getConnection();
           PreparedStatement ps = conn.prepareStatement(Query)) {
               ResultSet rs = ps.executeQuery();

               while(rs.next()){
                   String clientId = String.valueOf(rs.getInt("Client_ID"));
                   String clientName = rs.getString("Name");
                   String clientEmail = rs.getString("Email");
                   String clientPhone = rs.getString("Phone_number");
                   String loyaltyPoints = String.valueOf(rs.getInt("Loyalty_points"));

                   clientRows.add(new ClientRow(clientId, clientName, clientEmail, clientPhone, loyaltyPoints));

                   Platform.runLater(() -> {
                       clientsTable.setItems(clientRows);
                   });
               }
           } catch (SQLException e) {
               throw new RuntimeException(e);
           }
        });
        dbThread.start();
    }

    public void handleBack(ActionEvent actionEvent) {

    }


}
