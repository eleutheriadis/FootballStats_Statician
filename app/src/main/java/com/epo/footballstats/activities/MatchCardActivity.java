package com.epo.footballstats.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.epo.footballstats.R;
import com.epo.footballstats.adapters.LineupAdapter;
import com.epo.footballstats.models.LineupPlayer;
import com.epo.footballstats.models.Substitution;
import com.epo.footballstats.utils.FirestoreHelper;
import com.epo.footballstats.utils.MatchClock;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MatchCardActivity extends AppCompatActivity {

    // Extras
    private String matchId, homeTeamId, awayTeamId, homeTeamName, awayTeamName;

    // Lineups
    private final List<LineupPlayer> homePlayers = new ArrayList<>();
    private final List<LineupPlayer> awayPlayers = new ArrayList<>();
    private final List<Substitution> substitutions = new ArrayList<>();

    // UI
    private TextView tvHomeTeam, tvAwayTeam, tvScore, tvMinute;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    // Live clock — υπολογίζεται client-side από το liveStartTime
    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private Timestamp liveStartTime;
    private String currentStatus;
    /** Σημαία για να μη γράφουμε το status=FINISHED πολλές φορές στη Firestore. */
    private boolean autoFinishAttempted = false;
    private final Runnable clockTick = new Runnable() {
        @Override
        public void run() {
            refreshMinute();
            clockHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_card);

        matchId      = getIntent().getStringExtra("MATCH_ID");
        homeTeamId   = getIntent().getStringExtra("HOME_TEAM_ID");
        awayTeamId   = getIntent().getStringExtra("AWAY_TEAM_ID");
        homeTeamName = getIntent().getStringExtra("HOME_TEAM_NAME");
        awayTeamName = getIntent().getStringExtra("AWAY_TEAM_NAME");
        int homeScore = getIntent().getIntExtra("HOME_SCORE", 0);
        int awayScore = getIntent().getIntExtra("AWAY_SCORE", 0);

        tvHomeTeam = findViewById(R.id.tvHomeTeam);
        tvAwayTeam = findViewById(R.id.tvAwayTeam);
        tvScore    = findViewById(R.id.tvScore);
        tvMinute   = findViewById(R.id.tvMinute);
        tabLayout  = findViewById(R.id.tabLayout);
        viewPager  = findViewById(R.id.viewPager);

        tvHomeTeam.setText(homeTeamName);
        tvAwayTeam.setText(awayTeamName);
        tvScore.setText(homeScore + " - " + awayScore);

        String[] tabTitles = {homeTeamName, awayTeamName, "Αλλαγές"};
        viewPager.setAdapter(new LineupPagerAdapter(this, tabTitles));
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();

        findViewById(R.id.btnManageStats).setOnClickListener(v -> openManageMatch());

        findViewById(R.id.btnSubstitution).setOnClickListener(v -> showSubstitutionDialog());

        loadLineup(homeTeamId, homePlayers);
        loadLineup(awayTeamId, awayPlayers);

        listenForSubstitutions();
        listenForMatchUpdates();
    }

    private void loadLineup(String teamId, List<LineupPlayer> targetList) {
        FirestoreHelper.lineupsRef(matchId)
                .document(teamId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        List<Map<String, Object>> raw =
                                (List<Map<String, Object>>) doc.get("players");
                        if (raw != null) {
                            targetList.clear();
                            for (Map<String, Object> m : raw) {
                                LineupPlayer lp = mapToLineupPlayer(m);
                                targetList.add(lp);
                            }
                            viewPager.getAdapter().notifyDataSetChanged();
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Σφάλμα φόρτωσης ενδεκάδας: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
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

    private void listenForSubstitutions() {
        FirestoreHelper.substitutionsRef(matchId)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    substitutions.clear();
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Substitution sub = doc.toObject(Substitution.class);
                        if (sub != null) {
                            sub.setId(doc.getId());
                            substitutions.add(sub);
                            markPlayerInactive(sub.getTeamId(), sub.getPlayerOutId());
                        }
                    }
                    viewPager.getAdapter().notifyDataSetChanged();
                });
    }

    private void listenForMatchUpdates() {
        FirestoreHelper.matchesRef().document(matchId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;

                    Long homeScore = snapshot.getLong("homeScore");
                    Long awayScore = snapshot.getLong("awayScore");
                    if (homeScore != null && awayScore != null)
                        tvScore.setText(homeScore + " - " + awayScore);

                    // Live clock state — διαβάζουμε kickoff timestamp + status.
                    // Δεν διαβάζουμε πια currentMinute από το backend — το
                    // υπολογίζουμε client-side μέσω MatchClock.
                    currentStatus = snapshot.getString("status");
                    liveStartTime = snapshot.getTimestamp("liveStartTime");

                    refreshMinute();

                    if ("LIVE".equals(currentStatus) && liveStartTime != null) {
                        clockHandler.removeCallbacks(clockTick);
                        clockHandler.post(clockTick);
                    } else {
                        clockHandler.removeCallbacks(clockTick);
                    }
                });
    }

    /** Ενημερώνει το tvMinute βάσει του τρέχοντος status + liveStartTime. */
    private void refreshMinute() {
        if (tvMinute == null) return;
        if ("FINISHED".equals(currentStatus)) {
            tvMinute.setText("Τελικό");
        } else if ("LIVE".equals(currentStatus) && liveStartTime != null) {
            int m = MatchClock.currentMinute(liveStartTime);
            tvMinute.setText(m + "'");
            if (m >= MatchClock.MAX_MINUTE) {
                clockHandler.removeCallbacks(clockTick);
                autoFinishMatch();
            }
        } else {
            tvMinute.setText("—");
        }
    }

    /**
     * Όταν ο μετρητής φτάσει στο 90', γράφει status=FINISHED στη Firestore.
     * Όλοι οι snapshot listeners (στατιστικός + φίλαθλοι) θα δουν την αλλαγή
     * και θα εμφανίσουν "Τελικό". Η σημαία αποτρέπει διπλά writes.
     */
    private void autoFinishMatch() {
        if (autoFinishAttempted) return;
        autoFinishAttempted = true;
        FirestoreHelper.matchesRef().document(matchId)
                .update("status", "FINISHED");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clockHandler.removeCallbacks(clockTick);
    }

    private void markPlayerInactive(String teamId, String playerId) {
        List<LineupPlayer> list = teamId.equals(homeTeamId) ? homePlayers : awayPlayers;
        for (LineupPlayer lp : list) {
            if (lp.getPlayerId().equals(playerId)) {
                lp.setActive(false);
                break;
            }
        }
    }

    private void showSubstitutionDialog() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_substitution, null);

        Spinner spinnerTeam      = dialogView.findViewById(R.id.spinnerTeam);
        Spinner spinnerPlayerOut = dialogView.findViewById(R.id.spinnerPlayerOut);
        Spinner spinnerPlayerIn  = dialogView.findViewById(R.id.spinnerPlayerIn);
        EditText etMinute        = dialogView.findViewById(R.id.etMinute);

        ArrayAdapter<String> teamAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{homeTeamName, awayTeamName});
        teamAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTeam.setAdapter(teamAdapter);

        spinnerTeam.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                boolean isHome = pos == 0;
                List<LineupPlayer> activePlayers = (isHome ? homePlayers : awayPlayers)
                        .stream().filter(LineupPlayer::isActive).collect(Collectors.toList());
                List<LineupPlayer> benchPlayers = (isHome ? homePlayers : awayPlayers)
                        .stream().filter(lp -> !lp.isStarter() && lp.isActive()).collect(Collectors.toList());

                String[] outNames = activePlayers.stream()
                        .map(lp -> lp.getJerseyNumber() + ". " + lp.getPlayerName())
                        .toArray(String[]::new);
                String[] inNames = benchPlayers.stream()
                        .map(lp -> lp.getJerseyNumber() + ". " + lp.getPlayerName())
                        .toArray(String[]::new);

                ArrayAdapter<String> outAdapter = new ArrayAdapter<>(MatchCardActivity.this,
                        android.R.layout.simple_spinner_item, outNames);
                outAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerPlayerOut.setAdapter(outAdapter);

                ArrayAdapter<String> inAdapter = new ArrayAdapter<>(MatchCardActivity.this,
                        android.R.layout.simple_spinner_item, inNames);
                inAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerPlayerIn.setAdapter(inAdapter);
            }

            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        spinnerTeam.setSelection(0);

        new AlertDialog.Builder(this)
                .setTitle("Αλλαγή Παίκτη")
                .setView(dialogView)
                .setPositiveButton("Αποθήκευση", (dialog, which) -> {
                    int teamPos = spinnerTeam.getSelectedItemPosition();
                    boolean isHome = teamPos == 0;
                    List<LineupPlayer> activePlayers = (isHome ? homePlayers : awayPlayers)
                            .stream().filter(LineupPlayer::isActive).collect(Collectors.toList());
                    List<LineupPlayer> benchPlayers = (isHome ? homePlayers : awayPlayers)
                            .stream().filter(lp -> !lp.isStarter() && lp.isActive()).collect(Collectors.toList());

                    if (spinnerPlayerOut.getSelectedItem() == null ||
                            spinnerPlayerIn.getSelectedItem() == null) {
                        Toast.makeText(this, "Επιλέξτε παίκτες", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int outIdx = spinnerPlayerOut.getSelectedItemPosition();
                    int inIdx  = spinnerPlayerIn.getSelectedItemPosition();
                    if (outIdx >= activePlayers.size() || inIdx >= benchPlayers.size()) return;

                    LineupPlayer playerOut = activePlayers.get(outIdx);
                    LineupPlayer playerIn  = benchPlayers.get(inIdx);

                    String minStr = etMinute.getText().toString().trim();
                    int minute = minStr.isEmpty() ? 0 : Integer.parseInt(minStr);

                    saveSubstitution(
                            isHome ? homeTeamId : awayTeamId,
                            isHome ? homeTeamName : awayTeamName,
                            playerOut, playerIn, minute);
                })
                .setNegativeButton("Ακύρωση", null)
                .show();
    }

    private void saveSubstitution(String teamId, String teamName,
                                  LineupPlayer playerOut, LineupPlayer playerIn, int minute) {
        Map<String, Object> subData = new HashMap<>();
        subData.put("teamId",        teamId);
        subData.put("teamName",      teamName);
        subData.put("playerOutId",   playerOut.getPlayerId());
        subData.put("playerOutName", playerOut.getPlayerName());
        subData.put("playerInId",    playerIn.getPlayerId());
        subData.put("playerInName",  playerIn.getPlayerName());
        subData.put("minute",        minute);
        subData.put("timestamp",     Timestamp.now());

        FirestoreHelper.substitutionsRef(matchId)
                .add(subData)
                .addOnSuccessListener(ref ->
                        Toast.makeText(this,
                                "Αλλαγή: " + playerOut.getPlayerName() +
                                        " → " + playerIn.getPlayerName(), Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Σφάλμα: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private class LineupPagerAdapter extends FragmentStateAdapter {
        private final String[] titles;

        LineupPagerAdapter(FragmentActivity fa, String[] titles) {
            super(fa);
            this.titles = titles;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) return LineupFragment.newInstance(matchId, homeTeamId, homeTeamName);
            if (position == 1) return LineupFragment.newInstance(matchId, awayTeamId, awayTeamName);
            return SubstitutionsFragment.newInstance(substitutions);
        }

        @Override
        public int getItemCount() { return 3; }
    }

    private void openManageMatch() {
        Intent intent = new Intent(this, ManageMatchActivity.class);
        intent.putExtra("MATCH_ID",       matchId);
        intent.putExtra("HOME_TEAM_ID",   homeTeamId);
        intent.putExtra("AWAY_TEAM_ID",   awayTeamId);
        intent.putExtra("HOME_TEAM_NAME", homeTeamName);
        intent.putExtra("AWAY_TEAM_NAME", awayTeamName);
        startActivity(intent);
    }

}