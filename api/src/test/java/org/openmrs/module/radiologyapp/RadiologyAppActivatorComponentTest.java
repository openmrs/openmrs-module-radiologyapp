package org.openmrs.module.radiologyapp;


import org.junit.Test;
import org.openmrs.test.BaseModuleContextSensitiveTest;

import static org.junit.Assert.assertNotNull;

public class RadiologyAppActivatorComponentTest extends BaseModuleContextSensitiveTest{

    @Test
    public void testActivator() throws Exception{
        RadiologyAppActivator activator = new RadiologyAppActivator();
        assertNotNull(activator);
    }
}
