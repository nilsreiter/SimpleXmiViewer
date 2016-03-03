package de.unistuttgart.ims.annotationviewer;

import java.awt.Frame;

import org.apache.uima.cas.CAS;
import org.apache.uima.tools.viewer.CasAnnotationViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

public class SWTCasAnnotationViewer {
	public SWTCasAnnotationViewer(CAS cas, Shell shell) {
		Composite composite =
				new Composite(shell, SWT.EMBEDDED | SWT.NO_BACKGROUND);

		Frame frame = SWT_AWT.new_Frame(composite);
		CasAnnotationViewer viewer = new CasAnnotationViewer();
		viewer.setCAS(cas);
		frame.add(viewer);
	}
}
