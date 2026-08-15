package dsa.visual.array;

import dsa.visual.StepRenderer;
import dsa.visual.VisualizationEngine;
import dsa.visual.Visualizer;

import java.awt.Color;
import java.util.Arrays;

/**
 * Visualizes four classic array-traversal patterns, one step at a time, with an
 * explicit "current index" highlight and a running accumulator:
 *
 *  1. Forward iteration (for i = 0..n-1) — running sum of elements.
 *  2. Reverse iteration (for i = n-1..0) — running sum of elements.
 *  3. Max-element scan — tracks current index and the running max.
 *  4. Linear search for a target value — shows comparisons and the found index.
 *
 * Each recorded step is an immutable {@link ArraySnapshot}, so scrubbing back
 * and forth through history is always consistent.
 */
public final class ArrayTraversalVisualizer extends Visualizer {

    private final int[] data;
    private final int target;

    public ArrayTraversalVisualizer(StepRenderer renderer, int[] data, int target) {
        super(renderer);
        // Defensive copy: the algorithm must not mutate the source array.
        this.data = Arrays.copyOf(data, data.length);
        this.target = target;
    }

    public int[] data() {
        return Arrays.copyOf(data, data.length);
    }

    public int target() {
        return target;
    }

    @Override
    public String title() {
        return "Array Traversal";
    }

    @Override
    public String description() {
        return "Forward & reverse iteration, max-scan, and linear search over an int[].";
    }

    @Override
    protected void recordSteps(VisualizationEngine.Recorder r) {
        forwardSum(r);
        reverseSum(r);
        maxScan(r);
        linearSearch(r);
    }

    private void forwardSum(VisualizationEngine.Recorder r) {
        r.enterFrame("forwardSum()");
        int sum = 0;
        r.record("Forward sum: start with sum = 0.",
                snapshot(0, sum, -1, -1, Mode.FORWARD_SUM));
        for (int i = 0; i < data.length; i++) {
            sum += data[i];
            r.record("Visit data[" + i + "] = " + data[i] + "  →  sum = " + sum,
                    snapshot(i, sum, -1, -1, Mode.FORWARD_SUM));
        }
        r.record("Done. Forward sum = " + sum + ".",
                snapshot(data.length - 1, sum, -1, -1, Mode.FORWARD_SUM));
        r.leaveFrame();
    }

    private void reverseSum(VisualizationEngine.Recorder r) {
        r.enterFrame("reverseSum()");
        int sum = 0;
        int start = data.length - 1;
        r.record("Reverse sum: start from the last index with sum = 0.",
                snapshot(start, sum, -1, -1, Mode.REVERSE_SUM));
        for (int i = data.length - 1; i >= 0; i--) {
            sum += data[i];
            r.record("Visit data[" + i + "] = " + data[i] + "  →  sum = " + sum,
                    snapshot(i, sum, -1, -1, Mode.REVERSE_SUM));
        }
        r.record("Done. Reverse sum = " + sum + " (same as forward sum).",
                snapshot(0, sum, -1, -1, Mode.REVERSE_SUM));
        r.leaveFrame();
    }

    private void maxScan(VisualizationEngine.Recorder r) {
        r.enterFrame("maxScan()");
        int maxIndex = 0;
        r.record("Max scan: assume data[0] = " + data[0] + " is the max so far.",
                snapshot(0, data[0], maxIndex, -1, Mode.MAX_SCAN));
        for (int i = 1; i < data.length; i++) {
            int previousMax = data[maxIndex];
            if (data[i] > data[maxIndex]) {
                maxIndex = i;
                r.record("data[" + i + "] = " + data[i] + " > current max " + previousMax
                        + "  →  new max = " + data[maxIndex] + " (index " + maxIndex + ")",
                        snapshot(i, data[maxIndex], maxIndex, -1, Mode.MAX_SCAN));
            } else {
                r.record("data[" + i + "] = " + data[i] + " ≤ current max " + data[maxIndex]
                        + ". No change.",
                        snapshot(i, data[maxIndex], maxIndex, -1, Mode.MAX_SCAN));
            }
        }
        r.record("Done. Max element = " + data[maxIndex] + " at index " + maxIndex + ".",
                snapshot(maxIndex, data[maxIndex], maxIndex, -1, Mode.MAX_SCAN));
        r.leaveFrame();
    }

    private void linearSearch(VisualizationEngine.Recorder r) {
        r.enterFrame("linearSearch(target=" + target + ")");
        int found = -1;
        for (int i = 0; i < data.length; i++) {
            boolean match = data[i] == target;
            r.record("Compare data[" + i + "] = " + data[i]
                    + (match ? "  == target → found!" : "  ≠ target → keep searching."),
                    snapshot(i, target, -1, match ? i : -1, Mode.LINEAR_SEARCH));
            if (match) {
                found = i;
                break;
            }
        }
        if (found < 0) {
            r.record("Done. Target " + target + " not present in the array.",
                    snapshot(data.length - 1, target, -1, -1, Mode.LINEAR_SEARCH));
        } else {
            r.record("Done. Target " + target + " found at index " + found + ".",
                    snapshot(found, target, -1, found, Mode.LINEAR_SEARCH));
        }
        r.leaveFrame();
    }

    /** What phase of the algorithm this snapshot belongs to — drives coloring. */
    public enum Mode {
        FORWARD_SUM(Color.decode("#1f6feb")),
        REVERSE_SUM(Color.decode("#8957e5")),
        MAX_SCAN(Color.decode("#2ea043")),
        LINEAR_SEARCH(Color.decode("#db6d28"));

        private final Color accent;

        Mode(Color accent) {
            this.accent = accent;
        }

        public Color accent() {
            return accent;
        }
    }

    /**
     * Immutable per-step state for the array visualizer.
     *
     * @param currentIndex the index the algorithm is currently visiting (-1 = none)
     * @param accumulator  the running value (sum, max, or target) for this phase
     * @param maxIndex     index of the running maximum during MAX_SCAN (-1 otherwise)
     * @param foundIndex   index where the target was found during LINEAR_SEARCH (-1 otherwise)
     */
    public record ArraySnapshot(int[] data, int currentIndex, int accumulator,
                                int maxIndex, int foundIndex, Mode mode) {
        public ArraySnapshot {
            data = data.clone(); // defensive copy on construction
        }
    }

    private ArraySnapshot snapshot(int currentIndex, int accumulator,
                                   int maxIndex, int foundIndex, Mode mode) {
        return new ArraySnapshot(data, currentIndex, accumulator, maxIndex, foundIndex, mode);
    }
}
