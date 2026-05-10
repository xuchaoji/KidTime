# TimeBank (KidTime) — Android

## Project state

- **Fully functional** — all 5 phases from `docs/design.md` are implemented and building.
- Product name is **KidTime** (童心护航); project/app name is **TimeBank**. Both appear in the codebase.

## Design doc

`docs/design.md` contains the original phased implementation plan (data layer → domain logic → foreground service → custom views → UI). The current code implements all phases with additional parent-management and UI-polish features beyond the original spec.

## Architecture

- Language: **Java** (Java 8 source/target compat)
- Pattern: **MVVM** (ViewModel + LiveData)
- Storage: Room Database + SharedPreferences
- No RxJava — use `ExecutorService` / `Handler` / `LiveData`
- UI: AndroidX + Material Design 3 (Light theme, NoActionBar)

### Package structure

```
com.xuchaoji.android.timebank
├── TimeBankApplication.java      # App init (PrefsManager + DB + TimeBankManager)
├── data/
│   ├── PrefsManager.java         # SharedPreferences singleton
│   ├── TimeTransaction.java      # Room entity (type enums: 1-4)
│   ├── TaskRecord.java           # Room entity (status: 0=PENDING,1=REVIEWING,2=COMPLETED)
│   ├── TransactionDao.java       # Room DAO
│   ├── TaskDao.java              # Room DAO
│   └── AppDatabase.java          # Room DB singleton (allowMainThreadQueries)
├── manager/
│   └── TimeBankManager.java      # Core business logic
├── service/
│   └── TimerForegroundService.java  # 60s tick foreground service
├── view/
│   └── CircleTimerView.java      # Custom Canvas ring with color thresholds
└── ui/
    ├── MainActivity.java         # Fragment host
    ├── ChildDashboardFragment.java  # Main child dashboard
    ├── ChildDashboardViewModel.java
    ├── ChildTaskActivity.java    # Child-accessible task accept list
    ├── AlertActivity.java        # Time-up fullscreen alert
    ├── ParentSettingsActivity.java  # PIN-protected parent hub
    └── TaskManageActivity.java   # Parent task CRUD + approve/reject
```

## Build system

| Tool           | Version |
|----------------|---------|
| Gradle         | 8.7     |
| AGP            | 8.6.0   |
| compileSdk     | 34      |
| minSdk         | 24      |
| targetSdk      | 34      |

Version catalog: `gradle/libs.versions.toml` (Room 2.6.1, Lifecycle 2.7.0, annotationProcessor for Room).

## Commands

```sh
./gradlew assembleDebug          # build debug APK
./gradlew test                   # run unit tests
./gradlew connectedAndroidTest   # run instrumented tests (needs device/emulator)
./gradlew lint                   # run lint
```

### Environment gotcha

`local.properties` points to the Windows SDK path (`D:\android\sdk`) which is invalid under WSL. The `build-tools/34.0.0` ships Windows binaries that WSL cannot run. Before building in WSL, ensure:

1. `local.properties` has `sdk.dir=/mnt/d/android/sdk`
2. Linux build-tools are extracted to `build-tools/34.0.0/` (download from `https://dl.google.com/android/repository/build-tools_r34-linux.zip`)

A quick fix script:
```sh
echo "sdk.dir=/mnt/d/android/sdk" > local.properties
rm -rf /mnt/d/android/sdk/build-tools/34.0.0
unzip -q /tmp/build-tools_r34-linux.zip -d /mnt/d/android/sdk/build-tools/
mv /mnt/d/android/sdk/build-tools/android-14 /mnt/d/android/sdk/build-tools/34.0.0
```

## Key conventions

- Namespace: `com.xuchaoji.android.timebank`
- Source dir: `app/src/main/java/com/xuchaoji/android/timebank/`
- Room entities use `Long` auto-generated primary keys; transaction types are `int` enums (1=daily grant, 2=task reward, 3=usage consume, 4=penalty)
- Database uses `.allowMainThreadQueries()` — sync DAO calls are safe from main thread
- Task status flow: `PENDING(0) → REVIEWING(1) → COMPLETED(2)`. Child accepts (0→1), parent approves (1→2) or rejects (1→0)
- The `TimeBankManager` business class must call `checkAndApplyDailyReset()` before any balance read/write
- Foreground service deducts balance every 60s via `ScheduledExecutorService`; time-up alert is posted to main thread via `Handler(Looper.getMainLooper())`
- Custom `CircleTimerView` uses Canvas drawing with color thresholds (>=50% green, 20-50% yellow, <=20% red)
- Theme: MaterialComponents Light NoActionBar with warm coral/orange primary palette
- AlertActivity uses `Theme.TimeBank.Alert` extending `Theme.MaterialComponents.NoActionBar`
- Release: minifyEnabled false, ProGuard not active
- `.gitignore` covers IDE files, builds, local.properties
