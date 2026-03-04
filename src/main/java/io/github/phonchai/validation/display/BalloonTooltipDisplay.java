package io.github.phonchai.validation.display;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Error display implementation that shows web-style balloon tooltips
 * with triangular arrows pointing at the invalid component.
 *
 * <p>
 * Balloons are rendered as overlay components on the window's
 * {@link JLayeredPane}, making them <em>layout-agnostic</em> — they
 * work with any layout manager (MigLayout, GridBagLayout, BoxLayout, etc.).
 * </p>
 *
 * <h2>Preset Themes</h2>
 * <ul>
 * <li>{@link #dark()} — dark charcoal background, white text (like Bootstrap
 * dark tooltip)</li>
 * <li>{@link #danger()} — dark red background, white text (like Bootstrap
 * danger alert)</li>
 * <li>{@link #warning()} — amber/yellow background, dark text</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * 
 * <pre>{@code
 * FormValidator validator = new FormValidator();
 * validator.setErrorDisplay(BalloonTooltipDisplay.dark());
 * }</pre>
 *
 * @author phonchai
 * @since 1.0.0
 */
public class BalloonTooltipDisplay implements ErrorDisplay {

    // ── Theme configuration ─────────────────────────────────────

    private final Color bgColor;
    private final Color textColor;
    private final Color borderColor;
    private final int arc;
    private final int arrowSize;
    private final BalloonTooltip.ArrowPosition preferredPosition;
    private final boolean shadowEnabled;
    private final boolean fadeEnabled;
    private final int fadeMs;
    private final boolean outlineEnabled;
    private final Font textFont;

    // ── State tracking ──────────────────────────────────────────

    private final Map<JComponent, TooltipState> activeTooltips = new ConcurrentHashMap<>();

    /**
     * Internal state for each active tooltip.
     */
    private record TooltipState(
            BalloonTooltip tooltip,
            JComponent target,
            ComponentAdapter componentListener,
            JViewport viewport,
            javax.swing.event.ChangeListener viewportListener,
            Timer fadeTimer) {
    }

    // ── Constructors ────────────────────────────────────────────

    private BalloonTooltipDisplay(Builder builder) {
        this.bgColor = builder.bgColor;
        this.textColor = builder.textColor;
        this.borderColor = builder.borderColor;
        this.arc = builder.arc;
        this.arrowSize = builder.arrowSize;
        this.preferredPosition = builder.preferredPosition;
        this.shadowEnabled = builder.shadowEnabled;
        this.fadeEnabled = builder.fadeEnabled;
        this.fadeMs = builder.fadeMs;
        this.outlineEnabled = builder.outlineEnabled;
        this.textFont = builder.textFont;
    }

    // ── Preset factories ────────────────────────────────────────

    /**
     * Creates a dark-themed balloon tooltip display.
     * <p>
     * Dark charcoal background ({@code #333333}), white text, arrow on top.
     * </p>
     */
    public static BalloonTooltipDisplay dark() {
        return new Builder()
                .bgColor(new Color(0x33, 0x33, 0x33))
                .textColor(Color.WHITE)
                .borderColor(null)
                .arc(8)
                .arrowSize(8)
                .preferredPosition(BalloonTooltip.ArrowPosition.TOP)
                .shadow(true)
                .fade(true, 200)
                .outline(true)
                .build();
    }

    /**
     * Creates a danger-themed balloon tooltip display.
     * <p>
     * Dark red background ({@code #8B1A1A}), white text, arrow on bottom (tooltip
     * below field).
     * </p>
     */
    public static BalloonTooltipDisplay danger() {
        return new Builder()
                .bgColor(new Color(0x8B, 0x1A, 0x1A))
                .textColor(Color.WHITE)
                .borderColor(null)
                .arc(8)
                .arrowSize(8)
                .preferredPosition(BalloonTooltip.ArrowPosition.BOTTOM)
                .shadow(true)
                .fade(true, 200)
                .outline(true)
                .build();
    }

    /**
     * Creates a warning-themed balloon tooltip display.
     * <p>
     * Amber background, dark text, arrow on top.
     * </p>
     */
    public static BalloonTooltipDisplay warning() {
        return new Builder()
                .bgColor(new Color(0xFF, 0xC1, 0x07))
                .textColor(new Color(0x33, 0x33, 0x33))
                .borderColor(new Color(0xE0, 0xA8, 0x00))
                .arc(8)
                .arrowSize(8)
                .preferredPosition(BalloonTooltip.ArrowPosition.TOP)
                .shadow(true)
                .fade(true, 200)
                .outline(true)
                .build();
    }

    /**
     * Creates a new builder for custom theme configuration.
     */
    public static Builder builder() {
        return new Builder();
    }

    // ── ErrorDisplay implementation ─────────────────────────────

    @Override
    public void showError(JComponent component, String message) {
        // Set FlatLaf outline
        if (outlineEnabled) {
            getTargetComponent(component).putClientProperty(FlatClientProperties.OUTLINE, "error");
        }

        // Check if tooltip already exists — update message
        TooltipState existing = activeTooltips.get(component);
        if (existing != null) {
            existing.tooltip.setMessage(message);
            updateTooltipPosition(existing.tooltip, component);
            return;
        }

        // Defer creation until the component is showing
        if (!component.isShowing()) {
            component.addHierarchyListener(e -> {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
                        && component.isShowing()) {
                    // Re-check — might have been cleared already
                    if (!activeTooltips.containsKey(component)) {
                        createAndShowTooltip(component, message);
                    }
                }
            });
            // Store placeholder so we know error is pending
            return;
        }

        createAndShowTooltip(component, message);
    }

    @Override
    public void hideError(JComponent component) {
        // Clear FlatLaf outline
        if (outlineEnabled) {
            getTargetComponent(component).putClientProperty(FlatClientProperties.OUTLINE, null);
        }

        TooltipState state = activeTooltips.remove(component);
        if (state != null) {
            removeTooltipFromOverlay(state);
        }
    }

    @Override
    public void dispose() {
        for (var entry : activeTooltips.entrySet()) {
            JComponent comp = entry.getKey();
            TooltipState state = entry.getValue();
            if (outlineEnabled) {
                getTargetComponent(comp).putClientProperty(FlatClientProperties.OUTLINE, null);
            }
            removeTooltipFromOverlay(state);
        }
        activeTooltips.clear();
    }

    // ── Internal methods ────────────────────────────────────────

    private void createAndShowTooltip(JComponent component, String message) {
        JComponent target = getTargetComponent(component);

        // Determine arrow position (auto-flip if needed)
        BalloonTooltip.ArrowPosition actualPosition = calculatePosition(target);

        // Create balloon
        BalloonTooltip tooltip = new BalloonTooltip(
                message, bgColor, textColor, borderColor,
                arc, arrowSize, actualPosition, shadowEnabled, textFont);

        // Find the layered pane
        JLayeredPane layeredPane = findLayeredPane(target);
        if (layeredPane == null) {
            return; // No window context — can't display
        }

        // Calculate position and add to overlay
        Dimension tooltipSize = tooltip.getPreferredSize();
        tooltip.setSize(tooltipSize);
        Point pos = calculateTooltipPosition(target, tooltipSize, actualPosition, layeredPane);
        tooltip.setLocation(pos);

        // Add to highest layer
        layeredPane.add(tooltip, JLayeredPane.POPUP_LAYER);
        layeredPane.moveToFront(tooltip);

        // Install position-tracking listener on the target
        ComponentAdapter listener = new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                updateTooltipPosition(tooltip, target);
            }

            @Override
            public void componentResized(ComponentEvent e) {
                updateTooltipPosition(tooltip, target);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                tooltip.setVisible(false);
            }

            @Override
            public void componentShown(ComponentEvent e) {
                tooltip.setVisible(true);
                updateTooltipPosition(tooltip, target);
            }
        };
        target.addComponentListener(listener);

        // Also listen for target hierarchy changes (e.g., tab switches, scroll)
        target.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (target.isShowing()) {
                    tooltip.setVisible(true);
                    updateTooltipPosition(tooltip, target);
                } else {
                    tooltip.setVisible(false);
                }
            }
        });

        // Setup viewport listener — listen to the ACTUAL viewport of the internal
        // component
        JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, component);
        javax.swing.event.ChangeListener viewportListener = null;
        if (viewport != null) {
            viewportListener = e -> updateTooltipPosition(tooltip, target);
            viewport.addChangeListener(viewportListener);
        }

        // Store state using the ORIGINAL component as key
        activeTooltips.put(component, new TooltipState(tooltip, target, listener, viewport, viewportListener, null));

        // Fade in
        if (fadeEnabled) {
            tooltip.setAlpha(0f);
            fadeIn(tooltip);
        }
    }

    /**
     * Calculates the optimal arrow position, auto-flipping if the
     * preferred position would cause the tooltip to go off-screen.
     *
     * <p>
     * ArrowPosition semantics:
     * </p>
     * <ul>
     * <li>{@code TOP} — arrow at top of balloon → balloon is BELOW the field</li>
     * <li>{@code BOTTOM} — arrow at bottom of balloon → balloon is ABOVE the
     * field</li>
     * </ul>
     */
    private BalloonTooltip.ArrowPosition calculatePosition(JComponent target) {
        if (!target.isShowing()) {
            return preferredPosition;
        }

        try {
            Point screenPos = target.getLocationOnScreen();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            FontMetrics fm = target.getFontMetrics(
                    textFont != null ? textFont : target.getFont());
            int estimatedHeight = fm.getHeight() + 12 + arrowSize + (shadowEnabled ? 8 : 0);

            return switch (preferredPosition) {
                // TOP = arrow at top → balloon below field
                // Flip if balloon would go past bottom of screen
                case TOP -> screenPos.y + target.getHeight() + estimatedHeight > screenSize.height
                        ? BalloonTooltip.ArrowPosition.BOTTOM
                        : BalloonTooltip.ArrowPosition.TOP;
                // BOTTOM = arrow at bottom → balloon above field
                // Flip if balloon would go past top of screen
                case BOTTOM -> screenPos.y - estimatedHeight < 0
                        ? BalloonTooltip.ArrowPosition.TOP
                        : BalloonTooltip.ArrowPosition.BOTTOM;
                default -> preferredPosition;
            };
        } catch (IllegalComponentStateException e) {
            return preferredPosition;
        }
    }

    /**
     * Calculates the tooltip position in the layered pane coordinate system.
     *
     * <p>
     * ArrowPosition determines where the arrow sits on the balloon,
     * which in turn determines where the balloon is placed relative to
     * the target component:
     * </p>
     * <ul>
     * <li>{@code TOP} — arrow at top → balloon BELOW target</li>
     * <li>{@code BOTTOM} — arrow at bottom → balloon ABOVE target</li>
     * <li>{@code LEFT} — arrow at left → balloon RIGHT of target</li>
     * <li>{@code RIGHT} — arrow at right → balloon LEFT of target</li>
     * </ul>
     */
    private Point calculateTooltipPosition(JComponent target, Dimension tooltipSize,
            BalloonTooltip.ArrowPosition position,
            JLayeredPane layeredPane) {
        Point targetPos;
        try {
            targetPos = SwingUtilities.convertPoint(target, 0, 0, layeredPane);
        } catch (Exception e) {
            return new Point(0, 0);
        }

        int targetW = target.getWidth();
        int targetH = target.getHeight();
        int tipW = tooltipSize.width;
        int tipH = tooltipSize.height;

        int x, y;

        switch (position) {
            case TOP -> {
                // Arrow at top of balloon → balloon placed BELOW target
                x = targetPos.x + (targetW - tipW) / 2;
                y = targetPos.y + targetH - 2;
            }
            case BOTTOM -> {
                // Arrow at bottom of balloon → balloon placed ABOVE target
                x = targetPos.x + (targetW - tipW) / 2;
                y = targetPos.y - tipH + 2;
            }
            case LEFT -> {
                // Arrow at left of balloon → balloon placed RIGHT of target
                x = targetPos.x + targetW - 2;
                y = targetPos.y + (targetH - tipH) / 2;
            }
            case RIGHT -> {
                // Arrow at right of balloon → balloon placed LEFT of target
                x = targetPos.x - tipW + 2;
                y = targetPos.y + (targetH - tipH) / 2;
            }
            default -> {
                x = targetPos.x;
                y = targetPos.y + targetH;
            }
        }

        // Clamp to layered pane bounds
        int lpW = layeredPane.getWidth();
        int lpH = layeredPane.getHeight();
        x = Math.max(2, Math.min(x, lpW - tipW - 2));
        y = Math.max(2, Math.min(y, lpH - tipH - 2));

        return new Point(x, y);
    }

    /**
     * Updates the position of an existing tooltip when the target moves.
     */
    private void updateTooltipPosition(BalloonTooltip tooltip, JComponent target) {
        if (!target.isShowing()) {
            tooltip.setVisible(false);
            return;
        }

        // Check if inside viewport and completely scrolled out
        JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, target);
        if (viewport != null) {
            // Use viewport's own bounds (component coordinates) — NOT getViewRect()
            // which returns view-content coordinates that don't match convertRectangle
            // output
            Rectangle viewRect = new Rectangle(0, 0, viewport.getWidth(), viewport.getHeight());
            Rectangle targetRect = SwingUtilities.convertRectangle(target,
                    new Rectangle(0, 0, target.getWidth(), target.getHeight()), viewport);
            if (!viewRect.intersects(targetRect)) {
                tooltip.setVisible(false);
                return;
            }
        }

        JLayeredPane layeredPane = findLayeredPane(target);
        if (layeredPane == null)
            return;

        tooltip.setVisible(true);
        BalloonTooltip.ArrowPosition pos = calculatePosition(target);
        if (pos != tooltip.getArrowPosition()) {
            tooltip.setArrowPosition(pos);
        }
        Dimension size = tooltip.getPreferredSize();
        tooltip.setSize(size);
        Point location = calculateTooltipPosition(target, size, pos, layeredPane);
        tooltip.setLocation(location);
        tooltip.repaint();
    }

    /**
     * Finds the JLayeredPane for overlay rendering.
     */
    private JLayeredPane findLayeredPane(JComponent component) {
        Window window = SwingUtilities.getWindowAncestor(component);
        if (window instanceof JFrame frame) {
            return frame.getLayeredPane();
        } else if (window instanceof JDialog dialog) {
            return dialog.getLayeredPane();
        }
        // Fallback: try to find through root pane
        JRootPane rootPane = SwingUtilities.getRootPane(component);
        return rootPane != null ? rootPane.getLayeredPane() : null;
    }

    /**
     * Removes a tooltip from overlay and cleans up listeners.
     */
    private void removeTooltipFromOverlay(TooltipState state) {
        if (state.fadeTimer != null) {
            state.fadeTimer.stop();
        }

        // Remove component listener
        if (state.componentListener != null) {
            state.target.removeComponentListener(state.componentListener);
        }

        // Remove viewport listener
        if (state.viewport != null && state.viewportListener != null) {
            state.viewport.removeChangeListener(state.viewportListener);
        }

        // Remove from layered pane
        Container parent = state.tooltip.getParent();
        if (parent != null) {
            parent.remove(state.tooltip);
            parent.repaint(
                    state.tooltip.getX(), state.tooltip.getY(),
                    state.tooltip.getWidth(), state.tooltip.getHeight());
        }
    }

    private JComponent getTargetComponent(JComponent c) {
        if (c instanceof javax.swing.text.JTextComponent && c.getParent() instanceof JViewport
                && c.getParent().getParent() instanceof JScrollPane scrollPane) {
            return scrollPane;
        }
        return c;
    }

    /**
     * Animates the tooltip fading in.
     */
    private void fadeIn(BalloonTooltip tooltip) {
        final int steps = 10;
        final int stepDelay = fadeMs / steps;
        final float[] currentAlpha = { 0f };

        Timer timer = new Timer(stepDelay, null);
        timer.addActionListener(e -> {
            currentAlpha[0] += 1.0f / steps;
            if (currentAlpha[0] >= 1.0f) {
                currentAlpha[0] = 1.0f;
                timer.stop();
            }
            tooltip.setAlpha(currentAlpha[0]);
        });
        timer.start();
    }

    // ── Builder ─────────────────────────────────────────────────

    /**
     * Builder for creating custom-themed {@code BalloonTooltipDisplay} instances.
     */
    public static class Builder {
        private Color bgColor = UIManager.getColor("Validation.background") != null
                ? UIManager.getColor("Validation.background")
                : new Color(0x33, 0x33, 0x33);
        private Color textColor = UIManager.getColor("Validation.foreground") != null
                ? UIManager.getColor("Validation.foreground")
                : Color.WHITE;
        private Color borderColor = UIManager.getColor("Validation.borderColor");
        private int arc = 8;
        private int arrowSize = 8;
        private BalloonTooltip.ArrowPosition preferredPosition = BalloonTooltip.ArrowPosition.TOP;
        private boolean shadowEnabled = true;
        private boolean fadeEnabled = true;
        private int fadeMs = 200;
        private boolean outlineEnabled = true;
        private Font textFont = null;

        public Builder bgColor(Color bgColor) {
            this.bgColor = bgColor;
            return this;
        }

        public Builder textColor(Color textColor) {
            this.textColor = textColor;
            return this;
        }

        public Builder borderColor(Color borderColor) {
            this.borderColor = borderColor;
            return this;
        }

        public Builder arc(int arc) {
            this.arc = arc;
            return this;
        }

        public Builder arrowSize(int arrowSize) {
            this.arrowSize = arrowSize;
            return this;
        }

        public Builder preferredPosition(BalloonTooltip.ArrowPosition position) {
            this.preferredPosition = position;
            return this;
        }

        public Builder shadow(boolean enabled) {
            this.shadowEnabled = enabled;
            return this;
        }

        public Builder fade(boolean enabled, int durationMs) {
            this.fadeEnabled = enabled;
            this.fadeMs = durationMs;
            return this;
        }

        public Builder outline(boolean enabled) {
            this.outlineEnabled = enabled;
            return this;
        }

        public Builder textFont(Font font) {
            this.textFont = font;
            return this;
        }

        public BalloonTooltipDisplay build() {
            return new BalloonTooltipDisplay(this);
        }
    }
}
