package hr.algebra.hockey.thread;

import hr.algebra.hockey.model.HockeyMove;
import javafx.application.Platform;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class ReadTheLastHockeyMoveThread extends AbstractTheLastHockeyMoveThread implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ReadTheLastHockeyMoveThread.class.getName());

    private final Consumer<HockeyMove> lastMoveConsumer;

    @Override
    public void run() {
        try {
            List<HockeyMove> hockeyMoves = loadHockeyMoves();
            if (!hockeyMoves.isEmpty()) {
                HockeyMove lastMove = hockeyMoves.getLast();
                Platform.runLater(() -> lastMoveConsumer.accept(lastMove));
            }
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Could not read the latest hockey move", exception);
        }
    }
}
