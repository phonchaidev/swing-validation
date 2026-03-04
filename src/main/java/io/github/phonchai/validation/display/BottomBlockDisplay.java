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
 * Error display that shows a solid colored block attached to the bottom
 * of the component, matching the component's width.
 *
 * <p>
 * This style works best for forms with vertical spacing between fields,
 * as the block overlays whatever is immediately below the field.
 * </p>
 *
 * @author phonchai
 * @since 1.0.0
 */
public class BottomBlockDisplay implements ErrorDisplay {

    private final Color bgColor;
    private final Color textColor;
    private final Font font;
    private final Map<JComponent, LabelState> activeLabels = new ConcurrentHashMap<>();

    private record LabelState(JLabel label, JComponent target, ComponentAdapter listener) {
    }

    public BottomBlockDisplay() {
        this(new Color(0xD9, 0x53, 0x4F), Color.WHITE); // Bootstrap danger red
    }

    public BottomBlockDisplay(Color bgColor, Color textColor) {
        this.bgColor = bgColor;
        this.textColor = textColor;
        this.font = UIManager.getFont("Label.font").deriveFont(12f);
    }

    @Override
    public void showError(JComponent component, String message) {
        // Outline
        getTargetComponent(component).putClientProperty(FlatClientProperties.OUTLINE, "error");

        LabelState state = activeLabels.get(component);
        if (state != null) {
            state.label.setText(message);
            updatePosition(state.label, component);
            return;
        }

        if (!component.isShowing()) {
            // Wait until showing to get valid geometry
            component.addHierarchyListener(new java.awt.event.HierarchyListener() {
                @Override
                public void hierarchyChanged(HierarchyEvent e) {
                    if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
                            && component.isShowing()) {
                        component.removeHierarchyListener(this);
                        if (!activeLabels.containsKey(component)) {
                            createAndShowLabel(component, message);
                        }
                    }
                }
            });
            return;
        }

        createAndShowLabel(component, message);
    }

    @Override
    public void hideError(JComponent component) {
        getTargetComponent(component).putClientProperty(FlatClientProperties.OUTLINE, null);
        LabelState state = activeLabels.remove(component);
        if (state != null) {
            removeLabel(state);
        }
    }

    @Override
    public void dispose() {
        for (JComponent comp : activeLabels.keySet()) {
            hideError(comp);
        }
    }

    private void createAndShowLabel(JComponent component, String message) {
        JComponent target = getTargetComponent(component);
        JLabel label = new JLabel(message);
        label.setOpaque(true);
        label.setBackground(bgColor);
        label.setForeground(textColor);
        label.setFont(font);
        label.setHorizontalAlignment(SwingConstants.LEFT);

        // Padding and border
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, bgColor.darker()),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));

        JLayeredPane layeredPane = findLayeredPane(target);
        if (layeredPane == null)
            return;

        ComponentAdapter listener = new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                updatePosition(label, target);
            }

            @Override
            public void componentResized(ComponentEvent e) {
                updatePosition(label, target);
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                label.setVisible(false);
            }

            @Override
            public void componentShown(ComponentEvent e) {
                label.setVisible(true);
                updatePosition(label, target);
            }
        };
        target.addComponentListener(listener);

        // Track hierarchy changes (e.g. scrolling)
        target.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                label.setVisible(target.isShowing());
                if (target.isShowing()) {
                    updatePosition(label, target);
                }
            }
        });

        activeLabels.put(component, new LabelState(label, target, listener));

        layeredPane.add(label, JLayeredPane.POPUP_LAYER);
        updatePosition(label, target);
    }

    private void updatePosition(JLabel label, JComponent target) {
        if (!target.isShowing()) {
            label.setVisible(false);
            return;
        }

        JLayeredPane layeredPane = findLayeredPane(target);
        if (layeredPane == null)
            return;

        try {
            Point pos = SwingUtilities.convertPoint(target, 0, target.getHeight(), layeredPane);

            // Adjust width to match target, height preferred
            Dimension size = label.getPreferredSize();
            label.setBounds(pos.x, pos.y, target.getWidth(), size.height);
            label.setVisible(true);
            label.revalidate();
            label.repaint();
        } catch (Exception e) {
            // Ignore geometry errors
        }
    }

    private void removeLabel(LabelState state) {
        state.target.removeComponentListener(state.listener);
        Container parent = state.label.getParent();
        if (parent != null) {
            parent.remove(state.label);
            parent.repaint(state.label.getX(), state.label.getY(),
                    state.label.getWidth(), state.label.getHeight());
        }
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
        if (window instanceof JFrame f)
            return f.getLayeredPane();
        if (window instanceof JDialog d)
            return d.getLayeredPane();

        JRootPane root = SwingUtilities.getRootPane(component);
        return root != null ? root.getLayeredPane() : null;
    }
}
