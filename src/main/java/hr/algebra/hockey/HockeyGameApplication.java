package hr.algebra.hockey;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HockeyGameApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(HockeyGameApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1120, 760);
        stage.setTitle("Ice Hockey Game");
        stage.setScene(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(680);
        stage.show();
    }
}