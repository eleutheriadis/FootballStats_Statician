package com.epo.footballstats.models;

/**
 * Συγκεντρωτικά στατιστικά παίκτη για έναν αγώνα (R4).
 * Υπολογίζεται client-side από τη λίστα {@link MatchStat}.
 */
public class PlayerStat {
    public final String playerId;
    public final String playerName;
    public final String teamName;

    public int goals;
    public int assists;
    public int yellowCards;
    public int redCards;
    public int shots;
    public int shotsOnTarget;

    public PlayerStat(String playerId, String playerName, String teamName) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.teamName = teamName;
    }

    /** Score χρήσιμο για ταξινόμηση: γκολ > ασσιστ. */
    public int impactScore() {
        return goals * 10 + assists * 5;
    }
}
