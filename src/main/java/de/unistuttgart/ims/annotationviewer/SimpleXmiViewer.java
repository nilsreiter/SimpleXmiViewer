package de.unistuttgart.ims.annotationviewer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.tools.util.gui.AboutDialog;

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
	JDialog aboutDialog;

	JDialog prefDialog;

	JFileChooser openDialog;

	public SimpleXmiViewer(String[] args) {
		preferences = Preferences.userRoot().node(XmiDocumentWindow.class.getName());

		aboutDialog = new AboutDialog(null, "About Annotation Viewer");

		prefDialog = new PreferencesDialog(null, preferences);

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

	public synchronized XmiDocumentWindow open(final File file) {
		final XmiDocumentWindow v = new XmiDocumentWindow(this, file);
		new Thread() {
			@Override
			public void run() {
				v.loadFile(file);
			}
		}.run();
		openFiles.add(v);
		openDialog.setCurrentDirectory(file.getParentFile());
		addRecentFile(file);
		preferences.put("lastDirectory", file.getParentFile().getAbsolutePath());
		updateAllMenus();
		return v;
	}

	public void handleAbout(AboutEvent e) {
		aboutDialog.setVisible(true);
	}

	public void handlePreferences(PreferencesEvent e) {
		prefDialog.setVisible(true);
	}

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

	public void fileOpenDialog() {
		int r = openDialog.showOpenDialog(null);
		switch (r) {
		case JFileChooser.APPROVE_OPTION:
			open(openDialog.getSelectedFile());
			break;
		}
	}
}
