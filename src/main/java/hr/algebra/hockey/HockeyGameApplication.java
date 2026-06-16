package hr.algebra.hockey;

import hr.algebra.hockey.model.PlayerType;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class HockeyGameApplication extends Application {
    private static PlayerType loggedInPlayerType = PlayerType.SINGLE_PLAYER;

    @Override
    public void start(Stage stage) throws IOException {
        configurePlayerType(getParameters().getRaw());

        FXMLLoader fxmlLoader = new FXMLLoader(HockeyGameApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1120, 760);
        stage.setTitle("Ice Hockey Game - " + loggedInPlayerType);
        stage.setScene(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(680);
        stage.show();
    }

    public static PlayerType getLoggedInPlayerType() {
        return loggedInPlayerType;
    }

    private void configurePlayerType(List<String> rawArguments) {
        if (rawArguments == null || rawArguments.isEmpty()) {
            loggedInPlayerType = PlayerType.SINGLE_PLAYER;
            return;
        }

        try {
            loggedInPlayerType = PlayerType.valueOf(rawArguments.getFirst().toUpperCase());
        } catch (IllegalArgumentException exception) {
            loggedInPlayerType = PlayerType.SINGLE_PLAYER;
        }
    }
}