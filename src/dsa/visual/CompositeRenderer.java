package dsa.visual;

import dsa.visual.VisualizationEngine.Step;

import java.util.List;

/**
 * Forwards every step event to a list of child renderers. Lets the engine drive
 * both the drawing panel and the window chrome (step counter, play button) from
 * a single source of truth.
 */
public final class CompositeRenderer implements StepRenderer {

    private final List<StepRenderer> children;

    public CompositeRenderer(StepRenderer... children) {
        this.children = List.of(children);
    }

    @Override
    public void render(Step step) {
        for (StepRenderer r : children) {
            r.render(step);
        }
    }

    @Override
    public void playbackStopped() {
        for (StepRenderer r : children) {
            r.playbackStopped();
        }
    }
}
