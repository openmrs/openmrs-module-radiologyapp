package org.openmrs.module.radiologyapp.comparator;

import org.joda.time.DateTime;
import org.junit.Test;
import org.openmrs.module.radiologyapp.RadiologyReport;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class RadiologyReportByDateComparatorTest {

    private RadiologyReportByDataComparator comparator = new RadiologyReportByDataComparator();

    @Test
    public void shouldCompareNullReportAfterNotNullReport() {
        Date firstReportDate = new DateTime(2012,1,1,0,0,0,0).toDate();
        RadiologyReport firstRadiologyReport = new RadiologyReport();
        firstRadiologyReport.setReportDate(firstReportDate);

        assertThat(comparator.compare(firstRadiologyReport, null), is(-1));
    }

    @Test
    public void shouldCompareNullDateAfterNotNullDate() {
        Date firstReportDate = new DateTime(2012,1,1,0,0,0,0).toDate();
        RadiologyReport firstRadiologyReport = new RadiologyReport();
        firstRadiologyReport.setReportDate(firstReportDate);

        RadiologyReport secondRadiologyReport = new RadiologyReport();

        assertThat(comparator.compare(firstRadiologyReport, secondRadiologyReport), is(-1));
    }

    @Test
    public void shouldCompareByDateWithMostRecentDateFirst() {

        Date firstReportDate = new DateTime(2012,1,1,0,0,0,0).toDate();
        RadiologyReport firstRadiologyReport = new RadiologyReport();
        firstRadiologyReport.setReportDate(firstReportDate);

        Date secondReportDate = new DateTime(2013,1,1,0,0,0,0).toDate();
        RadiologyReport secondRadiologyReport = new RadiologyReport();
        secondRadiologyReport.setReportDate(secondReportDate);

        assertThat(comparator.compare(firstRadiologyReport, secondRadiologyReport), is(1));
    }

    @Test
    public void shouldFindReportsWithSameDateEqual() {

        Date firstReportDate = new DateTime(2012,1,1,0,0,0,0).toDate();
        RadiologyReport firstRadiologyReport = new RadiologyReport();
        firstRadiologyReport.setReportDate(firstReportDate);

        Date secondReportDate = new DateTime(2012,1,1,0,0,0,0).toDate();
        RadiologyReport secondRadiologyReport = new RadiologyReport();
        secondRadiologyReport.setReportDate(secondReportDate);

        assertThat(comparator.compare(firstRadiologyReport, secondRadiologyReport), is(0));
    }

    @Test
    public void shouldNotFailIfBothReportsNull() {
        comparator.compare(null, null);
    }


    @Test
    public void shouldNotFailIfBothDatesNull() {
        comparator.compare(new RadiologyReport(), new RadiologyReport());
    }

}
