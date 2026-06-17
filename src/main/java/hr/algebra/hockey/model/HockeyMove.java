package hr.algebra.hockey.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public class HockeyMove implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private HockeyMoveType moveType;
    private PlayerType playerType;
    private LocalDateTime timestamp;
    private int playerOneScore;
    private int playerTwoScore;
    private double playerOneX;
    private double playerOneY;
    private double playerTwoX;
    private double playerTwoY;
    private double puckX;
    private double puckY;
    private int timeLeft;

    public HockeyMove() {
    }

    public HockeyMove(HockeyMoveType moveType, PlayerType playerType, GameState gameState) {
        this.moveType = moveType;
        this.playerType = playerType;
        this.timestamp = LocalDateTime.now();
        this.playerOneScore = gameState.getPlayerOne().getScore();
        this.playerTwoScore = gameState.getPlayerTwo().getScore();
        this.playerOneX = gameState.getPlayerOne().getX();
        this.playerOneY = gameState.getPlayerOne().getY();
        this.playerTwoX = gameState.getPlayerTwo().getX();
        this.playerTwoY = gameState.getPlayerTwo().getY();
        this.puckX = gameState.getPuck().getX();
        this.puckY = gameState.getPuck().getY();
        this.timeLeft = gameState.getTimeLeft();
    }

    public HockeyMoveType getMoveType() {
        return moveType;
    }

    public void setMoveType(HockeyMoveType moveType) {
        this.moveType = moveType;
    }

    public PlayerType getPlayerType() {
        return playerType;
    }

    public void setPlayerType(PlayerType playerType) {
        this.playerType = playerType;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getPlayerOneScore() {
        return playerOneScore;
    }

    public void setPlayerOneScore(int playerOneScore) {
        this.playerOneScore = playerOneScore;
    }

    public int getPlayerTwoScore() {
        return playerTwoScore;
    }

    public void setPlayerTwoScore(int playerTwoScore) {
        this.playerTwoScore = playerTwoScore;
    }

    public double getPlayerOneX() {
        return playerOneX;
    }

    public void setPlayerOneX(double playerOneX) {
        this.playerOneX = playerOneX;
    }

    public double getPlayerOneY() {
        return playerOneY;
    }

    public void setPlayerOneY(double playerOneY) {
        this.playerOneY = playerOneY;
    }

    public double getPlayerTwoX() {
        return playerTwoX;
    }

    public void setPlayerTwoX(double playerTwoX) {
        this.playerTwoX = playerTwoX;
    }

    public double getPlayerTwoY() {
        return playerTwoY;
    }

    public void setPlayerTwoY(double playerTwoY) {
        this.playerTwoY = playerTwoY;
    }

    public double getPuckX() {
        return puckX;
    }

    public void setPuckX(double puckX) {
        this.puckX = puckX;
    }

    public double getPuckY() {
        return puckY;
    }

    public void setPuckY(double puckY) {
        this.puckY = puckY;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public void setTimeLeft(int timeLeft) {
        this.timeLeft = timeLeft;
    }
}