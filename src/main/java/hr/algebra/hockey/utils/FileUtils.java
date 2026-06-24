package hr.algebra.hockey.utils;

import java.nio.file.Path;

public final class FileUtils {
    private static final String DEFAULT_GAME_MOVE_HISTORY_FILE = "dat/gameMoves.dat";
    private static final String GAME_MOVE_HISTORY_PROPERTY = "hockey.move.history.path";

    private FileUtils() {
    }

    public static Path getGameMoveHistoryPath() {
        return Path.of(System.getProperty(GAME_MOVE_HISTORY_PROPERTY, DEFAULT_GAME_MOVE_HISTORY_FILE));
    }
}
