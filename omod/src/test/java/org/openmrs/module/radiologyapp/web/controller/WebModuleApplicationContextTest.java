package org.openmrs.module.radiologyapp.web.controller;

import org.junit.Test;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class WebModuleApplicationContextTest {

    // core's web context only scans org.openmrs.web.controller, so without this element the REST controller is not
    // registered in a running server (the mediator would get a 404)
    @Test
    public void shouldComponentScanTheRestControllerPackage() throws Exception {
        InputStream in = getClass().getClassLoader().getResourceAsStream("webModuleApplicationContext.xml");
        NodeList scans = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in)
                .getElementsByTagName("context:component-scan");
        boolean found = false;
        for (int i = 0; i < scans.getLength(); i++) {
            String basePackage = scans.item(i).getAttributes().getNamedItem("base-package").getNodeValue();
            found |= RadiologyImportRestController.class.getName().startsWith(basePackage + ".");
        }
        assertThat(found, is(true));
    }
}
