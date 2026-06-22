package hr.algebra.hockey.controller;

import hr.algebra.hockey.HockeyGameApplication;
import hr.algebra.hockey.engine.HockeyGameEngine;
import hr.algebra.hockey.jndi.ConfigurationReader;
import hr.algebra.hockey.model.GameState;
import hr.algebra.hockey.model.GameStatus;
import hr.algebra.hockey.model.HockeyMove;
import hr.algebra.hockey.model.HockeyMoveType;
import hr.algebra.hockey.model.Player;
import hr.algebra.hockey.model.PlayerType;
import hr.algebra.hockey.model.Puck;
import hr.algebra.hockey.utils.DocumentationUtils;
import hr.algebra.hockey.utils.GameSaveUtils;
import hr.algebra.hockey.utils.XmlUtils;
import javafx.animation.AnimationTimer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class HockeyGameController {
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
    private Timeline replayTimeline;
    private boolean gameLoopRunning;
    private long lastTimerTick;

    @FXML
    private void initialize() {
        configureAimArrow();
        configureKeyboardInput();
        configureGameLoop();
        showConfigurationInfo();
        drawGameState();
        updateStatusLabel();
    }

    private void showConfigurationInfo() {
        try {
            chatTextArea.appendText("Configuration: " + ConfigurationReader.describeConfiguration() + System.lineSeparator());
        } catch (Exception exception) {
            chatTextArea.appendText("Configuration could not be loaded: " + exception.getMessage() + System.lineSeparator());
        }
    }


    @FXML
    private void onNewGame(ActionEvent event) {
        engine.startNewGame();
        resetMoveHistory();
        saveMoveEvent(new HockeyMove(HockeyMoveType.GAME_START, engine.getGameState().getActivePlayer(), engine.getGameState()));
        lastTimerTick = 0;
        drawGameState();
        updateStatusLabel();
        rinkPane.requestFocus();
    }

    @FXML
    private void onSaveGame(ActionEvent event) {
        try {
            GameSaveUtils.saveGame(engine.getGameState());
            statusLabel.setText("Game saved to game/save.dat.");
            rinkPane.requestFocus();
        } catch (Exception exception) {
            statusLabel.setText("Save failed: " + exception.getMessage());
        }
    }

    @FXML
    private void onLoadGame(ActionEvent event) {
        try {
            if (!GameSaveUtils.saveGameExists()) {
                statusLabel.setText("No saved game found at game/save.dat.");
                return;
            }
            engine.loadGameState(GameSaveUtils.loadGame());
            lastTimerTick = 0;
            drawGameState();
            updateStatusLabel();
            rinkPane.requestFocus();
        } catch (Exception exception) {
            statusLabel.setText("Load failed: " + exception.getMessage());
        }
    }

    @FXML
    private void onReplay(ActionEvent event) {
        startReplay();
    }

    @FXML
    private void onGenerateDocumentation(ActionEvent event) {
        try {
            statusLabel.setText("Documentation generated: " + DocumentationUtils.generateDocumentation());
            rinkPane.requestFocus();
        } catch (Exception exception) {
            statusLabel.setText("Documentation failed: " + exception.getMessage());
        }
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

    private void resetMoveHistory() {
        try {
            XmlUtils.resetMoveHistory();
        } catch (Exception exception) {
            statusLabel.setText("XML reset failed: " + exception.getMessage());
        }
    }

    private void savePendingMoveEvents() {
        for (HockeyMove hockeyMove : engine.drainMoveEvents()) {
            saveMoveEvent(hockeyMove);
        }
    }

    private void saveMoveEvent(HockeyMove hockeyMove) {
        try {
            XmlUtils.saveMove(hockeyMove);
        } catch (Exception exception) {
            statusLabel.setText("XML save failed: " + exception.getMessage());
        }
    }

    private void startReplay() {
        try {
            List<HockeyMove> hockeyMoves = XmlUtils.loadMoves();
            if (hockeyMoves.isEmpty()) {
                statusLabel.setText("No XML move history available for replay.");
                return;
            }

            stopGameLoop();
            if (replayTimeline != null) {
                replayTimeline.stop();
            }

            GameStatus statusBeforeReplay = engine.getGameState().getGameStatus();
            engine.getGameState().setGameStatus(GameStatus.REPLAYING);
            setControlsDisabledForReplay(true);
            aimArrowLine.setVisible(false);
            aimArrowHead.setVisible(false);

            AtomicInteger index = new AtomicInteger(0);
            replayTimeline = new Timeline(new KeyFrame(Duration.seconds(0.9), actionEvent -> {
                HockeyMove hockeyMove = hockeyMoves.get(index.getAndIncrement());
                drawReplayMove(hockeyMove);
                statusLabel.setText("Replay " + index.get() + "/" + hockeyMoves.size()
                        + " - " + hockeyMove.getMoveType() + " by " + playerName(hockeyMove.getPlayerType()));
            }));
            replayTimeline.setCycleCount(hockeyMoves.size());
            replayTimeline.setOnFinished(actionEvent -> {
                engine.getGameState().setGameStatus(statusBeforeReplay);
                setControlsDisabledForReplay(false);
                drawGameState();
                updateStatusLabel();
                rinkPane.requestFocus();
            });
            replayTimeline.play();
        } catch (Exception exception) {
            engine.getGameState().setGameStatus(GameStatus.READY);
            setControlsDisabledForReplay(false);
            statusLabel.setText("Replay failed: " + exception.getMessage());
        }
    }

    private void drawReplayMove(HockeyMove hockeyMove) {
        playerPaddle.setLayoutX(hockeyMove.getPlayerOneX());
        playerPaddle.setLayoutY(hockeyMove.getPlayerOneY());
        opponentPaddle.setLayoutX(hockeyMove.getPlayerTwoX());
        opponentPaddle.setLayoutY(hockeyMove.getPlayerTwoY());
        puckCircle.setLayoutX(hockeyMove.getPuckX());
        puckCircle.setLayoutY(hockeyMove.getPuckY());
        playerScoreLabel.setText(String.valueOf(hockeyMove.getPlayerOneScore()));
        opponentScoreLabel.setText(String.valueOf(hockeyMove.getPlayerTwoScore()));
        timerLabel.setText(formatTime(hockeyMove.getTimeLeft()));
    }

    private void setControlsDisabledForReplay(boolean disabled) {
        startButton.setDisable(disabled);
        pauseButton.setDisable(disabled);
        sendChatButton.setDisable(disabled);
        chatInputField.setDisable(disabled);
    }

    private void stopGameLoop() {
        if (gameLoop != null && gameLoopRunning) {
            gameLoop.stop();
            gameLoopRunning = false;
        }
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

                updateTimer(now);
                engine.updateFrame();
                savePendingMoveEvents();
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

    private void updateTimer(long now) {
        if (lastTimerTick == 0) {
            lastTimerTick = now;
            return;
        }

        if (now - lastTimerTick >= 1_000_000_000L) {
            engine.tickTimer();
            savePendingMoveEvents();
            lastTimerTick = now;
        }
    }

    private void launchActivePlayer() {
        GameStatus status = engine.getGameState().getGameStatus();
        if (status == GameStatus.PAUSED || status == GameStatus.FINISHED || status == GameStatus.REPLAYING) {
            return;
        }

        startGameLoop();
        engine.launchActivePlayer();
        savePendingMoveEvents();
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
            case READY, RUNNING -> {
                if (gameState.getLastScoringPlayer() != null) {
                    statusLabel.setText(playerName(gameState.getLastScoringPlayer()) + " scored! " + modeName
                            + " - " + activePlayerName + " aiming. Press P to launch.");
                } else {
                    statusLabel.setText(modeName + " - " + activePlayerName + " aiming. Press P to launch. Opponent patrols vertically.");
                }
            }
            case LAUNCHING, PUCK_MOVING -> statusLabel.setText(activePlayerName + " launched. Waiting for movement to settle...");
            case PAUSED -> statusLabel.setText("Paused - press Start to continue.");
            case FINISHED -> {
                String result = gameState.getWinner() == null
                        ? "Draw"
                        : playerName(gameState.getWinner()) + " wins";
                statusLabel.setText(result + "! Final score "
                        + gameState.getPlayerOne().getScore() + " - " + gameState.getPlayerTwo().getScore() + ".");
            }
            case REPLAYING -> statusLabel.setText("Replay in progress.");
            case TURN_SWITCHING -> statusLabel.setText("Switching turns...");
        }
    }

    private String playerName(PlayerType playerType) {
        if (playerType == PlayerType.PLAYER_1) {
            return "Player 1";
        }
        if (engine.getGameState().getLocalPlayerType() == PlayerType.SINGLE_PLAYER) {
            return "Opponent";
        }
        return "Player 2";
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}