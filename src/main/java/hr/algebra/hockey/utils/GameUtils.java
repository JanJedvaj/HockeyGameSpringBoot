package hr.algebra.hockey.utils;

import hr.algebra.hockey.engine.CollisionService;
import hr.algebra.hockey.model.GameState;
import hr.algebra.hockey.model.GameStatus;
import hr.algebra.hockey.model.HockeyMove;
import hr.algebra.hockey.model.HockeyMoveType;
import hr.algebra.hockey.model.Player;
import hr.algebra.hockey.model.PlayerType;
import hr.algebra.hockey.model.Puck;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GameUtils {
    private static final Logger LOGGER = Logger.getLogger(GameUtils.class.getName());
    private static final Path SAVE_GAME_PATH = Path.of("game", "save.dat");
    private static final double FULL_CIRCLE_RADIANS = Math.PI * 2;
    private static final double DEFAULT_AIM_ROTATION_SPEED = 0.045;
    private static final double PLAYER_FRICTION = 0.95;
    private static final double PUCK_FRICTION = 0.988;
    private static final double SINGLE_PLAYER_OPPONENT_SPEED = 2.25;
    private static final double RINK_MIN_X = 12;
    private static final double RINK_MIN_Y = 12;
    private static final double RINK_MAX_X = 748;
    private static final double RINK_MAX_Y = 608;
    private static final double GOAL_MIN_Y = 235;
    private static final double GOAL_MAX_Y = 385;

    private final GameState gameState;
    private final List<HockeyMove> pendingMoveEvents = new ArrayList<>();
    private int singlePlayerOpponentDirection = 1;

    public GameUtils() {
        gameState = new GameState();
    }

    public GameUtils(PlayerType localPlayerType) {
        this();
        gameState.setLocalPlayerType(localPlayerType);
        prepareModeDefaults();
    }

    public GameUtils(GameState gameState) {
        this.gameState = gameState;
        prepareModeDefaults();
    }

    public GameState getGameState() {
        return gameState;
    }

    public static void saveGame(GameState gameState) throws IOException {
        Files.createDirectories(SAVE_GAME_PATH.getParent());
        try (ObjectOutputStream output = new ObjectOutputStream(Files.newOutputStream(SAVE_GAME_PATH))) {
            output.writeObject(gameState);
            LOGGER.info("Game state serialized to " + SAVE_GAME_PATH);
        }
    }

    public static GameState loadGame() throws IOException, ClassNotFoundException {
        try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(SAVE_GAME_PATH))) {
            GameState loadedState = (GameState) input.readObject();
            LOGGER.info("Game state deserialized from " + SAVE_GAME_PATH);
            return loadedState;
        } catch (IOException | ClassNotFoundException exception) {
            LOGGER.log(Level.SEVERE, "Game state could not be loaded", exception);
            throw exception;
        }
    }

    public static boolean saveGameExists() {
        return Files.exists(SAVE_GAME_PATH);
    }

    public List<HockeyMove> drainMoveEvents() {
        List<HockeyMove> moveEvents = new ArrayList<>(pendingMoveEvents);
        pendingMoveEvents.clear();
        return moveEvents;
    }

    public void loadGameState(GameState loadedGameState) {
        PlayerType currentLocalPlayerType = gameState.getLocalPlayerType();
        gameState.copyFrom(loadedGameState);
        gameState.setLocalPlayerType(currentLocalPlayerType);
        if (gameState.getGameStatus() == GameStatus.FINISHED
                || gameState.getGameStatus() == GameStatus.REPLAYING) {
            gameState.setGameStatus(GameStatus.READY);
        }
        pendingMoveEvents.clear();
        prepareModeDefaults();
    }
    public void applyNetworkState(GameState networkGameState) {
        PlayerType currentLocalPlayerType = gameState.getLocalPlayerType();
        gameState.copyFrom(networkGameState);
        gameState.setLocalPlayerType(currentLocalPlayerType);
        pendingMoveEvents.clear();
    }


    public void startNewGame() {
        PlayerType localPlayerType = gameState.getLocalPlayerType();
        gameState.resetForNewGame();
        gameState.setLocalPlayerType(localPlayerType);
        pendingMoveEvents.clear();
        prepareModeDefaults();
        singlePlayerOpponentDirection = 1;
        gameState.setGameStatus(GameStatus.READY);
    }

    public void tickTimer() {
        if (gameState.getGameStatus() == GameStatus.PAUSED
                || gameState.getGameStatus() == GameStatus.FINISHED
                || gameState.getGameStatus() == GameStatus.REPLAYING) {
            return;
        }

        if (gameState.getTimeLeft() <= 0) {
            finishGameByTimer();
            addMoveEvent(HockeyMoveType.GAME_OVER, gameState.getWinner());
            return;
        }

        gameState.setTimeLeft(gameState.getTimeLeft() - 1);
        if (gameState.getTimeLeft() <= 0) {
            finishGameByTimer();
            addMoveEvent(HockeyMoveType.GAME_OVER, gameState.getWinner());
        }
    }

    public void finishGameByTimer() {
        gameState.setTimeLeft(0);
        gameState.setWinner(gameState.calculateScoreLeader());
        gameState.setGameStatus(GameStatus.FINISHED);
        gameState.getPlayerOne().stop();
        gameState.getPlayerTwo().stop();
        gameState.getPuck().stop();
    }

    public void pauseGame() {
        if (gameState.getGameStatus() != GameStatus.FINISHED
                && gameState.getGameStatus() != GameStatus.REPLAYING) {
            gameState.setGameStatus(GameStatus.PAUSED);
        }
    }

    public void resumeGame() {
        if (gameState.getGameStatus() == GameStatus.PAUSED) {
            gameState.setGameStatus(GameStatus.READY);
        }
    }

    public void updateAimAngle() {
        updateAimAngle(DEFAULT_AIM_ROTATION_SPEED);
    }

    public void updateAimAngle(double deltaRadians) {
        if (gameState.getGameStatus() == GameStatus.READY
                || gameState.getGameStatus() == GameStatus.RUNNING) {
            double nextAngle = gameState.getAimAngleRadians() + deltaRadians;
            gameState.setAimAngleRadians(normalizeAngle(nextAngle));
        }
    }

    public void launchActivePlayer() {
        if (gameState.getGameStatus() != GameStatus.READY
                && gameState.getGameStatus() != GameStatus.RUNNING) {
            return;
        }

        if (isSinglePlayerMode()) {
            gameState.setActivePlayer(PlayerType.PLAYER_1);
        }

        gameState.setLastScoringPlayer(null);
        Player activePlayer = gameState.getActivePlayerModel();
        activePlayer.launch(gameState.getAimAngleRadians());
        gameState.setGameStatus(GameStatus.LAUNCHING);
        addMoveEvent(HockeyMoveType.LAUNCH, gameState.getActivePlayer());
    }

    public void updateFrame() {
        if (gameState.getGameStatus() == GameStatus.PAUSED
                || gameState.getGameStatus() == GameStatus.FINISHED
                || gameState.getGameStatus() == GameStatus.REPLAYING) {
            return;
        }

        Player playerOne = gameState.getPlayerOne();
        Player playerTwo = gameState.getPlayerTwo();
        Puck puck = gameState.getPuck();

        moveSinglePlayerOpponent();

        playerOne.move();
        if (!isSinglePlayerMode()) {
            playerTwo.move();
        }
        puck.move();

        handlePlayerPuckImpact(playerOne, puck);
        if (isSinglePlayerMode()) {
            handleSinglePlayerOpponentPuckImpact(playerTwo, puck);
        } else {
            handlePlayerPuckImpact(playerTwo, puck);
        }

        if (handleGoalIfScored(puck)) {
            return;
        }

        bouncePuckFromWalls(puck);

        CollisionService.applyFriction(playerOne, PLAYER_FRICTION);
        if (!isSinglePlayerMode()) {
            CollisionService.applyFriction(playerTwo, PLAYER_FRICTION);
        }
        CollisionService.applyFriction(puck, PUCK_FRICTION);

        CollisionService.stopIfSlow(playerOne);
        if (!isSinglePlayerMode()) {
            CollisionService.stopIfSlow(playerTwo);
        }
        CollisionService.stopIfSlow(puck);

        if (isTurnMovementSettled() && gameState.getGameStatus() != GameStatus.READY) {
            switchTurn();
        }
    }

    public void switchTurn() {
        if (isSinglePlayerMode()) {
            gameState.setActivePlayer(PlayerType.PLAYER_1);
            gameState.setAimAngleRadians(0);
            gameState.setGameStatus(GameStatus.READY);
            return;
        }

        gameState.switchTurn();
    }

    public boolean isPhysicsSettled() {
        return isTurnMovementSettled();
    }

    private void prepareModeDefaults() {
        if (isSinglePlayerMode()) {
            gameState.setActivePlayer(PlayerType.PLAYER_1);
            gameState.getPlayerTwo().stop();
            double minY = RINK_MIN_Y + gameState.getPlayerTwo().getRadius();
            double maxY = RINK_MAX_Y - gameState.getPlayerTwo().getRadius();
            gameState.getPlayerTwo().setY(Math.max(minY, Math.min(maxY, gameState.getPlayerTwo().getY())));
        }
    }

    private void moveSinglePlayerOpponent() {
        if (!isSinglePlayerMode()) {
            return;
        }

        Player opponent = gameState.getPlayerTwo();
        double nextY = opponent.getY() + SINGLE_PLAYER_OPPONENT_SPEED * singlePlayerOpponentDirection;
        double minY = RINK_MIN_Y + opponent.getRadius();
        double maxY = RINK_MAX_Y - opponent.getRadius();

        if (nextY <= minY) {
            nextY = minY;
            singlePlayerOpponentDirection = 1;
        } else if (nextY >= maxY) {
            nextY = maxY;
            singlePlayerOpponentDirection = -1;
        }

        opponent.setY(nextY);
        opponent.stop();
    }

    private boolean handleGoalIfScored(Puck puck) {
        if (puck.getY() < GOAL_MIN_Y || puck.getY() > GOAL_MAX_Y) {
            return false;
        }

        if (puck.getX() - puck.getRadius() <= RINK_MIN_X) {
            scoreFor(PlayerType.PLAYER_2);
            return true;
        }

        if (puck.getX() + puck.getRadius() >= RINK_MAX_X) {
            scoreFor(PlayerType.PLAYER_1);
            return true;
        }

        return false;
    }

    private void scoreFor(PlayerType scoringPlayer) {
        Player scorer = scoringPlayer == PlayerType.PLAYER_1 ? gameState.getPlayerOne() : gameState.getPlayerTwo();
        scorer.setScore(scorer.getScore() + 1);
        gameState.setLastScoringPlayer(scoringPlayer);
        addMoveEvent(HockeyMoveType.GOAL, scoringPlayer);

        PlayerType winner = gameState.calculateWinner();
        if (winner != null) {
            gameState.setWinner(winner);
            gameState.setGameStatus(GameStatus.FINISHED);
            gameState.getPlayerOne().stop();
            gameState.getPlayerTwo().stop();
            gameState.getPuck().stop();
            addMoveEvent(HockeyMoveType.GAME_OVER, winner);
            return;
        }

        gameState.resetPositionsAfterGoal();
        prepareModeDefaults();
        gameState.setGameStatus(GameStatus.READY);
    }

    private boolean isTurnMovementSettled() {
        boolean playersSettled = isSinglePlayerMode()
                ? !gameState.getPlayerOne().isMoving()
                : !gameState.getPlayerOne().isMoving() && !gameState.getPlayerTwo().isMoving();

        return playersSettled && !gameState.getPuck().isMoving();
    }

    private boolean isSinglePlayerMode() {
        return gameState.getLocalPlayerType() == PlayerType.SINGLE_PLAYER;
    }

    private void handlePlayerPuckImpact(Player player, Puck puck) {
        if (CollisionService.playerHitsPuck(player, puck) && player.isMoving()) {
            CollisionService.transferPlayerImpactToPuck(player, puck);
            gameState.setGameStatus(GameStatus.PUCK_MOVING);
            addMoveEvent(HockeyMoveType.PUCK_HIT, player.getPlayerType());
        }
    }

    private void handleSinglePlayerOpponentPuckImpact(Player opponent, Puck puck) {
        double opponentVelocityY = SINGLE_PLAYER_OPPONENT_SPEED * singlePlayerOpponentDirection;
        if (CollisionService.deflectPuckFromKinematicPlayer(opponent, puck, 0, opponentVelocityY)) {
            gameState.setGameStatus(GameStatus.PUCK_MOVING);
            addMoveEvent(HockeyMoveType.PUCK_HIT, PlayerType.PLAYER_2);
        }
    }

    private void bouncePuckFromWalls(Puck puck) {
        if (puck.getX() - puck.getRadius() <= RINK_MIN_X) {
            puck.setX(RINK_MIN_X + puck.getRadius());
            puck.setVelocityX(Math.abs(puck.getVelocityX()));
        } else if (puck.getX() + puck.getRadius() >= RINK_MAX_X) {
            puck.setX(RINK_MAX_X - puck.getRadius());
            puck.setVelocityX(-Math.abs(puck.getVelocityX()));
        }

        if (puck.getY() - puck.getRadius() <= RINK_MIN_Y) {
            puck.setY(RINK_MIN_Y + puck.getRadius());
            puck.setVelocityY(Math.abs(puck.getVelocityY()));
        } else if (puck.getY() + puck.getRadius() >= RINK_MAX_Y) {
            puck.setY(RINK_MAX_Y - puck.getRadius());
            puck.setVelocityY(-Math.abs(puck.getVelocityY()));
        }
    }

    private void addMoveEvent(HockeyMoveType moveType, PlayerType playerType) {
        PlayerType eventPlayerType = playerType == null ? gameState.getActivePlayer() : playerType;
        pendingMoveEvents.add(new HockeyMove(moveType, eventPlayerType, gameState));
    }

    private double normalizeAngle(double angleRadians) {
        double normalizedAngle = angleRadians % FULL_CIRCLE_RADIANS;
        return normalizedAngle < 0 ? normalizedAngle + FULL_CIRCLE_RADIANS : normalizedAngle;
    }
}
