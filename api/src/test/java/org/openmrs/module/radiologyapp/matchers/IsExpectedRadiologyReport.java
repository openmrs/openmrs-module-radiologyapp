package org.openmrs.module.radiologyapp.matchers;

import org.mockito.ArgumentMatcher;
import org.openmrs.module.radiologyapp.RadiologyReport;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class IsExpectedRadiologyReport extends ArgumentMatcher<RadiologyReport> {

    private RadiologyReport expectedRadiologyReport;

    public IsExpectedRadiologyReport(RadiologyReport expectedRadiologyReport) {
        this.expectedRadiologyReport = expectedRadiologyReport;
    }

    @Override
    public boolean matches(Object o) {

        RadiologyReport radiologyReport = (RadiologyReport) o;

        assertThat(radiologyReport.getReportDate(), is(expectedRadiologyReport.getReportDate()));
        assertThat(radiologyReport.getProcedure(), is(expectedRadiologyReport.getProcedure()));
        assertThat(radiologyReport.getOrderNumber(), is(expectedRadiologyReport.getOrderNumber()));
        assertThat(radiologyReport.getPatient(), is(expectedRadiologyReport.getPatient()));
        assertThat(radiologyReport.getPrincipalResultsInterpreter(), is(expectedRadiologyReport.getPrincipalResultsInterpreter()));
        assertThat(radiologyReport.getReportLocation(), is (expectedRadiologyReport.getReportLocation()));
        assertThat(radiologyReport.getReportType(), is(expectedRadiologyReport.getReportType()));
        assertThat(radiologyReport.getReportBody(), is(expectedRadiologyReport.getReportBody()));

        return true;
    }
}
