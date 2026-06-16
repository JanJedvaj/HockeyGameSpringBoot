package hr.algebra.hockey.engine;

import hr.algebra.hockey.model.GameState;
import hr.algebra.hockey.model.GameStatus;
import hr.algebra.hockey.model.Player;
import hr.algebra.hockey.model.PlayerType;
import hr.algebra.hockey.model.Puck;

public class HockeyGameEngine {
    private static final double FULL_CIRCLE_RADIANS = Math.PI * 2;
    private static final double DEFAULT_AIM_ROTATION_SPEED = 0.045;
    private static final double PLAYER_FRICTION = 0.95;
    private static final double PUCK_FRICTION = 0.988;
    private static final double SINGLE_PLAYER_OPPONENT_SPEED = 2.25;
    private static final double RINK_MIN_X = 12;
    private static final double RINK_MIN_Y = 12;
    private static final double RINK_MAX_X = 748;
    private static final double RINK_MAX_Y = 608;

    private final GameState gameState;
    private int singlePlayerOpponentDirection = 1;

    public HockeyGameEngine() {
        gameState = new GameState();
    }

    public HockeyGameEngine(PlayerType localPlayerType) {
        this();
        gameState.setLocalPlayerType(localPlayerType);
        prepareModeDefaults();
    }

    public HockeyGameEngine(GameState gameState) {
        this.gameState = gameState;
        prepareModeDefaults();
    }

    public GameState getGameState() {
        return gameState;
    }

    public void startNewGame() {
        PlayerType localPlayerType = gameState.getLocalPlayerType();
        gameState.resetForNewGame();
        gameState.setLocalPlayerType(localPlayerType);
        prepareModeDefaults();
        singlePlayerOpponentDirection = 1;
        gameState.setGameStatus(GameStatus.READY);
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

        Player activePlayer = gameState.getActivePlayerModel();
        activePlayer.launch(gameState.getAimAngleRadians());
        gameState.setGameStatus(GameStatus.LAUNCHING);
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
        handlePlayerPuckImpact(playerTwo, puck);
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

    private double normalizeAngle(double angleRadians) {
        double normalizedAngle = angleRadians % FULL_CIRCLE_RADIANS;
        return normalizedAngle < 0 ? normalizedAngle + FULL_CIRCLE_RADIANS : normalizedAngle;
    }
}