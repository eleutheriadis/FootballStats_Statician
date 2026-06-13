package com.epo.footballstats.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.epo.footballstats.R;
import com.epo.footballstats.models.Substitution;

import java.util.ArrayList;
import java.util.List;

/**
 * Tab 3: λίστα αλλαγών του αγώνα.
 */
public class SubstitutionsFragment extends Fragment {

    private List<Substitution> substitutions = new ArrayList<>();

    public static SubstitutionsFragment newInstance(List<Substitution> subs) {
        SubstitutionsFragment f = new SubstitutionsFragment();
        f.substitutions = new ArrayList<>(subs);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lineup, container, false);
        TextView tvTitle = view.findViewById(R.id.tvLineupTitle);
        tvTitle.setText("Αλλαγές (" + substitutions.size() + ")");

        // Όλα τα χρώματα δηλώνονται στο res/values/colors.xml
        int colorSecondary = ContextCompat.getColor(requireContext(), R.color.text_secondary);
        int colorWhite     = ContextCompat.getColor(requireContext(), R.color.white);
        int colorDivider   = ContextCompat.getColor(requireContext(), R.color.divider_dark);

        LinearLayout parent = (LinearLayout) view;
        if (substitutions.isEmpty()) {
            TextView empty = new TextView(getContext());
            empty.setText("Δεν έχουν γίνει αλλαγές ακόμα.");
            empty.setPadding(24, 16, 24, 16);
            empty.setTextColor(colorSecondary);
            parent.addView(empty);
        } else {
            for (Substitution sub : substitutions) {
                TextView tv = new TextView(getContext());
                tv.setText(sub.getMinute() + "' | " + sub.getTeamName() +
                           "\n  ↑ " + sub.getPlayerInName() +
                           "\n  ↓ " + sub.getPlayerOutName());
                tv.setTextColor(colorWhite);
                tv.setPadding(24, 12, 24, 12);
                parent.addView(tv);

                View divider = new View(getContext());
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(colorDivider);
                parent.addView(divider);
            }
        }
        return view;
    }
}
