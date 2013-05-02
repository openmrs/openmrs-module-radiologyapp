package org.openmrs.module.radiologyapp.comparator;

import org.openmrs.module.radiologyapp.RadiologyStudy;

import java.util.Comparator;

public class RadiologyStudyByDateComparator implements Comparator<RadiologyStudy> {

    @Override
    public int compare(RadiologyStudy radiologyStudy, RadiologyStudy radiologyStudy2) {

        if (radiologyStudy == null || radiologyStudy.getDatePerformed() == null) {
            return 1;
        }

        if (radiologyStudy2 == null || radiologyStudy2.getDatePerformed() == null)  {
            return -1;
        }

        // note that we are sorting so that most recent date is first
        return radiologyStudy2.getDatePerformed().compareTo(radiologyStudy.getDatePerformed());
    }
}
