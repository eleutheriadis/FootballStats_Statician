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
import com.epo.footballstats.adapters.LineupAdapter;
import com.epo.footballstats.models.LineupPlayer;
import com.epo.footballstats.utils.FirestoreHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Εμφανίζει την ενδεκάδα (starters) μιας ομάδας.
 * Φορτώνει τα δεδομένα μόνη της από Firestore για να αποφύγει
 * το timing bug (κενή λίστα κατά τη δημιουργία).
 */
public class LineupFragment extends Fragment {

    private static final String ARG_MATCH_ID = "matchId";
    private static final String ARG_TEAM_ID  = "teamId";
    private static final String ARG_TEAM     = "teamName";

    private LineupAdapter adapter;
    private final List<LineupPlayer> starters = new ArrayList<>();

    /** Factory method — περνάμε matchId + teamId για αυτόνομη φόρτωση */
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
        RecyclerView rv  = view.findViewById(R.id.rvStarting);

        tvTitle.setText("Ενδεκάδα " + teamName);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new LineupAdapter(starters);
        rv.setAdapter(adapter);

        loadLineup(matchId, teamId);

        return view;
    }

    private void loadLineup(String matchId, String teamId) {
        if (matchId == null || matchId.isEmpty() || teamId == null || teamId.isEmpty()) return;

        FirestoreHelper.lineupsRef(matchId)
                .document(teamId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> raw =
                                (List<Map<String, Object>>) doc.get("players");
                        if (raw != null) {
                            starters.clear();
                            for (Map<String, Object> m : raw) {
                                LineupPlayer lp = mapToLineupPlayer(m);
                                if (lp.isStarter()) {
                                    starters.add(lp);
                                }
                            }
                            if (adapter != null) adapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private LineupPlayer mapToLineupPlayer(Map<String, Object> m) {
        LineupPlayer lp = new LineupPlayer();
        lp.setPlayerId((String) m.get("playerId"));
        lp.setPlayerName((String) m.get("playerName"));
        lp.setPosition((String) m.get("position"));
        Object num = m.get("jerseyNumber");
        lp.setJerseyNumber(num != null ? ((Long) num).intValue() : 0);
        Boolean starter = (Boolean) m.get("isStarter");
        lp.setStarter(starter != null && starter);
        Boolean active = (Boolean) m.get("isActive");
        lp.setActive(active == null || active);
        return lp;
    }
}