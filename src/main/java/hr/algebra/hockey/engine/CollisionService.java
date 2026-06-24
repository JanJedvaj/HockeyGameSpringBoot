package hr.algebra.hockey.engine;

import hr.algebra.hockey.model.Player;
import hr.algebra.hockey.model.Puck;

public final class CollisionService {
    private static final double DEFAULT_STOP_THRESHOLD = 0.05;
    private static final double MINIMUM_PUCK_IMPACT_SPEED = 9.0;
    private static final double PUCK_IMPACT_MULTIPLIER = 1.75;
    private static final double KINEMATIC_COLLISION_RESTITUTION = 0.9;

    private CollisionService() {
    }

    public static boolean circlesCollide(double firstX, double firstY, double firstRadius,
                                         double secondX, double secondY, double secondRadius) {
        return distance(firstX, firstY, secondX, secondY) <= firstRadius + secondRadius;
    }

    public static boolean playerHitsPuck(Player player, Puck puck) {
        return circlesCollide(
                player.getX(),
                player.getY(),
                player.getRadius(),
                puck.getX(),
                puck.getY(),
                puck.getRadius()
        );
    }

    public static void transferPlayerImpactToPuck(Player player, Puck puck) {
        double deltaX = puck.getX() - player.getX();
        double deltaY = puck.getY() - player.getY();
        double distance = Math.max(distance(player.getX(), player.getY(), puck.getX(), puck.getY()), 0.001);
        double normalX = deltaX / distance;
        double normalY = deltaY / distance;
        double playerSpeed = Math.sqrt(
                player.getVelocityX() * player.getVelocityX()
                        + player.getVelocityY() * player.getVelocityY()
        );
        double puckSpeed = Math.max(playerSpeed * PUCK_IMPACT_MULTIPLIER, MINIMUM_PUCK_IMPACT_SPEED);

        puck.setVelocityX(normalX * puckSpeed);
        puck.setVelocityY(normalY * puckSpeed);

        double minimumDistance = player.getRadius() + puck.getRadius() + 1;
        puck.setX(player.getX() + normalX * minimumDistance);
        puck.setY(player.getY() + normalY * minimumDistance);
    }

    public static boolean deflectPuckFromKinematicPlayer(
            Player player,
            Puck puck,
            double playerVelocityX,
            double playerVelocityY) {
        if (!playerHitsPuck(player, puck)) {
            return false;
        }

        double deltaX = puck.getX() - player.getX();
        double deltaY = puck.getY() - player.getY();
        double collisionDistance = distance(player.getX(), player.getY(), puck.getX(), puck.getY());
        if (collisionDistance < 0.001) {
            deltaX = 1;
            deltaY = 0;
            collisionDistance = 1;
        }

        double normalX = deltaX / collisionDistance;
        double normalY = deltaY / collisionDistance;
        double minimumDistance = player.getRadius() + puck.getRadius() + 1;
        puck.setX(player.getX() + normalX * minimumDistance);
        puck.setY(player.getY() + normalY * minimumDistance);

        double relativeVelocityX = puck.getVelocityX() - playerVelocityX;
        double relativeVelocityY = puck.getVelocityY() - playerVelocityY;
        double velocityAlongNormal = relativeVelocityX * normalX + relativeVelocityY * normalY;
        if (velocityAlongNormal >= 0) {
            return false;
        }

        double impulse = -(1 + KINEMATIC_COLLISION_RESTITUTION) * velocityAlongNormal;
        puck.setVelocityX(relativeVelocityX + impulse * normalX + playerVelocityX);
        puck.setVelocityY(relativeVelocityY + impulse * normalY + playerVelocityY);
        return true;
    }

    public static double distance(double firstX, double firstY, double secondX, double secondY) {
        double deltaX = secondX - firstX;
        double deltaY = secondY - firstY;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }

    public static void applyFriction(Player player, double friction) {
        player.setVelocityX(applyFriction(player.getVelocityX(), friction));
        player.setVelocityY(applyFriction(player.getVelocityY(), friction));
    }

    public static void applyFriction(Puck puck, double friction) {
        puck.setVelocityX(applyFriction(puck.getVelocityX(), friction));
        puck.setVelocityY(applyFriction(puck.getVelocityY(), friction));
    }

    public static boolean isMoving(Player player) {
        return isMoving(player.getVelocityX(), player.getVelocityY(), DEFAULT_STOP_THRESHOLD);
    }

    public static boolean isMoving(Puck puck) {
        return isMoving(puck.getVelocityX(), puck.getVelocityY(), DEFAULT_STOP_THRESHOLD);
    }

    public static boolean isMoving(double velocityX, double velocityY, double stopThreshold) {
        return Math.abs(velocityX) > stopThreshold || Math.abs(velocityY) > stopThreshold;
    }

    public static void stopIfSlow(Player player) {
        if (!isMoving(player)) {
            player.stop();
        }
    }

    public static void stopIfSlow(Puck puck) {
        if (!isMoving(puck)) {
            puck.stop();
        }
    }

    private static double applyFriction(double velocity, double friction) {
        double nextVelocity = velocity * friction;
        return Math.abs(nextVelocity) <= DEFAULT_STOP_THRESHOLD ? 0 : nextVelocity;
    }
}
