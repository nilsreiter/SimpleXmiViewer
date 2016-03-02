package de.unistuttgart.ims.annotationviewer;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.swing.JFrame;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.tools.viewer.CasAnnotationViewer;
import org.xml.sax.SAXException;

import com.apple.eawt.AppEvent.OpenFilesEvent;
import com.apple.eawt.Application;
import com.apple.eawt.OpenFilesHandler;

public class XMIViewer extends JFrame {

	private static final long serialVersionUID = 1L;

	public XMIViewer(File file) {
		super("UIMA XMI Viewer");

		// window events
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				// this.savePreferences();
				System.exit(0);
			}
		});

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

	}

	public static void main(String[] args) {

		// we want to open files by open-clicking in Finder & co.
		// not tested yet
		// source:
		// http://stackoverflow.com/questions/1575190/double-click-document-file-in-mac-os-x-to-open-java-application
		if (System.getProperty("os.name").contains("OS X")) {
			Application a = Application.getApplication();
			a.setOpenFileHandler(new OpenFilesHandler() {

				public void openFiles(OpenFilesEvent e) {
					for (File file : e.getFiles()) {
						new XMIViewer(file);

					}
				}

			});
		}
		if (args.length == 1) new XMIViewer(new File(args[0]));
	}
}
