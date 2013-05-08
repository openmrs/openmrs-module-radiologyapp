package org.openmrs.module.radiologyapp.comparator;

import org.openmrs.module.radiologyapp.RadiologyReport;

import java.util.Comparator;

public class RadiologyReportByDataComparator implements Comparator<RadiologyReport> {

    @Override
    public int compare(RadiologyReport radiologyReport, RadiologyReport radiologyReport2) {

        if (radiologyReport == null || radiologyReport.getReportDate() == null) {
            return 1;
        }

        if (radiologyReport2 == null || radiologyReport2.getReportDate() == null)  {
            return -1;
        }

        // note that we are sorting so that most recent date is first
        return radiologyReport2.getReportDate().compareTo(radiologyReport.getReportDate());
    }
}
