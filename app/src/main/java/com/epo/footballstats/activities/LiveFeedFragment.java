package com.epo.footballstats.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.epo.footballstats.R;
import com.epo.footballstats.adapters.MatchEventAdapter;
import com.epo.footballstats.models.MatchEvent;
import com.epo.footballstats.models.MatchStat;
import com.epo.footballstats.models.Substitution;
import com.epo.footballstats.utils.FirestoreHelper;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * R3 - Παρακολούθηση εξέλιξης αγώνα.
 *
 * Real-time timeline όλων των γεγονότων του αγώνα (πιο πρόσφατο πρώτο):
 *   • Γκολ
 *   • Κίτρινες / Κόκκινες κάρτες
 *   • Αλλαγές παικτών
 *   • Σουτ στην εστία / άστοχα
 *   • Ασσιστ, Κόρνερ, Φάουλ
 *
 * Δύο Firestore listeners:
 *   1) matches/{id}/stats         → MatchStat events
 *   2) matches/{id}/substitutions → Substitution events
 *
 * Όλα συγχωνεύονται σε ένα ταξινομημένο feed.
 */
public class LiveFeedFragment extends Fragment {

    private static final String ARG_MATCH_ID     = "MATCH_ID";
    private static final String ARG_HOME_TEAM_ID = "HOME_TEAM_ID";

    private String matchId;
    private String homeTeamId;

    private RecyclerView rvEvents;
    private TextView tvEmpty;
    private MatchEventAdapter adapter;

    /** Διαχωρισμένα maps για να αποφεύγουμε duplicates κατά τα re-emits του listener. */
    private final Map<String, MatchEvent> statEvents = new HashMap<>();
    private final Map<String, MatchEvent> subEvents  = new HashMap<>();
    private final List<MatchEvent> displayList = new ArrayList<>();

    private ListenerRegistration statsListener;
    private ListenerRegistration subsListener;

    public static LiveFeedFragment newInstance(String matchId, String homeTeamId) {
        LiveFeedFragment f = new LiveFeedFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MATCH_ID, matchId);
        args.putString(ARG_HOME_TEAM_ID, homeTeamId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            matchId    = getArguments().getString(ARG_MATCH_ID);
            homeTeamId = getArguments().getString(ARG_HOME_TEAM_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_live_feed, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvEvents = view.findViewById(R.id.rvEvents);
        tvEmpty  = view.findViewById(R.id.tvEmpty);

        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MatchEventAdapter(displayList);
        rvEvents.setAdapter(adapter);

        attachListeners();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (statsListener != null) statsListener.remove();
        if (subsListener  != null) subsListener.remove();
    }

    // ── Firestore listeners ───────────────────────────────────────────────────

    private void attachListeners() {
        statsListener = FirestoreHelper.statsRef(matchId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    statEvents.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        MatchEvent ev = statToEvent(doc);
                        if (ev != null) statEvents.put(doc.getId(), ev);
                    }
                    refresh();
                });

