package hr.algebra.hockey;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

public class HockeyGameController {
    @FXML private Pane rinkPane;
    @FXML private Rectangle playerPaddle;
    @FXML private Rectangle opponentPaddle;
    @FXML private Circle puckCircle;
    @FXML private Label playerScoreLabel;
    @FXML private Label opponentScoreLabel;
    @FXML private Label timerLabel;
    @FXML private Label statusLabel;
    @FXML private Button startButton;
    @FXML private Button pauseButton;
    @FXML private TextArea chatTextArea;
    @FXML private TextField chatInputField;
    @FXML private Button sendChatButton;

    @FXML
    private void initialize() {
        statusLabel.setText("Ready - WASD moves Player 1, arrows move Player 2 in multiplayer.");
    }

    @FXML
    private void onNewGame(ActionEvent event) {
        statusLabel.setText("New game layout is ready. Gameplay comes in the next MVP.");
    }

    @FXML
    private void onSaveGame(ActionEvent event) {
        statusLabel.setText("Save game will be implemented in the serialization MVP.");
    }

    @FXML
    private void onLoadGame(ActionEvent event) {
        statusLabel.setText("Load game will be implemented in the serialization MVP.");
    }

    @FXML
    private void onReplay(ActionEvent event) {
        statusLabel.setText("Replay will be implemented after XML move history.");
    }

    @FXML
    private void onGenerateDocumentation(ActionEvent event) {
        statusLabel.setText("Reflection documentation will be implemented in a later MVP.");
    }

    @FXML
    private void onStartGame(ActionEvent event) {
        statusLabel.setText("Game loop will be implemented in the movement MVP.");
        rinkPane.requestFocus();
    }

    @FXML
    private void onPauseGame(ActionEvent event) {
        statusLabel.setText("Pause will be connected once the game loop exists.");
    }

    @FXML
    private void onSendChatMessage(ActionEvent event) {
        String message = chatInputField.getText();
        if (message == null || message.isBlank()) {
            return;
        }
        chatTextArea.appendText("Me: " + message + System.lineSeparator());
        chatInputField.clear();
    }
}