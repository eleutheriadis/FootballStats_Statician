package com.epo.footballstats.activities;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.epo.footballstats.R;
import com.epo.footballstats.models.LineupPlayer;
import com.epo.footballstats.models.MatchStat;
import com.epo.footballstats.utils.FirestoreHelper;
import com.epo.footballstats.utils.MatchClock;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * R2 - Διαχείριση Αγώνα (καταγραφή στατιστικών σε πραγματικό χρόνο).
 *
 * Κάθε κουμπί ανοίγει dialog για επιλογή παίκτη + λεπτό,
 * και αποθηκεύει το στατιστικό στο Firestore:
 *   matches/{matchId}/stats/{autoId}
 *
 * Στατιστικά:
 *   Σουτ (4 τύποι), Τακλίν, Πάσες, Σέντρες,
 *   Ασσίστ, Λάθος, Φάουλ Υπέρ/Κατά,
 *   Κόρνερ, Κάρτες (Κίτρινη/Κόκκινη × Υπέρ/Κατά)
 */
public class ManageMatchActivity extends AppCompatActivity {

    private String matchId, homeTeamId, awayTeamId, homeTeamName, awayTeamName;

    private final List<LineupPlayer> homePlayers = new ArrayList<>();
    private final List<LineupPlayer> awayPlayers = new ArrayList<>();

    private RadioGroup rgTeam;
    private TextView tvHomeTeamName, tvAwayTeamName, tvLiveScore, tvLiveMinute;

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
        setContentView(R.layout.activity_manage_match);

        matchId      = getIntent().getStringExtra("MATCH_ID");
        homeTeamId   = getIntent().getStringExtra("HOME_TEAM_ID");
        awayTeamId   = getIntent().getStringExtra("AWAY_TEAM_ID");
        homeTeamName = getIntent().getStringExtra("HOME_TEAM_NAME");
        awayTeamName = getIntent().getStringExtra("AWAY_TEAM_NAME");

        tvHomeTeamName = findViewById(R.id.tvHomeTeamName);
        tvAwayTeamName = findViewById(R.id.tvAwayTeamName);
        tvLiveScore    = findViewById(R.id.tvLiveScore);
        tvLiveMinute   = findViewById(R.id.tvLiveMinute);
        rgTeam         = findViewById(R.id.rgTeam);

        tvHomeTeamName.setText(homeTeamName);
        tvAwayTeamName.setText(awayTeamName);

        // Ετικέτες radio buttons
        ((android.widget.RadioButton) findViewById(R.id.rbHomeTeam)).setText(homeTeamName);
        ((android.widget.RadioButton) findViewById(R.id.rbAwayTeam)).setText(awayTeamName);

        // Φόρτωση παικτών
        loadLineupPlayers(homeTeamId, homePlayers);
        loadLineupPlayers(awayTeamId, awayPlayers);

        // Real-time σκορ
        listenScore();

        // ── ΣΟΥΤ ──────────────────────────────────────────────────────────────
        bind(R.id.btnShotOnTarget,    () -> recordShotDialog(MatchStat.SHOT_ON_TARGET));
        bind(R.id.btnShotOffTarget,   () -> recordShotDialog(MatchStat.SHOT_OFF_TARGET));
        bind(R.id.btnShotBlocked,     () -> recordShotDialog(MatchStat.SHOT_BLOCKED));
        bind(R.id.btnShotHeader,      () -> recordShotDialog(MatchStat.SHOT_HEADER));

        // ── ΤΑΚΛΙΝ ────────────────────────────────────────────────────────────
        bind(R.id.btnTackleSuccess, () -> recordSimpleStat(MatchStat.TYPE_TACKLE, MatchStat.RESULT_SUCCESS, null));
        bind(R.id.btnTackleFail,    () -> recordSimpleStat(MatchStat.TYPE_TACKLE, MatchStat.RESULT_FAIL,    null));

        // ── ΠΑΣΕΣ ─────────────────────────────────────────────────────────────
        bind(R.id.btnPassSuccess, () -> recordSimpleStat(MatchStat.TYPE_PASS, MatchStat.RESULT_SUCCESS, null));
        bind(R.id.btnPassFail,    () -> recordSimpleStat(MatchStat.TYPE_PASS, MatchStat.RESULT_FAIL,    null));

