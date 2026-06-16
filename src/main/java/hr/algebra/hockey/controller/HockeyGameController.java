package hr.algebra.hockey.controller;

import hr.algebra.hockey.HockeyGameApplication;
import hr.algebra.hockey.engine.HockeyGameEngine;
import hr.algebra.hockey.model.GameState;
import hr.algebra.hockey.model.GameStatus;
import hr.algebra.hockey.model.Player;
import hr.algebra.hockey.model.PlayerType;
import hr.algebra.hockey.model.Puck;
import javafx.animation.AnimationTimer;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;

public class HockeyGameController {
    private static final double PLAYER_WIDTH = 24;
    private static final double PLAYER_HEIGHT = 88;
    private static final double AIM_ARROW_LENGTH = 54;
    private static final double AIM_ARROW_HEAD_LENGTH = 12;
    private static final double AIM_ARROW_HEAD_WIDTH = 8;
    private static final double RINK_MIN_X = 12;
    private static final double RINK_MIN_Y = 12;
    private static final double RINK_MAX_X = 748;
    private static final double RINK_MAX_Y = 608;

    @FXML private Pane rinkPane;
    @FXML private Circle playerPaddle;
    @FXML private Circle opponentPaddle;
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

    private final HockeyGameEngine engine = new HockeyGameEngine(HockeyGameApplication.getLoggedInPlayerType());
    private final Line aimArrowLine = new Line();
    private final Polygon aimArrowHead = new Polygon();
    private AnimationTimer gameLoop;
    private boolean gameLoopRunning;

    @FXML
    private void initialize() {
        configureAimArrow();
        configureKeyboardInput();
        configureGameLoop();
        drawGameState();
        updateStatusLabel();
    }

    @FXML
    private void onNewGame(ActionEvent event) {
        engine.startNewGame();
        drawGameState();
        updateStatusLabel();
        rinkPane.requestFocus();
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
        engine.resumeGame();
        startGameLoop();
        drawGameState();
        updateStatusLabel();
        rinkPane.requestFocus();
    }

