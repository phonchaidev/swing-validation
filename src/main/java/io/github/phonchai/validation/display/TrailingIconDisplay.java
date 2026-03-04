package io.github.phonchai.validation.display;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Error display that shows an icon at the end of the text field (trailing
 * position)
 * using FlatLaf's client properties.
 *
 * <p>
 * Hovering over the icon displays the error message as a tooltip.
 * </p>
 *
 * @author phonchai
 * @since 1.0.0
 */
public class TrailingIconDisplay implements ErrorDisplay {

    private final Icon errorIcon;
    private final Map<JComponent, JLabel> activeIcons = new ConcurrentHashMap<>();

    public TrailingIconDisplay() {
        this(new ErrorIcon(16, new Color(0xD9, 0x53, 0x4F))); // Red X
    }

    public TrailingIconDisplay(Icon errorIcon) {
        this.errorIcon = errorIcon;
    }

    @Override
    public void showError(JComponent component, String message) {
        // Only supports components that support trailing components (JTextField, etc.)
        // FlatLaf supports this on JTextField, JFormattedTextField, JPasswordField,
        // JSpinner, JComboBox

        JLabel label = activeIcons.get(component);
        if (label == null) {
            label = new JLabel(errorIcon);
            activeIcons.put(component, label);
            component.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, label);
        }

        label.setToolTipText(message);

        // Also set outline for visibility
        getTargetComponent(component).putClientProperty(FlatClientProperties.OUTLINE, "error");
    }

    @Override
    public void hideError(JComponent component) {
        getTargetComponent(component).putClientProperty(FlatClientProperties.OUTLINE, null);

        if (activeIcons.containsKey(component)) {
            component.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, null);
            activeIcons.remove(component);
        }
    }

    @Override
    public void dispose() {
        for (JComponent comp : activeIcons.keySet()) {
            hideError(comp);
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
     * Simple vector icon drawing a cross (X).
     */
    private static class ErrorIcon implements Icon {
        private final int size;
        private final Color color;

        public ErrorIcon(int size, Color color) {
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);

            int padding = size / 4;
            int w = size - 2 * padding;
            int h = size - 2 * padding;

            // Center
            int cx = x + padding;
            int cy = y + padding;

            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(cx, cy, cx + w, cy + h);
            g2.drawLine(cx + w, cy, cx, cy + h);

            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
