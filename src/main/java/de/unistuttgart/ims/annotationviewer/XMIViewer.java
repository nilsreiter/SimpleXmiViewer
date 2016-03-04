package de.unistuttgart.ims.annotationviewer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.xml.sax.SAXException;

import com.apple.eawt.AppEvent.OpenFilesEvent;
import com.apple.eawt.Application;
import com.apple.eawt.OpenFilesHandler;

public class XMIViewer {

	static boolean createdScreenBar = false;

	Menu recentMenu;

	protected void loadFile(Shell shell, File file) {

		MenuItem mi = new MenuItem(recentMenu, SWT.LEFT_TO_RIGHT);
		mi.setText(file.getName());
		mi.setData(file);
		mi.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				MenuItem m = (MenuItem) e.getSource();
				loadFile(createShell(), (File) m.getData());
			}
		});
		recentMenu.setEnabled(true);

		shell.setLayout(new FillLayout());

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
		new SWTCasAnnotationViewer(cas, shell);

		shell.setText(file.getName());
		shell.open();

	}

	public static void main(String[] args) {
		System.setProperty("com.apple.macos.useScreenMenuBar", "true");
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		Display display = new Display();

		final XMIViewer app = new XMIViewer();
		if (System.getProperty("os.name").contains("OS X")) {
			Application a = Application.getApplication();
			a.setOpenFileHandler(new OpenFilesHandler() {

				public void openFiles(OpenFilesEvent e) {

					for (Object file : e.getFiles()) {
						if (file instanceof File) {
							Shell sh = app.createShell();
							app.loadFile(sh, (File) file);
						}
					}
				}

			});
		}
		Shell shell = app.open(display);

		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		} // we want to open files by open-clicking in Finder & co.
		  // not tested yet
		  // source:
		  // http://stackoverflow.com/questions/1575190/double-click-document-file-in-mac-os-x-to-open-java-application

	}

	Shell createShell() {
		final Shell shell = new Shell(SWT.SHELL_TRIM);
		createMenuBar(shell);

		shell.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				Display d = Display.getCurrent();
				Menu bar = d.getMenuBar();
				boolean hasAppMenuBar = (bar != null);
				if (!hasAppMenuBar) {
					shell.getMenuBar().dispose();
					Shell[] shells = d.getShells();
					if ((shells.length == 1) && (shells[0] == shell)) {
						if (!d.isDisposed()) d.dispose();
					}
				}
			}
		});

		return shell;
	}

	private Shell open(Display display) {

		return createShell();
	}

	private Menu createMenuBar(Shell shell) {
		Menu bar = Display.getCurrent().getMenuBar();
		boolean hasAppMenuBar = (bar != null);
		if (bar == null) {
			bar = new Menu(shell, SWT.BAR);
		}
		// Menu menuBar = new Menu(shell, SWT.BAR);
		// shell.setMenuBar(menuBar);
		if (!createdScreenBar || !hasAppMenuBar) {
			// create each header and subMenu for the menuBar
			createFileMenu(bar);
			// createEditMenu(menuBar);
			// createSearchMenu(menuBar);
			// createHelpMenu(menuBar);
		}
		createdScreenBar = true;
		return bar;
	}

	private void createFileMenu(Menu menuBar) {
		// File menu.
		MenuItem item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("File");
		Menu menu = new Menu(item);
		item.setMenu(menu);

		// File -> Open
		MenuItem subItem = new MenuItem(menu, SWT.NONE);
		subItem.setText("Open ...");
		subItem.setAccelerator(SWT.MOD1 + 'O');
		subItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Shell sh = new Shell(Display.getCurrent());
				FileDialog dialog = new FileDialog(sh, SWT.OPEN);
				dialog.setFilterNames(new String[] { "Xmi Files" }); //$NON-NLS-1$
				dialog.setFilterExtensions(new String[] { "*.xmi" }); //$NON-NLS-1$
				String name = dialog.open();
				if (name == null) return;
				loadFile(sh, new File(name));
			}
		});
		subItem = new MenuItem(menu, SWT.CASCADE);
		subItem.setText("Open Recent");
		subItem.setEnabled(false);
		recentMenu = new Menu(subItem);
		subItem.setMenu(recentMenu);

		new MenuItem(menu, SWT.SEPARATOR);

		// File -> Close.
		subItem = new MenuItem(menu, SWT.NONE);
		subItem.setText("Close");
		subItem.setAccelerator(SWT.MOD1 + 'W');
		subItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Shell shell = Display.getCurrent().getActiveShell();
				shell.close();
			}
		});

		/**
		 * Adds a listener to handle enabling and disabling
		 * some items in the Edit submenu.
		 */
		menu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuShown(MenuEvent e) {
				Menu menu = (Menu) e.widget;
				MenuItem[] items = menu.getItems();
				items[1].setEnabled(Display.getCurrent().getShells().length > 0); // edit
				// contact
				// items[5].setEnabled((file != null) && isModified); // save
				// items[6].setEnabled(table.getItemCount() != 0); // save as
			}
		});

		// Windows menu.
		/*
		 * item = new MenuItem(menuBar, SWT.CASCADE);
		 * item.setText("Windows");
		 * menu = new Menu(item);
		 * item.setMenu(menu);
		 * menu.addMenuListener(new MenuAdapter() {
		 * 
		 * @Override
		 * public void menuShown(MenuEvent e) {
		 * Menu menu = (Menu) e.widget;
		 * for (Shell shell : Display.getCurrent().getShells()) {
		 * MenuItem mi = new MenuItem(menu, SWT.NONE);
		 * mi.setText(shell.getText());
		 * // mi.setAccelerator(SWT.MOD1 + '');
		 * }
		 * }
		 * });
		 */

	}
}
