package dsa.visual.android;

import android.app.Activity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import dsa.visual.android.core.ArrayAlgorithm;
import dsa.visual.android.core.ArrayState;
import dsa.visual.android.core.Renderer;
import dsa.visual.android.core.Step;
import dsa.visual.android.core.VisualizationEngine;
import dsa.visual.android.render.ArrayCanvasView;

/**
 * Hosts the array visualizer: the canvas surface on top, controls below.
 *
 * <p>The Activity implements {@link Renderer} so the {@link VisualizationEngine}
 * can drive both the {@link ArrayCanvasView} (drawing) and the control-bar chrome
 * (step counter, play/pause label, scrubber position) from a single source of
 * truth — mirroring the desktop app's {@code CompositeRenderer} idea, but without
 * a separate class because there are only two targets here.
 *
 * <p>Playback stops in {@link #onPause()} so the app never animates while
 * backgrounded, keeping CPU/battery use flat when the user leaves the screen.
 */
public final class MainActivity extends Activity implements Renderer<ArrayState> {

    private static final int[] DEFAULT_DATA = {3, 1, 7, 4, 9, 2, 5};
    private static final int DEFAULT_TARGET = 9;

    // Speed slider maps progress 0..1000 to a delay in ms. Higher progress = faster.
    // Range: ~50ms (fast) .. ~1200ms (slow), quadratic so the usable middle is wide.
    private static final int SPEED_MAX = 1000;
    private static final long SPEED_FLOOR_MS = 50L;
    private static final long SPEED_CEIL_MS = 1200L;

    private VisualizationEngine<ArrayState> engine;
    private ArrayCanvasView canvas;
    private TextView stepLabel;
    private TextView stepDescription;
    private Button playButton;
    private Button prevButton;
    private Button nextButton;
    private Button resetButton;
    private SeekBar scrubber;
    private SeekBar speedBar;
    private TextView speedLabel;

    private boolean scrubbing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        engine = new VisualizationEngine<>(this);
        canvas = new ArrayCanvasView(this);

        LinearLayout root = buildLayout();
        setContentView(root);

