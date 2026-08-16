package dsa.visual.android.core;

/**
 * Marker interface for a single snapshot of a visualization.
 *
 * <p>A {@code State} is a small, immutable description of <em>what the screen
 * should show</em> at one discrete step of an algorithm. It must contain enough
 * information for a {@link Renderer} to reproduce the picture deterministically,
 * and nothing more.
 *
 * <h2>Memory guidance</h2>
 * Do <strong>not</strong> blindly copy large structures into every step. For an
 * array traversal, the underlying array never changes, so the state holds a
 * reference to one shared immutable array plus a handful of integer cursors
 * rather than cloning the array for every recorded step.
 *
 * <p>Future structures (trees, graphs) should follow the same idea: snapshot the
 * minimal mutable bookkeeping (pointers, visited set, frontier) and reference the
 * static structure by identity, unless the algorithm genuinely mutates it.
 */
public interface State {
}
