package hr.algebra.hockey.utils;

import hr.algebra.hockey.model.GameState;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class GameSaveUtils {
    private static final Path SAVE_GAME_PATH = Path.of("game", "save.dat");

    private GameSaveUtils() {
    }

    public static void saveGame(GameState gameState) throws IOException {
        Files.createDirectories(SAVE_GAME_PATH.getParent());
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(Files.newOutputStream(SAVE_GAME_PATH))) {
            objectOutputStream.writeObject(gameState);
        }
    }

    public static GameState loadGame() throws IOException, ClassNotFoundException {
        try (ObjectInputStream objectInputStream = new ObjectInputStream(Files.newInputStream(SAVE_GAME_PATH))) {
            return (GameState) objectInputStream.readObject();
        }
    }

    public static boolean saveGameExists() {
        return Files.exists(SAVE_GAME_PATH);
    }
}