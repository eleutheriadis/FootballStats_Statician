package com.epo.footballstats.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.epo.footballstats.R;
import com.epo.footballstats.models.MatchEvent;

import java.util.List;

/**
 * Adapter για timeline γεγονότων αγώνα (R3).
 * Κάθε γραμμή δείχνει: λεπτό, εικονίδιο, τίτλο/περιγραφή, ομάδα.
 */
public class MatchEventAdapter extends RecyclerView.Adapter<MatchEventAdapter.EventVH> {

    private final List<MatchEvent> events;

    public MatchEventAdapter(List<MatchEvent> events) {
        this.events = events;
    }

    @NonNull
    @Override
    public EventVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match_event, parent, false);
        return new EventVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EventVH h, int position) {
        MatchEvent e = events.get(position);

        h.tvMinute.setText(e.getMinute() + "'");
        h.tvTitle.setText(e.getTitle());
        h.tvDescription.setText(e.getDescription());
        h.tvTeam.setText(e.getTeamName() != null ? e.getTeamName() : "");
        h.tvIcon.setText(iconFor(e.getKind()));

        // Χρωματισμός τίτλου ανάλογα με τη σημαντικότητα
        int titleColor;
        switch (e.getKind()) {
            case GOAL:
                titleColor = 0xFF4CAF50;  // πράσινο
                break;
            case RED_CARD:
                titleColor = 0xFFE53935;  // κόκκινο
                break;
            case YELLOW_CARD:
                titleColor = 0xFFFFC107;  // κίτρινο
                break;
            case SUBSTITUTION:
                titleColor = 0xFF42A5F5;  // μπλε
                break;
            default:
                titleColor = Color.WHITE;
        }
        h.tvTitle.setTextColor(titleColor);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    private String iconFor(MatchEvent.Kind kind) {
        switch (kind) {
            case GOAL:           return "⚽";
            case YELLOW_CARD:    return "🟨";
            case RED_CARD:       return "🟥";
            case SUBSTITUTION:   return "🔄";
            case SHOT_ON_TARGET: return "🎯";
            case SHOT_MISS:      return "↗";
            case ASSIST:         return "👟";
            case CORNER:         return "⛳";
            case FOUL:           return "⚠";
            default:             return "•";
        }
    }

    static class EventVH extends RecyclerView.ViewHolder {
        TextView tvMinute, tvIcon, tvTitle, tvDescription, tvTeam;

        EventVH(@NonNull View itemView) {
            super(itemView);
            tvMinute      = itemView.findViewById(R.id.tvMinute);
            tvIcon        = itemView.findViewById(R.id.tvIcon);
            tvTitle       = itemView.findViewById(R.id.tvTitle);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvTeam        = itemView.findViewById(R.id.tvTeam);
        }
    }
}
