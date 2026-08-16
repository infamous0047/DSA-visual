package dsa.visual;

import dsa.visual.array.ArrayPanel;
import dsa.visual.array.ArrayTraversalVisualizer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

/**
 * The application window: holds a single {@link Visualizer}'s drawing panel in
 * the centre and a control bar at the bottom (prev / play-pause / next / reset
 * / step counter / speed slider). When the visualizer is rebuilt (new data
 * input), the panel is refreshed from the recorded steps.
 *
 * It also implements {@link StepRenderer} so the engine can keep the step
 * counter and play/pause button in sync with playback; the actual drawing is
 * delegated to {@link ArrayPanel}, and both are driven through a
 * {@link CompositeRenderer}.
 */
public final class MainWindow extends JFrame implements StepRenderer {

    private final ArrayPanel panel = new ArrayPanel();
    private ArrayTraversalVisualizer visualizer;

    private final JButton playPause = new JButton("▶ Play");
    private final JButton prev = new JButton("⏮ Prev");
    private final JButton next = new JButton("Next ⏭");
    private final JButton reset = new JButton("↺ Reset");
    private final JLabel stepLabel = new JLabel("0 / 0");
    private final JSlider speed = new JSlider(0, 2000, 700);
    private final JLabel speedLabel = new JLabel("700 ms");

    private final JTextField dataField = new JTextField("3,1,7,4,9,2,5", 20);
    private final JTextField targetField = new JTextField("9", 5);

    public MainWindow() {
        super("DSA Visualizer — Array Traversal");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(java.awt.Color.decode("#0d1117"));

        JScrollPane scroll = new JScrollPane(panel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(java.awt.Color.decode("#0d1117"));
        add(scroll, BorderLayout.CENTER);

        add(buildControls(), BorderLayout.SOUTH);

        // Default sample data so the app shows something immediately.
        // The engine drives both the panel (drawing) and this window (step counter / buttons)
        // through a single CompositeRenderer.
        attachVisualizer(new ArrayTraversalVisualizer(
                new CompositeRenderer(panel, this), new int[]{3, 1, 7, 4, 9, 2, 5}, 9));

        pack();
        setMinimumSize(new Dimension(760, 420));
        setLocationRelativeTo(null);
    }

    private JPanel buildControls() {
        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
        bar.setBackground(java.awt.Color.decode("#0d1117"));
        bar.setBorder(new EmptyBorder(8, 12, 10, 12));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row.setBackground(java.awt.Color.decode("#0d1117"));
        styleButton(prev); styleButton(playPause); styleButton(next); styleButton(reset);
        row.add(prev); row.add(playPause); row.add(next); row.add(reset);
        row.add(Box.createHorizontalStrut(12));
        stepLabel.setForeground(java.awt.Color.decode("#8b949e"));
        row.add(stepLabel);
        row.add(Box.createHorizontalStrut(18));
        speedLabel.setForeground(java.awt.Color.decode("#8b949e"));
        row.add(speedLabel);
        row.add(speed);

        bar.add(row);
        bar.add(Box.createVerticalStrut(6));
        bar.add(buildInputRow());
        return bar;
    }

    private JPanel buildInputRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        row.setBackground(java.awt.Color.decode("#0d1117"));
        JLabel dl = new JLabel("data:");
        dl.setForeground(java.awt.Color.decode("#8b949e"));
        row.add(dl); row.add(dataField);
        JLabel tl = new JLabel("search target:");
        tl.setForeground(java.awt.Color.decode("#8b949e"));
        row.add(tl); row.add(targetField);
        JButton apply = new JButton("Apply & rebuild");
        styleButton(apply);
        apply.addActionListener(e -> rebuildFromInputs());
        row.add(apply);
        return row;
    }

    private void rebuildFromInputs() {
        try {
            String[] parts = dataField.getText().trim().split(",");
            int[] data = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                data[i] = Integer.parseInt(parts[i].trim());
            }
            int target = Integer.parseInt(targetField.getText().trim());
            attachVisualizer(new ArrayTraversalVisualizer(
                    new CompositeRenderer(panel, this), data, target));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Enter comma-separated integers for data and one integer for the target.",
                    "Invalid input", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void attachVisualizer(ArrayTraversalVisualizer v) {
        this.visualizer = v;
        v.rebuild();
        refreshStepLabel();
        syncPlayButton();
    }

    private void styleButton(JButton b) {
        b.setBackground(java.awt.Color.decode("#21262d"));
        b.setForeground(java.awt.Color.decode("#e6edf3"));
        b.setBorder(BorderFactory.createLineBorder(java.awt.Color.decode("#30363d")));
        b.setFocusPainted(false);
    }

    private void refreshStepLabel() {
        int idx = visualizer.engine().currentIndex() + 1;
        int total = visualizer.engine().stepCount();
        stepLabel.setText(idx + " / " + total);
    }

    private void syncPlayButton() {
        playPause.setText(visualizer.engine().isPlaying() ? "⏸ Pause" : "▶ Play");
    }

    private void wireEvents() {
        prev.addActionListener(e -> {
            visualizer.engine().stop(); syncPlayButton();
            visualizer.engine().prev(); refreshStepLabel();
        });
        next.addActionListener(e -> {
            visualizer.engine().stop(); syncPlayButton();
            visualizer.engine().next(); refreshStepLabel();
        });
        playPause.addActionListener(e -> {
            if (visualizer.engine().isPlaying()) {
                visualizer.engine().stop();
            } else {
                visualizer.engine().play();
            }
            syncPlayButton();
            refreshStepLabel();
        });
        reset.addActionListener(e -> {
            visualizer.engine().stop();
            visualizer.engine().goTo(0);
            syncPlayButton(); refreshStepLabel();
        });
        speed.addChangeListener(e -> {
            int v = speed.getValue();
            visualizer.engine().setSpeedMillis(v);
            speedLabel.setText(v + " ms");
        });
        speed.setInverted(true); // slide right = faster (smaller delay)
        speedLabel.setText(speed.getValue() + " ms");
    }

    // --- StepRenderer: keep chrome in sync with the engine -----------------

    @Override
    public void render(VisualizationEngine.Step step) {
        SwingUtilities.invokeLater(this::refreshStepLabel);
    }

    @Override
    public void playbackStopped() {
        SwingUtilities.invokeLater(() -> {
            syncPlayButton();
            refreshStepLabel();
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainWindow w = new MainWindow();
            w.wireEvents();
            w.setVisible(true);
        });
    }
}
