package dsa.visual.android.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Produces the recorded steps for four classic array-traversal patterns:
 *
 * <ol>
 *   <li><b>Forward sum</b> — {@code for i = 0..n-1}, running sum of elements.</li>
 *   <li><b>Reverse sum</b> — {@code for i = n-1..0}, running sum of elements.</li>
 *   <li><b>Max-element scan</b> — tracks the current index and the running max.</li>
 *   <li><b>Linear search</b> — shows each comparison and the found index.</li>
 * </ol>
 *
 * <p>This class is pure Java: it knows nothing about Android, {@code View},
 * {@code Canvas}, or the engine's playback timing. It only decides <em>which
 * states are interesting</em> and <em>what text explains each one</em>. The
 * recorded descriptions are specific (e.g. {@code "data[3] = 4  \u2260 target 9  \u2192 keep searching"})
 * so the visual highlight and the explanation reinforce each other, per the
 * project's educational philosophy: answer "what is the algorithm doing right now?"
 */
public final class ArrayAlgorithm {

    private final int[] data;
    private final int target;

    public ArrayAlgorithm(int[] data, int target) {
        // The algorithm owns its own copy of the input; callers can reuse the original.
        this.data = data.clone();
        this.target = target;
    }

    /** Builds the full ordered list of steps for all four traversals. */
    public List<Step<ArrayState>> build() {
        List<Step<ArrayState>> steps = new ArrayList<>();
        forwardSum(steps);
        reverseSum(steps);
        maxScan(steps);
        linearSearch(steps);
        return steps;
    }

    // --------------------------------------------------------------- forward sum

    private void forwardSum(List<Step<ArrayState>> s) {
        int sum = 0;
        s.add(step("Forward sum: start with sum = 0. We will add each element left to right.",
                new ArrayState(data, -1, sum, -1, -1, ArrayState.Mode.FORWARD_SUM)));
        for (int i = 0; i < data.length; i++) {
            int prev = sum;
            sum += data[i];
            s.add(step("Visit data[" + i + "] = " + data[i]
                            + ".   sum = " + prev + " + " + data[i] + " = " + sum + ".",
                    new ArrayState(data, i, sum, -1, -1, ArrayState.Mode.FORWARD_SUM)));
        }
        s.add(step("Done. Forward sum = " + sum + ". (Every element was added exactly once.)",
                new ArrayState(data, data.length - 1, sum, -1, -1, ArrayState.Mode.FORWARD_SUM)));
    }

    // --------------------------------------------------------------- reverse sum

    private void reverseSum(List<Step<ArrayState>> s) {
        int sum = 0;
        int start = data.length - 1;
        s.add(step("Reverse sum: start from the last index (" + start
                        + ") with sum = 0. We will add right to left.",
                new ArrayState(data, start, sum, -1, -1, ArrayState.Mode.REVERSE_SUM)));
        for (int i = data.length - 1; i >= 0; i--) {
            int prev = sum;
            sum += data[i];
            s.add(step("Visit data[" + i + "] = " + data[i]
                            + ".   sum = " + prev + " + " + data[i] + " = " + sum + ".",
                    new ArrayState(data, i, sum, -1, -1, ArrayState.Mode.REVERSE_SUM)));
        }
        s.add(step("Done. Reverse sum = " + sum + ". (Same as forward sum — order does not matter for addition.)",
                new ArrayState(data, 0, sum, -1, -1, ArrayState.Mode.REVERSE_SUM)));
    }

    // ------------------------------------------------------------------- max scan

    private void maxScan(List<Step<ArrayState>> s) {
        int maxIndex = 0;
        s.add(step("Max scan: assume data[0] = " + data[0] + " is the maximum so far.",
                new ArrayState(data, 0, data[0], maxIndex, -1, ArrayState.Mode.MAX_SCAN)));
        for (int i = 1; i < data.length; i++) {
            int currentMax = data[maxIndex];
            if (data[i] > currentMax) {
                maxIndex = i;
                s.add(step("data[" + i + "] = " + data[i] + "  >  current max " + currentMax
                                + ".   → new max = " + data[maxIndex] + " (index " + maxIndex + ").",
                        new ArrayState(data, i, data[maxIndex], maxIndex, -1, ArrayState.Mode.MAX_SCAN)));
            } else {
                s.add(step("data[" + i + "] = " + data[i] + "  ≤  current max " + currentMax
                                + ".   No change.",
                        new ArrayState(data, i, currentMax, maxIndex, -1, ArrayState.Mode.MAX_SCAN)));
            }
        }
        s.add(step("Done. Maximum = " + data[maxIndex] + " at index " + maxIndex + ".",
                new ArrayState(data, maxIndex, data[maxIndex], maxIndex, -1, ArrayState.Mode.MAX_SCAN)));
    }

    // --------------------------------------------------------------- linear search

    private void linearSearch(List<Step<ArrayState>> s) {
        s.add(step("Linear search for target = " + target + ". Check each element from index 0 upward.",
                new ArrayState(data, 0, target, -1, -1, ArrayState.Mode.LINEAR_SEARCH)));
        int found = -1;
        for (int i = 0; i < data.length; i++) {
            boolean match = data[i] == target;
            if (match) {
                found = i;
                s.add(step("Checking index " + i + ":   data[" + i + "] = " + data[i]
                                + "  == target " + target + ".   → FOUND!",
                        new ArrayState(data, i, target, -1, i, ArrayState.Mode.LINEAR_SEARCH)));
                break;
            }
            s.add(step("Checking index " + i + ":   data[" + i + "] = " + data[i]
                            + "  ≠ target " + target + ".   → keep searching.",
                    new ArrayState(data, i, target, -1, -1, ArrayState.Mode.LINEAR_SEARCH)));
        }
        if (found < 0) {
            s.add(step("Done. Target " + target + " is not present in the array.",
                    new ArrayState(data, data.length - 1, target, -1, -1, ArrayState.Mode.LINEAR_SEARCH)));
        } else {
            s.add(step("Done. Target " + target + " found at index " + found
                            + " after " + (found + 1) + " comparison(s).",
                    new ArrayState(data, found, target, -1, found, ArrayState.Mode.LINEAR_SEARCH)));
        }
    }

    private static Step<ArrayState> step(String description, ArrayState state) {
        return new Step<>(description, state);
    }
}
