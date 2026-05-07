package com.project;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    final int WINDOW_WIDTH = 780;
    final int WINDOW_HEIGHT = 640;

    @Override
    public void start(Stage stage) throws Exception {

        UtilsViews.addView(getClass(), "mainView", "/assets/layout.fxml");

        Scene scene = new Scene(UtilsViews.parentContainer);

        stage.setScene(scene);
        stage.setTitle("Exercici03 - IETI Chat");
        stage.setMinWidth(WINDOW_WIDTH);
        stage.setMinHeight(WINDOW_HEIGHT);
        
        stage.show();

        // Icono opcional: solo se carga si existe en resources.
        try {
            Image icon = new Image(getClass().getResourceAsStream("/icons/icon.png"));
            if (!icon.isError()) stage.getIcons().add(icon);
        } catch (Exception ignored) {}
    }

    public static void main(String[] args) {
        launch(args);
    }
}