package dsa.visual.android.render;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.View;

import dsa.visual.android.core.ArrayState;
import dsa.visual.android.core.Renderer;
import dsa.visual.android.core.Step;

/**
 * Draws one {@link ArrayState} as a row of cells on a single {@link Canvas}.
 *
 * <p>This is the only Android surface used for visualization — a single custom
 * {@code View} that paints the whole picture itself. It creates <strong>no View
 * objects per element</strong> and <strong>allocates nothing during {@link #onDraw}</strong>:
 * all {@link Paint}s, the scratch {@link RectF}/{@link Path}, and the
 * {@link StaticLayout} used for wrapping the description text are created once and
 * reused. This is what keeps playback smooth on low-end hardware: stepping just
 * mutates the current state and calls {@link #invalidate()}, which triggers one
 * cheap repaint.
 *
 * <p>The view implements {@link Renderer} so the {@code VisualizationEngine} can
 * drive it directly, without an adapter.
 */
public final class ArrayCanvasView extends View implements Renderer<ArrayState> {

    // --- palette (kept in code so the renderer is fully self-contained) ---
    private static final int BG = 0xFF0D1117;
    private static final int CELL_FILL = 0xFF1C232C;
    private static final int CELL_EDGE = 0xFF2D333B;
    private static final int TEXT_PRIMARY = 0xFFE6EDF3;
    private static final int TEXT_SECONDARY = 0xFF8B949E;
    private static final int TEXT_DIM = 0xFF6E7681;
    private static final int FOUND = 0xFF3FB950;

    private static final int ACCENT_FORWARD = 0xFF2F81F7;
    private static final int ACCENT_REVERSE = 0xFFA371F7;
    private static final int ACCENT_MAX = 0xFF3FB950;
    private static final int ACCENT_SEARCH = 0xFFD29922;

    // --- reused paint objects (created once, mutated per draw) ---
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint descPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF scratch = new RectF();
    private final Path trianglePath = new Path();

    private final float cellValueSize;
    private final float indexSize;
    private final float bannerSize;
    private final float captionSize;

    private ArrayState state;
    private String description = "";

    // Reused layout for the wrapped description. Rebuilt (not reallocated) when the
    // available width or text changes.
    private StaticLayout descLayout;
    private int descLayoutWidth = -1;
    private String descLayoutText = null;

    public ArrayCanvasView(Context context) {
        super(context);
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        text.setTextAlign(Paint.Align.CENTER);
        descPaint.setColor(TEXT_SECONDARY);
        cellValueSize = sp(28);
        indexSize = sp(13);
        bannerSize = sp(16);
        captionSize = sp(12);
        setWillNotDraw(false);
    }

