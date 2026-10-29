# BrainVault — Premium Offline Puzzle Game

Kotlin · Jetpack Compose · Material 3 · Room 2.6.1 (KSP) · Package `com.cartoniq.brainvault`

BrainVault is an offline brain training game featuring three core cognitive disciplines:
1. **Memory Grid** — 3D card-flip symbol matching across 7 escalating difficulty tiers with anti-spam tap locks.
2. **Quick Math** — High-speed arithmetic with streak multiplier combos (+2, +4, +6, +8, +10 bonus points), timed & relaxed modes, and emergency countdown indicators.
3. **Pattern Lock** — Cybernetic 3x3 memory node matrix with sequential pulse waves, harmonized audio cues, and multi-life grace system.

Additional features:
- **Daily Brain Challenge** — 100% offline, deterministic seed based on 64-bit FNV-1a of local date (`brainvault:v1:<yyyy-MM-dd>`).
- **Player Progression** — Persistent XP, dynamic level curve (`25 * (l-1) * l`), coin rewards, and daily streak system.
- **Cyber Vault Skin Shop** — Unlockable card skins: *Neon Cyber*, *Purple Haze*, *Emerald Matrix*, and *Solar Amber*.
- **Achievements & Lifetime Stats** — 9 comprehensive achievements, personal bests, move counts, and accuracy ratings.
- **Fair Game Economy** — Atomic Room transactions ensure exactly-once reward delivery and no duplicate claims.
- **Accessibility & Preferences** — Audio toggle, haptic feedback toggle, reduced-motion mode, system/dark/light themes.
- **Graceful Offline Ad Integration** — Optional Google AdMob with official test IDs; gameplay is 100% playable with zero network access.

---

## Build & Test Instructions

### Prerequisites
- JDK 17+ (configured via `JAVA_HOME`)
- Android SDK Platform 34 (configured via `ANDROID_HOME` or `local.properties` with `sdk.dir=...`)
- Gradle 8.9 (Gradle wrapper `gradlew` and `gradle/wrapper/gradle-wrapper.jar` are pre-bundled in this archive)

### Running Unit Tests
```bash
./gradlew testDebugUnitTest
```
Tests pure Kotlin game rules, daily seed determinism, reward monotonicity, and achievement triggers.

### Building the Debug APK
```bash
./gradlew assembleDebug
```
The output APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Installing on Device / Emulator
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## Architectural Highlights
- **`game/logic/`**: Pure Kotlin game engines (`MemoryLogic`, `MathGenerator`, `PatternGame`) without Android dependencies for instant unit testability.
- **`game/model/GameConfig.kt`**: Central authority for game tuning, reward formulas, skin catalogs, XP scaling, and achievement criteria.
- **`data/`**: Room database with version 2 schema and `MIGRATION_1_2` preventing data loss for existing players.
- **`ui/`**: Jetpack Compose with custom Neon Vault design system (`#0A0A0A` background, `#00E5FF` electric cyan, `#B388FF` neon purple, `#00E676` success green).
- **`ads/AdManager.kt`**: Resilient, non-blocking AdMob integration wrapped in defensive error handling.
