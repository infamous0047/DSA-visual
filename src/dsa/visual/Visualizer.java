package dsa.visual;

/**
 * A self-contained visualizer: it records steps into an engine and renders
 * them. Concrete subclasses implement {@link #recordSteps(VisualizationEngine.Recorder)}
 * (the algorithm)
 *
 * The split keeps algorithm code (pure step recording) separate from drawing
 * code, mirroring how you'd structure a real educational tool.
 */
public abstract class Visualizer {

    protected final VisualizationEngine engine;
    protected final StepRenderer renderer;

    protected Visualizer(StepRenderer renderer) {
        this.renderer = renderer;
        this.engine = new VisualizationEngine(renderer);
    }

    public VisualizationEngine engine() {
        return engine;
    }

    /** (Re)run the algorithm, populating the engine with steps. */
    public final void rebuild() {
        recordSteps(engine.recorder());
        engine.goTo(0);
    }

    /** Record every interesting state of the algorithm into the given recorder. */
    protected abstract void recordSteps(VisualizationEngine.Recorder recorder);

    /** Human-readable title shown in the launcher and window header. */
    public abstract String title();

    /** Short description of what this visualizer demonstrates. */
    public abstract String description();
}
