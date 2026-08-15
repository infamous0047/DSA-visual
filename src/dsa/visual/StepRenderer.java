package dsa.visual;

import dsa.visual.VisualizationEngine.Step;

/**
 * Bridge between the UI-agnostic {@link VisualizationEngine} and a concrete
 * Swing component. The engine calls {@link #render(Step)} whenever the visible
 * step changes and {@link #playbackStopped()} when auto-play ends so the
 * renderer can sync its play/pause button state.
 */
public interface StepRenderer {

    void render(Step step);

    void playbackStopped();
}