        subsListener = FirestoreHelper.substitutionsRef(matchId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    subEvents.clear();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        MatchEvent ev = subToEvent(doc);
                        if (ev != null) subEvents.put(doc.getId(), ev);
                    }
                    refresh();
                });
    }

    /** Συγχώνευση events από stats + subs σε ταξινομημένη λίστα. */
    private void refresh() {
        displayList.clear();
        displayList.addAll(statEvents.values());
        displayList.addAll(subEvents.values());

        Collections.sort(displayList, new Comparator<MatchEvent>() {
            @Override
            public int compare(MatchEvent a, MatchEvent b) {
                // Πρώτα κατά λεπτό φθίνον (πιο πρόσφατο πρώτο)
                int c = Integer.compare(b.getMinute(), a.getMinute());
                if (c != 0) return c;
                // Στη συνέχεια κατά timestamp φθίνον
                if (a.getTimestamp() != null && b.getTimestamp() != null) {
                    return b.getTimestamp().compareTo(a.getTimestamp());
                }
                return 0;
            }
        });

        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(displayList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    // ── Μετατροπείς από Firestore documents σε MatchEvent ─────────────────────

    private MatchEvent statToEvent(DocumentSnapshot doc) {
        String type       = doc.getString("type");
        String teamId     = doc.getString("teamId");
        String teamName   = doc.getString("teamName");
        String playerName = doc.getString("playerName");
        String result     = doc.getString("result");
        String direction  = doc.getString("direction");
        String shotType   = doc.getString("shotType");
        String cardColor  = doc.getString("cardColor");
        Long minuteL      = doc.getLong("minute");
        Timestamp ts      = doc.getTimestamp("timestamp");

        if (type == null) return null;
        int minute = minuteL != null ? minuteL.intValue() : 0;
        boolean isHome = teamId != null && teamId.equals(homeTeamId);

        MatchEvent.Kind kind;
        String title;
        String description = playerName != null ? playerName : "";

        switch (type) {
            case MatchStat.TYPE_SHOT:
                if (MatchStat.RESULT_GOAL.equals(result)) {
                    kind = MatchEvent.Kind.GOAL;
                    title = "ΓΚΟΛ!";
                } else if (MatchStat.SHOT_ON_TARGET.equals(shotType)) {
                    kind = MatchEvent.Kind.SHOT_ON_TARGET;
                    title = "Σουτ στην εστία";
                } else {
                    kind = MatchEvent.Kind.SHOT_MISS;
                    title = shotLabel(shotType);
                }
                break;

            case MatchStat.TYPE_CARD:
                if (MatchStat.CARD_RED.equals(cardColor)) {
                    kind = MatchEvent.Kind.RED_CARD;
                    title = "Κόκκινη Κάρτα";
                } else {
                    kind = MatchEvent.Kind.YELLOW_CARD;
                    title = "Κίτρινη Κάρτα";
                }
                break;

            case MatchStat.TYPE_ASSIST:
                kind = MatchEvent.Kind.ASSIST;
                title = "Ασσίστ";
                break;

            case MatchStat.TYPE_CORNER:
                kind = MatchEvent.Kind.CORNER;
                title = "Κερδισμένο Κόρνερ";
                break;

            case MatchStat.TYPE_FOUL:
                kind = MatchEvent.Kind.FOUL;
                title = MatchStat.DIR_FOR.equals(direction) ? "Φάουλ Υπέρ" : "Φάουλ Κατά";
                break;

            default:
                // Πάσες, σέντρες, τάκλιν, λάθη: τα παραλείπουμε από το live feed
                // για να μην γεμίζει με θόρυβο. Φαίνονται όλα στα στατιστικά (R4).
                return null;
        }

        return new MatchEvent(kind, minute, title, description, teamName, isHome, ts);
    }

    private MatchEvent subToEvent(DocumentSnapshot doc) {
        String teamId        = doc.getString("teamId");
        String teamName      = doc.getString("teamName");
        String playerInName  = doc.getString("playerInName");
        String playerOutName = doc.getString("playerOutName");
        Long minuteL         = doc.getLong("minute");
        Timestamp ts         = doc.getTimestamp("timestamp");

        int minute = minuteL != null ? minuteL.intValue() : 0;
        boolean isHome = teamId != null && teamId.equals(homeTeamId);

        String description = "↑ " + (playerInName != null ? playerInName : "") +
                             "    ↓ " + (playerOutName != null ? playerOutName : "");

        return new MatchEvent(MatchEvent.Kind.SUBSTITUTION, minute,
                "Αλλαγή", description, teamName, isHome, ts);
    }

    private String shotLabel(String shotType) {
        if (shotType == null) return "Σουτ";
        switch (shotType) {
            case MatchStat.SHOT_OFF_TARGET: return "Σουτ εκτός";
            case MatchStat.SHOT_BLOCKED:    return "Σουτ μπλοκαρισμένο";
            case MatchStat.SHOT_HEADER:     return "Κεφαλιά";
            default:                         return "Σουτ";
        }
    }
}
