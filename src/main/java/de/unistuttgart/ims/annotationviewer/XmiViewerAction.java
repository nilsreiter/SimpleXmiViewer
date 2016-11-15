package de.unistuttgart.ims.annotationviewer;

import javax.swing.AbstractAction;

public abstract class XmiViewerAction extends AbstractAction {
	private static final long serialVersionUID = 1L;

	SimpleXmiViewer mainApplication;

	public XmiViewerAction(SimpleXmiViewer mApplication) {
		this.mainApplication = mApplication;
	}
}
