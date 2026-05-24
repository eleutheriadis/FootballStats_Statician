package com.epo.footballstats.utils;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.CollectionReference;

/**
 * Κεντρικό utility για πρόσβαση στο Firestore.
 *
 * Δομή βάσης (Firestore):
 * ├── teams/{teamId}               - Ομάδες
 * ├── players/{playerId}           - Παίκτες
 * ├── championships/{champId}      - Πρωταθλήματα
 * └── matches/{matchId}            - Αγώνες
 *       ├── lineups/{teamId}       - Ενδεκάδες (document per team)
 *       ├── substitutions/{subId}  - Αλλαγές παικτών
 *       └── stats/{statId}         - Στατιστικά γεγονότα (R2)
 */
public class FirestoreHelper {

    private static FirebaseFirestore db;

    public static FirebaseFirestore getDb() {
        if (db == null) {
            db = FirebaseFirestore.getInstance();
        }
        return db;
    }

    // Collections
    public static CollectionReference teamsRef() {
        return getDb().collection("teams");
    }

    public static CollectionReference playersRef() {
        return getDb().collection("players");
    }

    public static CollectionReference championshipsRef() {
        return getDb().collection("championships");
    }

    public static CollectionReference matchesRef() {
        return getDb().collection("matches");
    }

    // Sub-collections για συγκεκριμένο αγώνα
    public static CollectionReference lineupsRef(String matchId) {
        return getDb().collection("matches").document(matchId).collection("lineups");
    }

    public static CollectionReference substitutionsRef(String matchId) {
        return getDb().collection("matches").document(matchId).collection("substitutions");
    }

    public static CollectionReference statsRef(String matchId) {
        return getDb().collection("matches").document(matchId).collection("stats");
    }
}
