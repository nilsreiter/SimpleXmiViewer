package de.unistuttgart.ims.annotationviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import org.apache.commons.configuration2.Configuration;

public class SearchPanel extends JFrame implements DocumentListener, ListSelectionListener {
	final static Color HILIT_COLOR = Color.PINK;

	private static final long serialVersionUID = 1L;
	Highlighter hilit;
	Highlighter.HighlightPainter painter;

	XmiDocumentWindow documentWindow;
	String text;
	DefaultListModel<SearchResult> lm;
	JList<SearchResult> list;
	JTextField textField;
	int contexts = 20;

	public SearchPanel(XmiDocumentWindow xdw, Configuration configuration) {
		setTitle("Search");
		documentWindow = xdw;
		text = xdw.getViewer().getTextPane().getText();

		hilit = new DefaultHighlighter();
		painter = new DefaultHighlighter.DefaultHighlightPainter(HILIT_COLOR);
		xdw.getViewer().getTextPane().setHighlighter(hilit);

		lm = new DefaultListModel<SearchResult>();
		getContentPane().add(createSearchPanel(), BorderLayout.PAGE_START);
		list = new JList<SearchResult>(lm);
		list.getSelectionModel().addListSelectionListener(this);
		list.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane listScroller = new JScrollPane(list);
		listScroller.setPreferredSize(new Dimension(300, 500));
		getContentPane().add(listScroller, BorderLayout.CENTER);
		setLocation(xdw.getLocation().x + xdw.getWidth(), xdw.getLocation().y);

		contexts = configuration.getInt("General.resultContext", 20);

		pack();
	}

	public JPanel createSearchPanel() {
		JPanel searchPanel = new JPanel();
		textField = new JTextField(20);
		textField.setToolTipText("Search");
		textField.getDocument().addDocumentListener(this);

		searchPanel.add(textField);
		return searchPanel;
	}

	public void insertUpdate(DocumentEvent e) {
		search(textField.getText());
	}

	public void removeUpdate(DocumentEvent e) {
		search(textField.getText());
	}

	public void changedUpdate(DocumentEvent e) {
		search(textField.getText());
	}

	public void search(String s) {
		list.getSelectionModel().removeListSelectionListener(this);
		list.clearSelection();
		lm.clear();
		hilit.removeAllHighlights();

		if (s.length() > 0) {

			Pattern p = Pattern.compile(s);
			Matcher m = p.matcher(text);
			while (m.find()) {
				try {
					lm.addElement(new SearchResult(m.start(), m.end()));
					hilit.addHighlight(m.start(), m.end(), painter);

				} catch (BadLocationException e) {
					e.printStackTrace();
				}
			}
			list.getSelectionModel().addListSelectionListener(this);
		}
		pack();
	}

	public void valueChanged(ListSelectionEvent e) {

		SearchResult result = lm.getElementAt(((ListSelectionModel) e.getSource()).getMinSelectionIndex());
		documentWindow.getViewer().getTextPane().setCaretPosition(result.getEnd());
	}

	class SearchResult {
		public SearchResult(int begin, int end) {
			super();
			this.begin = begin;
			this.end = end;
		}

		int begin, end;

		public int getBegin() {
			return begin;
		}

		public int getEnd() {
			return end;
		}

		@Override
		public String toString() {
			return text.substring(Integer.max(begin - contexts, 0), Integer.min(end + contexts, text.length() - 1));
		}
	}
}
