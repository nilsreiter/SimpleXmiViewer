package de.unistuttgart.ims.annotationviewer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.ImmutableNode;
import org.apache.commons.configuration2.tree.OverrideCombiner;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.tools.util.gui.AboutDialog;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.FileUtils;

import com.apple.eawt.AboutHandler;
import com.apple.eawt.AppEvent.AboutEvent;
import com.apple.eawt.AppEvent.OpenFilesEvent;
import com.apple.eawt.AppEvent.PreferencesEvent;
import com.apple.eawt.AppEvent.QuitEvent;
import com.apple.eawt.Application;
import com.apple.eawt.OpenFilesHandler;
import com.apple.eawt.PreferencesHandler;
import com.apple.eawt.QuitHandler;
import com.apple.eawt.QuitResponse;

public class SimpleXmiViewer implements AboutHandler, PreferencesHandler, OpenFilesHandler, QuitHandler {

	Set<XmiDocumentWindow> openFiles = new HashSet<XmiDocumentWindow>();

	Preferences preferences;
	Configuration configuration;

	JDialog aboutDialog;

	JDialog prefDialog;

	JFileChooser openDialog;

	Set<URI> typeSystemLocations = new HashSet<URI>();
	TypeSystemDescription typeSystemDescription = null;

	static final Logger logger = LogManager.getLogger(SimpleXmiViewer.class);

	public SimpleXmiViewer(String[] args) {
		logger.info("Application startup");

		preferences = Preferences.userRoot().node(XmiDocumentWindow.class.getName());

		INIConfiguration defaultConfig = new INIConfiguration();
		INIConfiguration userConfig = new INIConfiguration();

		InputStream is = null;
		try {
			// reading of default properties from inside the war
			is = getClass().getResourceAsStream("/default-config.ini");
			if (is != null) {
				defaultConfig.read(new InputStreamReader(is, "UTF-8"));
				// defaults.load();
			}
		} catch (Exception e) {
			logger.warn("Could not read default configuration.");
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(is);
		}

		File userConfigFile = null;
		try {
			File homeDirectory = new File(System.getProperty("user.home"));
			userConfigFile = new File(homeDirectory, ".SimpleXmiViewer.ini");
			if (userConfigFile.exists())
				userConfig.read(new FileReader(userConfigFile));
			else
				userConfigFile.createNewFile();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (ConfigurationException | IOException e) {
			logger.warn("Could not read or parse user configuration in file {}. Exception: {}.", userConfigFile,
					e.getMessage());
			e.printStackTrace();
		}

		CombinedConfiguration config = new CombinedConfiguration(new OverrideCombiner());
		config.addConfiguration(userConfig);
		config.addConfiguration(defaultConfig);
		configuration = config;

		Iterator<String> it = config.getKeys();
		while (it.hasNext()) {
			String key = it.next();
			logger.debug("Config: {} = {}.", key, config.getString(key));
		}

		aboutDialog = new AboutDialog(null, "About Annotation Viewer");

		prefDialog = new PreferencesDialog(null, preferences);

		openDialog = new JFileChooser();
		openDialog.setMultiSelectionEnabled(true);
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

		for (Object s : configuration.getList("General.typeSystems", new ArrayList<Object>())) {
			try {
				loadTypeSystem(new URI(s.toString()));
			} catch (ResourceInitializationException e) {
				logger.warn("Exception {} when loading type system from {}", e.getMessage(), s.toString());
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}

		if (System.getProperty("os.name").contains("OS X")) {
			Application a = Application.getApplication();
			a.setOpenFileHandler(this);
			a.setAboutHandler(this);
			a.setPreferencesHandler(this);
		}
		if (args.length == 1) {
			open(new File(args[0]));
		} else if (openFiles.isEmpty())
			this.fileOpenDialog();
	}

	public void loadTypeSystem(URI file) throws ResourceInitializationException {

		logger.debug("Loading a typesystem from {}.", file);
		if (typeSystemDescription == null)
			typeSystemDescription = TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(file.toString());
		else
			typeSystemDescription = CasCreationUtils.mergeTypeSystems(Arrays.asList(typeSystemDescription,
					TypeSystemDescriptionFactory.createTypeSystemDescriptionFromPath(file.toString())));

		typeSystemLocations.add(file);
		getConfiguration().setProperty("General.typeSystems", new ArrayList<URI>(typeSystemLocations));
		savePreferences();

	}

	public synchronized void savePreferences() {
		try {
			logger.debug("Saving preferences.");
			@SuppressWarnings("unchecked")
			INIConfiguration icfg = new INIConfiguration((HierarchicalConfiguration<ImmutableNode>) getConfiguration());
			Writer w = new FileWriter(new File(new File(System.getProperty("user.home")), ".SimpleXmiViewer.ini"));
			icfg.write(w);
			w.flush();
			w.close();
			logger.debug("Saving preferences successful.");
		} catch (ConfigurationException | IOException e) {
			e.printStackTrace();
			logger.warn("Preferences could not be saved: {}.", e.getMessage());
		}
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public static void main(String[] args) {
		System.setProperty("com.apple.macos.useScreenMenuBar", "true");
		System.setProperty("apple.laf.useScreenMenuBar", "true");

		new SimpleXmiViewer(args);
	}

	@Deprecated
	public JMenu getWindowsMenu() {
		JMenu menu = new JMenu("Windows");
		for (final XmiDocumentWindow v : openFiles) {
			JCheckBoxMenuItem item = new JCheckBoxMenuItem(v.getTitle());
			if (v == getFrontWindow()) {
				item.setSelected(true);
			} else
				item.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						v.toFront();
					}
				});
			menu.add(item);
		}
		return menu;
	}

