package de.unistuttgart.ims.annotationviewer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.commons.configuration2.Configuration;
import org.apache.uima.cas.ArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CommonArrayFS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.text.AnnotationFS;

public class FeatureStructureTree extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	JTree tree;

	Collection<org.apache.uima.cas.Type> typesToShow = Arrays.asList();
	XmiDocumentWindow mainWindow;
	CAS cas;
	TypeSystem typeSystem;
	org.apache.uima.cas.Type stringType;
	org.apache.uima.cas.Type fsArrayType;
	org.apache.uima.cas.Type annotationType;

	public FeatureStructureTree(XmiDocumentWindow xdw, Configuration configuration) {
		mainWindow = xdw;
		DefaultMutableTreeNode top = new DefaultMutableTreeNode("Feature Structures");
		tree = new JTree(top);
		JScrollPane treeView = new JScrollPane(tree);
		this.add(treeView);
		cas = mainWindow.getJcas().getCas();
		typeSystem = mainWindow.getJcas().getTypeSystem();
		annotationType = typeSystem.getType(CAS.TYPE_NAME_ANNOTATION);
		stringType = typeSystem.getType(CAS.TYPE_NAME_STRING);
		fsArrayType = typeSystem.getType(CAS.TYPE_NAME_FS_ARRAY);
		createNodes(top);
		setSize(400, 800);
		setLocation(xdw.getLocation().x + xdw.getWidth(), xdw.getLocation().y);

		tree.expandRow(0);
		pack();

	}

	private void createNodes(DefaultMutableTreeNode top) {
		Iterator<org.apache.uima.cas.Type> typeIterator = mainWindow.getJcas().getTypeSystem().getTypeIterator();
		List<org.apache.uima.cas.Type> typeList = new LinkedList<org.apache.uima.cas.Type>();
		while (typeIterator.hasNext()) {
			org.apache.uima.cas.Type type = typeIterator.next();
			if (typeSystem != null && !typeSystem.getProperlySubsumedTypes(annotationType).contains(type)
					&& !type.getName().startsWith("uima.")) {
				FSIterator<FeatureStructure> iter = mainWindow.getJcas().getCas().getIndexRepository()
						.getAllIndexedFS(type);
				if (iter.hasNext()) {
					DefaultMutableTreeNode typeNode = new DefaultMutableTreeNode(type);
					top.add(typeNode);
					int i = 0;
					while (iter.hasNext()) {

						FeatureStructure fs = iter.next();
						DefaultMutableTreeNode fsNode = new DefaultMutableTreeNode(String.valueOf(i++));
						typeNode.add(fsNode);
						addFeatureTreeNodes(fsNode, fs);
					}
					typeList.add(type);
				}
			}
		}

	}

	private void addFeatureTreeNodes(DefaultMutableTreeNode aParentNode, FeatureStructure aFS) {
		List<Feature> features = aFS.getType().getFeatures();
		if (features == null || features.size() == 0) {
			return;
		}

		for (Feature feature : features) {
			String featureName = feature.getShortName();
			// how we get feature value depends on feature's range type)
			String featureValue = this.getFeatureValueInString(aFS, feature);
			if (featureValue == null) {
				continue;
			}
			if (featureValue.equalsIgnoreCase("*FSArray*")) {
				// The feature value is an FSArray. Cannot render it as simple
				// as "name=value".
				ArrayFS arrayFS = (ArrayFS) aFS.getFeatureValue(feature);
				if (arrayFS != null) {
					// Create a node to represent the FSArray.
					DefaultMutableTreeNode arrayNode = new DefaultMutableTreeNode(featureName + " = FSArray");
					for (int i = 0; i < arrayFS.size(); i++) {
						// Each FSArray element will be represented by a new
						// tree node and added under the array node.
						FeatureStructure featureStructure = arrayFS.get(i);
						if (featureStructure != null) {
							DefaultMutableTreeNode fsValueNode = new DefaultMutableTreeNode(
									new FsTreeNodeObject(featureStructure, featureName));
							if (!featureStructure.getType().getFeatures().isEmpty()) {
								fsValueNode.add(new DefaultMutableTreeNode(null));
							}
							arrayNode.add(fsValueNode);
						} else {
							arrayNode.add(new DefaultMutableTreeNode("null"));
						}
					}
					aParentNode.add(arrayNode);
				}
			} else {
				if (featureValue.equalsIgnoreCase("*FS*")) {
					// The feature value is an annotation object. Cannot render
					// it as simple as "name=value".
					FeatureStructure featureStructure = aFS.getFeatureValue(feature);
					if (featureStructure != null) {
						// Need to create a node to represent the annotation.
						DefaultMutableTreeNode fsValueNode = new DefaultMutableTreeNode(
								new FsTreeNodeObject(featureStructure, featureName));
						if (!featureStructure.getType().getFeatures().isEmpty()) {
							fsValueNode.add(new DefaultMutableTreeNode(null));
						}
						aParentNode.add(fsValueNode);
					}
				} else {
					// The feature value can be rendered as simple as
					// "name=value". There is no need to go down any further.
					aParentNode.add(new DefaultMutableTreeNode(featureName + " = " + featureValue));
				}
			}
		}
	}

	/**
	 * Inner class containing data for a tree node representing a
	 * FeatureStructure
	 */
	private static class FsTreeNodeObject {
		private FeatureStructure featureStructure;
		private String featureName;
		private String caption;

		public FsTreeNodeObject(FeatureStructure inFeatureStructure, String inFeatureName) {
			this.featureStructure = inFeatureStructure;
			this.featureName = inFeatureName;
			this.caption = this.featureStructure.getType().getShortName();
			if (this.featureStructure instanceof AnnotationFS) {
				String coveredText = ((AnnotationFS) this.featureStructure).getCoveredText();
				if (coveredText.length() > 64)
					coveredText = coveredText.substring(0, 64) + "...";
				this.caption += " (\"" + coveredText + "\")";
			}
			if (this.featureName != null) {
				this.caption = this.featureName + " = " + this.caption;
			}
		}

		@Override
		public String toString() {
			return this.caption;
		}
	}

	/**
	 * Get feature value in string, if value is not another annotation and not
	 * an array of annotations.
	 * 
	 * @param aFS
	 * @param feature
	 * @return
	 */
	private String getFeatureValueInString(FeatureStructure aFS, Feature feature) {
		if (this.cas == null || this.typeSystem == null || this.stringType == null || this.fsArrayType == null) {
			return "null";
		}

		org.apache.uima.cas.Type rangeType = feature.getRange();
		if (this.typeSystem.subsumes(this.fsArrayType, rangeType)) {
			// If the feature is an FSArray, cannot render it as simple as
			// "name=value".
			return "*FSArray*";
		}
		if (this.typeSystem.subsumes(this.stringType, rangeType)) {
			return checkString(aFS.getStringValue(feature), "null", 64);
		}
		if (rangeType.isPrimitive()) {
			return checkString(aFS.getFeatureValueAsString(feature), "null", 64);
		}
		if (rangeType.isArray()) {
			// String rangeTypeName = rangeType.getName();
			CommonArrayFS arrayFS = (CommonArrayFS) aFS.getFeatureValue(feature);
			String[] values = (arrayFS == null) ? null : arrayFS.toStringArray();
			if (values == null || values.length == 0) {
				return "null";
			}

			StringBuffer displayValue = new StringBuffer();
			displayValue.append("[");
			for (int i = 0; i < values.length - 1; i++) {
				displayValue.append(values[i]);
				displayValue.append(",");
			}
			displayValue.append(values[values.length - 1]);
			displayValue.append("]");
			return displayValue.toString();
		}

		// If none of the above, then it is an annotation object. Cannot render
		// it as simple as "name=value".
		return "*FS*";
	}

	/**
	 * Check if a string is null or longer than specified limit. If null, use
	 * default value. If longer than specified limit, take only the leading
	 * substring that would fit in the limit.
	 * 
	 * @param stringValue
	 * @param defaultIfNull
	 * @param maxLength
	 * @return
	 */
	private static String checkString(String stringValue, String defaultIfNull, int maxLength) {
		if (stringValue == null) {
			return defaultIfNull;
		}

		if (maxLength > 0 && stringValue.length() > maxLength) {
			return stringValue.substring(0, maxLength) + "...";
		}

		return stringValue;
	}
}
