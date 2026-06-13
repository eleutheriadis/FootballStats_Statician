package com.epo.footballstats.utils;

import com.epo.footballstats.models.Match;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Κοινή λογική ταξινόμησης αγώνων για όλες τις λίστες της εφαρμογής.
 *
 * Σειρά εμφάνισης:
 *   1. Αγωνιστική (gameweek) αύξουσα — αγ.1 πριν αγ.2 κ.ο.κ.
 *   2. Κατάσταση: LIVE → SCHEDULED → FINISHED (μέσα σε ίδια αγωνιστική)
 *
 * Παράδειγμα:
 *   Αγωνιστική 1
 *     PAOK – OSFP (LIVE)        ← live ξεκάθαρα πάνω
 *     AEK  – PAO  (SCHEDULED)
 *     ARIS – ATR  (FINISHED)
 *   Αγωνιστική 2
 *     ...
 */
public final class MatchSorting {

    private MatchSorting() {}

    /** Ταξινομεί in-place τη λίστα αγώνων. */
    public static void sort(List<Match> matches) {
        Collections.sort(matches, BY_GAMEWEEK_THEN_STATUS);
    }

    public static final Comparator<Match> BY_GAMEWEEK_THEN_STATUS = new Comparator<Match>() {
        @Override
        public int compare(Match a, Match b) {
            int gw = Integer.compare(a.getGameweek(), b.getGameweek());
            if (gw != 0) return gw;
            return Integer.compare(statusRank(a.getStatus()), statusRank(b.getStatus()));
        }
    };

    /** Μικρότερο = ψηλότερα στη λίστα. */
    public static int statusRank(String status) {
        if (status == null) return 99;
        switch (status) {
            case "LIVE":      return 0;
            case "SCHEDULED": return 1;
            case "FINISHED":  return 2;
            default:          return 99;
        }
    }
}
