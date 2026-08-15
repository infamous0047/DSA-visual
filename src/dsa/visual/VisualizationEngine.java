package dsa.visual;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Core step engine for all visualizers.
 *
 * A visualizer records a list of {@link Step}s that describe discrete states of
 * the visualization (e.g. "index i now points at element 3"). The engine
 * replays them one at a time, with an adjustable delay, and notifies a
 * {@link StepRenderer} so it can repaint the panel for the current step.
 *
 * The design is intentionally UI-agnostic: the engine knows nothing about
 * Swing. A renderer bridges steps to a {@link javax.swing.JComponent}.
 */
public final class VisualizationEngine {

    private final List<Step> steps = new ArrayList<>();
    private final StepRenderer renderer;

    private int current = -1;
    private int speedMillis = 700;
    private boolean playing;
    private Thread worker;

    public VisualizationEngine(StepRenderer renderer) {
        if (renderer == null) {
            throw new IllegalArgumentException("renderer must not be null");
        }
        this.renderer = renderer;
    }

    /** A single recorded state of the visualization, including a description. */
    public static final class Step {
        private final String description;
        private final Deque<String> callStack;
        private final Object payload;

        public Step(String description, Object payload, Deque<String> callStack) {
            this.description = description == null ? "" : description;
            this.payload = payload;
            // Defensive copy so later mutations don't rewrite history.
            this.callStack = new ArrayDeque<>(callStack == null ? new ArrayDeque<>() : callStack);
        }

        public String description() {
            return description;
        }

        public Object payload() {
            return payload;
        }

        public Deque<String> callStack() {
            return new ArrayDeque<>(callStack);
        }
    }

    /** A builder used while recording an algorithm so each step carries its own state snapshot. */
    public static final class Recorder {
        private final VisualizationEngine engine;
        private final Deque<String> callStack = new ArrayDeque<>();

        Recorder(VisualizationEngine engine) {
            this.engine = engine;
        }

        public void enterFrame(String frame) {
            callStack.push(frame);
        }

        public void leaveFrame() {
            if (!callStack.isEmpty()) {
                callStack.pop();
            }
        }

        public void record(String description, Object snapshot) {
            engine.steps.add(new Step(description, snapshot, callStack));
        }

        public int stepCount() {
            return engine.steps.size();
        }
    }

    /** Returns a fresh recorder bound to this engine. Recording clears prior steps. */
    public Recorder recorder() {
        steps.clear();
        resetPlayback();
        return new Recorder(this);
    }

    public int stepCount() {
        return steps.size();
    }

    public int currentIndex() {
        return current;
    }

    public boolean hasSteps() {
        return !steps.isEmpty();
    }

    public Step currentStep() {
        if (current < 0 || current >= steps.size()) {
            return null;
        }
        return steps.get(current);
    }

    public int speedMillis() {
        return speedMillis;
    }

    public void setSpeedMillis(int millis) {
        this.speedMillis = Math.max(0, millis);
    }

    /** Clears recorded steps and resets playback to the beginning. */
    public void resetPlayback() {
        stop();
        current = steps.isEmpty() ? -1 : 0;
        renderer.render(currentStep());
    }

    public void goTo(int index) {
        stop();
        if (steps.isEmpty()) {
            current = -1;
        } else {
            current = Math.max(0, Math.min(index, steps.size() - 1));
        }
        renderer.render(currentStep());
    }

    public boolean next() {
        if (current + 1 < steps.size()) {
            current++;
            renderer.render(currentStep());
            return true;
        }
        return false;
    }

    public boolean prev() {
        if (current > 0) {
            current--;
            renderer.render(currentStep());
            return true;
        }
        return false;
    }

    public boolean isPlaying() {
        return playing;
    }

    public void play() {
        if (playing || steps.isEmpty()) {
            return;
        }
        playing = true;
        worker = new Thread(this::playLoop, "dsa-visual-player");
        worker.setDaemon(true);
        worker.start();
    }

    public void stop() {
        playing = false;
        if (worker != null) {
            worker.interrupt();
            worker = null;
        }
    }

    private void playLoop() {
        try {
            // If we're already at the end, restart from the beginning so replay
            // shows the whole sequence instead of nothing.
            if (current >= steps.size() - 1) {
                current = 0;
                renderer.render(currentStep());
            }
            // Hold the current (already-rendered) step for one interval first,
            // then advance. This ensures step 0 is actually visible during playback
            // rather than being skipped on the first iteration.
            while (playing) {
                Thread.sleep(speedMillis);
                if (!playing) {
                    break;
                }
                if (!next()) {
                    break; // reached the end
                }
            }
        } catch (InterruptedException e) {
            // stop() interrupted us; that's expected.
            Thread.currentThread().interrupt();
        } finally {
            playing = false;
            renderer.playbackStopped();
        }
    }
}
