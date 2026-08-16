# DSA-visual

Interactive visualizations for data structures & algorithms — built to help a
student understand **what an algorithm is doing at each step**, not just to
animate the screen.

This repository contains two independent implementations of the same
visualization design, kept side by side:

## 1. Desktop (Java/Swing)

A standalone Java Swing visualizer. Entry point: `dsa.visual.MainWindow`.

```bash
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