        wireControls();
        rebuild();
    }

    // ----------------------------------------------------------------- layout

    private LinearLayout buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.setBackgroundColor(0xFF0D1117);

        TextView title = new TextView(this);
        title.setText("DSA Visual  \u2022  Array Traversal");
        title.setTextColor(0xFFE6EDF3);
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(title, linear(-1, dp(44)));

        // Visualization surface grows to fill available space.
        root.addView(canvas, linear(-1, 0, 1));

        stepLabel = new TextView(this);
        stepLabel.setTextColor(0xFF8B949E);
        stepLabel.setTextSize(13);
        stepLabel.setPadding(dp(2), dp(8), dp(2), dp(0));
        root.addView(stepLabel, linear(-1, dp(24)));

        // Scrubber: seek/scrub through steps.
        scrubber = new SeekBar(this);
        root.addView(scrubber, linear(-1, dp(40)));

        // Step description text.
        stepDescription = new TextView(this);
        stepDescription.setTextColor(0xFFE6EDF3);
        stepDescription.setTextSize(14);
        stepDescription.setPadding(dp(2), dp(6), dp(2), dp(8));
        stepDescription.setGravity(Gravity.START);
        root.addView(stepDescription, linear(-1, dp(56)));

        // Transport controls.
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);
        prevButton = makeButton("PREV");
        playButton = makeButton("PLAY");
        nextButton = makeButton("NEXT");
        resetButton = makeButton("RESET");
        controls.addView(prevButton, linear(0, dp(48), 1));
        controls.addView(playButton, linear(0, dp(48), 1));
        controls.addView(nextButton, linear(0, dp(48), 1));
        controls.addView(resetButton, linear(0, dp(48), 1));
        root.addView(controls);

        // Speed control.
        LinearLayout speedRow = new LinearLayout(this);
        speedRow.setOrientation(LinearLayout.HORIZONTAL);
        speedRow.setGravity(Gravity.CENTER_VERTICAL);
        speedLabel = new TextView(this);
        speedLabel.setTextColor(0xFF8B949E);
        speedLabel.setTextSize(12);
        speedLabel.setText("Speed");
        speedRow.addView(speedLabel, linear(-2, dp(40)));
        speedBar = new SeekBar(this);
        speedBar.setMax(SPEED_MAX);
        speedBar.setProgress(speedToProgress(450)); // start at 450ms delay
        speedRow.addView(speedBar, linear(0, dp(40), 1));
        root.addView(speedRow);

        return root;
    }

    private Button makeButton(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextColor(0xFFE6EDF3);
        return b;
    }

    private void wireControls() {
        prevButton.setOnClickListener(v -> engine.previous());
        nextButton.setOnClickListener(v -> engine.next());
        resetButton.setOnClickListener(v -> engine.reset());
        playButton.setOnClickListener(v -> {
            if (engine.isPlaying()) {
                engine.pause();
            } else {
                engine.play();
            }
        });

        scrubber.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (fromUser) {
                    engine.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
                scrubbing = true;
                engine.pause();
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                scrubbing = false;
            }
        });

        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                engine.setDelayMillis(progressToDelay(progress));
                updateSpeedLabel();
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) { }

            @Override
            public void onStopTrackingTouch(SeekBar bar) { }
        });
    }

    private void rebuild() {
        engine.record(new ArrayAlgorithm(DEFAULT_DATA, DEFAULT_TARGET).build());
        engine.setDelayMillis(progressToDelay(speedBar.getProgress()));
        updateSpeedLabel();
    }

    // --------------------------------------------------------------- Renderer

    @Override
    public void render(Step<ArrayState> step) {
        canvas.render(step);
        if (step != null) {
            stepDescription.setText(step.description);
        } else {
            stepDescription.setText("");
        }
    }

    @Override
    public void onPlaybackStateChanged(boolean playing, int index, int total) {
        canvas.onPlaybackStateChanged(playing, index, total);
        if (total > 0 && !scrubbing) {
            scrubber.setMax(Math.max(0, total - 1));
            scrubber.setProgress(Math.max(0, index));
        }
        stepLabel.setText((total > 0 ? (index + 1) : 0) + " / " + total);
        playButton.setText(playing ? "PAUSE" : "PLAY");
    }

    // ----------------------------------------------------------- lifecycle

    @Override
    protected void onPause() {
        engine.pause();
        super.onPause();
    }

    // ----------------------------------------------------------- helpers

    private void updateSpeedLabel() {
        long delay = engine.delayMillis();
        speedLabel.setText("Speed  (" + delay + "ms/step)");
    }

    /** progress 0..1000 -> delay ~50ms..~1200ms (higher progress = faster = smaller delay). */
    private static long progressToDelay(int progress) {
        float t = SPEED_MAX - progress;               // 0 = fast
        float norm = t / SPEED_MAX;                     // 0..1
        // quadratic curve so the usable middle is wide
        float delay = SPEED_FLOOR_MS + (SPEED_CEIL_MS - SPEED_FLOOR_MS) * (norm * norm);
        return (long) delay;
    }

    /** inverse of {@link #progressToDelay(int)} for the initial slider position. */
    private static int speedToProgress(long delayMs) {
        // Solve delay = floor + (ceil-floor) * norm^2  ->  norm = sqrt((delay-floor)/(ceil-floor))
        double norm = Math.sqrt(Math.max(0, (delayMs - SPEED_FLOOR_MS)
                / (double) (SPEED_CEIL_MS - SPEED_FLOOR_MS)));
        return Math.max(0, Math.min(SPEED_MAX, SPEED_MAX - (int) (norm * SPEED_MAX)));
    }

    private int dp(int value) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }

    private static LinearLayout.LayoutParams linear(int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private static LinearLayout.LayoutParams linear(int width, int height, float weight) {
        return new LinearLayout.LayoutParams(width, height, weight);
    }
}
