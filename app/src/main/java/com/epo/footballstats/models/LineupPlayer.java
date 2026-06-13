package com.epo.footballstats.models;


public class LineupPlayer {
    private String playerId;
    private String playerName;
    private String position;
    private int jerseyNumber;
    private boolean isStarter;
    private boolean isActive;

    public LineupPlayer() {}

    public LineupPlayer(String playerId, String playerName, String position,
                        int jerseyNumber, boolean isStarter) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.position = position;
        this.jerseyNumber = jerseyNumber;
        this.isStarter = isStarter;
        this.isActive = isStarter;
    }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public int getJerseyNumber() { return jerseyNumber; }
    public void setJerseyNumber(int jerseyNumber) { this.jerseyNumber = jerseyNumber; }

    public boolean isStarter() { return isStarter; }
    public void setStarter(boolean starter) { isStarter = starter; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
