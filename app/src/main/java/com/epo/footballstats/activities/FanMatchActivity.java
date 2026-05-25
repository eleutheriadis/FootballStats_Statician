package com.epo.footballstats.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.epo.footballstats.R;
import com.epo.footballstats.utils.FirestoreHelper;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.ListenerRegistration;

/**
 * R3 + R4 - Παρακολούθηση αγώνα από φίλαθλο.
 *
 *   • Header: σκορ + λεπτό σε real-time
 *   • Tab 1 (Εξέλιξη)    → {@link LiveFeedFragment}   (R3)
 *   • Tab 2 (Στατιστικά) → {@link MatchStatsFragment} (R4)
 */
public class FanMatchActivity extends AppCompatActivity {

    private String matchId, homeTeamId, awayTeamId, homeTeamName, awayTeamName;

    private TextView tvFanHomeTeam, tvFanAwayTeam, tvFanScore, tvFanStatus, tvLiveIndicator;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    private ListenerRegistration matchListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fan_match);

        matchId      = getIntent().getStringExtra("MATCH_ID");
        homeTeamId   = getIntent().getStringExtra("HOME_TEAM_ID");
        awayTeamId   = getIntent().getStringExtra("AWAY_TEAM_ID");
        homeTeamName = getIntent().getStringExtra("HOME_TEAM_NAME");
        awayTeamName = getIntent().getStringExtra("AWAY_TEAM_NAME");

        tvFanHomeTeam   = findViewById(R.id.tvFanHomeTeam);
        tvFanAwayTeam   = findViewById(R.id.tvFanAwayTeam);
        tvFanScore      = findViewById(R.id.tvFanScore);
        tvFanStatus     = findViewById(R.id.tvFanStatus);
        tvLiveIndicator = findViewById(R.id.tvLiveIndicator);
        tabLayout       = findViewById(R.id.tabLayoutFan);
        viewPager       = findViewById(R.id.viewPagerFan);

        tvFanHomeTeam.setText(homeTeamName);
        tvFanAwayTeam.setText(awayTeamName);

        String[] tabTitles = {
                getString(R.string.tab_live_feed),
                getString(R.string.tab_match_stats)
        };
        viewPager.setAdapter(new FanPagerAdapter(this));
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(tabTitles[position])
        ).attach();

        listenMatch();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (matchListener != null) matchListener.remove();
    }

    /** Real-time updates για σκορ, λεπτό και κατάσταση. */
    private void listenMatch() {
        matchListener = FirestoreHelper.matchesRef().document(matchId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null || !snap.exists()) return;

                    Long h = snap.getLong("homeScore");
                    Long a = snap.getLong("awayScore");
                    Long m = snap.getLong("currentMinute");
                    String status = snap.getString("status");

                    if (h != null && a != null) {
                        tvFanScore.setText(h + " - " + a);
                    }

                    if ("LIVE".equals(status)) {
                        tvLiveIndicator.setVisibility(View.VISIBLE);
                        tvFanStatus.setText(m != null ? (m + "'") : "—");
                    } else if ("FINISHED".equals(status)) {
                        tvLiveIndicator.setVisibility(View.GONE);
                        tvFanStatus.setText("Τελικό");
                    } else {
                        tvLiveIndicator.setVisibility(View.GONE);
                        tvFanStatus.setText(status != null ? status : "");
                    }
                });
    }

    // ── ViewPager Adapter ─────────────────────────────────────────────────────

    private class FanPagerAdapter extends FragmentStateAdapter {
        FanPagerAdapter(FragmentActivity fa) { super(fa); }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return LiveFeedFragment.newInstance(matchId, homeTeamId);
            }
            return MatchStatsFragment.newInstance(
                    matchId, homeTeamId, homeTeamName, awayTeamName);
        }

        @Override
        public int getItemCount() { return 2; }
    }
}
