package hr.algebra.hockey.thread;

import hr.algebra.hockey.model.HockeyMove;
import lombok.RequiredArgsConstructor;

import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class SaveTheLastHockeyMoveThread extends AbstractTheLastHockeyMoveThread implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(SaveTheLastHockeyMoveThread.class.getName());

    private final HockeyMove hockeyMove;

    @Override
    public void run() {
        try {
            saveTheLastHockeyMove(hockeyMove);
            LOGGER.fine("Saved the latest hockey move on " + Thread.currentThread().getName());
        } catch (RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Could not save the latest hockey move", exception);
        }
    }
}
