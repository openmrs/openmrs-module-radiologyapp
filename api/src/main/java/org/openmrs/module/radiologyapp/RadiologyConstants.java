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

public class RadiologyConstants {

    // radiology global properties (will be most likely to be refactored into radiology module)

    public final static String GP_RADIOLOGY_ORDER_ENCOUNTER_TYPE = "emr.radiologyOrderEncounterType";

    public final static String GP_RADIOLOGY_STUDY_ENCOUNTER_TYPE = "emr.radiologyStudyEncounterType";

    public final static String GP_RADIOLOGY_REPORT_ENCOUNTER_TYPE = "emr.radiologyReportEncounterType";

    public final static String GP_RADIOLOGY_TECHNICIAN_ENCOUNTER_ROLE = "emr.radiologyTechnicianEncounterRole";

    public static final String GP_PRINCIPAL_RESULTS_INTERPRETER_ENCOUNTER_ROLE = "emr.principalResultsInterpreterEncounterRole";

    public static final String GP_XRAY_ORDERABLES_CONCEPT = "emr.xrayOrderablesConcept";

    public static final String GP_RADIOLOGY_TEST_ORDER_TYPE = "emr.radiologyTestOrderType";

    // concept codes used by radiology

    public static final String CONCEPT_CODE_RADIOLOGY_STUDY_SET = "Radiology study construct";

    public static final String CONCEPT_CODE_RADIOLOGY_REPORT_SET = "Radiology report construct";

    public static final String CONCEPT_CODE_RADIOLOGY_REPORT_BODY = "Radiology report comments";

    public static final String CONCEPT_CODE_RADIOLOGY_REPORT_TYPE = "Type of radiology report";

    public static final String CONCEPT_CODE_RADIOLOGY_PROCEDURE = "Radiology procedure performed";

    public static final String CONCEPT_CODE_RADIOLOGY_ACCESSION_NUMBER = "Radiology accession number";

    public static final String CONCEPT_CODE_RADIOLOGY_IMAGES_AVAILABLE = "Radiology images available";

    public static final String CONCEPT_CODE_RADIOLOGY_REPORT_PRELIM = "Radiology report preliminary";

    public static final String CONCEPT_CODE_RADIOLOGY_REPORT_FINAL = "Radiology report final";

    public static final String CONCEPT_CODE_RADIOLOGY_REPORT_CORRECTION = "Radiology report correction";

}
