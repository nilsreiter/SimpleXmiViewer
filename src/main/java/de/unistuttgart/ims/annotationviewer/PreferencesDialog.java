package de.unistuttgart.ims.annotationviewer;

import java.util.prefs.Preferences;

import javax.swing.JDialog;
import javax.swing.JFrame;

public class PreferencesDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	Preferences preferences;

	public PreferencesDialog(JFrame parent, Preferences pref) {
		super(parent, "Preferences");
	}
}
