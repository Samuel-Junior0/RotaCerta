package br.edu.ifpb.rotacerta;

import br.edu.ifpb.rotacerta.cli.CliApp;
import br.edu.ifpb.rotacerta.gui.MainWindow;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        boolean modoGui = args.length == 0
                || (args.length == 1 && args[0].equalsIgnoreCase("--gui"));

        if (modoGui) {
            SwingUtilities.invokeLater(() -> {
                configurarAparencia();
                new MainWindow().setVisible(true);
            });
            return;
        }

        System.exit(new CliApp().executar(args));
    }

    private static void configurarAparencia() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("ToolTip.font", new Font("SansSerif", Font.PLAIN, 12));
        } catch (Exception ignored) {

        }
    }
}
