package de.unistuttgart.ims.annotationviewer;

import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.tools.viewer.CasTreeViewer;
import org.xml.sax.SAXException;

import com.apple.eawt.AppEvent.AboutEvent;
import com.apple.eawt.AppEvent.QuitEvent;
import com.apple.eawt.QuitResponse;

public class XmiDocumentWindow extends JFrame {

	private static final String HELP_MESSAGE = "Instructions for using Xmi Viewer";

	private static final long serialVersionUID = 1L;
	private MyCASAnnotationViewer viewer = null;

	String segmentAnnotation = null;

	JMenu documentMenu;
	JMenu recentMenu;
	JMenu windowsMenu;

	boolean showTreeView = false;

	public boolean isShowTreeView() {
		return showTreeView;
	}

	public void setShowTreeView(boolean showTreeView) {
		this.showTreeView = showTreeView;
	}

	static Logger logger = Logger.getAnonymousLogger();

	private JMenuBar menuBar = new JMenuBar();

	SimpleXmiViewer mainApplication;

	public XmiDocumentWindow(SimpleXmiViewer mApplication) {
		super("XmiDocumentWindow");
		mainApplication = mApplication;

		segmentAnnotation = mainApplication.getConfiguration().getString("General.segmentAnnotationTop", null);

		initialise();
	}

	protected void closeWindow(boolean quit) {
		mainApplication.close(this);
	}

