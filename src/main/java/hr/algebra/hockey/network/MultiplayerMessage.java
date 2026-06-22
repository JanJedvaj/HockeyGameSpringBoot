package hr.algebra.hockey.network;

import hr.algebra.hockey.model.GameState;

import java.io.Serial;
import java.io.Serializable;

public class MultiplayerMessage implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public enum Type {
        GAME_STATE,
        LAUNCH_REQUEST
    }

    private final Type type;
    private final GameState gameState;

    private MultiplayerMessage(Type type, GameState gameState) {
        this.type = type;
        this.gameState = gameState;
    }

    public static MultiplayerMessage gameState(GameState gameState) {
        return new MultiplayerMessage(Type.GAME_STATE, gameState);
    }

    public static MultiplayerMessage launchRequest() {
        return new MultiplayerMessage(Type.LAUNCH_REQUEST, null);
    }

    public Type getType() {
        return type;
    }

    public GameState getGameState() {
        return gameState;
    }
}