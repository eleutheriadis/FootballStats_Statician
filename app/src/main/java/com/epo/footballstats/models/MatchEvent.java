package com.epo.footballstats.models;

import com.google.firebase.Timestamp;

/**
 * Ενιαίο μοντέλο γεγονότος αγώνα για την παρακολούθηση από φίλαθλο (R3).
 *
 * Ενοποιεί:
 *   • Στατιστικά γεγονότα ({@link MatchStat}) από matches/{id}/stats
 *   • Αλλαγές παικτών ({@link Substitution}) από matches/{id}/substitutions
 *
 * Η ενοποίηση γίνεται client-side: το {@code LiveFeedFragment}
 * διαβάζει και τα δύο streams και τα μετατρέπει σε MatchEvent.
 */
public class MatchEvent {

    /** Τύπος γεγονότος για την οπτική αναπαράσταση. */
    public enum Kind {
        GOAL,
        YELLOW_CARD,
        RED_CARD,
        SUBSTITUTION,
        SHOT_ON_TARGET,
        SHOT_MISS,
        ASSIST,
        CORNER,
        FOUL,
        OTHER
    }

    private final Kind kind;
    private final int minute;
    private final String title;          // π.χ. "ΓΚΟΛ!"
    private final String description;    // π.χ. "Σαββίδης (ΠΑΟΚ)"
    private final String teamName;
    private final Timestamp timestamp;
    private final boolean isHomeTeam;

    public MatchEvent(Kind kind, int minute, String title, String description,
                      String teamName, boolean isHomeTeam, Timestamp timestamp) {
        this.kind = kind;
        this.minute = minute;
        this.title = title;
        this.description = description;
        this.teamName = teamName;
        this.isHomeTeam = isHomeTeam;
        this.timestamp = timestamp;
    }

    public Kind getKind()              { return kind; }
    public int getMinute()             { return minute; }
    public String getTitle()           { return title; }
    public String getDescription()     { return description; }
    public String getTeamName()        { return teamName; }
    public boolean isHomeTeam()        { return isHomeTeam; }
    public Timestamp getTimestamp()    { return timestamp; }
}
