# Black Hole Artifact Companion

Black Hole Artifact Companion is a Kotlin and Jetpack Compose utility app for tracking a physical sci-fi board/card game session. The app is designed to provide fast, at-a-glance support during play rather than replacing the board game itself. It helps players manage hands, decks, discard piles, equipment, player resources, and revealed information while keeping interaction simple enough to use during a tabletop game.

## Purpose

The app supports a board game called Black Hole Artifact by reducing manual bookkeeping. Players can quickly see the current player, deck counts, discard counts, HP, energy, evolution points, attack range, hand size, and equipment. The app is a utility app because its purpose is focused, practical, and information-oriented: it tracks game state and provides small controls that support an existing real-world activity.

## Core Features

- Tracks four players and a Draft Pool.
- Draws cards from Normal and Premium decks.
- Tracks Normal and Premium discard pile counts.
- Supports withdrawing the most recently discarded card.
- Displays each player's HP, energy, EXP, attack range, hand size, and equipment count.
- Uses clickable HP and energy tokens to update current values quickly.
- Allows HP, energy, EXP, and attack range adjustment directly from the player summary.
- Supports equipping, discarding, showing, and previewing cards.
- Displays other players' shown equipment, with unrevealed cards shown as unknown cards.
- Supports equipment stealing, including unrevealed equipment.
- Provides a large card preview screen for checking card text.
- Includes a settings screen for game setup values such as starting hand size, starting energy, and max HP/energy limits.
- Fetches NASA Astronomy Picture of the Day data and can use the daily image as the app background.

## Implementation Overview

The project is implemented in Kotlin using Jetpack Compose and Material Design 3. The app keeps the starter template's simple two-tab structure:

- `Utility` tab: the main gameplay tracker.
- `Settings` tab: configuration and NASA space data controls.

Most of the app is currently implemented in:

`app/src/main/java/au/edu/jcu/cp3406_cp5307_utilityappstartertemplate/MainActivity.kt`

The app uses a `Scaffold` with bottom navigation to switch between the two main screens without adding complex navigation. UI state is stored with Compose state APIs such as `mutableStateOf` and `mutableStateListOf`, allowing the interface to update immediately when cards, tokens, or settings change.

## Architecture

The app follows a simple ViewModel and Repository structure inspired by the Android Basics with Compose course:

- `GameViewModel` owns the app-level state and survives recomposition.
- `GameSession` manages gameplay state, including players, decks, discard piles, equipment, and stat changes.
- `CardRepository` creates the card data used by the Normal and Premium decks.
- `SpaceIntelRepository` fetches NASA APOD data and downloads the background image.

This separation keeps the UI focused on rendering and user interaction while moving data creation and network work away from composable functions.

## Networking

The app includes networking to meet the external API requirement. It uses the NASA Astronomy Picture of the Day API to fetch a daily space title, date, explanation, media URL, and image. The Settings screen includes a refresh control, and the fetched image can be used as the app-wide background.

The app declares Internet access in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

The network request runs outside the main thread using Kotlin coroutines and `Dispatchers.IO`.

## Assets

The app uses custom card and token images from the original board game prototype. These assets are stored in the Android drawable resources and are displayed through Compose `Image` composables with `painterResource`.

## How to Run

1. Open the `board_game` project in Android Studio.
2. Let Gradle sync finish.
3. Select an emulator or physical Android device.
4. Run the `app` configuration.

Recommended target: a modern Android emulator or physical device using the current Android Studio setup.

## Notes

This app is intended as a companion utility for a physical game session. It does not automate every game rule. Some interactions, such as damage resolution and evolution decisions, are intentionally left as manual controls so that players can use the app flexibly while playing the board game.
