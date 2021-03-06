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

import org.apache.commons.lang.StringUtils;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.EncounterRole;
import org.openmrs.EncounterType;
import org.openmrs.OrderType;
import org.openmrs.module.emrapi.utils.ModuleProperties;
import org.springframework.stereotype.Component;

@Component("radiologyProperties")
public class RadiologyProperties extends ModuleProperties {

    public Concept getXrayOrderablesConcept() {
        return getConceptByGlobalProperty(RadiologyConstants.GP_XRAY_ORDERABLES_CONCEPT);
    }

    public Concept getCTScanOrderablesConcept() {
        return getConceptByGlobalProperty(RadiologyConstants.GP_CT_SCAN_ORDERABLES_CONCEPT);
    }

    public Concept getUltrasoundOrderablesConcept() {
        return getConceptByGlobalProperty(RadiologyConstants.GP_ULTRASOUND_ORDERABLES_CONCEPT);
    }

    public EncounterType getRadiologyOrderEncounterType() {
        return getEncounterTypeByGlobalProperty(RadiologyConstants.GP_RADIOLOGY_ORDER_ENCOUNTER_TYPE);
    }

    public EncounterType getRadiologyStudyEncounterType() {
        return getEncounterTypeByGlobalProperty(RadiologyConstants.GP_RADIOLOGY_STUDY_ENCOUNTER_TYPE);
    }

    public EncounterType getRadiologyReportEncounterType() {
        return getEncounterTypeByGlobalProperty(RadiologyConstants.GP_RADIOLOGY_REPORT_ENCOUNTER_TYPE);
    }

    public EncounterRole getRadiologyTechnicianEncounterRole() {
        return getEncounterRoleByGlobalProperty(RadiologyConstants.GP_RADIOLOGY_TECHNICIAN_ENCOUNTER_ROLE);
    }

    public EncounterRole getPrincipalResultsInterpreterEncounterRole() {
        return getEncounterRoleByGlobalProperty(RadiologyConstants.GP_PRINCIPAL_RESULTS_INTERPRETER_ENCOUNTER_ROLE);
    }

    public OrderType getRadiologyTestOrderType() {
        return getOrderTypeByGlobalProperty(RadiologyConstants.GP_RADIOLOGY_TEST_ORDER_TYPE);
    }

    public CareSetting getRadiologyCareSetting() {
        String globalProperty = administrationService.getGlobalProperty(RadiologyConstants.GP_RADIOLOGY_CARE_SETTING);
        CareSetting careSetting = orderService.getCareSettingByUuid(globalProperty);
        if (careSetting == null) {
            throw new IllegalStateException("Configuration required: " + RadiologyConstants.GP_RADIOLOGY_CARE_SETTING);
        }
        return careSetting;
    }

    // used to specify the orderables that require contrast; not mandatory, but if this concept is set
    // then went ordering an study the procedure ordered will be tested against this set if it is a member
    // of the set specific contrast-related questions will be asked (currently just the creatinine level of the patient)
    public Concept getContrastOrderablesConcept() {
        if (StringUtils.isNotBlank(administrationService.getGlobalProperty(RadiologyConstants.GP_CONTRAST_ORDERABLES_CONCEPT))) {
            return getConceptByGlobalProperty(RadiologyConstants.GP_CONTRAST_ORDERABLES_CONCEPT);
        }
        else {
            // allowed to be null
            return null;
        }
    }

    // only mandatory when specifying contrast orderables
    public Concept getCreatinineLevelConcept() {
       return getConceptByGlobalProperty(RadiologyConstants.GP_CREATININE_LEVEL_CONCEPT);
    }

    // not mandatory, only used to display contact info on some error messages
    public String getLeadRadiologyTechName() {
        return administrationService.getGlobalProperty(RadiologyConstants.GP_LEAD_RADIOLOGY_TECH_NAME);
    }

    // not mandatory, only used to display contact info on some error messages
    public String getLeadRadiologyTechContactInfo() {
        return administrationService.getGlobalProperty(RadiologyConstants.GP_LEAD_RADIOLOGY_TECH_CONTACT_INFO);
    }

}
