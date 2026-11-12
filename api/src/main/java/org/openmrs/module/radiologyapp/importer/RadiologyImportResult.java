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

package org.openmrs.module.radiologyapp.importer;

import lombok.Data;

/**
 * The outcome of importing a radiology study or report from an external system
 */
@Data
public class RadiologyImportResult {

    public enum Outcome {
        CREATED, ALREADY_EXISTS, NO_MATCHING_ORDER, PATIENT_MISMATCH
    }

    private Outcome outcome;

    // null unless the outcome is CREATED
    private String encounterUuid;

    // only meaningful for report imports: true if a study encounter was also created
    private boolean studyCreated;

    public static RadiologyImportResult created(String encounterUuid) {
        RadiologyImportResult result = new RadiologyImportResult();
        result.setOutcome(Outcome.CREATED);
        result.setEncounterUuid(encounterUuid);
        return result;
    }

    public static RadiologyImportResult of(Outcome outcome) {
        RadiologyImportResult result = new RadiologyImportResult();
        result.setOutcome(outcome);
        return result;
    }
}
