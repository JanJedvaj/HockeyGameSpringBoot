package hr.algebra.hockey.model;

public enum HockeyMoveTag {
    GAME_MOVES("GameMoves"),
    HOCKEY_MOVE("HockeyMove"),
    MOVE_TYPE("MoveType"),
    PLAYER_TYPE("PlayerType"),
    TIMESTAMP("Timestamp"),
    PLAYER_ONE_SCORE("PlayerOneScore"),
    PLAYER_TWO_SCORE("PlayerTwoScore"),
    PLAYER_ONE_X("PlayerOneX"),
    PLAYER_ONE_Y("PlayerOneY"),
    PLAYER_TWO_X("PlayerTwoX"),
    PLAYER_TWO_Y("PlayerTwoY"),
    PUCK_X("PuckX"),
    PUCK_Y("PuckY"),
    TIME_LEFT("TimeLeft");

    private final String tagName;

    HockeyMoveTag(String tagName) {
        this.tagName = tagName;
    }

    public String getTagName() {
        return tagName;
    }
}