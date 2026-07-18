package main;

import gui.MainWindow;

import javax.swing.*;

/**
 * Main.java
 * =========
 * Application entry point. Sets the system look-and-feel (so the app
 * matches the user's OS style instead of Java's default "Metal" look),
 * then builds and shows the GUI on the Event Dispatch Thread as Swing
 * requires.
 */
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Non-fatal - fall back to the default Swing look and feel.
            System.err.println("Could not apply system look and feel: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            try {
                MainWindow window = new MainWindow();
                window.setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                        "Failed to start application: " + e.getMessage(),
                        "Startup Error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