    @FXML
    private void onPauseGame(ActionEvent event) {
        engine.pauseGame();
        updateStatusLabel();
        rinkPane.requestFocus();
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

    private void configureAimArrow() {
        aimArrowLine.setStroke(Color.web("#ffb703"));
        aimArrowLine.setStrokeWidth(5);
        aimArrowLine.setStrokeLineCap(StrokeLineCap.ROUND);
        aimArrowLine.setMouseTransparent(true);

        aimArrowHead.setFill(Color.web("#ffb703"));
        aimArrowHead.setStroke(Color.web("#8a4b00"));
        aimArrowHead.setStrokeWidth(1);
        aimArrowHead.setMouseTransparent(true);

        rinkPane.getChildren().addAll(aimArrowLine, aimArrowHead);
    }

    private void configureKeyboardInput() {
        rinkPane.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.P) {
                launchActivePlayer();
                event.consume();
            }
        });
    }

    private void configureGameLoop() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                GameStatus status = engine.getGameState().getGameStatus();
                if (status == GameStatus.READY || status == GameStatus.RUNNING) {
                    engine.updateAimAngle();
                }

                engine.updateFrame();
                clampGameObjects();
                drawGameState();
                updateStatusLabel();
            }
        };
    }

    private void startGameLoop() {
        if (!gameLoopRunning) {
            gameLoop.start();
            gameLoopRunning = true;
        }
    }

    private void launchActivePlayer() {
        GameStatus status = engine.getGameState().getGameStatus();
        if (status == GameStatus.PAUSED || status == GameStatus.FINISHED || status == GameStatus.REPLAYING) {
            return;
        }

        startGameLoop();
        engine.launchActivePlayer();
        drawGameState();
        updateStatusLabel();
    }

    private void drawGameState() {
        GameState gameState = engine.getGameState();
        drawPlayer(playerPaddle, gameState.getPlayerOne());
        drawPlayer(opponentPaddle, gameState.getPlayerTwo());
        drawPuck(gameState.getPuck());
        drawAimArrow(gameState);
        playerScoreLabel.setText(String.valueOf(gameState.getPlayerOne().getScore()));
        opponentScoreLabel.setText(String.valueOf(gameState.getPlayerTwo().getScore()));
        timerLabel.setText(formatTime(gameState.getTimeLeft()));
    }

    private void drawPlayer(Circle paddle, Player player) {
        paddle.setLayoutX(player.getX());
        paddle.setLayoutY(player.getY());
        paddle.setRadius(player.getRadius());
    }

    private void drawPuck(Puck puck) {
        puckCircle.setLayoutX(puck.getX());
        puckCircle.setLayoutY(puck.getY());
    }

    private void drawAimArrow(GameState gameState) {
        Player activePlayer = gameState.getActivePlayerModel();
        double angle = gameState.getAimAngleRadians();
        double startX = activePlayer.getX();
        double startY = activePlayer.getY();
        double endX = startX + Math.cos(angle) * AIM_ARROW_LENGTH;
        double endY = startY + Math.sin(angle) * AIM_ARROW_LENGTH;

        aimArrowLine.setStartX(startX);
        aimArrowLine.setStartY(startY);
        aimArrowLine.setEndX(endX);
        aimArrowLine.setEndY(endY);

        double leftAngle = angle + Math.PI - 0.55;
        double rightAngle = angle + Math.PI + 0.55;
        double leftX = endX + Math.cos(leftAngle) * AIM_ARROW_HEAD_LENGTH;
        double leftY = endY + Math.sin(leftAngle) * AIM_ARROW_HEAD_LENGTH;
        double rightX = endX + Math.cos(rightAngle) * AIM_ARROW_HEAD_LENGTH;
        double rightY = endY + Math.sin(rightAngle) * AIM_ARROW_HEAD_LENGTH;

        aimArrowHead.getPoints().setAll(
                endX, endY,
                leftX + Math.sin(angle) * AIM_ARROW_HEAD_WIDTH / 2, leftY - Math.cos(angle) * AIM_ARROW_HEAD_WIDTH / 2,
                rightX - Math.sin(angle) * AIM_ARROW_HEAD_WIDTH / 2, rightY + Math.cos(angle) * AIM_ARROW_HEAD_WIDTH / 2
        );

        boolean arrowVisible = gameState.getGameStatus() == GameStatus.READY
                || gameState.getGameStatus() == GameStatus.RUNNING;
        aimArrowLine.setVisible(arrowVisible);
        aimArrowHead.setVisible(arrowVisible);
    }

    private void clampGameObjects() {
        clampPlayer(engine.getGameState().getPlayerOne());
        clampPlayer(engine.getGameState().getPlayerTwo());
    }

    private void clampPlayer(Player player) {
        player.setX(clamp(player.getX(), RINK_MIN_X + player.getRadius(), RINK_MAX_X - player.getRadius()));
        player.setY(clamp(player.getY(), RINK_MIN_Y + player.getRadius(), RINK_MAX_Y - player.getRadius()));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void updateStatusLabel() {
        GameState gameState = engine.getGameState();
        String modeName = gameState.getLocalPlayerType() == PlayerType.SINGLE_PLAYER ? "Single player" : gameState.getLocalPlayerType().name();
        String activePlayerName = gameState.getActivePlayer() == PlayerType.PLAYER_1 ? "Player 1" : "Player 2";
        if (gameState.getLocalPlayerType() == PlayerType.SINGLE_PLAYER) {
            activePlayerName = "Player 1";
        }

        switch (gameState.getGameStatus()) {
            case READY, RUNNING -> statusLabel.setText(modeName + " - " + activePlayerName + " aiming. Press P to launch. Opponent patrols vertically.");
            case LAUNCHING, PUCK_MOVING -> statusLabel.setText(activePlayerName + " launched. Waiting for movement to settle...");
            case PAUSED -> statusLabel.setText("Paused - press Start to continue.");
            case FINISHED -> statusLabel.setText("Game finished.");
            case REPLAYING -> statusLabel.setText("Replay in progress.");
            case TURN_SWITCHING -> statusLabel.setText("Switching turns...");
        }
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}