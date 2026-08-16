package dsa.visual.android.core;

/**
 * Bridge between the UI-agnostic {@link VisualizationEngine} and a concrete
 * drawing surface (an Android {@code Canvas} in this project, but the engine
 * does not know or care).
 *
 * <p>The engine calls {@link #render(Step)} whenever the visible step changes and
 * {@link #onPlaybackStateChanged(boolean, int, int)} whenever playback starts or
 * stops, so the renderer/UI can sync controls (play/pause label, step counter).
 *
 * @param <S> the concrete state type this renderer knows how to draw
 */
public interface Renderer<S extends State> {

    /** Called on the main thread whenever the current step changes. */
    void render(Step<S> step);

    /**
     * Called when playback starts or stops, and after a manual step change.
     *
     * @param playing   {@code true} if auto-play is now active
     * @param index     the current step index
     * @param total     the total number of steps
     */
    void onPlaybackStateChanged(boolean playing, int index, int total);
}