        // ── ΣΕΝΤΡΕΣ ───────────────────────────────────────────────────────────
        bind(R.id.btnCrossSuccess, () -> recordSimpleStat(MatchStat.TYPE_CROSS, MatchStat.RESULT_SUCCESS, null));
        bind(R.id.btnCrossFail,    () -> recordSimpleStat(MatchStat.TYPE_CROSS, MatchStat.RESULT_FAIL,    null));

        // ── ΑΣΣΙΣΤ / ΛΑΘΟΣ ───────────────────────────────────────────────────
        bind(R.id.btnAssist, () -> recordSimpleStat(MatchStat.TYPE_ASSIST, null, null));
        bind(R.id.btnError,  () -> recordSimpleStat(MatchStat.TYPE_ERROR,  null, null));

        // ── ΦΑΟΥΛ ─────────────────────────────────────────────────────────────
        bind(R.id.btnFoulFor,     () -> recordSimpleStat(MatchStat.TYPE_FOUL, null, MatchStat.DIR_FOR));
        bind(R.id.btnFoulAgainst, () -> recordSimpleStat(MatchStat.TYPE_FOUL, null, MatchStat.DIR_AGAINST));

        // ── ΚΟΡΝΕΡ ────────────────────────────────────────────────────────────
        bind(R.id.btnCorner, () -> recordSimpleStat(MatchStat.TYPE_CORNER, null, MatchStat.DIR_FOR));

        // ── ΚΑΡΤΕΣ ────────────────────────────────────────────────────────────
        bind(R.id.btnYellowCardFor,     () -> recordCardStat(MatchStat.CARD_YELLOW, MatchStat.DIR_FOR));
        bind(R.id.btnYellowCardAgainst, () -> recordCardStat(MatchStat.CARD_YELLOW, MatchStat.DIR_AGAINST));
        bind(R.id.btnRedCardFor,        () -> recordCardStat(MatchStat.CARD_RED,    MatchStat.DIR_FOR));
        bind(R.id.btnRedCardAgainst,    () -> recordCardStat(MatchStat.CARD_RED,    MatchStat.DIR_AGAINST));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Βοηθητικό για setOnClickListener */
    private void bind(int btnId, Runnable action) {
        ((MaterialButton) findViewById(btnId)).setOnClickListener(v -> action.run());
    }

    /** Επιστρέφει την επιλεγμένη ομάδα */
    private boolean isHomeSelected() {
        return rgTeam.getCheckedRadioButtonId() == R.id.rbHomeTeam;
    }

    private String selectedTeamId()   { return isHomeSelected() ? homeTeamId   : awayTeamId; }
    private String selectedTeamName() { return isHomeSelected() ? homeTeamName : awayTeamName; }
    private List<LineupPlayer> selectedPlayers() {
        return isHomeSelected() ? homePlayers : awayPlayers;
    }

    // ── Firestore φόρτωση παικτών ──────────────────────────────────────────────

