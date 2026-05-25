package com.epo.footballstats.utils;

import com.epo.footballstats.models.MatchStat;
import com.epo.footballstats.models.PlayerStat;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Υπολογίζει συγκεντρωτικά στατιστικά αγώνα (R4) από raw Firestore documents.
 *
 * Χρησιμοποιείται από το {@code MatchStatsFragment}:
 *   1) Παίρνει list από documents (από snapshot listener στο matches/{id}/stats).
 *   2) Επιστρέφει {@link Result} με per-team αθροίσματα + top players.
 */
public class MatchStatsAggregator {

    /** Per-team totals για κάθε κατηγορία στατιστικού. */
    public static class TeamTotals {
        public int shots;
        public int shotsOnTarget;
        public int goals;
        public int passes;
        public int passesSuccess;
        public int crosses;
        public int crossesSuccess;
        public int tackles;
        public int tacklesSuccess;
        public int corners;
        public int foulsCommitted;   // direction = AGAINST (κατά της ομάδας) = έγινε από αυτή
        public int yellowCards;       // χρέωση: direction = FOR (κατά της ομάδας από διαιτητή)
        public int redCards;
        public int errors;
        public int assists;
    }

    public static class Result {
        public final TeamTotals home = new TeamTotals();
        public final TeamTotals away = new TeamTotals();
        /** Παίκτες ταξινομημένοι κατά impact (γκολ + ασσιστ) φθίνον. */
        public final List<PlayerStat> topPlayers = new ArrayList<>();
    }

    /**
     * @param docs   τα documents από matches/{id}/stats
     * @param homeTeamId  id γηπεδούχου για να ξεχωρίσουμε home/away
     */
    public static Result aggregate(List<DocumentSnapshot> docs, String homeTeamId) {
        Result result = new Result();
        Map<String, PlayerStat> playerMap = new LinkedHashMap<>();

        for (DocumentSnapshot doc : docs) {
            String type      = doc.getString("type");
            String teamId    = doc.getString("teamId");
            String playerId  = doc.getString("playerId");
            String playerName= doc.getString("playerName");
            String teamName  = doc.getString("teamName");
            String result_   = doc.getString("result");
            String direction = doc.getString("direction");
            String shotType  = doc.getString("shotType");
            String cardColor = doc.getString("cardColor");
            if (type == null || teamId == null) continue;

            TeamTotals t = teamId.equals(homeTeamId) ? result.home : result.away;
            PlayerStat ps = null;
            if (playerId != null) {
                ps = playerMap.get(playerId);
                if (ps == null) {
                    ps = new PlayerStat(playerId, playerName, teamName);
                    playerMap.put(playerId, ps);
                }
            }

            switch (type) {
                case MatchStat.TYPE_SHOT:
                    t.shots++;
                    if (ps != null) ps.shots++;
                    if (MatchStat.SHOT_ON_TARGET.equals(shotType)
                            || MatchStat.SHOT_HEADER.equals(shotType)
                            || MatchStat.RESULT_GOAL.equals(result_)) {
                        t.shotsOnTarget++;
                        if (ps != null) ps.shotsOnTarget++;
                    }
                    if (MatchStat.RESULT_GOAL.equals(result_)) {
                        t.goals++;
                        if (ps != null) ps.goals++;
                    }
                    break;

                case MatchStat.TYPE_PASS:
                    t.passes++;
                    if (MatchStat.RESULT_SUCCESS.equals(result_)) t.passesSuccess++;
                    break;

                case MatchStat.TYPE_CROSS:
                    t.crosses++;
                    if (MatchStat.RESULT_SUCCESS.equals(result_)) t.crossesSuccess++;
                    break;

                case MatchStat.TYPE_TACKLE:
                    t.tackles++;
                    if (MatchStat.RESULT_SUCCESS.equals(result_)) t.tacklesSuccess++;
                    break;

                case MatchStat.TYPE_CORNER:
                    t.corners++;
                    break;

                case MatchStat.TYPE_FOUL:
                    // FOR = υπέρ → η αντίπαλη έκανε φάουλ
                    // AGAINST = κατά → αυτή η ομάδα έκανε φάουλ
                    if (MatchStat.DIR_AGAINST.equals(direction)) {
                        t.foulsCommitted++;
                    } else if (MatchStat.DIR_FOR.equals(direction)) {
                        // Φάουλ υπέρ της ομάδας = έγινε από την αντίπαλη
                        TeamTotals other = (t == result.home) ? result.away : result.home;
                        other.foulsCommitted++;
                    }
                    break;

                case MatchStat.TYPE_CARD:
                    // Κάρτα κατά (AGAINST) → χρέωση σε αυτή την ομάδα
                    // Κάρτα υπέρ (FOR) → χρέωση στην αντίπαλη
                    TeamTotals carded = t;
                    if (MatchStat.DIR_FOR.equals(direction)) {
                        carded = (t == result.home) ? result.away : result.home;
                    }
                    if (MatchStat.CARD_RED.equals(cardColor)) {
                        carded.redCards++;
                        if (ps != null && carded == t) ps.redCards++;
                    } else {
                        carded.yellowCards++;
                        if (ps != null && carded == t) ps.yellowCards++;
                    }
                    break;

                case MatchStat.TYPE_ASSIST:
                    t.assists++;
                    if (ps != null) ps.assists++;
                    break;

                case MatchStat.TYPE_ERROR:
                    t.errors++;
                    break;
            }
        }

        // Top players: μόνο όσοι έχουν γκολ ή ασσιστ ή κάρτα
        for (PlayerStat ps : playerMap.values()) {
            if (ps.impactScore() > 0 || ps.yellowCards > 0 || ps.redCards > 0) {
                result.topPlayers.add(ps);
            }
        }
        Collections.sort(result.topPlayers, new Comparator<PlayerStat>() {
            @Override
            public int compare(PlayerStat a, PlayerStat b) {
                return Integer.compare(b.impactScore(), a.impactScore());
            }
        });

        return result;
    }
}
