package io.github.phonchai;

import com.formdev.flatlaf.FlatLightLaf;
import io.github.phonchai.validation.FormValidator;
import io.github.phonchai.validation.display.BalloonTooltip;
import io.github.phonchai.validation.display.BalloonTooltipDisplay;
import io.github.phonchai.validation.display.BottomBlockDisplay;
import io.github.phonchai.validation.display.CompositeErrorDisplay;
import io.github.phonchai.validation.display.InlineLabelDisplay;
import io.github.phonchai.validation.display.OutlineErrorDisplay;
import io.github.phonchai.validation.display.TrailingIconDisplay;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;

/**
 * Demo application showcasing the Swing Validation library.
 * Now features Internationalization (i18n) support.
 */
public class Main {

    public static void main(String[] args) {
        // Set a global font that supports Thai (Tahoma is safe on Windows)
        // MUST BE SET BEFORE FlatLightLaf.setup()
        Font globalFont = new Font("Tahoma", Font.PLAIN, 14);
        UIManager.put("defaultFont", globalFont);

        // Install FlatLaf
        FlatLightLaf.setup();

        UIManager.put("TextComponent.arc", 8);
        UIManager.put("ComboBox.arc", 8);
        UIManager.put("Button.arc", 8);
        UIManager.put("TabbedPane.showTabSeparators", true);

        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Swing Validation — Demo (i18n)");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(750, 700);
        frame.setLocationRelativeTo(null);

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Title
        JLabel title = new JLabel("Swing Validation Demo");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(title, BorderLayout.NORTH);

        // Tabs for languages
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(tabbedPane.getFont().deriveFont(Font.BOLD, 14f));

        // 1. English Tab
        tabbedPane.addTab("English Form", createForm(false));

        // 2. Thai Tab
        tabbedPane.addTab("แบบฟอร์มภาษาไทย", createForm(true));

        // Listener to switch global locale on tab change
        tabbedPane.addChangeListener(e -> {
            int index = tabbedPane.getSelectedIndex();
            if (index == 0) {
                // Switch to English
                FormValidator.setLocale(Locale.ENGLISH);
            } else {
                // Switch to Thai
                FormValidator.setLocale(new Locale("th", "TH"));
            }
        });

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        frame.setContentPane(mainPanel);
        frame.setVisible(true);
    }

