package dsa.visual.array;

import dsa.visual.StepRenderer;
import dsa.visual.VisualizationEngine.Step;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;

/**
 * Swing component that draws an array as a row of boxes, highlights the index
 * the algorithm is currently visiting, and shows the running accumulator /
 * max / found marker depending on the active phase.
 *
 * Implements {@link StepRenderer} directly so the engine can drive repaints
 * without a separate adapter class.
 */
public final class ArrayPanel extends JComponent implements StepRenderer {

    private static final int CELL = 84;       // px width & height of each array cell
    private static final int GAP = 14;        // px between cells
    private static final int PADDING = 40;    // px around the whole drawing
    private static final int TOP_BAR = 150;   // reserved space for the description text
    private static final Color BG = Color.decode("#0d1117");
    private static final Color CELL_FILL = Color.decode("#161b22");
    private static final Color CELL_EDGE = Color.decode("#30363d");
    private static final Color TEXT = Color.decode("#e6edf3");
    private static final Color DIM = Color.decode("#8b949e");
    private static final Color FOUND = Color.decode("#2ea043");

    private ArrayTraversalVisualizer.ArraySnapshot snapshot;

    public ArrayPanel() {
        setBackground(BG);
        setOpaque(true);
    }

    @Override
    public Dimension getPreferredSize() {
        int n = snapshot == null ? 6 : snapshot.data().length;
        int w = PADDING * 2 + n * CELL + (n - 1) * GAP;
        int h = PADDING * 2 + CELL + TOP_BAR;
        return new Dimension(w, h);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(BG);
            g2.fillRect(0, 0, getWidth(), getHeight());

            if (snapshot == null) {
                drawHint(g2, "No data yet. Press Play.");
                return;
            }

            int[] data = snapshot.data();
            int n = data.length;
            Color accent = snapshot.mode().accent();

            // Centre the row of cells horizontally in the component.
            int rowWidth = n * CELL + (n - 1) * GAP;
            int startX = Math.max(PADDING, (getWidth() - rowWidth) / 2);
            int y = TOP_BAR;

            for (int i = 0; i < n; i++) {
                int x = startX + i * (CELL + GAP);
                boolean current = (i == snapshot.currentIndex());
                boolean max = (i == snapshot.maxIndex());
                boolean found = (i == snapshot.foundIndex());

                g2.setColor(CELL_FILL);
                g2.fillRect(x, y, CELL, CELL);

                Stroke old = g2.getStroke();
                if (found) {
                    g2.setColor(FOUND);
                    g2.setStroke(new BasicStroke(3));
                } else if (current) {
                    g2.setColor(accent);
                    g2.setStroke(new BasicStroke(3));
                } else if (max) {
                    g2.setColor(DIM);
                    g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                            1f, new float[]{6f, 6f}, 0f));
                } else {
                    g2.setColor(CELL_EDGE);
                    g2.setStroke(new BasicStroke(1));
                }
                g2.drawRect(x, y, CELL, CELL);
                g2.setStroke(old);

                // Element value.
                g2.setColor(current || found ? TEXT : DIM);
                g2.setFont(g2.getFont().deriveFont(Font.BOLD, 26f));
                centreText(g2, String.valueOf(data[i]), x + CELL / 2, y + CELL / 2 + 9);

                // Index label below the cell.
                g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 13f));
                g2.setColor(DIM);
                centreText(g2, String.valueOf(i), x + CELL / 2, y + CELL + 18);

                // Pointer arrow over the current cell.
                if (current) {
                    int tipX = x + CELL / 2;
                    int tipY = y - 12;
                    g2.setColor(accent);
                    int[] xs = {tipX, tipX - 7, tipX + 7};
                    int[] ys = {tipY, tipY - 12, tipY - 12};
                    g2.fillPolygon(xs, ys, 3);
                }
                if (max && !current && !found) {
                    centreText(g2, "max", x + CELL / 2, y - 6);
                }
            }

            // Mode + accumulator read-out above the array.
            drawModeBanner(g2, accent);
        } finally {
            g2.dispose();
        }
    }

    private void drawModeBanner(Graphics2D g2, Color accent) {
        ArrayTraversalVisualizer.ArraySnapshot s = snapshot;
        String mode = s.mode().name().replace('_', ' ');
        String acc;
        switch (s.mode()) {
            case FORWARD_SUM, REVERSE_SUM -> acc = "sum = " + s.accumulator();
            case MAX_SCAN -> acc = "max so far = " + s.accumulator()
                    + (s.maxIndex() >= 0 ? "  (idx " + s.maxIndex() + ")" : "");
            case LINEAR_SEARCH -> acc = "searching for " + s.accumulator()
                    + (s.foundIndex() >= 0 ? "  → found at idx " + s.foundIndex() : "  (not found yet)");
            default -> acc = "";
        }
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 20f));
        g2.setColor(accent);
        int y = 40;
        g2.drawString(mode, PADDING, y);
        g2.setColor(TEXT);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18f));
        g2.drawString(acc, PADDING, y + 28);

        if (currentStepDescription != null) {
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 15f));
            g2.setColor(DIM);
            drawWrapped(g2, currentStepDescription, PADDING, y + 60, getWidth() - PADDING * 2, 20);
        }
    }

    private String currentStepDescription;

    private void drawHint(Graphics2D g2, String text) {
        g2.setColor(DIM);
        g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 16f));
        centreText(g2, text, getWidth() / 2, getHeight() / 2);
    }

    private void drawWrapped(Graphics2D g2, String text, int x, int y, int maxW, int lineHeight) {
        FontMetrics fm = g2.getFontMetrics();
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        int cy = y;
        for (String word : words) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (fm.stringWidth(candidate) > maxW && !line.isEmpty()) {
                g2.drawString(line.toString(), x, cy);
                cy += lineHeight;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) {
            g2.drawString(line.toString(), x, cy);
        }
    }

    private void centreText(Graphics2D g2, String text, int cx, int cy) {
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(text);
        g2.drawString(text, cx - w / 2, cy);
    }

    // --- StepRenderer ------------------------------------------------------

    @Override
    public void render(Step step) {
        // The engine may call this from its playback thread; marshal to the EDT
        // so all Swing state mutations happen on a single thread.
        SwingUtilities.invokeLater(() -> {
            currentStepDescription = step == null ? null : step.description();
            if (step == null || step.payload() == null) {
                snapshot = null;
            } else {
                Object p = step.payload();
                if (p instanceof ArrayTraversalVisualizer.ArraySnapshot arr) {
                    snapshot = arr;
                }
            }
            revalidate();
            repaint();
        });
    }

    @Override
    public void playbackStopped() {
        // The MainWindow listens for button sync; nothing to do on the panel.
    }
}
