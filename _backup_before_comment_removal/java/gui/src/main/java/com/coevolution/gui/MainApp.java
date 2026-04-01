package com.coevolution.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * MainApp - JavaFX Entry Point
 * Semaine 8 - GUI + Release v1.0.0
 */
public class MainApp extends Application {

    public static final String APP_TITLE   = "Metamodel Coevolution Tool v1.0.0";
    public static final String APP_VERSION = "1.0.0";
    public static final int    WIN_WIDTH   = 1100;
    public static final int    WIN_HEIGHT  = 750;

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource(
                "/com/coevolution/gui/main-view.fxml"));

        Parent root = loader.load();

        Scene scene = new Scene(root, WIN_WIDTH, WIN_HEIGHT);

        String css = getClass().getResource(
            "/com/coevolution/gui/style.css") != null
            ? getClass().getResource(
                "/com/coevolution/gui/style.css").toExternalForm()
            : null;
        if (css != null) scene.getStylesheets().add(css);

        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.show();
        System.out.println("[GUI] " + APP_TITLE + " started.");
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        System.out.println("[GUI] Application closed.");
    }

    public static void main(String[] args) {
        Application.launch(MainApp.class, args);
    }
}