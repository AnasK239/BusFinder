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
            // 1. Load the new FXML file
            Parent root = FXMLLoader.load(getClass().getResource(fxmlFileName));

            // 2. Get the current Stage (Window) from the button click event
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // 3. Create a new Scene with the loaded FXML and put it on the Stage
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();

        } catch (IOException e) {
            System.out.println("Error loading the FXML file: " + fxmlFileName);
            e.printStackTrace();
        }
    }
}