	private XmiDocumentWindow getFrontWindow() {
		for (XmiDocumentWindow v : openFiles)
			if (v.isActive())
				return v;

		return null;
	}

	public void close(XmiDocumentWindow viewer) {
		openFiles.remove(viewer);
		updateAllMenus();
		viewer.dispose();
		if (openFiles.isEmpty())
			fileOpenDialog();
	};

	public synchronized XmiDocumentWindow open(final URL url) {
		final XmiDocumentWindow v = new XmiDocumentWindow(this);
		new Thread() {
			@Override
			public void run() {
				try {
					logger.info("Loading XMI document from {}.", url);
					v.loadFile(url.openStream(), typeSystemDescription, null);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.run();
		return v;
	}

	public synchronized XmiDocumentWindow open(final File file) {
		final XmiDocumentWindow v = new XmiDocumentWindow(this);

		if (configuration.getBoolean("General.typeSystemAutoLoad", true)) {
			File tsdFile = new File(file.getParentFile(), "typesystem.xml");

			if (tsdFile.exists() && tsdFile.canRead())
				try {
					loadTypeSystem(tsdFile.toURI());
				} catch (ResourceInitializationException e) {
					e.printStackTrace();
					return null;
				}
		}
		new Thread() {
			@Override
			public void run() {
				try {
					logger.info("Loading XMI document from {}.", file);
					v.loadFile(new FileInputStream(file), typeSystemDescription, file.getName());
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
		}.run();
		openFiles.add(v);
		openDialog.setCurrentDirectory(file.getParentFile());
		addRecentFile(file);
		preferences.put("lastDirectory", file.getParentFile().getAbsolutePath());
		updateAllMenus();
		return v;
	}

	@Override
	public void handleAbout(AboutEvent e) {
		aboutDialog.setVisible(true);
	}

	@Override
	public void handlePreferences(PreferencesEvent e) {
		prefDialog.setVisible(true);
	}

	@Override
	public void openFiles(OpenFilesEvent e) {
		for (Object file : e.getFiles()) {
			if (file instanceof File) {
				open((File) file);
			}
		}
	}

	public void updateAllMenus() {
		for (XmiDocumentWindow v : openFiles)
			v.windowsMenu(openFiles);
	}

	@Override
	public void handleQuitRequestWith(QuitEvent e, QuitResponse response) {
		for (XmiDocumentWindow v : openFiles)
			this.close(v);
		System.exit(0);
	}

	public void addRecentFile(File file) {
		preferences.put("recents", file.getAbsolutePath() + File.pathSeparator
				+ StringUtils.join(getRecentFilenames(10), File.pathSeparator));
	}

	public Preferences getPreferences() {
		return preferences;
	}

	public String[] getRecentFilenames(int n) {
		return (String[]) ArrayUtils.subarray(preferences.get("recents", "").split(File.pathSeparator), 0, n);
	}

	public void urlOpenDialog() {
		String s = (String) JOptionPane.showInputDialog(null, "URL:\n", "Load XMI file by URL",
				JOptionPane.PLAIN_MESSAGE, null, null, "");
		try {
			URL url = new URL(s);
			open(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}

	public void fileOpenDialog() {
		int r = openDialog.showOpenDialog(null);
		switch (r) {
		case JFileChooser.APPROVE_OPTION:
			for (File f : openDialog.getSelectedFiles()) {
				open(f);
			}
			break;
		default:
			if (openFiles.isEmpty())
				handleQuitRequestWith(null, null);
		}
	}

	static class FileOpenURLAction extends XmiViewerAction {

		private static final long serialVersionUID = 1L;

		public FileOpenURLAction(SimpleXmiViewer mApplication) {
			super(mApplication);
			putValue(Action.NAME, "Open URL");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			mainApplication.urlOpenDialog();
		}

	}

	static class LoadTypeSystemAction extends XmiViewerAction {
		private static final long serialVersionUID = 1L;

		public LoadTypeSystemAction(SimpleXmiViewer mainApplication) {
			super(mainApplication);
			putValue(Action.NAME, "Load type system ...");

		}

		@Override
		public void actionPerformed(ActionEvent e) {
			String s = (String) JOptionPane.showInputDialog(null, "URL:\n", "Load type system by URI",
					JOptionPane.PLAIN_MESSAGE, null, null, "");
			try {
				URI url = new URI(s);
				mainApplication.loadTypeSystem(url);
			} catch (ResourceInitializationException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (URISyntaxException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		}
	}

	static class FileOpenAction extends XmiViewerAction {

		private static final long serialVersionUID = 1L;

		public FileOpenAction(SimpleXmiViewer mApplication) {
			super(mApplication);
			putValue(Action.NAME, "Open");
			putValue(Action.ACCELERATOR_KEY,
					KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			mainApplication.fileOpenDialog();
		}

	}

	static class ShowLogAction extends XmiViewerAction {

		private static final long serialVersionUID = 1L;

		public ShowLogAction(SimpleXmiViewer mApplication) {
			super(mApplication);
			putValue(Action.NAME, "Show log window");
			putValue(Action.ACCELERATOR_KEY,
					KeyStroke.getKeyStroke(KeyEvent.VK_L, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

		}

		@Override
		public void actionPerformed(ActionEvent e) {
			JFrame logWindow = new JFrame("SimpleXmiViewer - Log");
			JTextArea textArea = new JTextArea();
			Dimension d = new Dimension(java.awt.Toolkit.getDefaultToolkit().getScreenSize());
			d.height -= 100;
			textArea.setMaximumSize(d);
			JScrollPane scroll = new JScrollPane(textArea);
			scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
			try {
				File logFile = new File(System.getProperty("user.home"), ".SimpleXmiViewer.log");
				if (logFile.exists() && logFile.canRead()) {
					String log = FileUtils.file2String(logFile);
					textArea.setText(log);
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			textArea.setEditable(false);

			logWindow.add(scroll, BorderLayout.CENTER);
			logWindow.pack();
			logWindow.setVisible(true);
		}

	}
}
