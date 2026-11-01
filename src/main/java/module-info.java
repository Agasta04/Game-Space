module com.example.spaceshootergamejavafx {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;

    opens com.example.spaceshootergamejavafx to javafx.fxml;
    exports com.example.spaceshootergamejavafx;
}
