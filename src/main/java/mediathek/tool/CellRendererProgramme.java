package mediathek.tool;

import mediathek.config.Icons;
import mediathek.daten.DatenProg;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

@SuppressWarnings("serial")
public class CellRendererProgramme extends DefaultTableCellRenderer {
    private static final ImageIcon ja_16 = Icons.ICON_TABELLE_EIN;
    private static final ImageIcon nein_12 = Icons.ICON_TABELLE_AUS;
    private static final Logger logger = LogManager.getLogger();

    public CellRendererProgramme() {
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) {
        setIcon(null);
        super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
        try {
            final int c = table.convertColumnIndexToModel(column);
            if (c == DatenProg.PROGRAMM_RESTART || c == DatenProg.PROGRAMM_DOWNLOADMANAGER) {
                setHorizontalAlignment(CENTER);
                if (getText().equals(Boolean.TRUE.toString())) {
                    setIcon(ja_16);
                } else {
                    setIcon(nein_12);
                }
                setText("");
            }
        } catch (Exception ex) {
            logger.error("getTableCellRendererComponent", ex);
        }
        return this;
    }
}