    private float sp(int value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value,
                getResources().getDisplayMetrics());
    }

    private float dp(int value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }

    // --------------------------------------------------------------- Renderer API

    @Override
    public void render(Step<ArrayState> step) {
        if (step == null) {
            state = null;
            description = "";
        } else {
            state = step.state;
            description = step.description;
        }
        invalidate();
    }

    @Override
    public void onPlaybackStateChanged(boolean playing, int index, int total) {
        // The Activity owns the controls; the canvas only needs to repaint.
        invalidate();
    }

    // --------------------------------------------------------------------- drawing

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(BG);

        if (state == null || state.data == null || state.data.length == 0) {
            return;
        }

        int[] data = state.data;
        int n = data.length;
        int accent = accentFor(state.mode);

        float width = getWidth();
        float height = getHeight();
        float pad = dp(16);

        // --- layout the row of cells, centred, capped so cells stay readable ---
        float maxCell = dp(88);
        float minCell = dp(40);
        float gap = dp(8);
        float avail = width - pad * 2 - gap * (n - 1);
        float cell = Math.min(maxCell, Math.max(minCell, avail / n));
        float cellH = Math.min(cell, dp(74));
        float rowWidth = cell * n + gap * (n - 1);
        float left = (width - rowWidth) / 2f;

        // Leave room at the top for the banner and at the bottom for the description.
        float descHeight = layoutDescription(width - pad * 2);
        float topSpace = dp(84);
        float bottomSpace = descHeight + pad;
        float rowTop = Math.max(topSpace, (height - bottomSpace - topSpace - cellH) / 2f + topSpace);

        // --- banner: mode + accumulator read-out ---
        drawBanner(canvas, accent, left, rowTop - dp(40), width - pad * 2);

        // --- cells ---
        for (int i = 0; i < n; i++) {
            float x = left + i * (cell + gap);
            boolean isCurrent = (i == state.currentIndex);
            boolean isMax = (i == state.maxIndex);
            boolean isFound = (i == state.foundIndex);

            // fill
            fill.setColor(isFound ? FOUND : (isCurrent ? dim(accent, 0.18f) : CELL_FILL));
            scratch.set(x + 1, rowTop, x + cell - 1, rowTop + cellH);
            canvas.drawRoundRect(scratch, dp(8), dp(8), fill);

            // stroke
            stroke.setStrokeWidth(dp(isFound ? 3 : (isCurrent ? 3 : 1)));
            if (isFound) {
                stroke.setColor(FOUND);
            } else if (isCurrent) {
                stroke.setColor(accent);
            } else if (isMax) {
                stroke.setColor(TEXT_DIM);
            } else {
                stroke.setColor(CELL_EDGE);
            }
            scratch.set(x + 1, rowTop, x + cell - 1, rowTop + cellH);
            canvas.drawRoundRect(scratch, dp(8), dp(8), stroke);

            // value
            text.setTextSize(cellValueSize);
            text.setColor(isCurrent || isFound ? TEXT_PRIMARY : TEXT_SECONDARY);
            float baseline = rowTop + cellH / 2f - (text.descent() + text.ascent()) / 2f;
            canvas.drawText(String.valueOf(data[i]), x + cell / 2f, baseline, text);

            // index label below
            text.setTextSize(indexSize);
            text.setColor(isCurrent ? accent : TEXT_DIM);
            canvas.drawText(String.valueOf(i), x + cell / 2f, rowTop + cellH + dp(18), text);

            // pointer arrow above the current cell
            if (isCurrent) {
                fill.setColor(accent);
                drawTriangle(canvas, x + cell / 2f, rowTop - dp(8), dp(7));
            }

            // "max" tag above the running max (unless it is also current/found)
            if (isMax && !isCurrent && !isFound) {
                text.setTextSize(captionSize);
                text.setColor(TEXT_DIM);
                canvas.drawText("max", x + cell / 2f, rowTop - dp(10), text);
            }
        }

        // --- description (wrapped) at the bottom ---
        if (descLayout != null) {
            canvas.save();
            canvas.translate(pad, height - descHeight - pad / 2f);
            descLayout.draw(canvas);
            canvas.restore();
        }
    }

    private void drawBanner(Canvas canvas, int accent, float left, float top, float width) {
        String mode = state.mode.name().replace('_', ' ');
        String acc = readout();

        text.setColor(accent);
        text.setTextSize(bannerSize);
        text.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(mode, left, top, text);

        text.setColor(TEXT_SECONDARY);
        text.setTextSize(sp(14));
        canvas.drawText(acc, left, top + dp(22), text);

        // restore default align so the cell loop's centred text is unaffected
        text.setTextAlign(Paint.Align.CENTER);
    }

    private String readout() {
        switch (state.mode) {
            case FORWARD_SUM:
            case REVERSE_SUM:
                return "sum = " + state.accumulator;
            case MAX_SCAN:
                return "max so far = " + state.accumulator
                        + (state.maxIndex >= 0 ? "   (index " + state.maxIndex + ")" : "");
            case LINEAR_SEARCH:
                return "target = " + state.accumulator
                        + (state.foundIndex >= 0 ? "   \u2192 found at index " + state.foundIndex : "   (not found yet)");
            default:
                return "";
        }
    }

    /** Builds (or rebuilds) the wrapped description layout; returns its height. */
    private float layoutDescription(float width) {
        if (description == null) {
            description = "";
        }
        int w = Math.max(1, (int) width);
        if (descLayout == null || w != descLayoutWidth || !description.equals(descLayoutText)) {
            descPaint.setTextSize(sp(13));
            descLayout = StaticLayout.Builder.obtain(description, 0, description.length(), descPaint, w)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.1f)
                    .setIncludePad(false)
                    .build();
            descLayoutWidth = w;
            descLayoutText = description;
        }
        return descLayout.getHeight();
    }

    private void drawTriangle(Canvas canvas, float tipX, float tipY, float half) {
        trianglePath.reset();
        trianglePath.moveTo(tipX, tipY);
        trianglePath.lineTo(tipX - half, tipY - half * 1.6f);
        trianglePath.lineTo(tipX + half, tipY - half * 1.6f);
        trianglePath.close();
        canvas.drawPath(trianglePath, fill);
    }

    private static int accentFor(ArrayState.Mode mode) {
        switch (mode) {
            case FORWARD_SUM: return ACCENT_FORWARD;
            case REVERSE_SUM: return ACCENT_REVERSE;
            case MAX_SCAN: return ACCENT_MAX;
            case LINEAR_SEARCH: return ACCENT_SEARCH;
            default: return ACCENT_FORWARD;
        }
    }

    /** Blend a colour towards black by {@code amount} (0..1) to tint a fill. */
    private static int dim(int color, float amount) {
        float f = 1f - amount;
        int r = (int) (Color.red(color) * f);
        int g = (int) (Color.green(color) * f);
        int b = (int) (Color.blue(color) * f);
        return Color.argb(255, r, g, b);
    }
}
