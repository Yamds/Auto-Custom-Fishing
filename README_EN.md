# Auto Custom Fishing for CustomFishing

## Notice

This project's code is **100% AI-generated**.

This means:

- The code works, but the implementation may not be elegant
- The project structure and details may be messy
- This repository is more like a "runnable proof of concept + a foundation for further improvement"

If you are reading the code, treat it as a reference for the approach rather than expecting hand-polished production code.

## Current Known Issue

The biggest problem with this project:

**It cannot reel in the rod instantly and reliably at the exact moment the correct Title/Subtitle message is received.**

In practice, the correct timing is theoretically detected, but there is still a slight delay when the click is actually executed. The cause of this delay has not been fully diagnosed, so the project uses a compromise approach instead:

- It does not directly rely on "click as soon as the correct title is seen"
- Instead, it first calculates the bobber / pointer movement speed
- Then predicts the future position based on the speed and reels in slightly early

This approach covers most cases, but with clear side effects:

- Fish are still occasionally missed
- Some bars or some speeds require manually tuning the advance amount

Therefore, the repository also includes an **advance curve editor tool**, specifically for adjusting the early-click amount per bar and per speed. The default parameters can catch most fish, but not 100%.

If you know why "reeling in without delay after detecting the correct timing" cannot be achieved, or you can make this logic more reliable, feel free to open an Issue / PR.

## Introduction

This is a client-side auto-fishing mod written for the [Custom-Fishing](https://github.com/Xiao-MoMi/Custom-Fishing) server plugin, running on `Minecraft 26.2 + Fabric`.

The goal is straightforward: for Custom-Fishing's "Stardew Valley-like" fishing minigame, automate the whole repetitive loop of casting, waiting, detecting the minigame, judging the timing, and reeling in.

Currently only the river fishing mode is automated; sea fishing mode is not supported.

## What This Project Does

Custom-Fishing's river fishing is not vanilla Minecraft's "reel in when the bobber sinks" mechanic. Instead, after a fish bites, a minigame with a pointer and a success zone appears. This mod:

- Automatically casts the rod
- Listens for a fish bite and automatically reels in to enter the minigame
- Parses Title / Subtitle UI data sent by the server
- Reverse-engineers the pointer position and movement speed
- Clicks automatically at the optimal moment to complete the minigame
- Automatically recasts after completion and starts the next round

Essentially, it is a client-side automation tool that performs custom parsing of the Custom-Fishing river fishing UI protocol.

## Supported Scope

- Supported: river fishing `accurate_click`-style minigame
- Not supported: sea fishing / tension-style gameplay
- Not supported: generic server auto-fishing
- Requirement: the client must be able to receive the title component data sent by Custom-Fishing

## Core Logic

This project does not use image recognition or screen reading. It directly analyzes the UI component data the client receives.

The overall flow:

1. `FishingHookMixin` listens to the local player's fishing hook entity `biting` state to detect a bite.
2. `TitlePacketMixin` intercepts `Hud#setTitle`, `setSubtitle`, `setOverlayMessage` and passes the received text to `TitleAnalyzer`.
3. `TitleAnalyzer` parses the custom font characters and offset characters used by Custom-Fishing from `Component.toString()`.
4. The pointer progress is reverse-engineered from the offsets and mapped to logical grid positions on the current bar.
5. Position changes over a recent window are recorded to calculate the average speed.
6. The optimal click timing is calculated from the success zone of the current bar, the current speed, and the "advance curve" in the config.
7. `FishingController` drives the whole flow with a small state machine: cast, wait for bite, enter minigame, click, reel in, recast.

In short:

`Bite detection + Title parsing + position speed measurement + advance prediction + state machine control`

## Main Features

- Full river fishing automation
- Automatic recast
- Timeout auto-reel and recast
- Per-bar-type advance amount configuration
- Global advance fine-tuning
- Built-in curve editor to adjust advance by speed
- Config UI integrated with Cloth Config / Mod Menu
- Logs of the last 20 clicks for tuning

## Usage

### Requirements

- Minecraft: `26.2`
- Java: `25`
- Fabric Loader: `0.19.3+`
- Fabric API: `0.157.0+26.2`

### Installation

1. Build the project, or use a pre-built jar.
2. Put the mod into the client `mods` folder.
3. Join a server with Custom-Fishing enabled.
4. Hold a fishing rod.
5. Press the hotkey to enable auto fishing.

### Default Hotkeys

- `R`: toggle auto fishing
- `O`: open the config screen

If Mod Menu is installed, the config screen can also be opened directly from Mod Menu.

## Configuration

Config file location:

`config/autofishing.json`

Main options:

- `modEnabled`: master switch
- `globalAdvance`: global advance fine-tune, applies to all bars
- `autoRecastMinutes`: timeout before auto-reeling and recasting after casting, in minutes, `0` to disable
- `castIntervalSeconds`: base waiting time between reeling in and recasting (in seconds)
- `castIntervalRandomSeconds`: random jitter range around the base cast interval (in seconds); each actual interval is randomized within `base ± range` to simulate human behavior, `0` to disable
- `barCurves`: advance curve per bar, configured as "speed(ms/section) -> advance amount"

Default philosophy:

- Low-difficulty bars use smaller advance amounts
- Mid/high-difficulty bars use more aggressive advance
- The faster the speed, the earlier the click usually needs to be

## Project Structure

```text
src/main/java/com/autofishing/
  AutoFishingMod.java                Mod entry point

src/client/java/com/autofishing/
  AutoFishingClient.java             Client entry point
  fishing/FishingController.java     Auto-fishing state machine
  game/TitleAnalyzer.java            Custom-Fishing UI parser
  game/GameState.java                Game state and speed measurement
  mixin/FishingHookMixin.java        Fish bite detection
  mixin/TitlePacketMixin.java        Title / Subtitle interception
  config/AutoFishConfig.java         Config loading and saving
  config/AutoFishConfigScreen.java   Config screen
  config/CurveEditorScreen.java      Advance curve editor
  config/FishingLog.java             Click log
```

## Who Should Read This

If you want to understand the code quickly, the most valuable classes to start with are:

- `FishingController`
- `TitleAnalyzer`
- `GameState`
- `FishingHookMixin`
- `TitlePacketMixin`

These files basically cover the core behavior.

## Building

```bash
./gradlew build
```

Windows:

```powershell
.\gradlew.bat build
```

Build output is located at:

`build/libs/`

## Known Limitations

- Only river fishing is automated so far
- The logic heavily depends on Custom-Fishing's current title component structure and custom font offset rules
- Cannot guarantee "instant, zero-delay reeling after detecting the correct timing"; it currently relies on speed prediction + advance compensation
- If the server plugin changes its UI encoding in the future, this mod may need to be updated accordingly
- This is a highly customized project; compatibility with other fishing plugins is not guaranteed

## License

The repository uses the existing `LICENSE` file in the project.
