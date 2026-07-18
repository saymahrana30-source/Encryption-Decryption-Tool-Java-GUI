package gui;

import crypto.CaesarCipher;
import crypto.CryptoUtils;
import exception.CryptoOperationException;
import service.FileEncryptionService;
import utility.InputValidator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.Arrays;

/**
 * MainWindow.java
 * ===============
 * The Swing GUI - the only class in the project that touches AWT/Swing
 * components. It NEVER does crypto math itself; every Encrypt/Decrypt
 * button click just calls into CryptoUtils or FileEncryptionService and
 * displays the result (or a friendly error message).
 *
 * Two tabs:
 *   - "Text"  : encrypt/decrypt pasted text using AES-GCM (or, if Learning
 *               Mode is checked, the intentionally-insecure Caesar cipher,
 *               purely for teaching purposes).
 *   - "File"  : encrypt/decrypt whole files using AES-GCM via JFileChooser.
 *
 * SWING THREADING NOTE: all the work here (PBKDF2 + AES-GCM on short text,
 * or on typically-small student test files) completes fast enough to run
 * directly on the Event Dispatch Thread without freezing the UI. For very
 * large files you'd want to move the crypto call to a background
 * SwingWorker - noted as a future improvement in the README.
 */
public class MainWindow extends JFrame {

    private final FileEncryptionService fileService = new FileEncryptionService();

    // --- Text tab components ---
    private JTextArea textInputArea;
    private JTextArea textOutputArea;
    private JPasswordField textPasswordField;
    private JCheckBox showPasswordCheck;
    private JCheckBox learningModeCheck;
    private JLabel passwordStrengthLabel;
    private JLabel textStatusLabel;

    // --- File tab components ---
    private JTextField selectedFilePathField;
    private JPasswordField filePasswordField;
    private File selectedFile;
    private JLabel fileStatusLabel;

