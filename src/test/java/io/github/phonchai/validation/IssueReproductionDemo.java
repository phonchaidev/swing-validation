package io.github.phonchai.validation;

import com.formdev.flatlaf.FlatLightLaf;
import io.github.phonchai.validation.display.BalloonTooltipDisplay;
import io.github.phonchai.validation.display.InlineLabelDisplay;

import javax.swing.*;
import java.awt.*;

public class IssueReproductionDemo {
    public static void main(String[] args) {
        FlatLightLaf.setup();

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("JTextArea Validation Issue Reproduction");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(10, 10, 10, 10);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;

            FormValidator validator = new FormValidator();
            // Use BalloonTooltipDisplay by default as it also sets the outline
            validator.setErrorDisplay(BalloonTooltipDisplay.dark());

            // 1. JTextField (Working fine)
            gbc.gridx = 0;
            gbc.gridy = 0;
            panel.add(new JLabel("JTextField:"), gbc);

            JTextField textField = new JTextField();
            gbc.gridx = 1;
            panel.add(textField, gbc);
            validator.field(textField).required("JTextField is required");

            // 2. JTextArea 1 in JScrollPane (The Issue)
            gbc.gridx = 0;
            gbc.gridy = 1;
            panel.add(new JLabel("JTextArea 1 (Scroll):"), gbc);

            JTextArea textArea1 = new JTextArea(3, 20);
            textArea1.setLineWrap(true);
            textArea1.setWrapStyleWord(true);
            textArea1.setMargin(new Insets(8, 8, 8, 8));
            textArea1.setBorder(BorderFactory.createEmptyBorder()); // Remove inner border to fix small focus
            JScrollPane scrollPane1 = new JScrollPane(textArea1);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weighty = 0.5;
            panel.add(scrollPane1, gbc);
            validator.field(textArea1).required("JTextArea 1 is required");

            // 3. JTextArea 2 in JScrollPane (Another instance)
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weighty = 0;
            panel.add(new JLabel("JTextArea 2 (Scroll):"), gbc);

            JTextArea textArea2 = new JTextArea(3, 20);
            textArea2.setLineWrap(true);
            textArea2.setWrapStyleWord(true);
            textArea2.setMargin(new Insets(8, 8, 8, 8));
            textArea2.setBorder(BorderFactory.createEmptyBorder());

            JScrollPane scrollPane2 = new JScrollPane(textArea2);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weighty = 0.5;
            panel.add(scrollPane2, gbc);
            validator.field(textArea2).required("JTextArea 2 is required").minLength(10, "Min 10 chars");

            // 4. JTextArea 3 with InlineLabelDisplay (Error below)
            gbc.gridx = 0;
            gbc.gridy = 3;
            panel.add(new JLabel("JTextArea 3 (Inline):"), gbc);

            JTextArea textArea3 = new JTextArea(3, 20);
            textArea3.setLineWrap(true);
            textArea3.setWrapStyleWord(true);
            textArea3.setMargin(new Insets(8, 8, 8, 8));
            textArea3.setBorder(BorderFactory.createEmptyBorder());

            JScrollPane scrollPane3 = new JScrollPane(textArea3);
            gbc.gridx = 1;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.weighty = 0.5;
            panel.add(scrollPane3, gbc);

            // ใช้ InlineLabelDisplay เพื่อแสดง Error ด้านล่าง (Scroll ลงด้านล่าง)
            validator.field(textArea3).required("JTextArea 3 is required")
                    .display(new InlineLabelDisplay());

            JButton validateButton = new JButton("Validate");
            validateButton.addActionListener(e -> {
                boolean valid = validator.validate();
                System.out.println("Form valid: " + valid);
            });

            frame.add(panel, BorderLayout.CENTER);
            frame.add(validateButton, BorderLayout.SOUTH);
            frame.setSize(500, 400);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
