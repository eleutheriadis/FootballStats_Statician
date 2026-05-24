package com.epo.footballstats.adapters;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.epo.footballstats.R;
import com.epo.footballstats.models.LineupPlayer;

import java.util.List;

public class LineupAdapter extends RecyclerView.Adapter<LineupAdapter.LineupViewHolder> {

    public interface OnPlayerClickListener {
        void onPlayerClick(LineupPlayer player, int position);
    }

    private final List<LineupPlayer> players;
    private OnPlayerClickListener listener;

    public LineupAdapter(List<LineupPlayer> players) {
        this.players = players;
    }

    public LineupAdapter(List<LineupPlayer> players, OnPlayerClickListener listener) {
        this.players = players;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LineupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_player_lineup, parent, false);
        return new LineupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LineupViewHolder holder, int position) {
        LineupPlayer p = players.get(position);

        holder.tvNumber.setText(String.valueOf(p.getJerseyNumber()));
        holder.tvName.setText(p.getPlayerName());
        holder.tvPosition.setText(p.getPosition());

        // Αν ο παίκτης έχει αντικατασταθεί → γκριζάρισμα + strikethrough
        if (!p.isActive()) {
            holder.tvName.setPaintFlags(holder.tvName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvName.setAlpha(0.4f);
            holder.tvNumber.setAlpha(0.4f);
        } else {
            holder.tvName.setPaintFlags(holder.tvName.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvName.setAlpha(1f);
            holder.tvNumber.setAlpha(1f);
        }

        if (listener != null) {
            holder.itemView.setOnClickListener(v -> listener.onPlayerClick(p, holder.getAdapterPosition()));
        }
    }

    @Override
    public int getItemCount() { return players.size(); }

    static class LineupViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumber, tvName, tvPosition;

        LineupViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNumber   = itemView.findViewById(R.id.tvPlayerNumber);
            tvName     = itemView.findViewById(R.id.tvPlayerName);
            tvPosition = itemView.findViewById(R.id.tvPlayerPosition);
        }
    }
}
