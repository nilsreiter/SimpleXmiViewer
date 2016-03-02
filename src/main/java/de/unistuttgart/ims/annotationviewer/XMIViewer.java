package de.unistuttgart.ims.annotationviewer;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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

	public XMIViewer(File... files) throws FileNotFoundException, SAXException,
	IOException, UIMAException {
		super("UIMA XMI Viewer");

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				// this.savePreferences();
				System.exit(0);
			}
		});

		TypeSystemDescription tsd;
		CAS cas = null;
		if (files.length >= 1) {
			File file = files[0];
			File dir = file.getParentFile();
			File tsdFile = new File(dir, "typesystem.xml");
			tsd =
					TypeSystemDescriptionFactory
							.createTypeSystemDescriptionFromPath(tsdFile
									.toURI().toString());
			JCas jcas = JCasFactory.createJCas(tsd);
			XmiCasDeserializer.deserialize(new FileInputStream(file),
					jcas.getCas(), true);
			cas = jcas.getCas();
		}

		CasAnnotationViewer viewer = new CasAnnotationViewer();
		viewer.setCAS(cas);
		getContentPane().add(viewer);
		pack();
		setVisible(true);

	}

	public static void main(String[] args) throws FileNotFoundException,
			UIMAException, SAXException, IOException {
		if (System.getProperty("os.name").contains("OS X")) {
			Application a = Application.getApplication();
			a.setOpenFileHandler(new OpenFilesHandler() {

				public void openFiles(OpenFilesEvent e) {
					for (File file : e.getFiles()) {
						try {
							new XMIViewer(file);
						} catch (FileNotFoundException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (UIMAException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (SAXException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				}

			});
		}
		if (args.length == 1) new XMIViewer(new File(args[0]));
	}
}
