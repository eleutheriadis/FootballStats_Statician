package com.epo.footballstats.models;

import com.google.firebase.Timestamp;

/**
 * Καταγραφή στατιστικού γεγονότος αγώνα (R2).
 * Firestore path: matches/{matchId}/stats/{statId}
 *
 * Τύποι (type):
 *   SHOT       - Σουτ
 *   TACKLE     - Τακλίν
 *   PASS       - Πάσα
 *   CROSS      - Σέντρα
 *   ASSIST     - Ασσίστ
 *   ERROR      - Λάθος
 *   FOUL       - Φάουλ
 *   CORNER     - Κόρνερ
 *   CARD       - Κάρτα
 *
 * Για SHOT: shotType (ON_TARGET, OFF_TARGET, BLOCKED, HEADER), result (GOAL, NO_GOAL)
 * Για TACKLE/PASS/CROSS: result (SUCCESS, FAIL)
 * Για FOUL: direction (FOR, AGAINST)  -- FOR=Υπέρ, AGAINST=Κατά
 * Για CARD: cardColor (YELLOW, RED), direction (FOR, AGAINST)
 * Για CORNER: direction (FOR=κερδισμένο)
 */
public class MatchStat {
    private String id;
    private String type;
    private String teamId;
    private String teamName;
    private String playerId;
    private String playerName;
    private int minute;
    private String shotType;   // για SHOT
    private String result;     // SUCCESS/FAIL ή GOAL/NO_GOAL
    private String direction;  // FOR ή AGAINST (για FOUL, CARD, CORNER)
    private String cardColor;  // YELLOW ή RED (για CARD)
    private Timestamp timestamp;

    public MatchStat() {}

    // Getters & Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }

    public String getShotType() { return shotType; }
    public void setShotType(String shotType) { this.shotType = shotType; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getCardColor() { return cardColor; }
    public void setCardColor(String cardColor) { this.cardColor = cardColor; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    // Constants για τους τύπους
    public static final String TYPE_SHOT   = "SHOT";
    public static final String TYPE_TACKLE = "TACKLE";
    public static final String TYPE_PASS   = "PASS";
    public static final String TYPE_CROSS  = "CROSS";
    public static final String TYPE_ASSIST = "ASSIST";
    public static final String TYPE_ERROR  = "ERROR";
    public static final String TYPE_FOUL   = "FOUL";
    public static final String TYPE_CORNER = "CORNER";
    public static final String TYPE_CARD   = "CARD";

    public static final String SHOT_ON_TARGET = "ON_TARGET";
    public static final String SHOT_OFF_TARGET = "OFF_TARGET";
    public static final String SHOT_BLOCKED    = "BLOCKED";
    public static final String SHOT_HEADER     = "HEADER";

    public static final String RESULT_GOAL    = "GOAL";
    public static final String RESULT_NO_GOAL = "NO_GOAL";
    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_FAIL    = "FAIL";

    public static final String DIR_FOR     = "FOR";
    public static final String DIR_AGAINST = "AGAINST";

    public static final String CARD_YELLOW = "YELLOW";
    public static final String CARD_RED    = "RED";
}
