package dsa.visual.android.core;

/**
 * One recorded step of a visualization: a textual explanation of <em>what the
 * algorithm is doing right now</em>, paired with the {@link State} needed to
 * draw it.
 *
 * <p>Immutable. The {@code description} is the human-facing explanation; it is
 * intentionally specific (e.g. {@code "data[3] = 4  \u2260 target 9  \u2192 keep searching"}
 * rather than a generic "step 3") so the visual state and the text reinforce
 * each other.
 *
 * @param description human-readable explanation of this step
 * @param state      immutable snapshot used to render this step (never {@code null})
 * @param <S>        the concrete state type
 */
public final class Step<S extends State> {
    public final String description;
    public final S state;

    public Step(String description, S state) {
        if (state == null) {
            throw new IllegalArgumentException("step state must not be null");
        }
        this.description = description == null ? "" : description;
        this.state = state;
    }
}
