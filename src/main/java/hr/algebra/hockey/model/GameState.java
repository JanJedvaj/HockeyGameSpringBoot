package hr.algebra.hockey.model;

import java.io.Serial;
import java.io.Serializable;

public class GameState implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Player playerOne;
    private Player playerTwo;
    private Puck puck;
    private int timeLeft;
    private int winningScore;
    private double aimAngleRadians;
    private PlayerType activePlayer;
    private PlayerType localPlayerType;
    private PlayerType lastScoringPlayer;
    private PlayerType winner;
    private GameStatus gameStatus;

    public GameState() {
        playerOne = new Player(PlayerType.PLAYER_1, 120, 310, 24, 8);
        playerTwo = new Player(PlayerType.PLAYER_2, 640, 310, 24, 8);
        puck = new Puck(380, 310, 12);
        timeLeft = 120;
        winningScore = 5;
        aimAngleRadians = 0;
        activePlayer = PlayerType.PLAYER_1;
        localPlayerType = PlayerType.SINGLE_PLAYER;
        gameStatus = GameStatus.READY;
    }

    public Player getPlayerOne() {
        return playerOne;
    }

    public void setPlayerOne(Player playerOne) {
        this.playerOne = playerOne;
    }

    public Player getPlayerTwo() {
        return playerTwo;
    }

    public void setPlayerTwo(Player playerTwo) {
        this.playerTwo = playerTwo;
    }

    public Puck getPuck() {
        return puck;
    }

    public void setPuck(Puck puck) {
        this.puck = puck;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public void setTimeLeft(int timeLeft) {
        this.timeLeft = timeLeft;
    }

    public int getWinningScore() {
        return winningScore;
    }

    public void setWinningScore(int winningScore) {
        this.winningScore = winningScore;
    }

    public double getAimAngleRadians() {
        return aimAngleRadians;
    }

    public void setAimAngleRadians(double aimAngleRadians) {
        this.aimAngleRadians = aimAngleRadians;
    }

    public PlayerType getActivePlayer() {
        return activePlayer;
    }

    public void setActivePlayer(PlayerType activePlayer) {
        this.activePlayer = activePlayer;
    }

    public PlayerType getLocalPlayerType() {
        return localPlayerType;
    }

    public void setLocalPlayerType(PlayerType localPlayerType) {
        this.localPlayerType = localPlayerType;
    }

    public PlayerType getLastScoringPlayer() {
        return lastScoringPlayer;
    }

    public void setLastScoringPlayer(PlayerType lastScoringPlayer) {
        this.lastScoringPlayer = lastScoringPlayer;
    }

    public PlayerType getWinner() {
        return winner;
    }

    public void setWinner(PlayerType winner) {
        this.winner = winner;
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(GameStatus gameStatus) {
        this.gameStatus = gameStatus;
    }

    public Player getActivePlayerModel() {
        return activePlayer == PlayerType.PLAYER_2 ? playerTwo : playerOne;
    }

    public Player getInactivePlayerModel() {
        return activePlayer == PlayerType.PLAYER_2 ? playerOne : playerTwo;
    }

    public void switchTurn() {
        activePlayer = activePlayer == PlayerType.PLAYER_1 ? PlayerType.PLAYER_2 : PlayerType.PLAYER_1;
        aimAngleRadians = 0;
        gameStatus = GameStatus.READY;
    }

    public boolean hasWinner() {
        return playerOne.getScore() >= winningScore || playerTwo.getScore() >= winningScore;
    }

    public PlayerType calculateScoreLeader() {
        if (playerOne.getScore() > playerTwo.getScore()) {
            return PlayerType.PLAYER_1;
        }
        if (playerTwo.getScore() > playerOne.getScore()) {
            return PlayerType.PLAYER_2;
        }
        return null;
    }

    public PlayerType calculateWinner() {
        if (playerOne.getScore() >= winningScore) {
            return PlayerType.PLAYER_1;
        }
        if (playerTwo.getScore() >= winningScore) {
            return PlayerType.PLAYER_2;
        }
        return null;
    }

    public void copyFrom(GameState gameState) {
        playerOne = gameState.getPlayerOne();
        playerTwo = gameState.getPlayerTwo();
        puck = gameState.getPuck();
        timeLeft = gameState.getTimeLeft();
        winningScore = gameState.getWinningScore();
        aimAngleRadians = gameState.getAimAngleRadians();
        activePlayer = gameState.getActivePlayer();
        localPlayerType = gameState.getLocalPlayerType();
        lastScoringPlayer = gameState.getLastScoringPlayer();
        winner = gameState.getWinner();
        gameStatus = gameState.getGameStatus();
    }

    public void resetForNewGame() {
        playerOne.reset(120, 310);
        playerTwo.reset(640, 310);
        puck.reset(380, 310);
        playerOne.setScore(0);
        playerTwo.setScore(0);
        timeLeft = 120;
        aimAngleRadians = 0;
        activePlayer = PlayerType.PLAYER_1;
        lastScoringPlayer = null;
        winner = null;
        gameStatus = GameStatus.READY;
    }

    public void resetPositionsAfterGoal() {
        playerOne.reset(120, 310);
        playerTwo.reset(640, 310);
        puck.reset(380, 310);
        aimAngleRadians = 0;
        activePlayer = PlayerType.PLAYER_1;
    }
}