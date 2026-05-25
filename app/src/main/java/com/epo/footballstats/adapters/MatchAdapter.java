package com.epo.footballstats.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.epo.footballstats.R;
import com.epo.footballstats.models.Match;

import java.util.List;

public class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.MatchViewHolder> {

    public interface OnMatchClickListener {
        void onMatchClick(Match match);
    }

    private final List<Match> matches;
    private final OnMatchClickListener listener;

    public MatchAdapter(List<Match> matches, OnMatchClickListener listener) {
        this.matches = matches;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match, parent, false);
        return new MatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        Match match = matches.get(position);
        holder.tvTeams.setText(match.getHomeTeamName() + " vs " + match.getAwayTeamName());
        holder.tvGameweek.setText("Αγωνιστική " + match.getGameweek());
        holder.tvScore.setText(match.getScoreDisplay());
        holder.tvStatus.setText(match.getStatus());

        // Χρωματισμός κατάστασης
        int color;
        switch (match.getStatus() != null ? match.getStatus() : "") {
            case "LIVE":
                color = 0xFF4CAF50;
                break;
            case "FINISHED":
                color = 0xFF9E9E9E;
                break;
            default:
                color = 0xFF2196F3;
        }
        holder.tvStatus.setTextColor(color);

        holder.itemView.setOnClickListener(v -> listener.onMatchClick(match));
    }

    @Override
    public int getItemCount() { return matches.size(); }

    static class MatchViewHolder extends RecyclerView.ViewHolder {
        TextView tvTeams, tvGameweek, tvScore, tvStatus;

        MatchViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTeams    = itemView.findViewById(R.id.tvMatchTeams);
            tvGameweek = itemView.findViewById(R.id.tvMatchGameweek);
            tvScore    = itemView.findViewById(R.id.tvMatchScore);
            tvStatus   = itemView.findViewById(R.id.tvMatchStatus);
        }
    }
}
