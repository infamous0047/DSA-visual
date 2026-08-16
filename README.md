# DSA-visual

Interactive visualizations for data structures & algorithms — built to help a
student understand **what an algorithm is doing at each step**, not just to
animate the screen.

This repository contains two independent implementations of the same
visualization design, kept side by side:

## 1. Desktop (Java/Swing)

A standalone Java Swing visualizer. Entry point: `dsa.visual.MainWindow`.

```bash
An interactive **Java Swing** visualizer for understanding how data structures
and algorithms execute, one step at a time. Built to make traversal, trees, and
maps *visible* — you watch each index visit, each comparison, and each running
accumulator as the algorithm progresses.

> Java is required to run this (no build tool needed — plain `javac`/`java`).
> Swing ships with the JDK, so there are no external dependencies.

## Status

| Structure / pattern | Status |
| --- | --- |
| **Array traversal** — forward sum, reverse sum, max-scan, linear search | ✅ Done |
| String traversal | 🚧 Planned |
| Binary tree (DFS / BFS) | 🚧 Planned |
| Maps / HashMap | 🚧 Planned |

The first complete visualizer covers **array traversal** (see
`src/dsa/visual/array/`). The framework (`VisualizationEngine`, `Visualizer`,
`StepRenderer`, `CompositeRenderer`) is designed so the next structures reuse the
same step-recording + control-bar plumbing.

## How it works

The architecture separates *algorithm* from *drawing*:

1. **`Visualizer`** — a subclass records its algorithm as a sequence of
   immutable `Step`s (each carrying a description + snapshot of state) into the
   `VisualizationEngine`. The algorithm code is pure — it never touches Swing.
2. **`StepRenderer`** — a Swing panel implements this to redraw itself for the
   current step. `ArrayPanel` is the renderer for arrays.
3. **`VisualizationEngine`** — replays steps forward/backward, auto-plays with
   an adjustable delay, and notifies the renderer on every change.

This split means adding a new structure = write `recordSteps(...)` + a
`JComponent` renderer. The window, controls, play/pause, and step counter come
for free.

## Build & run

```bash
# from the repo root
mkdir -p out
javac -d out $(find src -name '*.java')
java -cp out dsa.visual.MainWindow
```

Requires JDK 17+ (records + switch expressions). No external dependencies —
Swing ships with the JDK.

Covers array traversal: forward sum, reverse sum, max scan, and linear search.
The key abstraction is `VisualizationEngine`: algorithms record discrete,
immutable states; the engine replays them through a `StepRenderer`. This
separation is what the Android port preserves.

## 2. Android (native, offline)

A native Android app with a custom `Canvas` renderer, targeting low-end
hardware. See [`android/README.md`](android/README.md) for full details.

```bash
cd android
./gradlew assembleDebug
```

- **minSdk 23** (Android 6.0), targetSdk 35, Java 17.
- Fully offline — no INTERNET permission, no backend, no WebView.
- Architecture: `Algorithm → State → VisualizationEngine → Renderer → Canvas`,
  with the engine **generic** over the state type so trees/graphs/lists reuse it.
- Milestone 1 ships the four array traversals with play/pause/prev/next/reset,
  a step scrubber, an adjustable speed slider, and rich per-step explanations.

## Prototype reference

`DSAVisualAndroid.zip` is the original Android prototype this implementation
improved on. It is kept in the repo as an architectural reference.
Requires JDK 16+ (uses Java `record`s and the enhanced `switch` expressions).

## Using the visualizer

The window opens with the sample array `3,1,7,4,9,2,5` and search target `9`.

- **▶ Play / ⏸ Pause** — auto-advance through every step at the set speed.
- **⏮ Prev / Next ⏭** — step manually, one frame at a time.
- **↺ Reset** — jump back to the first step.
- **Speed slider** — delay per step in milliseconds (right = faster).
- **data** — comma-separated integers for your own array.
- **search target** — the value `linearSearch` looks for.
- **Apply & rebuild** — re-run all four traversals on your input.

Each phase (forward sum, reverse sum, max scan, linear search) is colour-coded:
the currently visited cell is outlined in the phase's accent colour, the
running max is shown with a dashed outline, and a found search target is
highlighted green.

## Project layout

```
src/dsa/visual/
├── VisualizationEngine.java        # step recording + playback engine (UI-agnostic)
├── StepRenderer.java              # bridge from engine steps to a Swing component
├── CompositeRenderer.java         # forwards events to multiple renderers
├── Visualizer.java                # abstract base: record steps + provide a title
├── MainWindow.java                # JFrame: drawing panel + control bar (main class)
└── array/
    ├── ArrayTraversalVisualizer.java  # the four traversal algorithms
    └── ArrayPanel.java                # Swing component that draws the array
```