    public MainWindow() {
        super("AES Encryption / Decryption Tool");
        buildUi();
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 560);
        setMinimumSize(new Dimension(600, 480));
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Text Encryption", buildTextTab());
        tabs.addTab("File Encryption", buildFileTab());
        add(tabs);
    }

    // =================================================================
    // TEXT TAB
    // =================================================================

    private JPanel buildTextTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel textAreas = new JPanel(new GridLayout(2, 1, 6, 6));
        textInputArea = new JTextArea(6, 40);
        textInputArea.setLineWrap(true);
        textInputArea.setWrapStyleWord(true);
        textOutputArea = new JTextArea(6, 40);
        textOutputArea.setLineWrap(true);
        textOutputArea.setWrapStyleWord(true);
        textOutputArea.setEditable(false);

        textAreas.add(labeledScroll("Input (plaintext or ciphertext):", textInputArea));
        textAreas.add(labeledScroll("Output:", textOutputArea));
        panel.add(textAreas, BorderLayout.CENTER);

        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));

        JPanel passwordRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        passwordRow.add(new JLabel("Password:"));
        textPasswordField = new JPasswordField(20);
        passwordRow.add(textPasswordField);

        showPasswordCheck = new JCheckBox("Show");
        showPasswordCheck.addActionListener(e ->
                textPasswordField.setEchoChar(showPasswordCheck.isSelected() ? (char) 0 : '\u2022'));
        passwordRow.add(showPasswordCheck);

        passwordStrengthLabel = new JLabel("Strength: -");
        passwordRow.add(passwordStrengthLabel);

        textPasswordField.addCaretListener(e -> updatePasswordStrengthLabel());

        controls.add(passwordRow);

        JPanel learningRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        learningModeCheck = new JCheckBox("Learning Mode (Caesar Cipher - NOT secure, for teaching only)");
        learningRow.add(learningModeCheck);
        controls.add(learningRow);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton encryptBtn = new JButton("Encrypt");
        JButton decryptBtn = new JButton("Decrypt");
        JButton copyBtn = new JButton("Copy Output");
        JButton clearBtn = new JButton("Clear");

        encryptBtn.addActionListener(e -> onTextEncrypt());
        decryptBtn.addActionListener(e -> onTextDecrypt());
        copyBtn.addActionListener(e -> onCopyOutput());
        clearBtn.addActionListener(e -> onClearText());

        buttonRow.add(encryptBtn);
        buttonRow.add(decryptBtn);
        buttonRow.add(copyBtn);
        buttonRow.add(clearBtn);
        controls.add(buttonRow);

        textStatusLabel = new JLabel(" ");
        textStatusLabel.setBorder(new EmptyBorder(4, 4, 4, 4));
        controls.add(textStatusLabel);

        panel.add(controls, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel labeledScroll(String label, JTextArea area) {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.add(new JLabel(label), BorderLayout.NORTH);
        p.add(new JScrollPane(area), BorderLayout.CENTER);
        return p;
    }

    private void updatePasswordStrengthLabel() {
        char[] pwd = textPasswordField.getPassword();
        passwordStrengthLabel.setText("Strength: " + InputValidator.estimatePasswordStrength(pwd));
        Arrays.fill(pwd, '\0');
    }

    private void onTextEncrypt() {
        char[] password = textPasswordField.getPassword();
        try {
            InputValidator.validateText(textInputArea.getText(), "Input text");

            if (learningModeCheck.isSelected()) {
                String result = CaesarCipher.encrypt(textInputArea.getText(), caesarShiftFromPassword(password));
                textOutputArea.setText(result);
                setTextStatus("Encrypted with Caesar Cipher (learning mode - NOT secure).", false);
            } else {
                InputValidator.validatePassword(password);
                String result = CryptoUtils.encryptText(textInputArea.getText(), password);
                textOutputArea.setText(result);
                setTextStatus("Encrypted successfully with AES-256-GCM.", false);
            }
        } catch (CryptoOperationException ex) {
            setTextStatus(ex.getMessage(), true);
        } finally {
            Arrays.fill(password, '\0'); // wipe the password from memory as soon as we're done with it
        }
    }

    private void onTextDecrypt() {
        char[] password = textPasswordField.getPassword();
        try {
            InputValidator.validateText(textInputArea.getText(), "Input text");

            if (learningModeCheck.isSelected()) {
                String result = CaesarCipher.decrypt(textInputArea.getText(), caesarShiftFromPassword(password));
                textOutputArea.setText(result);
                setTextStatus("Decrypted with Caesar Cipher (learning mode - NOT secure).", false);
            } else {
                InputValidator.validatePassword(password);
                String result = CryptoUtils.decryptText(textInputArea.getText(), password);
                textOutputArea.setText(result);
                setTextStatus("Decrypted successfully.", false);
            }
        } catch (CryptoOperationException ex) {
            setTextStatus(ex.getMessage(), true);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    /** Learning-mode-only helper: turns the password into a simple 1-25 shift value. */
    private int caesarShiftFromPassword(char[] password) {
        if (password.length == 0) return 3; // default shift if no password given
        int sum = 0;
        for (char c : password) sum += c;
        return (sum % 25) + 1;
    }

    private void onCopyOutput() {
        String output = textOutputArea.getText();
        if (output.isEmpty()) {
            setTextStatus("Nothing to copy - output is empty.", true);
            return;
        }
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(output), null);
        setTextStatus("Output copied to clipboard.", false);
    }

    private void onClearText() {
        textInputArea.setText("");
        textOutputArea.setText("");
        textPasswordField.setText("");
        setTextStatus(" ", false);
    }

    private void setTextStatus(String message, boolean isError) {
        textStatusLabel.setText(message);
        textStatusLabel.setForeground(isError ? new Color(180, 40, 40) : new Color(30, 120, 30));
    }

    // =================================================================
    // FILE TAB
    // =================================================================

    private JPanel buildFileTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel fileRow = new JPanel(new BorderLayout(6, 6));
        selectedFilePathField = new JTextField();
        selectedFilePathField.setEditable(false);
        JButton browseBtn = new JButton("Browse...");
        browseBtn.addActionListener(e -> onBrowseFile());
        fileRow.add(new JLabel("Selected file:"), BorderLayout.WEST);
        fileRow.add(selectedFilePathField, BorderLayout.CENTER);
        fileRow.add(browseBtn, BorderLayout.EAST);
        panel.add(fileRow);
        panel.add(Box.createVerticalStrut(12));

        JPanel passwordRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        passwordRow.add(new JLabel("Password:"));
        filePasswordField = new JPasswordField(20);
        passwordRow.add(filePasswordField);
        panel.add(passwordRow);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton encryptFileBtn = new JButton("Encrypt File");
        JButton decryptFileBtn = new JButton("Decrypt File");
        JButton clearFileBtn = new JButton("Clear");

        encryptFileBtn.addActionListener(e -> onFileEncrypt());
        decryptFileBtn.addActionListener(e -> onFileDecrypt());
        clearFileBtn.addActionListener(e -> onClearFile());

        buttonRow.add(encryptFileBtn);
        buttonRow.add(decryptFileBtn);
        buttonRow.add(clearFileBtn);
        panel.add(buttonRow);

        fileStatusLabel = new JLabel(" ");
        fileStatusLabel.setBorder(new EmptyBorder(8, 4, 4, 4));
        panel.add(fileStatusLabel);

        JTextArea infoArea = new JTextArea(
                "Encrypting a file creates a new \"<filename>.enc\" file next to it - the\n" +
                "original file is never modified or deleted. Decrypting an \".enc\" file\n" +
                "restores the original content to a new output file. Keep your password\n" +
                "safe - it is never stored anywhere, so a lost password means the\n" +
                "encrypted file cannot be recovered.");
        infoArea.setEditable(false);
        infoArea.setOpaque(false);
        infoArea.setBorder(new EmptyBorder(12, 4, 4, 4));
        panel.add(infoArea);

        return panel;
    }

    private void onBrowseFile() {
        JFileChooser chooser = new JFileChooser();
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
            selectedFilePathField.setText(selectedFile.getAbsolutePath());
            setFileStatus(" ", false);
        }
    }

    private void onFileEncrypt() {
        char[] password = filePasswordField.getPassword();
        try {
            InputValidator.validateFileForReading(selectedFile);
            InputValidator.validatePassword(password);

            File outputFile = FileEncryptionService.suggestEncryptedOutputPath(selectedFile);
            fileService.encryptFile(selectedFile, outputFile, password);
            setFileStatus("File encrypted successfully -> " + outputFile.getName(), false);
        } catch (CryptoOperationException ex) {
            setFileStatus(ex.getMessage(), true);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void onFileDecrypt() {
        char[] password = filePasswordField.getPassword();
        try {
            InputValidator.validateFileForReading(selectedFile);
            InputValidator.validatePassword(password);

            File outputFile = FileEncryptionService.suggestDecryptedOutputPath(selectedFile);
            fileService.decryptFile(selectedFile, outputFile, password);
            setFileStatus("File decrypted successfully -> " + outputFile.getName(), false);
        } catch (CryptoOperationException ex) {
            setFileStatus(ex.getMessage(), true);
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    private void onClearFile() {
        selectedFile = null;
        selectedFilePathField.setText("");
        filePasswordField.setText("");
        setFileStatus(" ", false);
    }

    private void setFileStatus(String message, boolean isError) {
        fileStatusLabel.setText(message);
        fileStatusLabel.setForeground(isError ? new Color(180, 40, 40) : new Color(30, 120, 30));
    }
}
