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

import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.openmrs.CareSetting;
import org.openmrs.Concept;
import org.openmrs.EncounterRole;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.OpenmrsMetadata;
import org.openmrs.OrderType;
import org.openmrs.Provider;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderService;
import org.openmrs.api.ProviderService;
import org.openmrs.module.emrapi.EmrApiConstants;
import org.openmrs.module.metadatamapping.MetadataTermMapping;
import org.openmrs.module.metadatamapping.api.MetadataMappingService;

public class RadiologyProperties {

    @Setter
    private AdministrationService administrationService;

    @Setter
    private ConceptService conceptService;

    @Setter
    private EncounterService encounterService;

    @Setter
    private OrderService orderService;

    @Setter
    private MetadataMappingService metadataMappingService;

    @Setter
    private ProviderService providerService;

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
        return getEncounterTypeByGlobalProperty(RadiologyConstants.GP_RADIOLOGY_ORDER_ENCOUNTER_TYPE, true);
    }

    public EncounterType getRadiologyStudyEncounterType() {
        return getEncounterTypeByGlobalProperty(RadiologyConstants.GP_RADIOLOGY_STUDY_ENCOUNTER_TYPE, true);
    }

    public EncounterType getRadiologyReportEncounterType() {
        return getEncounterTypeByGlobalProperty(RadiologyConstants.GP_RADIOLOGY_REPORT_ENCOUNTER_TYPE, true);
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

    // Copied over from emrapiproperties

    public EncounterRole getOrderingProviderEncounterRole() {
        return getEmrApiMetadataByCode(EncounterRole.class, EmrApiConstants.GP_ORDERING_PROVIDER_ENCOUNTER_ROLE, true);
    }

    public Location getUnknownLocation() {
        return getEmrApiMetadataByCode(Location.class, EmrApiConstants.GP_UNKNOWN_LOCATION, true);
    }

    public Provider getUnknownProvider() {
        return providerService.getProviderByUuid(getEmrApiMetadataUuidByCode(EmrApiConstants.GP_UNKNOWN_PROVIDER, true));
    }

    // Helper methods

    protected Concept getConceptByGlobalProperty(String globalPropertyName) {
        String globalProperty = administrationService.getGlobalProperty(globalPropertyName);
        Concept concept = conceptService.getConceptByUuid(globalProperty);
        if (concept == null) {
            throw new IllegalStateException("Configuration required: " + globalPropertyName);
        }
        return concept;
    }

    protected EncounterType getEncounterTypeByGlobalProperty(String globalPropertyName, boolean required) {
        String globalProperty = administrationService.getGlobalProperty(globalPropertyName);
        EncounterType encounterType = encounterService.getEncounterTypeByUuid(globalProperty);
        if (required && encounterType == null) {
            throw new IllegalStateException("Configuration required: " + globalPropertyName);
        }
        return encounterType;
    }

    protected EncounterRole getEncounterRoleByGlobalProperty(String globalPropertyName) {
        String globalProperty = administrationService.getGlobalProperty(globalPropertyName);
        EncounterRole encounterRole = encounterService.getEncounterRoleByUuid(globalProperty);
        if (encounterRole == null) {
            throw new IllegalStateException("Configuration required: " + globalPropertyName);
        }
        return encounterRole;
    }

    protected OrderType getOrderTypeByGlobalProperty(String globalPropertyName) {
        String globalProperty = administrationService.getGlobalProperty(globalPropertyName);
        OrderType orderType = orderService.getOrderTypeByUuid(globalProperty);
        if (orderType == null) {
            throw new IllegalStateException("Configuration required: " + globalPropertyName);
        }
        return orderType;
    }

    protected <T extends OpenmrsMetadata> T getEmrApiMetadataByCode(Class<T> type, String code, boolean required) {
        T metadataItem = metadataMappingService.getMetadataItem(type, "org.openmrs.module.emrapi", code);
        if (required && metadataItem == null) {
            throw new IllegalStateException("Configuration required: " + code);
        } else {
            return metadataItem;
        }
    }

    protected String getEmrApiMetadataUuidByCode(String mappingCode, boolean required) {
        MetadataTermMapping mapping = metadataMappingService.getMetadataTermMapping("org.openmrs.module.emrapi", mappingCode);
        if (mapping != null && mapping.getMetadataUuid() != null) {
            return mapping.getMetadataUuid();
        } else if (required) {
            throw new IllegalStateException("Configuration required: " + mappingCode);
        } else {
            return null;
        }
    }
}
