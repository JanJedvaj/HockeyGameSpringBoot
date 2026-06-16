package hr.algebra.hockey.model;

import java.io.Serial;
import java.io.Serializable;

public class Puck implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private double x;
    private double y;
    private double radius;
    private double velocityX;
    private double velocityY;

    public Puck() {
    }

    public Puck(double x, double y, double radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
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