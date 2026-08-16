package dsa.visual.android.core;

/**
 * Immutable per-step state for the array visualizer.
 *
 * <p><b>Memory:</b> the {@code data} array is <em>not</em> cloned per step. During
 * a traversal the array contents never change, so every step references the same
 * shared, immutable backing array by identity. Only the small mutable bookkeeping
 * (cursors, the running accumulator, the phase) is stored per step. This keeps a
 * long sequence cheap: each step is a tiny object holding ints + one reference,
 * not a fresh array copy.
 *
 * <p>If a future algorithm genuinely mutates the array (e.g. an in-place sort),
 * it should record a {@code Swap}/{@code Write} cursor into the state and let the
 * renderer apply that delta against the shared array, rather than cloning the
 * whole array for every comparison.
 */
public final class ArrayState implements State {

    /** Which phase of the algorithm this step belongs to — drives coloring. */
    public enum Mode {
        FORWARD_SUM,
        REVERSE_SUM,
        MAX_SCAN,
        LINEAR_SEARCH
    }

    /** Shared, immutable backing array. Do not mutate. */
    public final int[] data;

    /** Index the algorithm is currently visiting (-1 = none / not applicable). */
    public final int currentIndex;

    /** Running value for this phase: sum, current max, or the search target. */
    public final int accumulator;

    /** Index of the running maximum during MAX_SCAN (-1 otherwise). */
    public final int maxIndex;

    /** Index where the target was found during LINEAR_SEARCH (-1 otherwise). */
    public final int foundIndex;

    public final Mode mode;

    public ArrayState(int[] data, int currentIndex, int accumulator,
                      int maxIndex, int foundIndex, Mode mode) {
        // No clone: see class javadoc. The array is shared and treated immutable.
        this.data = data;
        this.currentIndex = currentIndex;
        this.accumulator = accumulator;
        this.maxIndex = maxIndex;
        this.foundIndex = foundIndex;
        this.mode = mode;
    }
}
