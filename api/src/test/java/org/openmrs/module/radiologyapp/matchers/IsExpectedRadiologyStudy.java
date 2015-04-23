package org.openmrs.module.radiologyapp.matchers;

import org.mockito.ArgumentMatcher;
import org.openmrs.module.radiologyapp.RadiologyStudy;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class IsExpectedRadiologyStudy extends ArgumentMatcher<RadiologyStudy> {

    private RadiologyStudy expectedStudy;

    public IsExpectedRadiologyStudy(RadiologyStudy expectedStudy) {
        this.expectedStudy = expectedStudy;
    }

    @Override
    public boolean matches(Object o) {
        RadiologyStudy radiologyStudy = (RadiologyStudy) o;

        assertThat(radiologyStudy.getDatePerformed(), is(expectedStudy.getDatePerformed()));
        assertThat(radiologyStudy.getPatient(), is(expectedStudy.getPatient()));
        assertThat(radiologyStudy.getStudyLocation(), is(expectedStudy.getStudyLocation()));
        assertThat(radiologyStudy.getTechnician(), is(expectedStudy.getTechnician()));
        assertThat(radiologyStudy.getOrderNumber(), is(expectedStudy.getOrderNumber()));
        assertThat(radiologyStudy.getProcedure(), is(expectedStudy.getProcedure()));
        assertThat(radiologyStudy.isImagesAvailable(), is(expectedStudy.isImagesAvailable()));

        return true;
    }
}