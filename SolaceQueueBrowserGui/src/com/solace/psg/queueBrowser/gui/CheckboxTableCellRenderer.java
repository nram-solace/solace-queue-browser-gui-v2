package com.solace.psg.queueBrowser.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

public class CheckboxTableCellRenderer implements TableCellRenderer {
	// Use the same green as other renderers for consistency
	private static final Color SELECTED_BACKGROUND = new Color(144, 238, 144); // Green
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		// Create checkbox component
		JCheckBox checkbox = new JCheckBox();
		checkbox.setSelected(value != null && (Boolean) value);
		checkbox.setHorizontalAlignment(SwingConstants.CENTER);
		checkbox.setOpaque(true);
		
		// Apply background colors based on selection state
		if (!isSelected) {
			if (row % 2 == 0) {
				checkbox.setBackground(new Color(248, 248, 248)); // Very soft gray
			} else {
				checkbox.setBackground(Color.WHITE);
			}
		} else {
			// Use green for selected rows - consistent with other columns
			checkbox.setBackground(SELECTED_BACKGROUND);
		}
		
		return checkbox;
	}
}

