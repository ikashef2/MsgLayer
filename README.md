# MsgLayer

Android-first messaging organizer: a non-destructive intelligence layer over your messages.

Messages -> Events -> Facts -> Current State -> Actions

## MVP

- Overview dashboard (Needs Attention, Finance, What changed?, cleaned-up groups)
- Chronological Inbox with Primary / Hidden filters
- Finance balances and transactions from Iranian-style banking SMS (mock)
- Ask My Messages (local retrieval + evidence)
- Search, Rules, Activity log
- Dark-mode-first Material 3 UI
- Mock SMS dataset (mostly Persian)

## Build

```bash
./gradlew assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

GitHub Actions uploads the debug APK as an artifact on every push to main/master.

## Principles

- Never permanently delete messages automatically
- Source messages are ground truth; AI facts are always traceable
- Existing user organization wins over AI classification
- Privacy-first: AiProvider is replaceable; no cloud by default