# DSA Visual (Android)

Offline Android app for visualizing data structures & algorithms, step by step.
Built as the Android counterpart to the desktop Java/Swing visualizer in this
repository (see the top-level [`README.md`](../README.md) and `src/dsa/visual/`).

> **Milestone 1:** one polished **array** visualizer. The architecture is
> generic so trees, graphs, linked lists, and sorting can be added later
> without rewriting the engine or the renderer.

## Design goals

- Runs **completely offline**. No INTERNET permission, no network calls, no backend.
- Targets **low-end Android hardware** (e.g. the class of a Xiaomi Redmi 8A Dual).
- **Native Android** with a single custom `Canvas` renderer — no WebView.
- Algorithm logic is **pure Java** with zero Android dependency, so it can be
  reasoned about (and unit-tested) in isolation.
- Minimal dependencies (none beyond the Android framework).

## Architecture

```
Algorithm (pure Java)  ->  Visualization State (pure Java, compact)
   ->  VisualizationEngine (Android Handler playback)
   ->  ArrayRenderer (Canvas drawing)
   ->  Canvas
```

| Layer | Class | Knows about Android? |
|------|-------|----------------------|
| State marker | `core.State` | No |
| Step (description + state) | `core.Step<S>` | No |
| Algorithm | `core.ArrayAlgorithm` | No |
| State snapshot | `core.ArrayState` | No |
| Renderer interface | `core.Renderer<S>` | No |
| Playback engine | `core.VisualizationEngine<S>` | Only `android.os.Handler` |
| Canvas drawing | `render.ArrayCanvasView` | Yes |
| UI / controls | `MainActivity` | Yes |

The engine is **generic over the state type `S`**, so the same engine that drives
`ArrayState` today will drive a future `TreeState`/`GraphState` without changes.

### Memory model (important)

States do **not** clone large structures per step. For arrays, the backing array
never changes during a traversal, so every `ArrayState` holds a **reference** to
one shared, immutable array plus a handful of integer cursors. A 33-step
sequence for a 7-element array therefore costs ~33 tiny objects, not 33 array
copies. Future mutating algorithms (e.g. in-place sort) should record
swap/write cursors and let the renderer apply the delta against the shared
array, rather than cloning.

## Current visualizer

`ArrayAlgorithm` records four traversals of the sample array `3, 1, 7, 4, 9, 2, 5`
with search target `9`:

1. **Forward sum** — left-to-right running sum.
2. **Reverse sum** — right-to-left running sum.
3. **Max scan** — tracks the running maximum and its index.
4. **Linear search** — shows each comparison and the found index.

Each step carries a **specific** explanation, e.g.:

```
Checking index 3:   data[3] = 4  ≠ target 9.   → keep searching.
```

so the visual highlight and the text reinforce each other — answering
*"what is the algorithm doing right now?"* rather than just moving the screen.

## Controls

- ▶ Play / ⏸ Pause
- ⏮ Previous step
- ⏭ Next step
- ↺ Reset
- Step **scrubber** (drag to seek through any step)
- **Speed** slider (delay per step, ~50ms–1200ms)

## How to build

Requires **Android Studio** (Hedgehog or newer) or a local Gradle 8.9 install
with **JDK 17+** and the Android SDK (compileSdk 35).

```bash
cd android
./gradlew assembleDebug       # debug APK -> app/build/outputs/apk/debug/
./gradlew installDebug        # install on a connected device
```

The `local.properties` file (which points at your Android SDK) is generated
automatically by Android Studio on first open.

> **Gradle wrapper jar:** the repo includes the wrapper scripts and
> `gradle-wrapper.properties` (pinning Gradle 8.9). The `gradle-wrapper.jar`
> binary is not committed; open the project in Android Studio (which materializes
> it automatically), or run `gradle wrapper --gradle-version 8.9` once with a
> local Gradle install. This is the only manual step; everything else is standard.

## Minimum Android version

- **minSdk 23** (Android 6.0 Marshmallow)
- **targetSdk 35** (Android 15)
- Compiled against **Java 17**

## Performance decisions

1. **Single Canvas surface.** The whole array is drawn by one `View.onDraw`.
   No `ListView`/`RecyclerView` of element views; no per-element `View` objects.
2. **Zero allocation in `onDraw`.** `Paint`, `RectF`, `Path`, and the
   `StaticLayout` used for wrapping the description are created once and reused.
   Stepping only mutates the current state and calls `invalidate()`.
3. **No playback thread.** Animation advances via `Handler.postDelayed` on the
   main looper — one cheap repost per step. No background service, no thread
   bookkeeping, no inter-thread marshalling.
4. **Compact states.** Shared immutable array + ints per step (see Memory model).
5. **Playback halts on background.** `onPause()` calls `engine.pause()`, so the
   app uses ~0 CPU while backgrounded.
6. **Graceful scaling.** Cell width is computed from available width and clamped
   to a readable `[40dp, 88dp]` range, so a large array degrades to smaller
   cells rather than overflowing or freezing.

## Known limitations (milestone 1)

- Only the **array** structure is implemented (four traversals).
- The Gradle wrapper jar is not committed (see build note above).
- The visualizer is not yet instrument-tested on a real device in this sandbox;
   the pure-Java algorithm layer is verified with `javac` + a runnable harness
   (see "Verification" below), and the Android layer is statically reviewed.

## Verification done in this sandbox

Network is unavailable in the build sandbox, so the Android SDK could not be
downloaded here. What *was* verified:

- **Pure-Java core compiles** with `javac` (JDK 21): `State`, `Step`, `Renderer`,
  `ArrayState`, `ArrayAlgorithm`.
- **Algorithm runs correctly** via a harness: forward/reverse sum = 31, max = 9
  at index 4, linear search finds 9 at index 4 after 5 comparisons, the input
  array is not mutated, and all 33 steps share the **same array instance**
  (confirming the no-clone memory model).
- **Desktop Swing visualizer compiles** with `javac` (restored intact from the
  original commit).

## What should come next

- Binary search, two pointers, sliding window (arrays).
- Linked list, stack/queue.
- Trees (BST, DFS/BFS, traversals).
- Sorting (bubble, selection, insertion, merge, quick).
- Graphs (BFS, DFS, Dijkstra, topological sort).
- Unit/instrument tests wired into `app/src/test` and `androidTest`.
- Optional input editing (let the user change the array/target in-app).
