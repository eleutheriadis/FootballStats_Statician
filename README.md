# ⚽ Football Championship Statistics — Android App

Εφαρμογή Android για τη διαχείριση στατιστικών ποδοσφαιρικού πρωταθλήματος.  
Εργασία Εξαμήνου — Προγραμματισμός Κινητών Συσκευών, Πανεπιστήμιο Μακεδονίας.

---

## Περιγραφή

Η εφαρμογή απευθύνεται σε **υπεύθυνους στατιστικής** αγώνων και υποστηρίζει δύο ρόλους:

| Ρόλος | Λειτουργίες |
|-------|------------|
| **R1 — Κάρτα Αγώνα** | Επιλογή αγώνα, εμφάνιση ενδεκάδων & αλλαγών παικτών |
| **R2 — Στατιστικά** | Καταγραφή γκολ, σουτ, κίτρινων/κόκκινων καρτών, φάουλ, κόρνερ, πάσες, κ.ά. |

Όλα τα δεδομένα αποθηκεύονται και συγχρονίζονται **real-time** μέσω Firebase Firestore.

---

## Τεχνολογίες

- **Java** (Android SDK)
- **Firebase Firestore** — βάση δεδομένων cloud με real-time listeners
- **Firebase Authentication** — (υποδομή)
- **ViewPager2 + TabLayout** — tabs για ενδεκάδες & αλλαγές
- **RecyclerView** — λίστες παικτών και αγώνων
- **Glide** — φόρτωση εικόνων
- **Material Design 3**

---

## Απαιτήσεις

- Android Studio **Flamingo / 2022.1.1** ή νεότερο
- Java **11**
- Android **API 24+** (Android 7.0 Nougat)
- Λογαριασμός Firebase με ενεργό project

---

## Εγκατάσταση

### 1. Κλωνοποίηση repository

```bash
git clone https://github.com/<username>/FootballStats.git
cd FootballStats
```

### 2. Σύνδεση με Firebase

1. Άνοιξε το [Firebase Console](https://console.firebase.google.com/)
2. Δημιούργησε νέο project (ή χρησιμοποίησε υπάρχον)
3. Πρόσθεσε **Android app** με package name: `com.epo.footballstats`
4. Κατέβασε το `google-services.json` και τοποθέτησέ το στον φάκελο `app/`

```
FootballStats/
└── app/
    └── google-services.json   ← εδώ
```

### 3. Firestore Rules

Στο Firebase Console → Firestore → Rules, ορίστε:

```js
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read: if true;
      allow write: if request.auth != null;
    }
  }
}
```

### 4. Αρχικοποίηση δεδομένων (προαιρετικό)

Ο φάκελος `seed_firestore.py` περιέχει script για αυτόματη εισαγωγή δοκιμαστικών δεδομένων (5 ομάδες, παίκτες, αγώνες):

```bash
pip install firebase-admin
# Τοποθέτησε το serviceAccountKey.json στον ίδιο φάκελο
python3 seed_firestore.py
```

> Το `serviceAccountKey.json` κατεβαίνει από: Firebase Console → Project Settings → Service Accounts → Generate new private key

### 5. Build & Run

Άνοιξε το project στο Android Studio και πάτησε **Run ▶**.

---

## Δομή Project

```
app/src/main/java/com/epo/footballstats/
├── activities/
│   ├── MainActivity.java          # Εκκίνηση — επιλογή ρόλου
│   ├── SelectMatchActivity.java   # R1: Επιλογή αγώνα
│   ├── MatchCardActivity.java     # R1: Κάρτα αγώνα (ενδεκάδες + αλλαγές)
│   ├── ManageMatchActivity.java   # R2: Καταγραφή στατιστικών
│   ├── LineupFragment.java        # Tab ενδεκάδας
│   └── SubstitutionsFragment.java # Tab αλλαγών
├── adapters/
│   ├── MatchAdapter.java          # RecyclerView αγώνων
│   ├── LineupAdapter.java         # RecyclerView παικτών
│   └── SubstitutionAdapter.java   # RecyclerView αλλαγών
├── models/
│   ├── Match.java
│   ├── LineupPlayer.java
│   ├── Substitution.java
│   └── MatchStat.java             # Σταθερές τύπων στατιστικών
└── utils/
    └── FirestoreHelper.java       # Κεντρικές αναφορές Firestore collections
```

---

## Δομή Firestore

```
teams/{teamId}
players/{playerId}
championships/{champId}
matches/{matchId}
  ├── lineups/{teamId}        ← ενδεκάδα ανά ομάδα
  ├── substitutions/{subId}  ← αλλαγές παικτών
  └── stats/{statId}         ← στατιστικά γεγονότα (R2)
```

---

## Στατιστικά (R2)

Τα είδη στατιστικών που καταγράφονται ανά αγώνα:

| Κατηγορία | Τύποι |
|-----------|-------|
| Σουτ | Εντός εστίας, Εκτός, Μπλοκαρισμένο, Κεφαλιά |
| Μαρκαρίσματα | Επιτυχές / Αποτυχημένο |
| Πάσες | Επιτυχής / Αποτυχημένη |
| Κέντρες | Επιτυχής / Αποτυχημένη |
| Άσιστ | — |
| Λάθη | — |
| Φάουλ | Υπέρ / Κατά |
| Κόρνερ | — |
| Κάρτες | Κίτρινη / Κόκκινη (υπέρ / κατά) |

---

## Gradle Versions

| Εργαλείο | Έκδοση |
|----------|--------|
| Android Gradle Plugin | 7.4.2 |
| Gradle Wrapper | 7.6.1 |
| compileSdk / targetSdk | 33 |
| minSdk | 24 |
| Firebase BOM | 32.2.3 |

---

## Άδεια

Ακαδημαϊκό project — μη εμπορική χρήση.
