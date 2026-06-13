package com.epo.footballstats.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.epo.footballstats.R;
import com.epo.footballstats.adapters.MatchAdapter;
import com.epo.footballstats.models.Match;
import com.epo.footballstats.utils.FirestoreHelper;
import com.epo.footballstats.utils.MatchSorting;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SelectMatchActivity extends AppCompatActivity {

    private RecyclerView rvMatches;
    private ProgressBar progressBar;
    private MatchAdapter adapter;
    private final List<Match> matchList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_match);

        rvMatches   = findViewById(R.id.rvMatches);
        progressBar = findViewById(R.id.progressBar);

        rvMatches.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MatchAdapter(matchList, this::onMatchSelected);
        rvMatches.setAdapter(adapter);

        loadMatches();
    }

    private void loadMatches() {
        progressBar.setVisibility(View.VISIBLE);

        FirestoreHelper.matchesRef()
                .whereIn("status", List.of("SCHEDULED", "LIVE"))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    matchList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Match match = doc.toObject(Match.class);
                        if (match != null) {
                            match.setId(doc.getId());
                            matchList.add(match);
                        }
                    }
                    MatchSorting.sort(matchList);
                    adapter.notifyDataSetChanged();

                    if (matchList.isEmpty()) {
                        Toast.makeText(this, "Δεν βρέθηκαν προγραμματισμένοι αγώνες.", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Σφάλμα φόρτωσης: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void onMatchSelected(Match match) {
        Intent intent = new Intent(this, MatchCardActivity.class);
        intent.putExtra("MATCH_ID",        match.getId());
        intent.putExtra("HOME_TEAM_ID",    match.getHomeTeamId());
        intent.putExtra("AWAY_TEAM_ID",    match.getAwayTeamId());
        intent.putExtra("HOME_TEAM_NAME",  match.getHomeTeamName());
        intent.putExtra("AWAY_TEAM_NAME",  match.getAwayTeamName());
        intent.putExtra("HOME_SCORE",      match.getHomeScore());
        intent.putExtra("AWAY_SCORE",      match.getAwayScore());
        startActivity(intent);
    }
}
