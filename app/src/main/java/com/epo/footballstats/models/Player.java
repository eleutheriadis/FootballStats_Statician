package com.epo.footballstats.models;

public class Player {
    private String id;
    private String name;
    private String position; // GK, DEF, MID, FWD
    private String teamId;
    private String photoUrl;
    private int jerseyNumber;

    public Player() {}

    public Player(String id, String name, String position, String teamId, String photoUrl, int jerseyNumber) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.teamId = teamId;
        this.photoUrl = photoUrl;
        this.jerseyNumber = jerseyNumber;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public int getJerseyNumber() { return jerseyNumber; }
    public void setJerseyNumber(int jerseyNumber) { this.jerseyNumber = jerseyNumber; }
}
