package dsa.visual.android.core;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Core step engine for every visualizer.
 *
 * <p>An algorithm records a flat list of {@link Step}s that describe discrete,
 * immutable states of the visualization. The engine then replays them one at a
 * time, with an adjustable delay, on the <strong>main thread</strong> via a
 * {@link Handler}, and notifies a {@link Renderer} so it can repaint.
 *
 * <h2>Design</h2>
 * The engine is intentionally UI-agnostic: it knows nothing about {@code View},
 * {@code Canvas}, or Activities. It is generic over the state type {@code S},
 * so the same engine drives arrays now and trees/graphs/linked-lists later.
 *
 * <h2>Threading</h2>
 * Playback runs on the main looper using {@code postDelayed}. There is no worker
 * thread and no background service — this keeps memory predictable and avoids
 * the bookkeeping (and churn) of a dedicated playback thread for an animation
 * whose only job is to advance a pointer every few hundred milliseconds.
 *
 * <p>The owning {@code Activity} is responsible for calling {@link #stop()} (or
 * {@link #release()}) in {@code onPause()} so playback halts while backgrounded.
 */
public final class VisualizationEngine<S extends State> {

    private final List<Step<S>> steps = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Renderer<S> renderer;

    private int current = -1;
    private long delayMillis = 450;
    private boolean playing;

    /** Self-reposting runnable that advances one step per tick. */
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (!playing) {
                return;
            }
            // Advance; if we just showed the last step, stop at the end.
            if (!nextInternal()) {
                stopInternal();
                renderer.onPlaybackStateChanged(false, current, steps.size());
                return;
            }
            handler.postDelayed(this, delayMillis);
        }
    };

    public VisualizationEngine(Renderer<S> renderer) {
        if (renderer == null) {
            throw new IllegalArgumentException("renderer must not be null");
        }
        this.renderer = renderer;
    }

    /** Records a fresh batch of steps, replacing any previous recording. */
    public void record(List<Step<S>> newSteps) {
        stop();
        steps.clear();
        steps.addAll(newSteps);
        current = steps.isEmpty() ? -1 : 0;
        notifyStep();
    }

    /** Unmodifiable view of the recorded steps (used by the scrubber). */
    public List<Step<S>> steps() {
        return Collections.unmodifiableList(steps);
    }

    public int size() {
        return steps.size();
    }

    public int currentIndex() {
        return current;
    }

    public boolean isPlaying() {
        return playing;
    }

    /** Current playback delay between steps, in milliseconds (>= 60). */
    public long delayMillis() {
        return delayMillis;
    }

    public Step<S> currentStep() {
        return (current >= 0 && current < steps.size()) ? steps.get(current) : null;
    }

    /**
     * Sets the delay between steps. Clamped to a sane floor so playback never
     * spins faster than the eye (or a low-end device's UI thread) can follow.
     */
    public void setDelayMillis(long millis) {
        this.delayMillis = Math.max(60L, millis);
    }

    // ------------------------------------------------------------------ playback

    public void play() {
        if (playing || steps.isEmpty()) {
            return;
        }
        // If already at the end, restart from the beginning so replay shows
        // the whole sequence instead of doing nothing.
        if (current >= steps.size() - 1) {
            current = 0;
            notifyStep();
        }
        playing = true;
        renderer.onPlaybackStateChanged(true, current, steps.size());
        handler.postDelayed(ticker, delayMillis);
    }

    public void pause() {
        if (!playing) {
            return;
        }
        stopInternal();
        renderer.onPlaybackStateChanged(false, current, steps.size());
    }

    /** Stops playback; alias of {@link #pause()} for parity with reset semantics. */
    public void stop() {
        stopInternal();
    }

    public boolean next() {
        boolean moved = nextInternal();
        notifyPlaybackState();
        return moved;
    }

    public boolean previous() {
        boolean moved = previousInternal();
        notifyPlaybackState();
        return moved;
    }

    public void reset() {
        stopInternal();
        current = steps.isEmpty() ? -1 : 0;
        notifyStep();
        notifyPlaybackState();
    }

    /** Seek (scrub) to an arbitrary step index. Stops playback. */
    public void seekTo(int index) {
        stopInternal();
        if (steps.isEmpty()) {
            current = -1;
        } else {
            current = Math.max(0, Math.min(index, steps.size() - 1));
        }
        notifyStep();
        notifyPlaybackState();
    }

    /** Must be called when the owning Activity is destroyed/paused for good. */
    public void release() {
        stopInternal();
        renderer.onPlaybackStateChanged(false, current, steps.size());
    }

    // ----------------------------------------------------------------- internals

    private boolean nextInternal() {
        if (current + 1 >= steps.size()) {
            return false;
        }
        current++;
        notifyStep();
        return true;
    }

    private boolean previousInternal() {
        if (current <= 0) {
            // Clamp at the first step; still notify so the UI stays consistent.
            current = steps.isEmpty() ? -1 : 0;
            notifyStep();
            return false;
        }
        current--;
        notifyStep();
        return true;
    }

    private void stopInternal() {
        playing = false;
        handler.removeCallbacks(ticker);
    }

    private void notifyStep() {
        renderer.render(currentStep());
    }

    private void notifyPlaybackState() {
        renderer.onPlaybackStateChanged(playing, current, steps.size());
    }
}
