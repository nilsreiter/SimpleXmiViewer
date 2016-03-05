package de.unistuttgart.ims.annotationviewer;

import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.JViewport;

import org.apache.uima.tools.viewer.CasAnnotationViewer;

public class MyCASAnnotationViewer extends CasAnnotationViewer {

	private static final long serialVersionUID = 1L;

	JTextPane textPane = null;

	public MyCASAnnotationViewer() {
		super();
	}

	public JTextPane getTextPane() {
		if (textPane == null)
			textPane =
					(JTextPane) ((JViewport) ((JScrollPane) ((JSplitPane) ((JSplitPane) getComponent(0))
							.getLeftComponent()).getTopComponent())
							.getComponent(0)).getComponent(0);
		return textPane;
	}
}