    private static JScrollPane createForm(boolean isThai) {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // Initialize validator for this specific form instance
        FormValidator validator = new FormValidator();

        // ── Display Style Selector ──────────────────────────────
        JPanel stylePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        stylePanel.setBorder(
                BorderFactory.createTitledBorder(isThai ? "รูปแบบการแสดงผล (Error Style)" : "Error Display Style"));
        stylePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        String[] styles = { "Dark Balloon (Top)", "Danger Balloon (Bottom)", "Inline Label", "Outline Only",
                "Bottom Block (Style A)", "Right Balloon (Style B)", "Trailing Icon", "Icon + Tooltip (Composite)" };

        JComboBox<String> cbStyle = new JComboBox<>(styles);
        cbStyle.setPreferredSize(new Dimension(250, 32));
        stylePanel.add(new JLabel(isThai ? "เลือกรูปแบบ: " : "Style: "));
        stylePanel.add(cbStyle);
        form.add(stylePanel);
        form.add(Box.createVerticalStrut(10));

        // ── Form Fields ─────────────────────────────────────────
        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        String groupTitle = isThai ? "แบบฟอร์มลงทะเบียน" : "Registration Form";
        fieldsPanel.setBorder(BorderFactory.createTitledBorder(groupTitle));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Helper for labels
        String lblUser = isThai ? "ชื่อผู้ใช้ *" : "Username *";
        String lblEmail = isThai ? "อีเมล *" : "Email *";
        String lblPhone = isThai ? "เบอร์โทรศัพท์" : "Phone";
        String lblPass = isThai ? "รหัสผ่าน *" : "Password *";
        String lblConfirm = isThai ? "ยืนยันรหัสผ่าน *" : "Confirm Password *";
        String lblAge = isThai ? "อายุ" : "Age";
        String lblCountry = isThai ? "ประเทศ *" : "Country *";
        String lblAddress = isThai ? "ที่อยู่" : "Address";
        String lblEmployed = isThai ? "มีงานทำอยู่" : "Currently Employed";
        String lblCompanyStub = isThai ? "ชื่อบริษัท (ระบุถ้ามีงานทำ)" : "Company name (required if employed)";
        String lblRemove = isThai ? "ลบช่องนี้" : "Remove";

        // Row 0: Username
        JTextField txtUsername = createField(fieldsPanel, gbc, 0, lblUser);

        // Row 1: Email
        JTextField txtEmail = createField(fieldsPanel, gbc, 1, lblEmail);

        // Row 2: Phone
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.weightx = 0;
        fieldsPanel.add(new JLabel(lblPhone), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JPanel phonePanel = new JPanel(new BorderLayout(5, 0));
        JTextField txtPhone = new JTextField();
        txtPhone.setPreferredSize(new Dimension(300, 32));
        phonePanel.add(txtPhone, BorderLayout.CENTER);
        JButton btnRemovePhone = new JButton(lblRemove);
        btnRemovePhone.putClientProperty("JButton.buttonType", "roundRect");
        phonePanel.add(btnRemovePhone, BorderLayout.EAST);
        fieldsPanel.add(phonePanel, gbc);

        // Row 3: Password
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.weightx = 0;
        fieldsPanel.add(new JLabel(lblPass), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JPasswordField txtPassword = new JPasswordField();
        txtPassword.setPreferredSize(new Dimension(300, 32));
        fieldsPanel.add(txtPassword, gbc);

        // Row 4: Confirm Password
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.weightx = 0;
        fieldsPanel.add(new JLabel(lblConfirm), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JPasswordField txtConfirm = new JPasswordField();
        txtConfirm.setPreferredSize(new Dimension(300, 32));
        fieldsPanel.add(txtConfirm, gbc);

        // Row 5: Age
        JTextField txtAge = createField(fieldsPanel, gbc, 5, lblAge);

        // Row 6: Country
        gbc.gridy = 6;
        gbc.gridx = 0;
        gbc.weightx = 0;
        fieldsPanel.add(new JLabel(lblCountry), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JComboBox<String> cbCountry = new JComboBox<>();
        cbCountry.addItem(null);
        cbCountry.addItem(isThai ? "ไทย" : "Thailand");
        cbCountry.addItem(isThai ? "ญี่ปุ่น" : "Japan");
        cbCountry.addItem(isThai ? "สหรัฐอเมริกา" : "United States");
        cbCountry.addItem(isThai ? "เยอรมนี" : "Germany");
        cbCountry.setSelectedIndex(-1);
        cbCountry.setPreferredSize(new Dimension(300, 32));
        fieldsPanel.add(cbCountry, gbc);

        // Row 7: Address (JTextArea + JScrollPane)
        gbc.gridy = 7;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(12, 8, 8, 8);
        fieldsPanel.add(new JLabel(lblAddress), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 0.3;
        JTextArea txtAddress = new JTextArea(3, 20);
        txtAddress.setLineWrap(true);
        txtAddress.setWrapStyleWord(true);
        txtAddress.setMargin(new Insets(8, 8, 8, 8));
        txtAddress.setBorder(null); // Use ScrollPane border

        JScrollPane scrollAddress = new JScrollPane(txtAddress);
        scrollAddress.setPreferredSize(new Dimension(300, 80));
        fieldsPanel.add(scrollAddress, gbc);

        // Row 8: Agree checkbox + conditional field
        gbc.gridy = 8;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(8, 8, 8, 8);
        JCheckBox chkEmployed = new JCheckBox(lblEmployed);
        fieldsPanel.add(chkEmployed, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField txtCompany = new JTextField();
        txtCompany.setPreferredSize(new Dimension(300, 32));
        txtCompany.putClientProperty("JTextField.placeholderText", lblCompanyStub);
        fieldsPanel.add(txtCompany, gbc);

        fieldsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, fieldsPanel.getPreferredSize().height + 20));
        form.add(fieldsPanel);
        form.add(Box.createVerticalStrut(15));

        // ── Buttons ─────────────────────────────────────────────
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        btnPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JButton btnValidate = new JButton(isThai ? "ตรวจสอบข้อมูล (Validate)" : "Validate All");
        btnValidate.setPreferredSize(new Dimension(180, 36));
        btnValidate.setBackground(new Color(0x43, 0x85, 0xF4));
        btnValidate.setForeground(Color.WHITE);

        JButton btnClear = new JButton(isThai ? "ล้างสถานะ (Clear)" : "Clear Validation");
        btnClear.setPreferredSize(new Dimension(180, 36));

        JButton btnSubmit = new JButton(isThai ? "ส่งข้อมูล (Submit)" : "Submit");
        btnSubmit.setPreferredSize(new Dimension(150, 36));
        btnSubmit.setBackground(new Color(0x28, 0xA7, 0x45));
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setEnabled(false);

        btnPanel.add(btnValidate);
        btnPanel.add(btnClear);
        btnPanel.add(btnSubmit);
        form.add(btnPanel);
        form.add(Box.createVerticalStrut(15));

        // ── Status Label ────────────────────────────────────────
        JLabel lblStatus = new JLabel(isThai ? "สถานะ: รอการตรวจสอบ..." : "Status: Waiting for validation...");
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);
        lblStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        form.add(lblStatus);
        form.add(Box.createVerticalGlue());

        // ════════════════════════════════════════════════════════
        // SET UP VALIDATION (Parameterless = Auto i18n)
        // ════════════════════════════════════════════════════════

        // Username: required, min 3 chars
        validator.field(txtUsername).required().minLength(3);

        // Email: required, valid format
        validator.field(txtEmail).required().email();

        // Phone: optional, but must match pattern if provided
        // Note: For custom pattern, we might want custom message,
        // but for now let's use default or simple string.
        // If we want localized custom message, we'd use Localization.get("key") here
        // manually.
        // But for demo simplicity, we use the raw string or rely on pattern default.
        validator.field(txtPhone).pattern("^\\d{10}$", isThai ? "ต้องเป็นตัวเลข 10 หลัก" : "Must be 10 digits");

        // Enterprise Feature: Cross-field validation (auto-trigger)
        validator.field(txtPassword).required().minLength(6);
        validator.field(txtConfirm).required().matches(txtPassword); // Auto localized

        // Age: optional, number, range 1-150
        validator.field(txtAge).number().min(1).max(150);

        // Country: required
        validator.field(cbCountry).required();

        // Address: required + min length 10
        validator.field(txtAddress).required(isThai ? "กรุณากรอกที่อยู่" : "Address is required")
                .minLength(10, isThai ? "ที่อยู่ต้องมีความยาวอย่างน้อย 10 ตัวอักษร"
                        : "Address must be at least 10 characters");

        // Company: required only when employed
        validator.field(txtCompany).requiredWhen(chkEmployed::isSelected);

        // Trigger re-validation when checkbox changes
        chkEmployed.addActionListener(e -> {
            if (validator.isRealTimeEnabled()) {
                validator.validate();
            }
        });

        // Enterprise Feature: Dynamic Form (Remove Field)
        btnRemovePhone.addActionListener(e -> {
            fieldsPanel.remove(phonePanel);
            validator.removeField(txtPhone);
            fieldsPanel.revalidate();
            fieldsPanel.repaint();
            JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(btnRemovePhone),
                    isThai ? "ลบช่องเบอร์โทรและกฎการตรวจสอบแล้ว!"
                            : "Phone field removed and validation rules cleared!");
        });

        // Listen for validation state
        validator.onValidationChanged(allValid -> {
            btnSubmit.setEnabled(allValid);
            String successMsg = isThai ? "✅ ข้อมูลครบถ้วนถูกต้อง!" : "✅ All fields are valid!";
            String errorMsg = isThai ? "❌ กรุณาตรวจสอบข้อมูลด้านบน" : "❌ Please fix the errors above";

            lblStatus.setText(allValid ? successMsg : errorMsg);
            lblStatus.setForeground(allValid
                    ? new Color(0x28, 0xA7, 0x45)
                    : new Color(0xDC, 0x35, 0x45));
        });

        // ── Checkboxes ──────────────────────────────────────────
        JCheckBox chkCustomFont = new JCheckBox(
                isThai ? "ใช้ฟอนต์แบบ Custom (TH SarabunPSK)" : "Use Custom Error Font (TH SarabunPSK)");
        chkCustomFont.setToolTipText("UIManager.put(\"Validation.font\", ...)");
        chkCustomFont.addActionListener(e -> {
            if (chkCustomFont.isSelected()) {
                // Try to use a common Thai font. If not found, it falls back.
                // Size 18 because Sarabun is usually small.
                UIManager.put("Validation.font", new Font("TH SarabunPSK", Font.BOLD, 18));
            } else {
                UIManager.put("Validation.font", null);
            }
            refreshStyle(validator, cbStyle);
        });
        btnPanel.add(chkCustomFont);

        JCheckBox chkCustomColor = new JCheckBox(
                isThai ? "ใช้สีแบบ Custom (Global)" : "Use Custom Error Color (Global)");
        chkCustomColor.setToolTipText("Validation.background / .foreground / .errorColor");
        chkCustomColor.addActionListener(e -> {
            if (chkCustomColor.isSelected()) {
                // Set global colors: Pink BG, Black Text, Magenta Inline
                UIManager.put("Validation.background", Color.PINK);
                UIManager.put("Validation.foreground", Color.BLACK);
                UIManager.put("Validation.errorColor", Color.MAGENTA);
            } else {
                UIManager.put("Validation.background", null);
                UIManager.put("Validation.foreground", null);
                UIManager.put("Validation.errorColor", null);
            }
            refreshStyle(validator, cbStyle);
        });
        btnPanel.add(chkCustomColor);

        // ── Button Actions ──────────────────────────────────────
        btnValidate.addActionListener(e -> validator.validate());

        btnClear.addActionListener(e -> {
            validator.clearValidation();
            lblStatus.setText(isThai ? "สถานะ: ล้างการตรวจสอบแล้ว" : "Status: Validation cleared");
            lblStatus.setForeground(UIManager.getColor("Label.foreground"));
        });

        btnSubmit.addActionListener(e -> {
            if (validator.validate()) {
                JOptionPane.showMessageDialog(
                        SwingUtilities.getWindowAncestor(btnSubmit),
                        isThai ? "บันทึกข้อมูลเรียบร้อย!" : "Form submitted successfully!",
                        isThai ? "สำเร็จ" : "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // ── Style Switcher ──────────────────────────────────────
        cbStyle.addActionListener(e -> {
            validator.clearValidation();
            switch (cbStyle.getSelectedIndex()) {
                case 0 -> validator.setErrorDisplay(BalloonTooltipDisplay.dark());
                case 1 -> validator.setErrorDisplay(BalloonTooltipDisplay.danger());
                case 2 -> validator.setErrorDisplay(new InlineLabelDisplay());
                case 3 -> validator.setErrorDisplay(new OutlineErrorDisplay());
                case 4 -> validator.setErrorDisplay(new BottomBlockDisplay());
                case 5 -> validator.setErrorDisplay(BalloonTooltipDisplay.builder()
                        .bgColor(new Color(0xDC, 0x35, 0x45))
                        .textColor(Color.WHITE)
                        .preferredPosition(BalloonTooltip.ArrowPosition.LEFT)
                        .build());
                case 6 -> validator.setErrorDisplay(new TrailingIconDisplay());
                case 7 -> validator.setErrorDisplay(new CompositeErrorDisplay(
                        new TrailingIconDisplay(),
                        BalloonTooltipDisplay.danger()));
            }
            lblStatus.setText((isThai ? "เปลี่ยนรูปแบบเป็น: " : "Style changed to: ") + cbStyle.getSelectedItem());
            lblStatus.setForeground(UIManager.getColor("Label.foreground"));
        });

        JScrollPane scrollPane = new JScrollPane(form);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(20);
        return scrollPane;
    }

    private static JTextField createField(JPanel panel, GridBagConstraints gbc,
            int row, String label) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(300, 32));
        panel.add(field, gbc);
        return field;
    }

    private static void refreshStyle(FormValidator validator, JComboBox<String> cbStyle) {
        // Force refresh of the style to pick up new UIManager values
        int idx = cbStyle.getSelectedIndex();
        // Toggle selection to trigger listener (which creates new Display instance)
        cbStyle.setSelectedIndex(-1);
        cbStyle.setSelectedIndex(idx);

        if (validator.isRealTimeEnabled()) {
            validator.validate();
        }
    }
}