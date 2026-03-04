package io.github.phonchai.validation.display;

import com.formdev.flatlaf.FlatClientProperties;

import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Error display implementation that shows inline error labels
 * below the invalid component — similar to Bootstrap's
 * {@code .invalid-feedback} style.
 *
 * <p>
 * The error label is inserted into the component's parent
 * container using the same layout manager. For best results,
 * use with layout managers that support dynamic addition
 * (e.g., MigLayout, BoxLayout). For absolute positioning
 * layouts, consider using {@link BalloonTooltipDisplay} instead.
 * </p>
 *
 * <p>
 * Error labels are styled with red text ({@code #DC3545})
 * and a small font size for an unobtrusive appearance.
 * </p>
 *
 * <h2>Usage</h2>
 * 
 * <pre>{@code
 * FormValidator validator = new FormValidator();
 * validator.setErrorDisplay(new InlineLabelDisplay());
 * }</pre>
 *
 * @author phonchai
 * @since 1.0.0
 */
public class InlineLabelDisplay implements ErrorDisplay {

    private final Color textColor;
    private final boolean outlineEnabled;
    private final float fontSize;

    private final Map<JComponent, JLabel> activeLabels = new ConcurrentHashMap<>();

    /**
     * Creates an inline label display with default styling.
     */
    public InlineLabelDisplay() {
        this(UIManager.getColor("Validation.errorColor") != null
                ? UIManager.getColor("Validation.errorColor")
                : new Color(0xDC, 0x35, 0x45),
                true, 12f);
    }

    /**
     * Creates an inline label display with custom styling.
     *
     * @param textColor      the color for error text
     * @param outlineEnabled whether to also set FlatLaf outline on the component
     * @param fontSize       font size for the error label
     */
    public InlineLabelDisplay(Color textColor, boolean outlineEnabled, float fontSize) {
        this.textColor = textColor;
        this.outlineEnabled = outlineEnabled;
        this.fontSize = fontSize;
    }

    @Override
    public void showError(JComponent component, String message) {
        if (outlineEnabled) {
            getTargetComponent(component).putClientProperty(FlatClientProperties.OUTLINE, "error");
        }

        // Check if label already exists — update text
        JLabel existing = activeLabels.get(component);
        if (existing != null) {
            existing.setText(message);
            return;
        }

        // Defer if not showing
        if (!component.isShowing()) {
            component.addHierarchyListener(e -> {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
                        && component.isShowing()
                        && !activeLabels.containsKey(component)) {
                    createLabel(component, message);
                }
            });
            return;
        }

        createLabel(component, message);
    }

    @Override
    public void hideError(JComponent component) {
        if (outlineEnabled) {
            getTargetComponent(component).putClientProperty(FlatClientProperties.OUTLINE, null);
        }

        JLabel label = activeLabels.remove(component);
        if (label != null) {
            Container parent = label.getParent();
            if (parent != null) {
                parent.remove(label);
                parent.revalidate();
                parent.repaint();
            }
        }
    }

    @Override
    public void dispose() {
        for (var entry : activeLabels.entrySet()) {
            if (outlineEnabled) {
                getTargetComponent(entry.getKey()).putClientProperty(FlatClientProperties.OUTLINE, null);
            }
            JLabel label = entry.getValue();
            Container parent = label.getParent();
            if (parent != null) {
                parent.remove(label);
                parent.revalidate();
                parent.repaint();
            }
        }
        activeLabels.clear();
    }

    /**
     * Creates and adds an error label below the target component.
     */
    private void createLabel(JComponent component, String message) {
        JComponent target = getTargetComponent(component);
        JLabel errorLabel = new JLabel(message);
        errorLabel.setForeground(textColor);

        Font baseFont = component.getFont();
        if (baseFont != null) {
            errorLabel.setFont(baseFont.deriveFont(fontSize));
        } else {
            errorLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (int) fontSize));
        }

        // Check for global validation font override
        Font globalFont = UIManager.getFont("Validation.font");
        if (globalFont != null) {
            errorLabel.setFont(globalFont.deriveFont(fontSize));
        }

        // Try to add the label as an overlay on JLayeredPane
        // This avoids disturbing the parent's layout
        JLayeredPane layeredPane = findLayeredPane(target);
        if (layeredPane != null) {
            addAsOverlay(target, errorLabel, layeredPane);
        } else {
            // Fallback: add to parent container
            addToParent(target, errorLabel);
        }

        activeLabels.put(component, errorLabel);
    }

    /**
     * Adds the error label as a JLayeredPane overlay below the component.
     */
    private void addAsOverlay(JComponent target, JLabel errorLabel, JLayeredPane layeredPane) {
        Dimension labelSize = errorLabel.getPreferredSize();
        errorLabel.setSize(labelSize);

        Point targetPos = SwingUtilities.convertPoint(target, 0, 0, layeredPane);
        int x = targetPos.x;
        int y = targetPos.y + target.getHeight() + 2;
        errorLabel.setLocation(x, y);

        layeredPane.add(errorLabel, JLayeredPane.POPUP_LAYER);
        layeredPane.moveToFront(errorLabel);

        // Track position changes
        target.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentMoved(java.awt.event.ComponentEvent e) {
                updateOverlayPosition(target, errorLabel, layeredPane);
            }

            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                updateOverlayPosition(target, errorLabel, layeredPane);
            }
        });
    }

    private void updateOverlayPosition(JComponent target, JLabel label, JLayeredPane layeredPane) {
        if (!target.isShowing()) {
            label.setVisible(false);
            return;
        }
        label.setVisible(true);
        Point pos = SwingUtilities.convertPoint(target, 0, 0, layeredPane);
        label.setLocation(pos.x, pos.y + target.getHeight() + 2);
    }

    /**
     * Fallback: adds the label directly to the parent container.
     */
    private void addToParent(JComponent component, JLabel errorLabel) {
        Container parent = component.getParent();
        if (parent == null)
            return;

        // Find the index of the component and insert after it
        int index = -1;
        for (int i = 0; i < parent.getComponentCount(); i++) {
            if (parent.getComponent(i) == component) {
                index = i + 1;
                break;
            }
        }

        if (index >= 0 && index <= parent.getComponentCount()) {
            parent.add(errorLabel, index);
        } else {
            parent.add(errorLabel);
        }
        parent.revalidate();
        parent.repaint();
    }

    private JComponent getTargetComponent(JComponent c) {
        if (c instanceof javax.swing.text.JTextComponent && c.getParent() instanceof JViewport
                && c.getParent().getParent() instanceof JScrollPane scrollPane) {
            return scrollPane;
        }
        return c;
    }

    private JLayeredPane findLayeredPane(JComponent component) {
        Window window = SwingUtilities.getWindowAncestor(component);
        if (window instanceof JFrame frame)
            return frame.getLayeredPane();
        if (window instanceof JDialog dialog)
            return dialog.getLayeredPane();
        JRootPane rootPane = SwingUtilities.getRootPane(component);
        return rootPane != null ? rootPane.getLayeredPane() : null;
    }
}
