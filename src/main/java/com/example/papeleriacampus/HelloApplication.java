package com.example.papeleriacampus;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1180, 780);
        stage.setTitle("Papelería Campus · Catálogo de productos");
        stage.setMinWidth(1000);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.show();
    }
}
