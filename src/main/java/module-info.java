module org.example.busfinder {
    // Core UI dependencies
    requires javafx.controls;
    requires javafx.fxml;

    // Core Database dependency (JDBC)
    requires java.sql;
    requires java.management;

    // Allows JavaFX to access FXML files
    opens org.example.busfinder to javafx.fxml;
    exports org.example.busfinder;

    // Allows JavaFX to access controllers
    opens org.example.busfinder.controllers to javafx.fxml;
    exports org.example.busfinder.controllers;

    // Allows JavaFX Base to access data models
    opens org.example.busfinder.datamodels to javafx.base;
    exports org.example.busfinder.datamodels;
}