package io.github.phonchai.validation;

import com.formdev.flatlaf.FlatClientProperties;
import io.github.phonchai.validation.display.BalloonTooltipDisplay;
import io.github.phonchai.validation.display.OutlineErrorDisplay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;

import static org.junit.jupiter.api.Assertions.*;

class JScrollPaneValidationTest {

        private JTextArea textArea;
        private JScrollPane scrollPane;

        @BeforeEach
        void setUp() {
                textArea = new JTextArea();
                scrollPane = new JScrollPane(textArea);
                // Ensure parentage is complete
                assertNotNull(textArea.getParent()); // Viewport
                assertNotNull(textArea.getParent().getParent()); // ScrollPane
        }

        @Test
        void testOutlineErrorDisplayTargetsScrollPane() {
                OutlineErrorDisplay display = new OutlineErrorDisplay();

                display.showError(textArea, "Error");
                assertEquals("error", scrollPane.getClientProperty(FlatClientProperties.OUTLINE),
                                "Outline should be set on ScrollPane, not JTextArea");
                assertNull(textArea.getClientProperty(FlatClientProperties.OUTLINE),
                                "Outline should NOT be set on JTextArea");

                display.hideError(textArea);
                assertNull(scrollPane.getClientProperty(FlatClientProperties.OUTLINE),
                                "Outline should be cleared from ScrollPane");
        }

        @Test
        void testBalloonTooltipDisplayTargetsScrollPaneForOutline() {
                // BalloonTooltipDisplay also sets the outline if enabled
                BalloonTooltipDisplay display = BalloonTooltipDisplay.dark();

                display.showError(textArea, "Error");
                assertEquals("error", scrollPane.getClientProperty(FlatClientProperties.OUTLINE),
                                "Outline should be set on ScrollPane by BalloonTooltipDisplay");
                assertNull(textArea.getClientProperty(FlatClientProperties.OUTLINE),
                                "Outline should NOT be set on JTextArea by BalloonTooltipDisplay");

                display.hideError(textArea);
                assertNull(scrollPane.getClientProperty(FlatClientProperties.OUTLINE),
                                "Outline should be cleared from ScrollPane by BalloonTooltipDisplay");
        }

        @Test
        void testStandardTextFieldDoesNotTargetParent() {
                JTextField textField = new JTextField();
                JPanel parent = new JPanel(new BorderLayout());
                parent.add(textField);

                OutlineErrorDisplay display = new OutlineErrorDisplay();
                display.showError(textField, "Error");

                assertEquals("error", textField.getClientProperty(FlatClientProperties.OUTLINE),
                                "Outline should be set on JTextField itself");
                assertNull(parent.getClientProperty(FlatClientProperties.OUTLINE),
                                "Outline should NOT be set on parent JPanel");
        }
}
