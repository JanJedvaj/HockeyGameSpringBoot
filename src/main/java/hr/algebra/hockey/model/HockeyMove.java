package hr.algebra.hockey.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class HockeyMove implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private HockeyMoveType moveType;
    private PlayerType playerType;
    private LocalDateTime timestamp;
    private int playerOneScore;
    private int playerTwoScore;
    private double playerOneX;
    private double playerOneY;
    private double playerTwoX;
    private double playerTwoY;
    private double puckX;
    private double puckY;
    private int timeLeft;

    public HockeyMove(HockeyMoveType moveType, PlayerType playerType, GameState gameState) {
        this.moveType = moveType;
        this.playerType = playerType;
        this.timestamp = LocalDateTime.now();
        this.playerOneScore = gameState.getPlayerOne().getScore();
        this.playerTwoScore = gameState.getPlayerTwo().getScore();
        this.playerOneX = gameState.getPlayerOne().getX();
        this.playerOneY = gameState.getPlayerOne().getY();
        this.playerTwoX = gameState.getPlayerTwo().getX();
        this.playerTwoY = gameState.getPlayerTwo().getY();
        this.puckX = gameState.getPuck().getX();
        this.puckY = gameState.getPuck().getY();
        this.timeLeft = gameState.getTimeLeft();
    }

}
