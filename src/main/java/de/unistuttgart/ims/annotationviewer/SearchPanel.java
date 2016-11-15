package de.unistuttgart.ims.annotationviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;

import org.apache.commons.configuration2.Configuration;

import de.unistuttgart.ims.commons.Counter;

public class SearchPanel extends JFrame implements DocumentListener, ListSelectionListener, WindowListener {
	final static Color HILIT_COLOR = Color.PINK;

	private static final long serialVersionUID = 1L;
	Highlighter hilit;
	Highlighter.HighlightPainter painter;

	XmiDocumentWindow documentWindow;
	String text;
	DefaultListModel<SearchResult> lm;
	JList<SearchResult> list;
	JTextField textField;
	JPanel statusbar;
	int contexts = 50;
	boolean showBarChart = true;
	JFrame chartFrame;

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
		list.setCellRenderer(new SearchResultRenderer());
		JScrollPane listScroller = new JScrollPane(list);
		// listScroller.setPreferredSize(new Dimension(300, 500));
		getContentPane().add(listScroller, BorderLayout.CENTER);
		setLocation(xdw.getLocation().x + xdw.getWidth(), xdw.getLocation().y);

		contexts = configuration.getInt("General.resultContext", 50);

		statusbar = new JPanel();
		getContentPane().add(statusbar, BorderLayout.SOUTH);

		addWindowListener(this);
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
		try {
			Pattern.compile(textField.getText());
			search(textField.getText());
		} catch (PatternSyntaxException ex) {
		}
	}

	public void removeUpdate(DocumentEvent e) {
		try {
			Pattern.compile(textField.getText());
			search(textField.getText());
		} catch (PatternSyntaxException ex) {
		}
	}

	public void changedUpdate(DocumentEvent e) {
		try {
			Pattern.compile(textField.getText());
			search(textField.getText());
		} catch (PatternSyntaxException ex) {
		}
	}

	public void search(String s) {
		list.getSelectionModel().removeListSelectionListener(this);
		list.clearSelection();
		statusbar.removeAll();
		lm.clear();
		hilit.removeAllHighlights();
		if (chartFrame != null)
			chartFrame.dispose();
		Counter<String> counter = new Counter<String>();
		if (s.length() > 0) {

			Pattern p = Pattern.compile(s);
			Matcher m = p.matcher(text);
			while (m.find()) {
				try {
					lm.addElement(new SearchResult(m.start(), m.end()));
					hilit.addHighlight(m.start(), m.end(), painter);
					if (showBarChart)
						counter.add(text.substring(m.start(), m.end()));
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
			}
			list.getSelectionModel().addListSelectionListener(this);
			statusbar.add(new JLabel(lm.size() + " search results."));
			statusbar.add(new JButton(new ShowHistogramAction(counter)));

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

	class SearchResultRenderer implements ListCellRenderer<SearchResult> {

		Font contextFont;
		Font centerFont;

		public SearchResultRenderer() {
			contextFont = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
			centerFont = new Font(Font.SANS_SERIF, Font.BOLD, 13);
		}

		public Component getListCellRendererComponent(JList<? extends SearchResult> list, SearchResult value, int index,
				boolean isSelected, boolean cellHasFocus) {

			JPanel panel = new JPanel();
			if (isSelected) {
				panel.setBackground(list.getSelectionBackground());
				panel.setForeground(list.getSelectionForeground());
			} else {
				panel.setBackground(list.getBackground());
				panel.setForeground(list.getForeground());
			}
			JLabel left = new JLabel(text.substring(Integer.max(value.begin - contexts, 0), value.begin));
			JLabel right = new JLabel(text.substring(value.end, Integer.min(value.end + contexts, text.length() - 1)));
			left.setFont(contextFont);
			right.setFont(contextFont);

			JLabel center = new JLabel(text.substring(value.begin, value.end));
			center.setFont(centerFont);
			panel.add(left);
			panel.add(center);
			panel.add(right);

			return panel;
		}

	}

	class ShowHistogramAction extends AbstractAction {

		private static final long serialVersionUID = 1L;

		Counter<String> counter;

		public ShowHistogramAction(Counter<String> c) {
			putValue(Action.NAME, "Show histogram");
			counter = c;
		}

		public void actionPerformed(ActionEvent e) {
			String[] keys = counter.keySet().toArray(new String[counter.keySet().size()]);
			double[] values = new double[keys.length];
			for (int i = 0; i < keys.length; i++) {
				values[i] = counter.get(keys[i]);
			}
			chartFrame = new JFrame();
			chartFrame.setBackground(Color.WHITE);
			chartFrame.add(new BarChart(values, keys, "Bar Chart"));
			chartFrame.setVisible(true);
			chartFrame.setLocation(SearchPanel.this.getLocation().x,
					SearchPanel.this.getLocation().y + SearchPanel.this.getHeight());
			chartFrame.setSize(300, 200);

		}
	}

	public void windowOpened(WindowEvent e) {

	}

	public void windowClosing(WindowEvent e) {
		if (chartFrame != null) {
			chartFrame.setVisible(false);
			chartFrame.dispose();
		}
		hilit.removeAllHighlights();

	}

	public void windowClosed(WindowEvent e) {

	}

	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub

	}
}