	protected void initialise() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			logger.severe("Could not set look and feel: " + e.getMessage());
		}

		JMenu fileMenu = new JMenu("File");
		JMenu helpMenu = new JMenu("Help");
		JMenu viewMenu = new JMenu("View");
		windowsMenu = new JMenu("Windows");
		if (segmentAnnotation != null) {
			documentMenu = new JMenu("Document");
			documentMenu.setEnabled(segmentAnnotation != null);
		}
		// Menu Items
		JMenuItem aboutMenuItem = new JMenuItem("About");
		JMenuItem helpMenuItem = new JMenuItem("Help");
		JMenuItem exitMenuItem = new JMenuItem("Quit");
		JMenuItem openMenuItem = new JMenuItem("Open...");
		JMenuItem openUrlMenuItem = new JMenuItem("Open URL...");
		recentMenu = new JMenu("Open Recent");
		openMenuItem.setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		JMenuItem closeMenuItem = new JMenuItem("Close");
		closeMenuItem.setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_W, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		JMenuItem fontSizeIncr = new JMenuItem("Bigger");
		fontSizeIncr.setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		JMenuItem fontSizeDecr = new JMenuItem("Smaller");
		fontSizeDecr.setAccelerator(
				KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		fileMenu.add(openMenuItem);
		fileMenu.add(openUrlMenuItem);
		fileMenu.add(recentMenu);
		fileMenu.addSeparator();
		fileMenu.add(closeMenuItem);
		fileMenu.addSeparator();
		fileMenu.add(exitMenuItem);
		viewMenu.add(fontSizeIncr);
		viewMenu.add(fontSizeDecr);

		helpMenu.add(aboutMenuItem);
		helpMenu.add(helpMenuItem);

		menuBar.add(fileMenu);
		menuBar.add(viewMenu);
		if (segmentAnnotation != null)
			menuBar.add(documentMenu);
		menuBar.add(windowsMenu);
		menuBar.add(helpMenu);

		setJMenuBar(menuBar);

		// window events
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				mainApplication.close((XmiDocumentWindow) e.getSource());
			}
		});

		// Event Handlling of "Quit" Menu Item
		exitMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				mainApplication.handleQuitRequestWith(new QuitEvent(), new QuitResponse());
			}
		});

		// Event Handlling of "Close" Menu Item
		closeMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				// savePreferences();
				closeWindow(false);
			}
		});

		// Event Handlling of "Open" Menu Item
		openMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				mainApplication.fileOpenDialog();
			}
		});

		openUrlMenuItem.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				mainApplication.urlOpenDialog();
			}

		});

		// Event Handlling of "About" Menu Item
		aboutMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				mainApplication.handleAbout(new AboutEvent());
			}
		});

		// Event Handlling of "Help" Menu Item
		helpMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				JOptionPane.showMessageDialog(XmiDocumentWindow.this, HELP_MESSAGE, "Annotation Viewer Help",
						JOptionPane.PLAIN_MESSAGE);
			}
		});

		fontSizeDecr.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				int oldSize = XmiDocumentWindow.this.viewer.getTextPane().getFont().getSize();
				XmiDocumentWindow.this.viewer.getTextPane().setFont(new Font(Font.SANS_SERIF, Font.PLAIN, oldSize - 1));
			}
		});
		fontSizeIncr.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				int oldSize = XmiDocumentWindow.this.viewer.getTextPane().getFont().getSize();
				XmiDocumentWindow.this.viewer.getTextPane().setFont(new Font(Font.SANS_SERIF, Font.PLAIN, oldSize + 1));
			}
		});

		logger.info("Initialised window.");

	}

	public void windowsMenu(Collection<XmiDocumentWindow> windows) {
		recentMenu.removeAll();
		for (String r : mainApplication.getRecentFilenames(10)) {
			final File f = new File(r);
			JMenuItem mi = new JMenuItem(f.getName());
			mi.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					mainApplication.open(f);
				}

			});
			recentMenu.add(mi);
		}

		windowsMenu.removeAll();
		for (final XmiDocumentWindow v : windows) {
			JCheckBoxMenuItem item = new JCheckBoxMenuItem(v.getTitle());
			if (v == this) {
				item.setSelected(true);
			} else
				item.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						v.toFront();
					}
				});
			windowsMenu.add(item);
		}
	}

	public void loadFile(InputStream inputStream, TypeSystemDescription typeSystemDescription, String windowTitle) {
		// load type system and CAS
		CAS cas = null;
		JCas jcas = null;
		try {
			jcas = JCasFactory.createJCas(typeSystemDescription);
		} catch (UIMAException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		try {
			XmiCasDeserializer.deserialize(inputStream, jcas.getCas(), true);
		} catch (SAXException e1) {
			e1.printStackTrace();
			System.exit(1);
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		cas = jcas.getCas();

		try {
			Feature titleFeature = jcas.getTypeSystem()
					.getFeatureByFullName(mainApplication.getConfiguration().getString("General.windowTitleFeature"));
			if (titleFeature != null)
				setTitle(jcas.getDocumentAnnotationFs().getFeatureValueAsString(titleFeature)
						+ (windowTitle != null ? " (" + windowTitle + ")" : ""));
			else
				setTitle((windowTitle != null ? " (" + windowTitle + ")" : ""));
		} catch (CASRuntimeException e) {

		}
		// assembly of the main view
		viewer = new MyCASAnnotationViewer();
		viewer.setCAS(cas);
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.add("Viewer", viewer);
		if (isShowTreeView())
			try {
				tabbedPane.add("Tree", new CasTreeViewer(cas));
			} catch (CASException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		getContentPane().add(tabbedPane);
		pack();
		setVisible(true);
		createDocumentMenu(cas);
	}

	private void createDocumentMenu(CAS cas) {
		if (segmentAnnotation == null)
			return;

		org.apache.uima.cas.Type type = cas.getTypeSystem().getType(segmentAnnotation);
		if (type == null)
			return;
		documentMenu.setEnabled(true);
		AnnotationIndex<? extends Annotation> index = cas.getAnnotationIndex(type);
		Iterator<? extends Annotation> iter = index.iterator();
		Map<org.apache.uima.cas.Type, List<Annotation>> segmentMap = new HashMap<org.apache.uima.cas.Type, List<Annotation>>();
		while (iter.hasNext()) {
			final Annotation anno = iter.next();
			if (!segmentMap.containsKey(anno.getType())) {
				segmentMap.put(anno.getType(), new LinkedList<Annotation>());
			}
			segmentMap.get(anno.getType()).add(anno);
		}
		for (org.apache.uima.cas.Type annoType : segmentMap.keySet()) {
			JMenu typeMenu = new JMenu(annoType.getShortName());
			for (final Annotation anno : segmentMap.get(annoType)) {
				JMenuItem mItem = new JMenuItem(StringUtils.abbreviate(anno.getCoveredText(), 25) + " ("
						+ anno.getBegin() + "-" + anno.getEnd() + ")");

				mItem.addActionListener(new ActionListener() {

					public void actionPerformed(ActionEvent e) {
						XmiDocumentWindow.this.viewer.getTextPane().setCaretPosition(anno.getBegin());
					}

				});
				typeMenu.add(mItem);
			}
			documentMenu.add(typeMenu);
		}
		documentMenu.validate();

	}

}
