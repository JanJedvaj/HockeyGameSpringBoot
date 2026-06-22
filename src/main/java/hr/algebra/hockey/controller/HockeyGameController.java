package hr.algebra.hockey.controller;

import hr.algebra.hockey.HockeyGameApplication;
import hr.algebra.hockey.engine.HockeyGameEngine;
import hr.algebra.hockey.jndi.ConfigurationKey;
import hr.algebra.hockey.jndi.ConfigurationReader;
import hr.algebra.hockey.model.GameState;
import hr.algebra.hockey.model.GameStatus;
import hr.algebra.hockey.model.HockeyMove;
import hr.algebra.hockey.model.HockeyMoveType;
import hr.algebra.hockey.model.Player;
import hr.algebra.hockey.model.PlayerType;
import hr.algebra.hockey.model.Puck;
import hr.algebra.hockey.network.SocketMultiplayerService;
import hr.algebra.hockey.rmi.ChatRemoteService;
import hr.algebra.hockey.utils.ChatUtils;
import hr.algebra.hockey.utils.DialogUtils;
import hr.algebra.hockey.utils.DocumentationUtils;
import hr.algebra.hockey.utils.GameSaveUtils;
import hr.algebra.hockey.utils.XmlUtils;
import javafx.animation.AnimationTimer;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class HockeyGameController {
    private static final double AIM_ARROW_LENGTH = 54;
    private static final double AIM_ARROW_HEAD_LENGTH = 12;
    private static final double AIM_ARROW_HEAD_WIDTH = 8;
    private static final double RINK_MIN_X = 12;
    private static final double RINK_MIN_Y = 12;
    private static final double RINK_MAX_X = 748;
    private static final double RINK_MAX_Y = 608;
    private static final long NETWORK_BROADCAST_INTERVAL_NANOS = 50_000_000L;

    @FXML private Pane rinkPane;
    @FXML private Circle playerPaddle;
    @FXML private Circle opponentPaddle;
    @FXML private Circle puckCircle;
    @FXML private Label playerScoreLabel;
    @FXML private Label opponentScoreLabel;
    @FXML private Label playerOneNameLabel;
    @FXML private Label playerTwoNameLabel;
    @FXML private Label timerLabel;
    @FXML private Label statusLabel;
    @FXML private Label modeInstructionsLabel;
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
    private Timeline chatRefreshTimeline;
    private SocketMultiplayerService multiplayerService;
    private volatile ChatRemoteService chatRemoteService;
    private final AtomicBoolean chatRefreshInProgress = new AtomicBoolean();
    private long lastNetworkBroadcast;
    private boolean gameLoopRunning;
    private boolean gameOverDialogShown;
    private long lastTimerTick;

    @FXML
    private void initialize() {
        configureAimArrow();
        configureKeyboardInput();
        configureGameLoop();
        configureModePresentation();
        showConfigurationInfo();
        configureMultiplayer();
        configureRmiChat();
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

    private void configureModePresentation() {
        PlayerType localPlayerType = engine.getGameState().getLocalPlayerType();
        if (localPlayerType == PlayerType.SINGLE_PLAYER) {
            playerOneNameLabel.setText("PLAYER");
            playerTwoNameLabel.setText("OPPONENT");
            modeInstructionsLabel.setText("Single player: left player shoots; opponent patrols vertically");
        } else {
            playerOneNameLabel.setText("PLAYER 1");
            playerTwoNameLabel.setText("PLAYER 2");
            modeInstructionsLabel.setText("Multiplayer: launch only when the status shows your turn");
        }
        updateControlAvailability();
    }

    private void updateControlAvailability() {
        boolean multiplayerClient = isMultiplayerClient();
        boolean hostWaiting = isMultiplayerHost()
                && (multiplayerService == null || !multiplayerService.isConnected());
        startButton.setDisable(multiplayerClient || hostWaiting);
        pauseButton.setDisable(multiplayerClient || hostWaiting);

        boolean chatAvailable = isMultiplayerMode() && chatRemoteService != null;
        chatInputField.setDisable(!chatAvailable);
        sendChatButton.setDisable(!chatAvailable);
    }


    private void configureMultiplayer() {
        PlayerType localPlayerType = engine.getGameState().getLocalPlayerType();
        if (localPlayerType == PlayerType.SINGLE_PLAYER) {
            return;
        }

        try {
            String hostName = ConfigurationReader.getString(ConfigurationKey.HOST_NAME);
            int hostPort = ConfigurationReader.getInteger(ConfigurationKey.PLAYER_ONE_SERVER_PORT);
            if (localPlayerType == PlayerType.PLAYER_1) {
                multiplayerService = SocketMultiplayerService.createHost(
                        hostPort,
                        () -> Platform.runLater(this::launchRemotePlayer),
                        this::showNetworkStatus);
            } else {
                multiplayerService = SocketMultiplayerService.createClient(
                        hostName,
                        hostPort,
                        gameState -> Platform.runLater(() -> applyNetworkState(gameState)),
                        this::showNetworkStatus);
            }
            multiplayerService.start();
        } catch (Exception exception) {
            showNetworkStatus("Multiplayer setup failed: " + exception.getMessage());
        }
    }

    private void applyNetworkState(GameState networkGameState) {
        if (!isMultiplayerClient()) {
            return;
        }
        engine.applyNetworkState(networkGameState);
        drawGameState();
        updateStatusLabel();
        rinkPane.requestFocus();
    }

    private void launchRemotePlayer() {
        if (!isMultiplayerHost()
                || engine.getGameState().getActivePlayer() != PlayerType.PLAYER_2
                || !canLaunchInCurrentStatus()) {
            return;
        }

        engine.launchActivePlayer();
        savePendingMoveEvents();
        startGameLoop();
        broadcastGameStateImmediately();
    }

    private void showNetworkStatus(String message) {
        Platform.runLater(() -> {
            chatTextArea.appendText("Network: " + message + System.lineSeparator());
            if (isMultiplayerHost() && "PLAYER_2 connected.".equals(message)) {
                broadcastGameStateImmediately();
            }
            updateControlAvailability();
        });
    }

    private boolean isMultiplayerHost() {
        return engine.getGameState().getLocalPlayerType() == PlayerType.PLAYER_1;
    }

    private boolean isMultiplayerClient() {
        return engine.getGameState().getLocalPlayerType() == PlayerType.PLAYER_2;
    }

    private boolean isMultiplayerMode() {
        return isMultiplayerHost() || isMultiplayerClient();
    }

    public void shutdown() {
        stopGameLoop();
        if (chatRefreshTimeline != null) {
            chatRefreshTimeline.stop();
        }
        if (multiplayerService != null) {
            multiplayerService.close();
        }
    }

    @FXML
    private void onNewGame(ActionEvent event) {
        if (isMultiplayerClient()) {
            statusLabel.setText("Only PLAYER_1 can start a new multiplayer game.");
            return;
        }

        engine.startNewGame();
        gameOverDialogShown = false;
        resetMoveHistory();
        saveMoveEvent(new HockeyMove(HockeyMoveType.GAME_START, engine.getGameState().getActivePlayer(), engine.getGameState()));
        lastTimerTick = 0;
        drawGameState();
        updateStatusLabel();
        broadcastGameStateImmediately();
        rinkPane.requestFocus();
    }

    @FXML
    private void onSaveGame(ActionEvent event) {
        try {
            GameSaveUtils.saveGame(engine.getGameState());
            statusLabel.setText("Game saved to game/save.dat.");
            DialogUtils.showInformation("Game Saved", "The current game was saved to game/save.dat.");
            rinkPane.requestFocus();
        } catch (Exception exception) {
            statusLabel.setText("Save failed: " + exception.getMessage());
            DialogUtils.showError("Save Failed", exception.getMessage());
        }
    }

    @FXML
    private void onLoadGame(ActionEvent event) {
        if (isMultiplayerClient()) {
            statusLabel.setText("Only PLAYER_1 can load a multiplayer game.");
            return;
        }

        try {
            if (!GameSaveUtils.saveGameExists()) {
                statusLabel.setText("No saved game found at game/save.dat.");
                return;
            }
            engine.loadGameState(GameSaveUtils.loadGame());
            gameOverDialogShown = false;
            lastTimerTick = 0;
            drawGameState();
            updateStatusLabel();
            broadcastGameStateImmediately();
            DialogUtils.showInformation("Game Loaded", "The saved game was restored successfully.");
            rinkPane.requestFocus();
        } catch (Exception exception) {
            statusLabel.setText("Load failed: " + exception.getMessage());
            DialogUtils.showError("Load Failed", exception.getMessage());
        }
    }

    @FXML
    private void onReplay(ActionEvent event) {
        if (isMultiplayerMode()) {
            statusLabel.setText("Replay is available in SINGLE_PLAYER mode.");
            return;
        }
        startReplay();
    }

    @FXML
    private void onGenerateDocumentation(ActionEvent event) {
        try {
            statusLabel.setText("Documentation generated: " + DocumentationUtils.generateDocumentation());
            DialogUtils.showInformation("Documentation Generated", "HTML documentation was written to doc/documentation.html.");
            rinkPane.requestFocus();
        } catch (Exception exception) {
            statusLabel.setText("Documentation failed: " + exception.getMessage());
            DialogUtils.showError("Documentation Failed", exception.getMessage());
        }
    }

    @FXML
    private void onStartGame(ActionEvent event) {
        if (isMultiplayerClient()) {
            statusLabel.setText("PLAYER_1 controls multiplayer start and pause.");
            return;
        }
        if (isMultiplayerHost() && (multiplayerService == null || !multiplayerService.isConnected())) {
            statusLabel.setText("Wait for PLAYER_2 to connect before starting.");
            return;
        }

        engine.resumeGame();
        startGameLoop();
        drawGameState();
        updateStatusLabel();
        broadcastGameStateImmediately();
        rinkPane.requestFocus();
    }

    @FXML
    private void onPauseGame(ActionEvent event) {
        if (isMultiplayerClient()) {
            statusLabel.setText("PLAYER_1 controls multiplayer start and pause.");
            return;
        }

        engine.pauseGame();
        updateStatusLabel();
        broadcastGameStateImmediately();
        rinkPane.requestFocus();
    }

    @FXML
    private void onSendChatMessage(ActionEvent event) {
        String message = chatInputField.getText();
        if (message == null || message.isBlank()) {
            return;
        }

        ChatRemoteService service = chatRemoteService;
        if (service == null) {
            statusLabel.setText("RMI chat is not connected. Start RMI_SERVER.");
            return;
        }

        sendChatButton.setDisable(true);
        PlayerType sender = engine.getGameState().getLocalPlayerType();
        CompletableFuture.runAsync(() -> ChatUtils.sendChatMessage(service, sender, message))
                .whenComplete((unused, exception) -> Platform.runLater(() -> {
                    if (exception != null) {
                        chatRemoteService = null;
                        statusLabel.setText("RMI chat send failed. Reconnecting...");
                    } else {
                        chatInputField.clear();
                        refreshChatMessages();
                    }
                    sendChatButton.setDisable(chatRemoteService == null);
                    rinkPane.requestFocus();
                }));
    }

    private void configureRmiChat() {
        if (!isMultiplayerMode()) {
            chatInputField.setDisable(true);
            sendChatButton.setDisable(true);
            chatTextArea.setPromptText("RMI chat is available in multiplayer mode.");
            return;
        }

        chatInputField.setDisable(true);
        sendChatButton.setDisable(true);
        chatTextArea.setText("RMI chat unavailable. Start RMI_SERVER; connection will retry automatically.");
        chatRefreshTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> refreshChatMessages()));
        chatRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        chatRefreshTimeline.play();
        refreshChatMessages();
    }

    private void refreshChatMessages() {
        if (!isMultiplayerMode() || !chatRefreshInProgress.compareAndSet(false, true)) {
            return;
        }

        CompletableFuture.supplyAsync(() -> {
            ChatRemoteService service = chatRemoteService;
            if (service == null) {
                service = ChatUtils.initializeChatRemoteService().orElse(null);
                chatRemoteService = service;
            }
            return service == null ? null : ChatUtils.getAllMessages(service);
        }).whenComplete((messages, exception) -> Platform.runLater(() -> {
            chatRefreshInProgress.set(false);
            if (exception != null || messages == null) {
                chatRemoteService = null;
                chatInputField.setDisable(true);
                sendChatButton.setDisable(true);
                chatTextArea.setText("RMI chat unavailable. Start RMI_SERVER; connection will retry automatically.");
                return;
            }

            chatInputField.setDisable(false);
            sendChatButton.setDisable(false);
            chatTextArea.setText(String.join(System.lineSeparator(), messages));
            chatTextArea.setPromptText("RMI chat connected. No messages yet.");
        }));
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
        if (disabled) {
            startButton.setDisable(true);
            pauseButton.setDisable(true);
            sendChatButton.setDisable(true);
            chatInputField.setDisable(true);
        } else {
            updateControlAvailability();
        }
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
                if (isMultiplayerClient()) {
                    return;
                }

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
                broadcastGameState(now);
            }
        };
    }

    private void broadcastGameState(long now) {
        if (!isMultiplayerHost() || multiplayerService == null || !multiplayerService.isConnected()) {
            return;
        }

        if (now - lastNetworkBroadcast >= NETWORK_BROADCAST_INTERVAL_NANOS) {
            multiplayerService.sendGameState(engine.getGameState().createSnapshot());
            lastNetworkBroadcast = now;
        }
    }

    private void broadcastGameStateImmediately() {
        if (isMultiplayerHost() && multiplayerService != null && multiplayerService.isConnected()) {
            multiplayerService.sendGameState(engine.getGameState().createSnapshot());
            lastNetworkBroadcast = 0;
        }
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
        if (!canLaunchInCurrentStatus()) {
            return;
        }

        PlayerType localPlayerType = engine.getGameState().getLocalPlayerType();
        if (isMultiplayerMode() && engine.getGameState().getActivePlayer() != localPlayerType) {
            statusLabel.setText("Wait for your multiplayer turn.");
            return;
        }
        if (isMultiplayerHost() && (multiplayerService == null || !multiplayerService.isConnected())) {
            statusLabel.setText("Wait for PLAYER_2 to connect before launching.");
            return;
        }

        if (isMultiplayerClient()) {
            if (multiplayerService == null || !multiplayerService.isConnected()) {
                statusLabel.setText("PLAYER_2 is not connected to PLAYER_1.");
                return;
            }
            multiplayerService.sendLaunchRequest();
            statusLabel.setText("Launch sent to PLAYER_1.");
            return;
        }

        startGameLoop();
        engine.launchActivePlayer();
        savePendingMoveEvents();
        drawGameState();
        updateStatusLabel();
        broadcastGameStateImmediately();
    }

    private boolean canLaunchInCurrentStatus() {
        GameStatus status = engine.getGameState().getGameStatus();
        return status == GameStatus.READY || status == GameStatus.RUNNING;
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
                if (isMultiplayerMode()) {
                    if (multiplayerService == null || !multiplayerService.isConnected()) {
                        statusLabel.setText(modeName + " - waiting for multiplayer connection.");
                    } else if (gameState.getActivePlayer() == gameState.getLocalPlayerType()) {
                        statusLabel.setText(modeName + " - your turn. Press P to launch.");
                    } else {
                        statusLabel.setText(modeName + " - waiting for " + activePlayerName + " to launch.");
                    }
                    return;
                }

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
                String finalResult = result + "! Final score "
                        + gameState.getPlayerOne().getScore() + " - " + gameState.getPlayerTwo().getScore() + ".";
                statusLabel.setText(finalResult);
                if (!gameOverDialogShown) {
                    gameOverDialogShown = true;
                    DialogUtils.showInformation("Game Over", finalResult);
                }
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
