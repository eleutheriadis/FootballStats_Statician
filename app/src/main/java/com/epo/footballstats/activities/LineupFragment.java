package com.epo.footballstats.activities;

import android.os.Bundle;
import android.util.Log;
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
import com.epo.footballstats.adapters.LineupAdapter;
import com.epo.footballstats.models.LineupPlayer;
import com.epo.footballstats.utils.FirestoreHelper;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Εμφανίζει την ενδεκάδα (starters) μιας ομάδας.
 * Φορτώνει τα δεδομένα μόνη της από Firestore στο path:
 *     matches/{matchId}/lineups/{teamId}
 *
 * Το document αναμένεται να έχει field "players" (array από maps).
 * Ο parser αποδέχεται και camelCase ΚΑΙ snake_case key names, ώστε να
 * λειτουργεί ανεξάρτητα από το πώς γράφει τα δεδομένα το seed script.
 */
public class LineupFragment extends Fragment {

    private static final String TAG = "LineupFragment";

    private static final String ARG_MATCH_ID = "matchId";
    private static final String ARG_TEAM_ID  = "teamId";
    private static final String ARG_TEAM     = "teamName";

    private LineupAdapter adapter;
    private TextView tvEmpty;
    private final List<LineupPlayer> starters = new ArrayList<>();

    public static LineupFragment newInstance(String matchId, String teamId, String teamName) {
        LineupFragment f = new LineupFragment();
        Bundle args = new Bundle();
        args.putString(ARG_MATCH_ID, matchId);
        args.putString(ARG_TEAM_ID,  teamId);
        args.putString(ARG_TEAM,     teamName);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_lineup, container, false);

        String teamName = getArguments() != null ? getArguments().getString(ARG_TEAM) : "";
        String matchId  = getArguments() != null ? getArguments().getString(ARG_MATCH_ID) : "";
        String teamId   = getArguments() != null ? getArguments().getString(ARG_TEAM_ID) : "";

        TextView tvTitle = view.findViewById(R.id.tvLineupTitle);
        tvEmpty          = view.findViewById(R.id.tvLineupEmpty);
        RecyclerView rv  = view.findViewById(R.id.rvStarting);

        tvTitle.setText("Ενδεκάδα " + teamName);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new LineupAdapter(starters);
        rv.setAdapter(adapter);

        Log.d(TAG, "Loading lineup for matchId=" + matchId + " teamId=" + teamId);
        loadLineup(matchId, teamId);

        return view;
    }

    private void loadLineup(String matchId, String teamId) {
        if (matchId == null || matchId.isEmpty() || teamId == null || teamId.isEmpty()) {
            Log.w(TAG, "matchId or teamId missing — abort load");
            showEmpty("Λείπει matchId ή teamId");
            return;
        }

        FirestoreHelper.lineupsRef(matchId)
                .document(teamId)
                .get()
                .addOnSuccessListener(doc -> handleLineupDocument(doc))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore failure loading lineup", e);
                    showEmpty("Σφάλμα Firestore: " + e.getMessage());
                });
    }

    private void handleLineupDocument(DocumentSnapshot doc) {
        if (!doc.exists()) {
            Log.w(TAG, "Lineup document does NOT exist at " + doc.getReference().getPath());
            showEmpty("Δεν υπάρχει ενδεκάδα για αυτή την ομάδα.");
            return;
        }

        Log.d(TAG, "Lineup document loaded: " + doc.getData());

        // Δοκίμασε διαφορετικά πιθανά ονόματα του πεδίου που περιέχει τους παίκτες
        Object playersObj = doc.get("players");
        if (playersObj == null) playersObj = doc.get("lineup");
        if (playersObj == null) playersObj = doc.get("starters");

        if (!(playersObj instanceof List)) {
            Log.w(TAG, "No players list found in document. Fields: " + doc.getData());
            showEmpty("Δεν βρέθηκε πεδίο players στο document.");
            return;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> raw = (List<Map<String, Object>>) playersObj;

        starters.clear();
        int totalPlayers = 0;
        for (Object obj : raw) {
            if (!(obj instanceof Map)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) obj;
            try {
                LineupPlayer lp = mapToLineupPlayer(m);
                totalPlayers++;
                if (lp.isStarter()) {
                    starters.add(lp);
                }
            } catch (Throwable t) {
                Log.e(TAG, "Failed to parse player entry: " + m, t);
            }
        }

        Log.d(TAG, "Parsed " + totalPlayers + " players, " + starters.size() + " starters");

        if (adapter != null) adapter.notifyDataSetChanged();

        if (starters.isEmpty()) {
            if (totalPlayers == 0) {
                showEmpty("Η λίστα παικτών είναι άδεια.");
            } else {
                // Έχει παίκτες αλλά κανείς δεν είναι starter → πιθανώς λάθος field name.
                // Εμφάνισέ τους όλους για να μη μείνει άδειο το tab.
                Log.w(TAG, "No starters found — falling back to showing all players");
                for (Object obj : raw) {
                    if (!(obj instanceof Map)) continue;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = (Map<String, Object>) obj;
                    try {
                        starters.add(mapToLineupPlayer(m));
                    } catch (Throwable ignored) {}
                }
                if (adapter != null) adapter.notifyDataSetChanged();
                hideEmpty();
            }
        } else {
            hideEmpty();
        }
    }

    /**
     * Defensive parsing: αποδέχεται camelCase ή snake_case keys, και κάθε numeric
     * τύπο που μπορεί να έρθει από Firestore (Long / Integer / Double).
     */
    private LineupPlayer mapToLineupPlayer(Map<String, Object> m) {
        LineupPlayer lp = new LineupPlayer();
        lp.setPlayerId(stringField(m, "playerId", "player_id", "id"));
        lp.setPlayerName(stringField(m, "playerName", "player_name", "name"));
        lp.setPosition(stringField(m, "position", "pos"));
        lp.setJerseyNumber(intField(m, "jerseyNumber", "jersey_number", "number"));
        lp.setStarter(boolField(m, "isStarter", "is_starter", "starter"));
        Boolean active = boolOrNull(m, "isActive", "is_active", "active");
        // αν δεν δηλωθεί ρητά το isActive, θεωρούμε τον παίκτη ενεργό
        lp.setActive(active == null || active);
        return lp;
    }

    private static String stringField(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v instanceof String) return (String) v;
            if (v != null) return v.toString();
        }
        return "";
    }

    private static int intField(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v instanceof Number) return ((Number) v).intValue();
            if (v instanceof String) {
                try { return Integer.parseInt(((String) v).trim()); }
                catch (NumberFormatException ignored) {}
            }
        }
        return 0;
    }

    private static boolean boolField(Map<String, Object> m, String... keys) {
        Boolean b = boolOrNull(m, keys);
        return b != null && b;
    }

    private static Boolean boolOrNull(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v instanceof Boolean) return (Boolean) v;
            if (v instanceof Number)  return ((Number) v).intValue() != 0;
            if (v instanceof String) {
                String s = ((String) v).trim().toLowerCase();
                if (s.equals("true") || s.equals("1") || s.equals("yes")) return true;
                if (s.equals("false") || s.equals("0") || s.equals("no")) return false;
            }
        }
        return null;
    }

    private void showEmpty(String msg) {
        if (tvEmpty != null) {
            tvEmpty.setText(msg);
            tvEmpty.setVisibility(View.VISIBLE);
        }
    }

    private void hideEmpty() {
        if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
    }
}
