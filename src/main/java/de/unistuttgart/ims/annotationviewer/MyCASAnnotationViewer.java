package de.unistuttgart.ims.annotationviewer;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import org.apache.uima.cas.CAS;
import org.apache.uima.tools.viewer.CasAnnotationViewer;

public class MyCASAnnotationViewer extends CasAnnotationViewer implements DocumentListener {

	private static final long serialVersionUID = 1L;
	final static Color HILIT_COLOR = Color.PINK;
	JTextPane textPane = null;
	JTextField textField;
	Button prevButton, nextButton;
	final Highlighter hilit;
	final Highlighter.HighlightPainter painter;

	public MyCASAnnotationViewer() {
		super();
		JPanel controlPanel = (JPanel) this.getComponent(1);
		controlPanel.add(createSearchPanel(), BorderLayout.NORTH);
		hilit = new DefaultHighlighter();
		painter = new DefaultHighlighter.DefaultHighlightPainter(HILIT_COLOR);
	}

	@Override
	public void setCAS(CAS aCAS) {
		super.setCAS(aCAS);

		getTextPane().setHighlighter(hilit);

	}

	public JPanel createSearchPanel() {
		JPanel searchPanel = new JPanel();
		textField = new JTextField(20);
		textField.setToolTipText("Search");
		textField.getDocument().addDocumentListener(this);

		prevButton = new Button("<");
		nextButton = new Button(">");

		searchPanel.add(textField);
		searchPanel.add(prevButton);
		searchPanel.add(nextButton);
		return searchPanel;
	}

	public JTextPane getTextPane() {
		if (textPane == null)
			textPane = (JTextPane) ((JViewport) ((JScrollPane) ((JSplitPane) ((JSplitPane) getComponent(0))
					.getLeftComponent()).getTopComponent()).getComponent(0)).getComponent(0);
		return textPane;
	}

	public void insertUpdate(DocumentEvent e) {
		search();
	}

	public void removeUpdate(DocumentEvent e) {
		search();
	}

	public void changedUpdate(DocumentEvent e) {
	}

	public void search() {
		hilit.removeAllHighlights();

		String s = textField.getText();

		if (s.length() > 0) {
			String content = textPane.getText();
			Pattern p = Pattern.compile(s);
			Matcher m = p.matcher(content);
			while (m.find()) {
				try {
					hilit.addHighlight(m.start(), m.end(), painter);
					// textPane.setCaretPosition(m.end());
					// entry.setBackground(entryBg);

				} catch (BadLocationException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
