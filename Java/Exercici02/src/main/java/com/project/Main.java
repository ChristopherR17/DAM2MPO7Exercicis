package com.project;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    final int WINDOW_WIDTH = 500;
    final int WINDOW_HEIGHT = 300;

    @Override
    public void start(Stage stage) throws Exception {

        UtilsViews.parentContainer.setStyle("-fx-font: 14 arial;");
        UtilsViews.addView(getClass(), "viewDesktop", "/assets/viewDesktop.fxml");
        UtilsViews.addView(getClass(), "viewMobile0", "/assets/viewMobile0.fxml");
        UtilsViews.addView(getClass(), "viewMobile1", "/assets/viewMobile1.fxml");
        UtilsViews.addView(getClass(), "viewMobile2", "/assets/viewMobile2.fxml");

        Scene scene = new Scene(UtilsViews.parentContainer);

        stage.setScene(scene);
        stage.setTitle("Nintendo DB");
        stage.setMinWidth(WINDOW_WIDTH);
        stage.setMinHeight(WINDOW_HEIGHT);
        
        stage.show();

        // Add icon only if not Mac
        if (!System.getProperty("os.name").contains("Mac")) {
            Image icon = new Image("file:/icons/icon.png");
            stage.getIcons().add(icon);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
