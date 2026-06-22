package hr.algebra.hockey.engine;

import hr.algebra.hockey.model.GameState;
import hr.algebra.hockey.model.GameStatus;
import hr.algebra.hockey.model.PlayerType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HockeyGameEngineTest {
    @Test
    void singlePlayerAlwaysReturnsTurnToPlayerOne() {
        HockeyGameEngine engine = new HockeyGameEngine(PlayerType.SINGLE_PLAYER);
        engine.getGameState().setActivePlayer(PlayerType.PLAYER_2);

        engine.switchTurn();

        assertEquals(PlayerType.PLAYER_1, engine.getGameState().getActivePlayer());
        assertEquals(GameStatus.READY, engine.getGameState().getGameStatus());
    }

    @Test
    void multiplayerSwitchesToTheOtherPlayer() {
        HockeyGameEngine engine = new HockeyGameEngine(PlayerType.PLAYER_1);

        engine.switchTurn();

        assertEquals(PlayerType.PLAYER_2, engine.getGameState().getActivePlayer());
    }

    @Test
    void timerFinishesWithDrawWhenScoresAreEqual() {
        HockeyGameEngine engine = new HockeyGameEngine(PlayerType.SINGLE_PLAYER);
        engine.getGameState().setTimeLeft(1);

        engine.tickTimer();

        assertEquals(0, engine.getGameState().getTimeLeft());
        assertEquals(GameStatus.FINISHED, engine.getGameState().getGameStatus());
        assertNull(engine.getGameState().getWinner());
    }

    @Test
    void networkSnapshotDoesNotShareMutableObjects() {
        GameState original = new GameState();
        GameState snapshot = original.createSnapshot();

        snapshot.getPlayerOne().setX(500);
        snapshot.getPuck().setVelocityX(9);

        assertNotSame(original.getPlayerOne(), snapshot.getPlayerOne());
        assertNotSame(original.getPuck(), snapshot.getPuck());
        assertTrue(original.getPlayerOne().getX() != snapshot.getPlayerOne().getX());
        assertTrue(original.getPuck().getVelocityX() != snapshot.getPuck().getVelocityX());
    }
}