    private void loadLineupPlayers(String teamId, List<LineupPlayer> target) {
        FirestoreHelper.lineupsRef(matchId).document(teamId).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    List<Map<String, Object>> raw =
                            (List<Map<String, Object>>) doc.get("players");
                    if (raw == null) return;
                    target.clear();
                    for (Map<String, Object> m : raw) {
                        LineupPlayer lp = new LineupPlayer();
                        lp.setPlayerId((String) m.get("playerId"));
                        lp.setPlayerName((String) m.get("playerName"));
                        Boolean active = (Boolean) m.get("isActive");
                        lp.setActive(active == null || active);
                        target.add(lp);
                    }
                });
    }

    /** Real-time ακρόαση για ανανέωση σκορ + live clock state. */
    private void listenScore() {
        FirestoreHelper.matchesRef().document(matchId)
                .addSnapshotListener((snap, e) -> {
                    if (snap == null || !snap.exists()) return;
                    Long h = snap.getLong("homeScore");
                    Long a = snap.getLong("awayScore");
                    if (h != null && a != null)
                        tvLiveScore.setText(h + " - " + a);

                    // Live clock — διαβάζουμε kickoff timestamp + status και
                    // υπολογίζουμε client-side το τρέχον λεπτό μέσω MatchClock.
                    currentStatus = snap.getString("status");
                    liveStartTime = snap.getTimestamp("liveStartTime");

                    refreshMinute();

                    if ("LIVE".equals(currentStatus) && liveStartTime != null) {
                        clockHandler.removeCallbacks(clockTick);
                        clockHandler.post(clockTick);
                    } else {
                        clockHandler.removeCallbacks(clockTick);
                    }
                });
    }

    /** Ενημερώνει το tvLiveMinute βάσει του τρέχοντος status + liveStartTime. */
    private void refreshMinute() {
        if (tvLiveMinute == null) return;
        if ("FINISHED".equals(currentStatus)) {
            tvLiveMinute.setText("Τελικό");
        } else if ("LIVE".equals(currentStatus) && liveStartTime != null) {
            int m = MatchClock.currentMinute(liveStartTime);
            tvLiveMinute.setText(m + "'");
            if (m >= MatchClock.MAX_MINUTE) {
                clockHandler.removeCallbacks(clockTick);
                autoFinishMatch();
            }
        } else {
            tvLiveMinute.setText("—");
        }
    }

    /**
     * Όταν ο μετρητής φτάσει στο 90', γράφει status=FINISHED στη Firestore.
     * Όλοι οι snapshot listeners (στατιστικός + φίλαθλοι) θα δουν την αλλαγή.
     * Η σημαία αποτρέπει διπλά writes.
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

    // ── Dialogs καταγραφής ────────────────────────────────────────────────────

    /**
     * Dialog για ΣΟΥΤ: επιλογή αποτελέσματος (Γκολ / Όχι) + παίκτης + λεπτό.
     */
    private void recordShotDialog(String shotType) {
        new AlertDialog.Builder(this)
                .setTitle("Σουτ - Αποτέλεσμα;")
                .setItems(new String[]{"Γκολ", "Άστοχο"}, (dialog, which) -> {
                    String result = which == 0 ? MatchStat.RESULT_GOAL : MatchStat.RESULT_NO_GOAL;
                    showPlayerMinuteDialog(MatchStat.TYPE_SHOT, result, null, shotType,
                            (which == 0) ? "Γκολ! +" + (isHomeSelected() ? homeTeamName : awayTeamName) : null);
                })
                .show();
    }

    /**
     * Απλό στατιστικό (χωρίς extra επιλογή).
     */
    private void recordSimpleStat(String type, String result, String direction) {
        showPlayerMinuteDialog(type, result, direction, null, null);
    }

    /**
     * Κάρτα: χρώμα + κατεύθυνση.
     */
    private void recordCardStat(String cardColor, String direction) {
        showPlayerMinuteDialog(MatchStat.TYPE_CARD, null, direction, cardColor, null);
    }

    /**
     * Κοινό dialog επιλογής παίκτη και λεπτού.
     */
    private void showPlayerMinuteDialog(String type, String result, String direction,
                                        String extra, String scoreUpdate) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_stat, null);
        TextView tvTitle = dialogView.findViewById(R.id.tvDialogTitle);
        Spinner spinnerPlayer = dialogView.findViewById(R.id.spinnerPlayer);
        EditText etMinute     = dialogView.findViewById(R.id.etMinute);

        tvTitle.setText(typeLabel(type, result, direction, extra));

        List<LineupPlayer> activePlayers = selectedPlayers().stream()
                .filter(LineupPlayer::isActive).collect(Collectors.toList());

        String[] names = activePlayers.stream()
                .map(p -> p.getPlayerName())
                .toArray(String[]::new);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPlayer.setAdapter(adapter);

        new AlertDialog.Builder(this)
                .setTitle(typeLabel(type, result, direction, extra))
                .setView(dialogView)
                .setPositiveButton("Αποθήκευση", (d, w) -> {
                    if (activePlayers.isEmpty()) {
                        Toast.makeText(this, "Δεν βρέθηκαν παίκτες", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int idx = spinnerPlayer.getSelectedItemPosition();
                    if (idx < 0 || idx >= activePlayers.size()) return;

                    LineupPlayer player = activePlayers.get(idx);
                    String minStr = etMinute.getText().toString().trim();
                    int minute = minStr.isEmpty() ? 0 : Integer.parseInt(minStr);

                    saveStat(type, result, direction, extra, player, minute, scoreUpdate);
                })
                .setNegativeButton("Ακύρωση", null)
                .show();
    }

    // ── Αποθήκευση στο Firestore ───────────────────────────────────────────────

    private void saveStat(String type, String result, String direction,
                          String extra, LineupPlayer player, int minute, String scoreUpdate) {

        Map<String, Object> data = new HashMap<>();
        data.put("type",       type);
        data.put("teamId",     selectedTeamId());
        data.put("teamName",   selectedTeamName());
        data.put("playerId",   player.getPlayerId());
        data.put("playerName", player.getPlayerName());
        data.put("minute",     minute);
        data.put("timestamp",  Timestamp.now());

        if (result    != null) data.put("result",    result);
        if (direction != null) data.put("direction", direction);

        // Για SHOT: shotType. Για CARD: cardColor
        if (type.equals(MatchStat.TYPE_SHOT) && extra != null)
            data.put("shotType", extra);
        if (type.equals(MatchStat.TYPE_CARD) && extra != null)
            data.put("cardColor", extra);

        FirestoreHelper.statsRef(matchId)
                .add(data)
                .addOnSuccessListener(ref -> {
                    String msg = typeLabel(type, result, direction, extra) +
                                 " → " + player.getPlayerName() + " (" + minute + "')";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

                    // Αν ήταν γκολ, ενημέρωσε το σκορ
                    if (MatchStat.TYPE_SHOT.equals(type) && MatchStat.RESULT_GOAL.equals(result)) {
                        updateScore();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Σφάλμα: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    /** Αυξάνει το σκορ της επιλεγμένης ομάδας κατά 1 (transaction). */
    private void updateScore() {
        boolean isHome = isHomeSelected();
        String field = isHome ? "homeScore" : "awayScore";

        FirestoreHelper.matchesRef().document(matchId)
                .get()
                .addOnSuccessListener(snap -> {
                    Long current = snap.getLong(field);
                    long newScore = (current != null ? current : 0) + 1;
                    FirestoreHelper.matchesRef().document(matchId)
                            .update(field, newScore);
                });
    }

    // ── Label helper ──────────────────────────────────────────────────────────

    private String typeLabel(String type, String result, String direction, String extra) {
        switch (type) {
            case MatchStat.TYPE_SHOT:
                String shotName = "Σουτ";
                if (extra != null) switch (extra) {
                    case MatchStat.SHOT_ON_TARGET:  shotName = "Σουτ Εντός Εστίας"; break;
                    case MatchStat.SHOT_OFF_TARGET: shotName = "Σουτ Εκτός Εστίας"; break;
                    case MatchStat.SHOT_BLOCKED:    shotName = "Σουτ Μπλοκαρισμένο"; break;
                    case MatchStat.SHOT_HEADER:     shotName = "Κεφαλιά"; break;
                }
                if (MatchStat.RESULT_GOAL.equals(result)) shotName += " → ΓΚΟΛ!";
                return shotName;
            case MatchStat.TYPE_TACKLE:
                return "Τακλίν " + (MatchStat.RESULT_SUCCESS.equals(result) ? "Επιτυχές" : "Αποτυχημένο");
            case MatchStat.TYPE_PASS:
                return "Πάσα " + (MatchStat.RESULT_SUCCESS.equals(result) ? "Επιτυχής" : "Αποτυχημένη");
            case MatchStat.TYPE_CROSS:
                return "Σέντρα " + (MatchStat.RESULT_SUCCESS.equals(result) ? "Επιτυχής" : "Αποτυχημένη");
            case MatchStat.TYPE_ASSIST:  return "Ασσίστ";
            case MatchStat.TYPE_ERROR:   return "Λάθος";
            case MatchStat.TYPE_FOUL:
                return "Φάουλ " + (MatchStat.DIR_FOR.equals(direction) ? "Υπέρ" : "Κατά");
            case MatchStat.TYPE_CORNER:  return "Κερδισμένο Κόρνερ";
            case MatchStat.TYPE_CARD:
                String color = MatchStat.CARD_YELLOW.equals(extra) ? "Κίτρινη" : "Κόκκινη";
                String dir   = MatchStat.DIR_FOR.equals(direction) ? "Υπέρ" : "Κατά";
                return color + " Κάρτα " + dir;
            default: return type;
        }
    }
}
