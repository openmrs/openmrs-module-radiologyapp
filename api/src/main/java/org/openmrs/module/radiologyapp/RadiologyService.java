/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.radiologyapp;

import org.openmrs.Encounter;
import org.openmrs.Patient;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.emr.EmrContext;

import java.util.List;

public interface RadiologyService extends OpenmrsService {

    Encounter placeRadiologyRequisition(EmrContext emrContext, RadiologyRequisition requisition);

    Encounter saveRadiologyReport(RadiologyReport radiologyReport);

    Encounter saveRadiologyStudy(RadiologyStudy radiologyStudy);

    RadiologyOrder getRadiologyOrderByAccessionNumber(String accessionNumber);

    RadiologyStudy getRadiologyStudyByAccessionNumber(String accessionNumber);

    /**
     * Returns all the radiology studies for the selected patient, sorted by date, with most recent first
     * @param patient
     * @return
     */
    List<RadiologyStudy> getRadiologyStudiesForPatient(Patient patient);

}