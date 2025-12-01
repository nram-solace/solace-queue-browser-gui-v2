package com.solace.psg.queueBrowser.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class AlternatingRowColorRenderer extends DefaultTableCellRenderer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Config config;
	
	public AlternatingRowColorRenderer() {
		this.config = null; // Will use defaults if null
	}
	
	public AlternatingRowColorRenderer(Config config) {
		this.config = config;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		Color evenBg = config != null ? config.rowEvenBackground : new Color(248, 248, 248);
		Color oddBg = config != null ? config.rowOddBackground : Color.WHITE;
		Color selectedBg = config != null ? config.rowSelectedBackground : new Color(144, 238, 144);
		Color rowFg = config != null ? config.rowForeground : Color.BLACK;
		Color selectedFg = config != null ? config.rowSelectedForeground : Color.BLACK;
		
		if (!isSelected) {
			if (row % 2 == 0) {
				c.setBackground(evenBg);
			} else {
				c.setBackground(oddBg);
			}
			c.setForeground(rowFg);
		} else {
			c.setBackground(selectedBg);
			c.setForeground(selectedFg);
		}
		return c;
	}
}