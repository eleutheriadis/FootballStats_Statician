# Δομή Firestore - ΕΠΟ Stats

## Collections

### `/teams/{teamId}`
```json
{
  "name": "ΠΑΟΚ",
  "city": "Θεσσαλονίκη",
  "logoUrl": "https://..."
}
```

### `/players/{playerId}`
```json
{
  "name": "Σαββίδης Γιώργης",
  "position": "FWD",
  "teamId": "teamId123",
  "photoUrl": "https://...",
  "jerseyNumber": 9
}
```

### `/championships/{champId}`
```json
{
  "name": "Super League 2025-26",
  "teams": ["teamId1", "teamId2", "..."]
}
```

### `/matches/{matchId}`
```json
{
  "championshipId": "champId1",
  "homeTeamId": "teamId1",
  "awayTeamId": "teamId2",
  "homeTeamName": "ΠΑΟΚ",
  "awayTeamName": "Ολυμπιακός",
  "gameweek": 5,
  "date": "Timestamp",
  "status": "SCHEDULED | LIVE | FINISHED",
  "homeScore": 0,
  "awayScore": 0,
  "currentMinute": 0
}
```

### `/matches/{matchId}/lineups/{teamId}`
```json
{
  "players": [
    {
      "playerId": "pid1",
      "playerName": "Πασχαλάκης",
      "position": "GK",
      "jerseyNumber": 1,
      "isStarter": true,
      "isActive": true
    },
    ...11 starters + 7 bench...
  ]
}
```

### `/matches/{matchId}/substitutions/{autoId}`
```json
{
  "teamId": "teamId1",
  "teamName": "ΠΑΟΚ",
  "playerOutId": "pid5",
  "playerOutName": "Μάτος",
  "playerInId": "pid15",
  "playerInName": "Νέος Παίκτης",
  "minute": 65,
  "timestamp": "Timestamp"
}
```

### `/matches/{matchId}/stats/{autoId}` (R2)
```json
{
  "type": "SHOT | TACKLE | PASS | CROSS | ASSIST | ERROR | FOUL | CORNER | CARD",
  "teamId": "teamId1",
  "teamName": "ΠΑΟΚ",
  "playerId": "pid9",
  "playerName": "Σαββίδης",
  "minute": 34,
  "shotType": "ON_TARGET | OFF_TARGET | BLOCKED | HEADER",
  "result": "GOAL | NO_GOAL | SUCCESS | FAIL",
  "direction": "FOR | AGAINST",
  "cardColor": "YELLOW | RED",
  "timestamp": "Timestamp"
}
```

## Firestore Rules (βασικές)
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /matches/{matchId} {
      allow read: if true;
      allow write: if request.auth != null;
      match /lineups/{teamId} { allow read, write: if request.auth != null; }
      match /substitutions/{id} { allow read, write: if request.auth != null; }
      match /stats/{id} { allow read, write: if request.auth != null; }
    }
    match /teams/{id} { allow read: if true; allow write: if request.auth != null; }
    match /players/{id} { allow read: if true; allow write: if request.auth != null; }
  }
}
```
