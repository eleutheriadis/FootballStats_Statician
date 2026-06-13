package com.epo.footballstats.utils;

import com.google.firebase.Timestamp;

/**
 * Υπολογίζει client-side το τρέχον λεπτό ενός LIVE αγώνα.
 *
 * Λογική:
 *   1. Όταν ο admin βάλει status=LIVE, αποθηκεύεται στο match document το
 *      πεδίο {@code liveStartTime} (Firestore Timestamp).
 *   2. Κάθε mobile client υπολογίζει το λεπτό ως:
 *          minute = (now - liveStartTime) / MATCH_MINUTE_DURATION_MS
 *      και το παρουσιάζει στο UI. Κανείς δεν χρειάζεται να γράψει
 *      στη βάση κάθε λεπτό — οι ώρες είναι deterministic.
 *   3. Το λεπτό κόβεται στο {@link #MAX_MINUTE} (= 90).
 *
 * Επιτάχυνση για demo: αλλάζοντας μόνο το {@link #MATCH_MINUTE_DURATION_MS}
 * μπορούμε να συμπιέσουμε όλο τον αγώνα σε λίγα δευτερόλεπτα.
 */
public final class MatchClock {

    private MatchClock() {}

    /**
     * Πόσα πραγματικά millis διαρκεί ένα «λεπτό» αγώνα.
     *
     * <ul>
     *   <li>{@code 60_000} → 1:1 πραγματικός χρόνος (90λεπτο = 90 λεπτά)</li>
     *   <li>{@code 1_000}  → 1 sec / λεπτό (90λεπτο σε 90 sec) — ιδανικό για παρουσίαση</li>
     *   <li>{@code 2_000}  → 2 sec / λεπτό (90λεπτο σε 3 min)</li>
     * </ul>
     */
    public static final long MATCH_MINUTE_DURATION_MS = 1_000L;

    /** Το ανώτατο λεπτό που εμφανίζουμε. */
    public static final int MAX_MINUTE = 90;

    /**
     * @return τρέχον λεπτό αγώνα, ή 0 αν δεν έχει ξεκινήσει.
     *         Κόβεται στο {@link #MAX_MINUTE}.
     */
    public static int currentMinute(Timestamp liveStartTime) {
        if (liveStartTime == null) return 0;
        long elapsed = System.currentTimeMillis() - liveStartTime.toDate().getTime();
        if (elapsed <= 0) return 0;
        long mins = elapsed / MATCH_MINUTE_DURATION_MS;
        return mins >= MAX_MINUTE ? MAX_MINUTE : (int) mins;
    }

    /** @return {@code true} αν ο αγώνας έχει «τελειώσει χρονικά» (έφτασε στο 90'). */
    public static boolean isFullTime(Timestamp liveStartTime) {
        return currentMinute(liveStartTime) >= MAX_MINUTE;
    }
}
