package de.unistuttgart.ims.annotationviewer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableModel;

public class TableDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	public TableDialog(JFrame parent, TableModel tableModel, String title) {
		super(parent, title);

		getContentPane().setLayout(new BorderLayout());
		JButton closeButton = new JButton("OK");

		JTable table = new JTable(tableModel);
		JScrollPane scrollpane = new JScrollPane(table);

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(closeButton);
		getContentPane().add(scrollpane, BorderLayout.CENTER);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		this.pack();
		this.setModal(true);
		// event for the closeButton button
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				TableDialog.this.setVisible(false);
			}
		});

	}

}
