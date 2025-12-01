package com.solace.psg.queueBrowser.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

public class CheckboxTableCellRenderer implements TableCellRenderer {
	private Config config;
	
	public CheckboxTableCellRenderer() {
		this.config = null; // Will use defaults if null
	}
	
	public CheckboxTableCellRenderer(Config config) {
		this.config = config;
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		// Create checkbox component
		JCheckBox checkbox = new JCheckBox();
		checkbox.setSelected(value != null && (Boolean) value);
		checkbox.setHorizontalAlignment(SwingConstants.CENTER);
		checkbox.setOpaque(true);
		
		// Apply background colors based on selection state
		Color evenBg = config != null ? config.rowEvenBackground : new Color(248, 248, 248);
		Color oddBg = config != null ? config.rowOddBackground : Color.WHITE;
		Color selectedBg = config != null ? config.rowSelectedBackground : new Color(144, 238, 144);
		
		if (!isSelected) {
			if (row % 2 == 0) {
				checkbox.setBackground(evenBg);
			} else {
				checkbox.setBackground(oddBg);
			}
		} else {
			// Use configured color for selected rows - consistent with other columns
			checkbox.setBackground(selectedBg);
		}
		
		return checkbox;
	}
}

