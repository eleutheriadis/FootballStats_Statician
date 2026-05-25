package com.epo.footballstats.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.epo.footballstats.R;
import com.epo.footballstats.adapters.MatchAdapter;
import com.epo.footballstats.models.Match;
import com.epo.footballstats.utils.FirestoreHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * R3/R4 - Φίλαθλος: επιλογή αγώνα για παρακολούθηση.
 *
 * Διαφορές από {@link SelectMatchActivity}:
 *   • Εμφανίζει αγώνες σε κατάσταση LIVE ή FINISHED (όχι SCHEDULED — δεν έχει νόημα
 *     ο φίλαθλος να μπει σε αγώνα που δεν έχει ξεκινήσει).
 *   • Χρησιμοποιεί real-time listener: όταν ένας αγώνας περάσει σε LIVE,
 *     εμφανίζεται αυτόματα.
 *   • Ανοίγει την {@link FanMatchActivity} (R3 + R4) αντί για κάρτα αγώνα.
 *   • Ταξινόμηση: LIVE πρώτα (κατά λεπτό φθίνον), μετά FINISHED.
 */
public class FanSelectMatchActivity extends AppCompatActivity {

    private RecyclerView rvMatches;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private MatchAdapter adapter;
    private final List<Match> matchList = new ArrayList<>();
    private ListenerRegistration listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fan_select_match);

        rvMatches   = findViewById(R.id.rvMatches);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty     = findViewById(R.id.tvEmpty);

        rvMatches.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MatchAdapter(matchList, this::onMatchSelected);
        rvMatches.setAdapter(adapter);

        startListeningMatches();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }

    /**
     * Real-time listener για όλους τους αγώνες σε LIVE / FINISHED.
     * Το Firestore στέλνει update κάθε φορά που αλλάζει κάτι (status, σκορ, λεπτό).
     */
    private void startListeningMatches() {
        progressBar.setVisibility(View.VISIBLE);

        listener = FirestoreHelper.matchesRef()
                .whereIn("status", List.of("LIVE", "FINISHED"))
                .addSnapshotListener((querySnapshot, e) -> {
                    progressBar.setVisibility(View.GONE);
                    if (e != null) {
                        Toast.makeText(this,
                                "Σφάλμα φόρτωσης: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (querySnapshot == null) return;

                    matchList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Match m = doc.toObject(Match.class);
                        if (m != null) {
                            m.setId(doc.getId());
                            matchList.add(m);
                        }
                    }
                    sortMatches();
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(matchList.isEmpty() ? View.VISIBLE : View.GONE);
                });
    }

    /** LIVE πρώτα (κατά λεπτό φθίνον), FINISHED μετά (κατά ημερομηνία φθίνουσα). */
    private void sortMatches() {
        Collections.sort(matchList, new Comparator<Match>() {
            @Override
            public int compare(Match a, Match b) {
                int rankA = "LIVE".equals(a.getStatus()) ? 0 : 1;
                int rankB = "LIVE".equals(b.getStatus()) ? 0 : 1;
                if (rankA != rankB) return Integer.compare(rankA, rankB);
                if (rankA == 0) {
                    return Integer.compare(b.getCurrentMinute(), a.getCurrentMinute());
                }
                if (a.getDate() != null && b.getDate() != null) {
                    return b.getDate().compareTo(a.getDate());
                }
                return 0;
            }
        });
    }

    private void onMatchSelected(Match match) {
        Intent intent = new Intent(this, FanMatchActivity.class);
        intent.putExtra("MATCH_ID",       match.getId());
        intent.putExtra("HOME_TEAM_ID",   match.getHomeTeamId());
        intent.putExtra("AWAY_TEAM_ID",   match.getAwayTeamId());
        intent.putExtra("HOME_TEAM_NAME", match.getHomeTeamName());
        intent.putExtra("AWAY_TEAM_NAME", match.getAwayTeamName());
        startActivity(intent);
    }
}
