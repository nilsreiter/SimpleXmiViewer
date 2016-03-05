package de.unistuttgart.ims.annotationviewer;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.tools.util.gui.AboutDialog;
import org.apache.uima.tools.viewer.CasAnnotationViewer;
import org.xml.sax.SAXException;

import com.apple.eawt.AppEvent.OpenFilesEvent;
import com.apple.eawt.Application;
import com.apple.eawt.OpenFilesHandler;

public class XMIViewer extends JFrame {

	private static final String HELP_MESSAGE =
			"Instructions for using Xmi Viewer";

	private static final long serialVersionUID = 1L;
	private JDialog aboutDialog;
	private JFileChooser openDialog;
	static List<XMIViewer> openFiles = new LinkedList<XMIViewer>();

	public XMIViewer() {
		super();
		initialise();
		openDialog
				.setCurrentDirectory(new File(System.getProperty("user.home")));
		int r = openDialog.showOpenDialog(XMIViewer.this);
		if (r == JFileChooser.APPROVE_OPTION) {
			File f = openDialog.getSelectedFile();
			loadFile(f);
			this.setTitle(f.getName());
		} else {
			System.exit(0);
		}
	}

	public XMIViewer(File file) {
		super(file.getName());
		initialise();
		openDialog.setCurrentDirectory(file.getParentFile());
		loadFile(file);
	}

	protected void closeWindow() {
		openFiles.remove(this);
		if (openFiles.isEmpty()) {
			System.exit(0);
		}

	}

	protected void initialise() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			System.err
					.println("Could not set look and feel: " + e.getMessage());
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

		// Create Menu Bar
		JMenuBar menuBar = new JMenuBar();

		JMenu fileMenu = new JMenu("File");
		JMenu helpMenu = new JMenu("Help");

		// Menu Items
		JMenuItem aboutMenuItem = new JMenuItem("About");
		JMenuItem helpMenuItem = new JMenuItem("Help");
		JMenuItem exitMenuItem = new JMenuItem("Quit");
		JMenuItem openMenuItem = new JMenuItem("Open...");
		openMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		JMenuItem closeMenuItem = new JMenuItem("Close");
		closeMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W,
				Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		fileMenu.add(openMenuItem);
		fileMenu.addSeparator();
		fileMenu.add(closeMenuItem);
		fileMenu.addSeparator();
		fileMenu.add(exitMenuItem);
		helpMenu.add(aboutMenuItem);
		helpMenu.add(helpMenuItem);
		menuBar.add(fileMenu);
		menuBar.add(helpMenu);

		setJMenuBar(menuBar);

		// window events
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				// this.savePreferences();
				XMIViewer.this.closeWindow();
			}
		});

		// Event Handlling of "Quit" Menu Item
		exitMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				// savePreferences();
				for (XMIViewer v : openFiles) {
					v.closeWindow();
				}
			}
		});

		// Event Handlling of "Close" Menu Item
		closeMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				// savePreferences();
				XMIViewer.this.closeWindow();
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
				JOptionPane.showMessageDialog(XMIViewer.this, HELP_MESSAGE,
						"Annotation Viewer Help", JOptionPane.PLAIN_MESSAGE);
			}
		});
	}

	protected void loadFile(File file) {
		// load type system and CAS
		TypeSystemDescription tsd;
		CAS cas = null;
		File dir = file.getParentFile();
		File tsdFile = new File(dir, "typesystem.xml");
		tsd =
				TypeSystemDescriptionFactory
						.createTypeSystemDescriptionFromPath(tsdFile.toURI()
								.toString());
		JCas jcas = null;
		try {
			jcas = JCasFactory.createJCas(tsd);
		} catch (UIMAException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		try {
			XmiCasDeserializer.deserialize(new FileInputStream(file),
					jcas.getCas(), true);
		} catch (SAXException e1) {
			e1.printStackTrace();
			System.exit(1);
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		cas = jcas.getCas();

		// assembly of the main view
		CasAnnotationViewer viewer = new CasAnnotationViewer();
		viewer.setCAS(cas);
		getContentPane().add(viewer);
		pack();
		setVisible(true);
		openFiles.add(this);
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
						if (file instanceof File) new XMIViewer((File) file);
					}
				}

			});
		}
		if (args.length == 1)
			new XMIViewer(new File(args[0]));
		else
			new XMIViewer();
	}
}
