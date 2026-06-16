package hr.algebra.hockey.model;

import java.io.Serial;
import java.io.Serializable;

public class Player implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private PlayerType playerType;
    private double x;
    private double y;
    private double radius;
    private double velocityX;
    private double velocityY;
    private double launchPower;
    private int score;

    public Player() {
    }

    public Player(PlayerType playerType, double x, double y, double radius, double launchPower) {
        this.playerType = playerType;
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.launchPower = launchPower;
    }

    public PlayerType getPlayerType() {
        return playerType;
    }

    public void setPlayerType(PlayerType playerType) {
        this.playerType = playerType;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public double getVelocityX() {
        return velocityX;
    }

    public void setVelocityX(double velocityX) {
        this.velocityX = velocityX;
    }

    public double getVelocityY() {
        return velocityY;
    }

    public void setVelocityY(double velocityY) {
        this.velocityY = velocityY;
    }

    public double getLaunchPower() {
        return launchPower;
    }

    public void setLaunchPower(double launchPower) {
        this.launchPower = launchPower;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void launch(double aimAngleRadians) {
        velocityX = Math.cos(aimAngleRadians) * launchPower;
        velocityY = Math.sin(aimAngleRadians) * launchPower;
    }

    public void move() {
        x += velocityX;
        y += velocityY;
    }

    public void stop() {
        velocityX = 0;
        velocityY = 0;
    }

    public boolean isMoving() {
        return Math.abs(velocityX) > 0.01 || Math.abs(velocityY) > 0.01;
    }

    public void reset(double x, double y) {
        this.x = x;
        this.y = y;
        stop();
    }
}