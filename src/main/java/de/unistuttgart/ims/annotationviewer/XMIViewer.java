package de.unistuttgart.ims.annotationviewer;

import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.cas.text.AnnotationIndex;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.tools.util.gui.AboutDialog;
import org.apache.uima.tools.viewer.CasTreeViewer;
import org.xml.sax.SAXException;

import com.apple.eawt.AppEvent.OpenFilesEvent;
import com.apple.eawt.Application;
import com.apple.eawt.OpenFilesHandler;

public class XMIViewer extends JFrame {

	private static final String HELP_MESSAGE = "Instructions for using Xmi Viewer";

	private static final long serialVersionUID = 1L;
	private JDialog aboutDialog;
	private JFileChooser openDialog;
	private JMenu documentMenu;
	private MyCASAnnotationViewer viewer = null;
	String segmentAnnotation = "de.unistuttgart.ims.drama.api.DramaSegment";
	String titleFeatureName = "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData:documentTitle";
	static Preferences prefs = Preferences.userRoot().node(XMIViewer.class.getName());
	static Set<XMIViewer> openFiles = new HashSet<XMIViewer>();

	JMenu windowsMenu;

	static Logger logger = Logger.getAnonymousLogger();

	private JMenuBar menuBar = new JMenuBar();

	public XMIViewer() {
		super();
		initialise();

		if (openFiles.isEmpty()) {
			logger.info("Showing open file dialog ...");
			setVisible(true);
			openDialog.setCurrentDirectory(new File(prefs.get("lastDirectory", System.getProperty("user.home"))));
			int r = openDialog.showOpenDialog(this);
			if (r == JFileChooser.APPROVE_OPTION) {
				File f = openDialog.getSelectedFile();
				loadFile(f);
				this.setTitle(f.getName());
			} else if (openFiles.isEmpty()) {
				System.exit(0);
			}
		}
	}

	public XMIViewer(File file) {
		super(file.getName());
		initialise();
		openDialog.setCurrentDirectory(file.getParentFile());
		loadFile(file);
	}

	protected void closeWindow(boolean quit) {
		openFiles.remove(this);
		this.updateAllWindowsMenus(openFiles);
		if (openFiles.isEmpty() && quit) {
			System.exit(0);
		} else if (openFiles.isEmpty()) {
			this.dispose();
			new XMIViewer();
		}

	}

	protected void initialise() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			logger.severe("Could not set look and feel: " + e.getMessage());
		}

		// create about dialog
		aboutDialog = new AboutDialog(this, "About Annotation Viewer");

		// create file chooser dialog
		openDialog = new JFileChooser();
		openDialog.setFileFilter(new FileFilter() {

			@Override
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().endsWith(".xmi");
			}

			@Override
			public String getDescription() {
				return "UIMA Xmi Files";
			}
		});

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
				// this.savePreferences();
				XMIViewer.this.closeWindow(false);
			}
		});

		// Event Handlling of "Quit" Menu Item
		exitMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				// savePreferences();
				for (XMIViewer v : openFiles) {
					v.closeWindow(true);
				}
			}
		});

		// Event Handlling of "Close" Menu Item
		closeMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				// savePreferences();
				XMIViewer.this.closeWindow(false);
			}
		});

		// Event Handlling of "Open" Menu Item
		openMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				int r = openDialog.showOpenDialog(XMIViewer.this);
				if (r == JFileChooser.APPROVE_OPTION) {
					new XMIViewer(openDialog.getSelectedFile());
				}
			}
		});

		// Event Handlling of "About" Menu Item
		aboutMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				aboutDialog.setVisible(true);
			}
		});

		// Event Handlling of "Help" Menu Item
		helpMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				JOptionPane.showMessageDialog(XMIViewer.this, HELP_MESSAGE, "Annotation Viewer Help",
						JOptionPane.PLAIN_MESSAGE);
			}
		});

		fontSizeDecr.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				int oldSize = XMIViewer.this.viewer.getTextPane().getFont().getSize();
				XMIViewer.this.viewer.getTextPane().setFont(new Font(Font.SANS_SERIF, Font.PLAIN, oldSize - 1));
			}
		});
		fontSizeIncr.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				int oldSize = XMIViewer.this.viewer.getTextPane().getFont().getSize();
				XMIViewer.this.viewer.getTextPane().setFont(new Font(Font.SANS_SERIF, Font.PLAIN, oldSize + 1));
			}
		});

		logger.info("Initialised window.");

	}

	public void updateAllWindowsMenus(Collection<XMIViewer> windows) {
		for (XMIViewer v : windows)
			v.updateThisWindowsMenu(windows);
	}

	public void updateThisWindowsMenu(Collection<XMIViewer> windows) {
		windowsMenu.removeAll();
		for (final XMIViewer v : windows) {
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

	protected void loadFile(File file) {
		openFiles.add(this);
		prefs.put("lastDirectory", file.getParentFile().getAbsolutePath());
		// load type system and CAS
		TypeSystemDescription tsd;
		CAS cas = null;
		File dir = file.getParentFile();
		File tsdFile = new File(dir, "typesystem.xml");
		if (!(tsdFile.exists() && tsdFile.canRead())) {
			this.closeWindow(false);
			return;
		}
		tsd = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(tsdFile.toURI().toString());
		JCas jcas = null;
		try {
			jcas = JCasFactory.createJCas(tsd);
		} catch (UIMAException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		try {
			XmiCasDeserializer.deserialize(new FileInputStream(file), jcas.getCas(), true);
		} catch (SAXException e1) {
			e1.printStackTrace();
			System.exit(1);
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		cas = jcas.getCas();

		Feature titleFeature = jcas.getTypeSystem().getFeatureByFullName(titleFeatureName);
		this.setTitle(jcas.getDocumentAnnotationFs().getFeatureValueAsString(titleFeature));

		// assembly of the main view
		viewer = new MyCASAnnotationViewer();
		viewer.setCAS(cas);
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.add("Viewer", viewer);
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
		updateAllWindowsMenus(openFiles);
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
						XMIViewer.this.viewer.getTextPane().setCaretPosition(anno.getBegin());
					}

				});
				typeMenu.add(mItem);
			}
			documentMenu.add(typeMenu);
		}
		documentMenu.validate();

	}

	public static void main(String[] args) {
		System.setProperty("com.apple.macos.useScreenMenuBar", "true");
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		// we want to open files by open-clicking in Finder & co.
		// not tested yet
		// source:
		// http://stackoverflow.com/questions/1575190/double-click-document-file-in-mac-os-x-to-open-java-application
		if (System.getProperty("os.name").contains("OS X")) {
			Application a = Application.getApplication();
			a.setOpenFileHandler(new OpenFilesHandler() {

				public void openFiles(OpenFilesEvent e) {
					for (Object file : e.getFiles()) {
						if (file instanceof File) {
							openFiles.add(new XMIViewer((File) file));
						}
					}
				}

			});
		}
		if (args.length == 1)
			new XMIViewer(new File(args[0]));
		else if (openFiles.isEmpty())
			new XMIViewer();
	}

}
