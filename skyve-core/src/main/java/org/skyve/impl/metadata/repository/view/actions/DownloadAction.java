package org.skyve.impl.metadata.repository.view.actions;

import org.skyve.impl.util.XMLMetaData;
import org.skyve.metadata.controller.ImplicitActionName;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlType(namespace = XMLMetaData.VIEW_NAMESPACE)
@XmlRootElement(namespace = XMLMetaData.VIEW_NAMESPACE, name = "download")
public class DownloadAction extends ClassAction {
	private static final long serialVersionUID = -3193664397813167826L;

	public DownloadAction() {
		implicitName = ImplicitActionName.Download;
	}
}
