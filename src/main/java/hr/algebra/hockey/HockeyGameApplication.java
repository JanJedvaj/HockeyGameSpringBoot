package hr.algebra.hockey;

import hr.algebra.hockey.controller.HockeyGameController;
import hr.algebra.hockey.model.PlayerType;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class HockeyGameApplication extends Application {
    private static final Logger LOGGER = Logger.getLogger(HockeyGameApplication.class.getName());
    private static PlayerType loggedInPlayerType = PlayerType.SINGLE_PLAYER;

    @Override
    public void start(Stage stage) throws IOException {
        configurePlayerType(getParameters().getRaw());

        FXMLLoader fxmlLoader = new FXMLLoader(HockeyGameApplication.class.getResource("hello-view.fxml"));
        Parent root = fxmlLoader.load();
        HockeyGameController controller = fxmlLoader.getController();
        Scene scene = new Scene(root, 1120, 760);
        stage.setTitle("Ice Hockey Game - " + loggedInPlayerType);
        stage.setScene(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(680);
        stage.setOnCloseRequest(event -> controller.shutdown());
        stage.show();
        LOGGER.info("Hockey game window started in " + loggedInPlayerType + " mode.");
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
