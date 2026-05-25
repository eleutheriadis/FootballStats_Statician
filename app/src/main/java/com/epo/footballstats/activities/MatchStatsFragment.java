package com.epo.footballstats.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.epo.footballstats.R;
import com.epo.footballstats.models.PlayerStat;
import com.epo.footballstats.utils.FirestoreHelper;
import com.epo.footballstats.utils.MatchStatsAggregator;
import com.epo.footballstats.utils.MatchStatsAggregator.Result;
import com.epo.footballstats.utils.MatchStatsAggregator.TeamTotals;
import com.google.firebase.firestore.ListenerRegistration;

/**
 * R4 - Συγκεντρωτικά στατιστικά αγώνα (κατά τη διάρκεια και ολοκληρωμένου).
 *
 * Listens σε real-time τη συλλογή matches/{id}/stats και ανανεώνει
 * τα per-team αθροίσματα για κάθε κατηγορία:
 *   Σουτ (συνολικά / στην εστία), Πάσες (με ποσοστό επιτυχίας),
 *   Σέντρες, Τάκλιν, Κόρνερ, Φάουλ, Κάρτες
 *
 * Στο τέλος εμφανίζεται λίστα κορυφαίων παικτών (γκολ + ασσιστ).
 */
public class MatchStatsFragment extends Fragment {

    private static final String ARG_MATCH_ID     = "MATCH_ID";
    private static final String ARG_HOME_TEAM_ID = "HOME_TEAM_ID";
    private static final String ARG_HOME_NAME    = "HOME_NAME";
    private static final String ARG_AWAY_NAME    = "AWAY_NAME";

    private String matchId, homeTeamId, homeName, awayName;

    private TextView tvStatsHome, tvStatsAway, tvNoPlayers;
    private LinearLayout statsContainer, playersContainer;

    private ListenerRegistration statsListener;

