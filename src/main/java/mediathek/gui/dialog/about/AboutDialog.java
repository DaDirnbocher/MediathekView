package mediathek.gui.dialog.about;

import com.jidesoft.dialog.ButtonPanel;
import com.jidesoft.dialog.StandardDialog;
import mediathek.tool.EscapeKeyHandler;

import javax.swing.*;
import java.awt.*;

public class AboutDialog extends StandardDialog {
    public AboutDialog(Frame owner) {
        super(owner,"Über dieses Programm", true);
        EscapeKeyHandler.installHandler(this, this::dispose);
        pack();
    }

    @Override
    public JComponent createBannerPanel() {
        return null;
    }

    @Override
    public JComponent createContentPanel() {
        return new AboutDialogPanel();
    }

    @Override
    public ButtonPanel createButtonPanel() {
        return null;
    }
}
