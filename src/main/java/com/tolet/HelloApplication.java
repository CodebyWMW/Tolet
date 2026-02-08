package com.tolet;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Initialize some dummy data so the app isn't empty
        DataStore.initData();

        FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("splash-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 600);
        DataStore.applyTheme(scene);
        stage.setTitle("Project To-Let");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}