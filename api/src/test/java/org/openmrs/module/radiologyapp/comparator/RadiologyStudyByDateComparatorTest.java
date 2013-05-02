package org.openmrs.module.radiologyapp.comparator;

import org.joda.time.DateTime;
import org.junit.Test;
import org.openmrs.module.radiologyapp.RadiologyStudy;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;

public class RadiologyStudyByDateComparatorTest {

    private RadiologyStudyByDateComparator comparator = new RadiologyStudyByDateComparator();

    @Test
    public void shouldCompareNullStudyAfterNotNullStudy() {
        Date firstStudyDate = new DateTime(2012,1,1,0,0,0,0).toDate();
        RadiologyStudy firstRadiologyStudy = new RadiologyStudy();
        firstRadiologyStudy.setDatePerformed(firstStudyDate);

        assertThat(comparator.compare(firstRadiologyStudy, null), is(-1));
    }

    @Test
    public void shouldCompareNullDateAfterNotNullDate() {
        Date firstStudyDate = new DateTime(2012,1,1,0,0,0,0).toDate();
        RadiologyStudy firstRadiologyStudy = new RadiologyStudy();
        firstRadiologyStudy.setDatePerformed(firstStudyDate);

        RadiologyStudy secondRadiologyStudy = new RadiologyStudy();

        assertThat(comparator.compare(firstRadiologyStudy, secondRadiologyStudy), is(-1));
    }

    @Test
    public void shouldCompareByDateWithMostRecentDateFirst() {

        Date firstStudyDate = new DateTime(2012,1,1,0,0,0,0).toDate();
        RadiologyStudy firstRadiologyStudy = new RadiologyStudy();
        firstRadiologyStudy.setDatePerformed(firstStudyDate);

        Date secondStudyDate = new DateTime(2013,1,1,0,0,0,0).toDate();
        RadiologyStudy secondRadiologyStudy = new RadiologyStudy();
        secondRadiologyStudy.setDatePerformed(secondStudyDate);

        assertThat(comparator.compare(firstRadiologyStudy, secondRadiologyStudy), is(1));
    }

    @Test
    public void shouldFindStudiesWithSameDateEqual() {

        Date firstStudyDate = new DateTime(2012,1,1,0,0,0,0).toDate();
        RadiologyStudy firstRadiologyStudy = new RadiologyStudy();
        firstRadiologyStudy.setDatePerformed(firstStudyDate);

        Date secondStudyDate = new DateTime(2012,1,1,0,0,0,0).toDate();
        RadiologyStudy secondRadiologyStudy = new RadiologyStudy();
        secondRadiologyStudy.setDatePerformed(secondStudyDate);

        assertThat(comparator.compare(firstRadiologyStudy, secondRadiologyStudy), is(0));
    }

    @Test
    public void shouldNotFailIfBothStudiesNull() {
        comparator.compare(null, null);
    }


    @Test
    public void shouldNotFailIfBothDatesNull() {
        comparator.compare(new RadiologyStudy(), new RadiologyStudy());
    }
}
