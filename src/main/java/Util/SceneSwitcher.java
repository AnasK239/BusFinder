package Util;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class SceneSwitcher {
    // A reusable method to switch pages
    public void switchPage(ActionEvent event, String fxmlFileName) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFileName));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            System.out.println("Error loading the FXML file: " + fxmlFileName);
            e.printStackTrace();
        }
    }
}