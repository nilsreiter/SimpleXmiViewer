package de.unistuttgart.ims.annotationviewer;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import org.apache.uima.UIMAFramework;
import org.apache.uima.util.Level;

public class AboutDialog extends JDialog {
	private static final String ABOUT_TEXT;

	private static final long serialVersionUID = 1L;

	public AboutDialog(JFrame aParentFrame, String aDialogTitle) {
		super(aParentFrame, aDialogTitle);

		getContentPane().setLayout(new BorderLayout());
		JButton closeButton = new JButton("OK");

		String aboutText = ABOUT_TEXT.replaceAll("\\$\\{uima.version\\}", UIMAFramework.getVersionString());
		aboutText = aboutText.replaceAll("\\$\\{sxv.version\\}", getClass().getPackage().getImplementationVersion());

		JTextArea textArea = new JTextArea(aboutText);
		textArea.setEditable(false);
		getContentPane().add(textArea, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(closeButton);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		this.pack();
		this.setResizable(false);
		this.setModal(true);
		// event for the closeButton button
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				AboutDialog.this.setVisible(false);
			}
		});
	}

	// Read the dialog text from a resource file
	static {
		StringBuffer buf = new StringBuffer();
		try {
			InputStream textStream = AboutDialog.class.getResourceAsStream("about.txt");
			BufferedReader reader = new BufferedReader(new InputStreamReader(textStream));
			String line;
			while ((line = reader.readLine()) != null) {
				buf.append(line).append('\n');
			}
		} catch (Exception e) {
			UIMAFramework.getLogger().log(Level.WARNING, "About text could not be loaded", e);
		}
		ABOUT_TEXT = buf.toString();
	}
}
