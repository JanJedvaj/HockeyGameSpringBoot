package hr.algebra.hockey.utils;

import hr.algebra.hockey.model.GameState;
import hr.algebra.hockey.model.GameStatus;
import hr.algebra.hockey.model.PlayerType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameUtilsTest {
    @Test
    void singlePlayerAlwaysReturnsTurnToPlayerOne() {
        GameUtils gameUtils = new GameUtils(PlayerType.SINGLE_PLAYER);
        gameUtils.getGameState().setActivePlayer(PlayerType.PLAYER_2);

        gameUtils.switchTurn();

        assertEquals(PlayerType.PLAYER_1, gameUtils.getGameState().getActivePlayer());
        assertEquals(GameStatus.READY, gameUtils.getGameState().getGameStatus());
    }

    @Test
    void multiplayerSwitchesToTheOtherPlayer() {
        GameUtils gameUtils = new GameUtils(PlayerType.PLAYER_1);

        gameUtils.switchTurn();

        assertEquals(PlayerType.PLAYER_2, gameUtils.getGameState().getActivePlayer());
    }

    @Test
    void timerFinishesWithDrawWhenScoresAreEqual() {
        GameUtils gameUtils = new GameUtils(PlayerType.SINGLE_PLAYER);
        gameUtils.getGameState().setTimeLeft(1);

        gameUtils.tickTimer();

        assertEquals(0, gameUtils.getGameState().getTimeLeft());
        assertEquals(GameStatus.FINISHED, gameUtils.getGameState().getGameStatus());
        assertNull(gameUtils.getGameState().getWinner());
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

    @Test
    void singlePlayerOpponentDeflectsThePuck() {
        GameUtils gameUtils = new GameUtils(PlayerType.SINGLE_PLAYER);
        GameState gameState = gameUtils.getGameState();
        gameState.getPuck().setX(600);
        gameState.getPuck().setY(310);
        gameState.getPuck().setVelocityX(6);

        gameUtils.updateFrame();

        assertTrue(gameState.getPuck().getVelocityX() < 0);
        assertEquals(GameStatus.PUCK_MOVING, gameState.getGameStatus());
    }

    @Test
    void playerAndOpponentDoNotTransferImpactToEachOther() {
        GameUtils gameUtils = new GameUtils(PlayerType.SINGLE_PLAYER);
        GameState gameState = gameUtils.getGameState();
        gameState.getPlayerOne().setX(600);
        gameState.getPlayerOne().setY(310);
        gameState.getPlayerOne().setVelocityX(5);

        gameUtils.updateFrame();

        assertEquals(605, gameState.getPlayerOne().getX(), 0.001);
        assertEquals(4.75, gameState.getPlayerOne().getVelocityX(), 0.001);
    }
}
