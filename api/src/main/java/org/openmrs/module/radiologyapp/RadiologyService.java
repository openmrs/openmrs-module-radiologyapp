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
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.emrapi.adt.exception.EncounterDateAfterVisitStopDateException;
import org.openmrs.module.emrapi.adt.exception.EncounterDateBeforeVisitStartDateException;

import java.util.List;

public interface RadiologyService extends OpenmrsService {

    Encounter placeRadiologyRequisition(RadiologyRequisition requisition)
            throws EncounterDateBeforeVisitStartDateException, EncounterDateAfterVisitStopDateException;

    /**
     * Converts a radiology report to an encounter and saves it
     * (Note that this method cannot be used to
     *
     * @param radiologyReport
     * @return
     */
    Encounter saveRadiologyReport(RadiologyReport radiologyReport);

    /**
     * Converts a radiology study to an encounter and saves it
     *
     * @param radiologyStudy
     * @return
     */
    Encounter saveRadiologyStudy(RadiologyStudy radiologyStudy);

    /**
     * Fetches the radiology order with the specified order number
     *
     * @param orderNumber
     * @return
     */
    RadiologyOrder getRadiologyOrderByOrderNumber(String orderNumber);

    /**
     * Fetches the radiology study with the specified order number
     * (If there is no explicit radiology study encounter with this order number,
     * it tries to derive this information from any radiology reports with the same order number)
     *
     * @param orderNumber
     * @return
     */
    RadiologyStudy getRadiologyStudyByOrderNumber(String orderNumber);

    /**
     * Fetches all radiology reports with the specified order number
     * (i.e., all the reports for a single study)
     *
     * @param orderNumber
     * @return
     */
    List<RadiologyReport> getRadiologyReportsByOrderNumber(String orderNumber);

    /**
     * Returns radiology orders that have no corresponding study for the selected patient,
     * sorted by date, with most recent first.
     * @param patient
     * @return
     */
    List<Order> getRadiologyOrdersForPatient(Patient patient);

    /**
     * Returns all the radiology studies for the selected patient, sorted by date, with most recent first
     * This method determines fetches studies by 1) fetching all Radiology Study encounters, and then
     * 2) fetching all orphaned Radiology Reports (i.e., Radiology Report encounters where there is no
     * Radiology Study encounter with the same order number) and creating transient Radiology Study
     * objects for these report sets
     *
     * @param patient
     * @return
     */
    List<RadiologyStudy> getRadiologyStudiesForPatient(Patient patient);

}