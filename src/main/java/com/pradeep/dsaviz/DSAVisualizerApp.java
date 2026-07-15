package com.pradeep.dsaviz;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class DSAVisualizerApp {
    private DSAVisualizerApp() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Uses Swing's default appearance when the system style is unavailable.
            }
            new VisualizerFrame().setVisible(true);
        });
    }
}
