package com.solace.psg.queueBrowser.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class IconicTableCellRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = 1L;
	
	private Config config;
	
	public IconicTableCellRenderer() {
		this.config = null; // Will use defaults if null
	}
	
	public IconicTableCellRenderer(Config config) {
		this.config = config;
	}

	@Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value instanceof ImageIcon) {
            label.setIcon((ImageIcon) value);
            label.setText(""); // Remove text
        }

        // Apply alternating row colors and selection highlighting
        Color evenBg = config != null ? config.rowEvenBackground : new Color(248, 248, 248);
        Color oddBg = config != null ? config.rowOddBackground : Color.WHITE;
        Color selectedBg = config != null ? config.rowSelectedBackground : new Color(144, 238, 144);
        Color rowFg = config != null ? config.rowForeground : Color.BLACK;
        Color selectedFg = config != null ? config.rowSelectedForeground : Color.BLACK;
        
        if (!isSelected) {
            if (row % 2 == 0) {
                label.setBackground(evenBg);
            } else {
                label.setBackground(oddBg);
            }
            label.setForeground(rowFg);
        } else {
            label.setBackground(selectedBg);
            label.setForeground(selectedFg);
        }

        return label;
    }
}
