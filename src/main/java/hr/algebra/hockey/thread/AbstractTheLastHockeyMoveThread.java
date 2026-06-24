package hr.algebra.hockey.thread;

import hr.algebra.hockey.exception.ConcurrentAccessException;
import hr.algebra.hockey.exception.FileAccessException;
import hr.algebra.hockey.model.HockeyMove;
import hr.algebra.hockey.utils.FileUtils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTheLastHockeyMoveThread {
    private static final Object FILE_ACCESS_MONITOR = new Object();
    private static boolean fileAccessInProgress;

    protected void saveTheLastHockeyMove(HockeyMove hockeyMove) {
        beginFileAccess();
        try {
            List<HockeyMove> hockeyMoves = readMovesFromFile();
            hockeyMoves.add(hockeyMove);
            Files.createDirectories(FileUtils.getGameMoveHistoryPath().getParent());
            try (ObjectOutputStream output = new ObjectOutputStream(
                    Files.newOutputStream(FileUtils.getGameMoveHistoryPath()))) {
                output.writeObject(hockeyMoves);
            }
        } catch (IOException exception) {
            throw new FileAccessException("Could not serialize hockey move history.", exception);
        } finally {
            endFileAccess();
        }
    }

    protected List<HockeyMove> loadHockeyMoves() {
        beginFileAccess();
        try {
            return readMovesFromFile();
        } finally {
            endFileAccess();
        }
    }

    private List<HockeyMove> readMovesFromFile() {
        if (!Files.exists(FileUtils.getGameMoveHistoryPath())) {
            return new ArrayList<>();
        }

        try (ObjectInputStream input = new ObjectInputStream(
                Files.newInputStream(FileUtils.getGameMoveHistoryPath()))) {
            Object storedValue = input.readObject();
            if (!(storedValue instanceof List<?> storedMoves)) {
                throw new FileAccessException("The move history has an unexpected format.", null);
            }

            List<HockeyMove> hockeyMoves = new ArrayList<>();
            for (Object storedMove : storedMoves) {
                if (storedMove instanceof HockeyMove hockeyMove) {
                    hockeyMoves.add(hockeyMove);
                }
            }
            return hockeyMoves;
        } catch (IOException | ClassNotFoundException exception) {
            throw new FileAccessException("Could not deserialize hockey move history.", exception);
        }
    }

    private void beginFileAccess() {
        synchronized (FILE_ACCESS_MONITOR) {
            while (fileAccessInProgress) {
                try {
                    FILE_ACCESS_MONITOR.wait();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new ConcurrentAccessException("Interrupted while waiting for move history access.", exception);
                }
            }
            fileAccessInProgress = true;
        }
    }

    private void endFileAccess() {
        synchronized (FILE_ACCESS_MONITOR) {
            fileAccessInProgress = false;
            FILE_ACCESS_MONITOR.notifyAll();
        }
    }
}