    public static MatchStatsFragment newInstance(String matchId, String homeTeamId,
                                                 String homeName, String awayName) {
        MatchStatsFragment f = new MatchStatsFragment();
        Bundle b = new Bundle();
        b.putString(ARG_MATCH_ID, matchId);
        b.putString(ARG_HOME_TEAM_ID, homeTeamId);
        b.putString(ARG_HOME_NAME, homeName);
        b.putString(ARG_AWAY_NAME, awayName);
        f.setArguments(b);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle a = getArguments();
        if (a != null) {
            matchId    = a.getString(ARG_MATCH_ID);
            homeTeamId = a.getString(ARG_HOME_TEAM_ID);
            homeName   = a.getString(ARG_HOME_NAME);
            awayName   = a.getString(ARG_AWAY_NAME);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_match_stats, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvStatsHome      = view.findViewById(R.id.tvStatsHome);
        tvStatsAway      = view.findViewById(R.id.tvStatsAway);
        tvNoPlayers      = view.findViewById(R.id.tvNoPlayers);
        statsContainer   = view.findViewById(R.id.statsContainer);
        playersContainer = view.findViewById(R.id.playersContainer);

        tvStatsHome.setText(homeName);
        tvStatsAway.setText(awayName);

        attachListener();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (statsListener != null) statsListener.remove();
    }

    // ── Listener στατιστικών ─────────────────────────────────────────────────

    private void attachListener() {
        statsListener = FirestoreHelper.statsRef(matchId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    Result agg = MatchStatsAggregator
                            .aggregate(snap.getDocuments(), homeTeamId);
                    renderStats(agg);
                });
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    private void renderStats(Result agg) {
        TeamTotals h = agg.home;
        TeamTotals a = agg.away;

        statsContainer.removeAllViews();
        addRow("Σουτ",                h.shots,         a.shots);
        addRow("Σουτ στην εστία",     h.shotsOnTarget, a.shotsOnTarget);
        addRow("Γκολ",                h.goals,         a.goals);
        addPercentRow("Πάσες", h.passesSuccess, h.passes, a.passesSuccess, a.passes);
        addPercentRow("Σέντρες", h.crossesSuccess, h.crosses, a.crossesSuccess, a.crosses);
        addPercentRow("Τάκλιν", h.tacklesSuccess, h.tackles, a.tacklesSuccess, a.tackles);
        addRow("Κόρνερ",             h.corners,         a.corners);
        addRow("Φάουλ",              h.foulsCommitted,  a.foulsCommitted);
        addRow("Κίτρινες Κάρτες",    h.yellowCards,     a.yellowCards);
        addRow("Κόκκινες Κάρτες",    h.redCards,        a.redCards);
        addRow("Λάθη",               h.errors,          a.errors);
        addRow("Ασσιστ",             h.assists,         a.assists);

        renderTopPlayers(agg);
    }

    /** Απλή γραμμή με δύο ακέραιες τιμές. */
    private void addRow(String label, int homeVal, int awayVal) {
        View row = LayoutInflater.from(getContext())
                .inflate(R.layout.item_stat_row, statsContainer, false);

        ((TextView) row.findViewById(R.id.tvLabel)).setText(label);
        ((TextView) row.findViewById(R.id.tvHomeValue)).setText(String.valueOf(homeVal));
        ((TextView) row.findViewById(R.id.tvAwayValue)).setText(String.valueOf(awayVal));

        adjustBars(row, homeVal, awayVal);
        statsContainer.addView(row);
    }

    /** Γραμμή με ποσοστό επιτυχίας (π.χ. πάσες 78/100). */
    private void addPercentRow(String label, int homeSuccess, int homeTotal,
                               int awaySuccess, int awayTotal) {
        View row = LayoutInflater.from(getContext())
                .inflate(R.layout.item_stat_row, statsContainer, false);

        ((TextView) row.findViewById(R.id.tvLabel)).setText(label);
        ((TextView) row.findViewById(R.id.tvHomeValue))
                .setText(formatPercent(homeSuccess, homeTotal));
        ((TextView) row.findViewById(R.id.tvAwayValue))
                .setText(formatPercent(awaySuccess, awayTotal));

        adjustBars(row, homeTotal, awayTotal);
        statsContainer.addView(row);
    }

    private String formatPercent(int success, int total) {
        if (total == 0) return "0";
        int pct = (success * 100) / total;
        return success + "/" + total + "\n" + pct + "%";
    }

    /**
     * Προσαρμόζει τα δύο bars ανάλογα με τις τιμές. Το bar που έχει την μεγαλύτερη
     * τιμή πιάνει 100% του διαθέσιμου χώρου στην πλευρά του.
     */
    private void adjustBars(View row, int homeVal, int awayVal) {
        View barHome = row.findViewById(R.id.barHome);
        View barAway = row.findViewById(R.id.barAway);

        int max = Math.max(homeVal, awayVal);
        float homeRatio = max == 0 ? 0f : (float) homeVal / max;
        float awayRatio = max == 0 ? 0f : (float) awayVal / max;

        // Σε FrameLayout με layout_gravity=end/start, ορίζουμε layout_width
        // δυναμικά ως percentage μέσω post()
        barHome.post(() -> {
            int parentW = ((View) barHome.getParent()).getWidth();
            ViewGroup.LayoutParams lp = barHome.getLayoutParams();
            lp.width = (int) (parentW * homeRatio);
            barHome.setLayoutParams(lp);
        });
        barAway.post(() -> {
            int parentW = ((View) barAway.getParent()).getWidth();
            ViewGroup.LayoutParams lp = barAway.getLayoutParams();
            lp.width = (int) (parentW * awayRatio);
            barAway.setLayoutParams(lp);
        });
    }

    // ── Top players ───────────────────────────────────────────────────────────

    private void renderTopPlayers(Result agg) {
        playersContainer.removeAllViews();
        if (agg.topPlayers.isEmpty()) {
            tvNoPlayers.setVisibility(View.VISIBLE);
            return;
        }
        tvNoPlayers.setVisibility(View.GONE);

        for (PlayerStat ps : agg.topPlayers) {
            View row = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_player_stat, playersContainer, false);

            ((TextView) row.findViewById(R.id.tvPlayerName)).setText(ps.playerName);
            ((TextView) row.findViewById(R.id.tvTeamName)).setText(ps.teamName);
            ((TextView) row.findViewById(R.id.tvBadges)).setText(buildBadges(ps));

            playersContainer.addView(row);
        }
    }

    /** Συμβολική παρουσίαση: ⚽×2  👟×1  🟨  🟥 */
    private String buildBadges(PlayerStat ps) {
        StringBuilder sb = new StringBuilder();
        if (ps.goals > 0)       sb.append("⚽×").append(ps.goals).append("  ");
        if (ps.assists > 0)     sb.append("👟×").append(ps.assists).append("  ");
        if (ps.yellowCards > 0) sb.append("🟨").append(ps.yellowCards > 1 ? "×" + ps.yellowCards : "").append("  ");
        if (ps.redCards > 0)    sb.append("🟥").append(ps.redCards > 1 ? "×" + ps.redCards : "").append("  ");
        return sb.toString().trim();
    }
}
