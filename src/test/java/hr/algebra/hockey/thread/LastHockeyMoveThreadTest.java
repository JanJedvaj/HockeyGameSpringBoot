package hr.algebra.hockey.thread;

import hr.algebra.hockey.model.GameState;
import hr.algebra.hockey.model.HockeyMove;
import hr.algebra.hockey.model.HockeyMoveType;
import hr.algebra.hockey.model.PlayerType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LastHockeyMoveThreadTest {
    private static final String MOVE_HISTORY_PROPERTY = "hockey.move.history.path";

    @TempDir
    Path temporaryDirectory;

    @BeforeEach
    void configureTemporaryMoveHistory() {
        System.setProperty(MOVE_HISTORY_PROPERTY, temporaryDirectory.resolve("gameMoves.dat").toString());
    }

    @AfterEach
    void clearTemporaryMoveHistory() {
        System.clearProperty(MOVE_HISTORY_PROPERTY);
    }

    @Test
    void savedMoveCanBeReadBack() {
        TestMoveAccess moveAccess = new TestMoveAccess();
        moveAccess.save(move(HockeyMoveType.GAME_START));

        List<HockeyMove> loadedMoves = moveAccess.load();

        assertEquals(1, loadedMoves.size());
        assertEquals(HockeyMoveType.GAME_START, loadedMoves.getFirst().getMoveType());
    }

    @Test
    void concurrentWritersDoNotLoseMoves() throws InterruptedException {
        int numberOfMoves = 12;
        ExecutorService executor = Executors.newFixedThreadPool(4);
        for (int index = 0; index < numberOfMoves; index++) {
            executor.submit(() -> new TestMoveAccess().save(move(HockeyMoveType.LAUNCH)));
        }
        executor.shutdown();

        boolean completed = executor.awaitTermination(5, TimeUnit.SECONDS);
        List<HockeyMove> loadedMoves = new TestMoveAccess().load();

        assertEquals(true, completed);
        assertEquals(numberOfMoves, loadedMoves.size());
    }

    private HockeyMove move(HockeyMoveType moveType) {
        return new HockeyMove(moveType, PlayerType.PLAYER_1, new GameState());
    }

    private static final class TestMoveAccess extends AbstractTheLastHockeyMoveThread {
        void save(HockeyMove hockeyMove) {
            saveTheLastHockeyMove(hockeyMove);
        }

        List<HockeyMove> load() {
            return loadHockeyMoves();
        }
    }
}
