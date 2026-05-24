package com.epo.footballstats.models;

import com.google.firebase.Timestamp;

public class Match {
    private String id;
    private String championshipId;
    private String homeTeamId;
    private String awayTeamId;
    private String homeTeamName;
    private String awayTeamName;
    private int gameweek;
    private Timestamp date;
    private String status; // SCHEDULED, LIVE, FINISHED
    private int homeScore;
    private int awayScore;
    private int currentMinute;

    public Match() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getChampionshipId() { return championshipId; }
    public void setChampionshipId(String championshipId) { this.championshipId = championshipId; }

    public String getHomeTeamId() { return homeTeamId; }
    public void setHomeTeamId(String homeTeamId) { this.homeTeamId = homeTeamId; }

    public String getAwayTeamId() { return awayTeamId; }
    public void setAwayTeamId(String awayTeamId) { this.awayTeamId = awayTeamId; }

    public String getHomeTeamName() { return homeTeamName; }
    public void setHomeTeamName(String homeTeamName) { this.homeTeamName = homeTeamName; }

    public String getAwayTeamName() { return awayTeamName; }
    public void setAwayTeamName(String awayTeamName) { this.awayTeamName = awayTeamName; }

    public int getGameweek() { return gameweek; }
    public void setGameweek(int gameweek) { this.gameweek = gameweek; }

    public Timestamp getDate() { return date; }
    public void setDate(Timestamp date) { this.date = date; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getHomeScore() { return homeScore; }
    public void setHomeScore(int homeScore) { this.homeScore = homeScore; }

    public int getAwayScore() { return awayScore; }
    public void setAwayScore(int awayScore) { this.awayScore = awayScore; }

    public int getCurrentMinute() { return currentMinute; }
    public void setCurrentMinute(int currentMinute) { this.currentMinute = currentMinute; }

    public String getScoreDisplay() {
        return homeScore + " - " + awayScore;
    }
}
