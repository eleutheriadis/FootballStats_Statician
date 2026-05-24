package com.epo.footballstats.models;

import com.google.firebase.Timestamp;

/**
 * Αλλαγή παίκτη κατά τη διάρκεια αγώνα.
 * Firestore path: matches/{matchId}/substitutions/{subId}
 */
public class Substitution {
    private String id;
    private String teamId;
    private String teamName;
    private String playerOutId;
    private String playerOutName;
    private String playerInId;
    private String playerInName;
    private int minute;
    private Timestamp timestamp;

    public Substitution() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public String getTeamName() { return teamName; }
    public void setTeamName(String teamName) { this.teamName = teamName; }

    public String getPlayerOutId() { return playerOutId; }
    public void setPlayerOutId(String playerOutId) { this.playerOutId = playerOutId; }

    public String getPlayerOutName() { return playerOutName; }
    public void setPlayerOutName(String playerOutName) { this.playerOutName = playerOutName; }

    public String getPlayerInId() { return playerInId; }
    public void setPlayerInId(String playerInId) { this.playerInId = playerInId; }

    public String getPlayerInName() { return playerInName; }
    public void setPlayerInName(String playerInName) { this.playerInName = playerInName; }

    public int getMinute() { return minute; }
    public void setMinute(int minute) { this.minute = minute; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
