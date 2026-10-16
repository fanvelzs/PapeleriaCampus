module com.example.papeleriacampus {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    exports com.example.papeleriacampus;
    opens com.example.papeleriacampus to javafx.fxml;
}
