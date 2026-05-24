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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Εμφανίζει την ενδεκάδα (starters) μιας ομάδας.
 */
public class LineupFragment extends Fragment {

    private static final String ARG_TEAM = "teamName";
    private List<LineupPlayer> allPlayers = new ArrayList<>();
    private String teamName;

    public static LineupFragment newInstance(List<LineupPlayer> players, String teamName) {
        LineupFragment fragment = new LineupFragment();
        fragment.allPlayers = new ArrayList<>(players);
        Bundle args = new Bundle();
        args.putString(ARG_TEAM, teamName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            teamName = getArguments().getString(ARG_TEAM);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lineup, container, false);

        TextView tvTitle = view.findViewById(R.id.tvLineupTitle);
        RecyclerView rv  = view.findViewById(R.id.rvStarting);

        tvTitle.setText("Ενδεκάδα " + teamName);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        // Εμφάνιση μόνο starters
        List<LineupPlayer> starters = allPlayers.stream()
                .filter(LineupPlayer::isStarter)
                .collect(Collectors.toList());
        rv.setAdapter(new LineupAdapter(starters));

        return view;
    }
}
