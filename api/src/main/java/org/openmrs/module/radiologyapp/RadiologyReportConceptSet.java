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

import org.apache.commons.lang3.StringUtils;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.module.emrapi.EmrApiConstants;
import org.openmrs.module.emrapi.descriptor.ConceptSetDescriptor;

public class RadiologyReportConceptSet extends ConceptSetDescriptor {

    private Concept radiologyReportSetConcept;

    private Concept accessionNumberConcept;

    private Concept reportBodyConcept;

    private Concept reportTypeConcept;

    private Concept procedureConcept;

    public RadiologyReportConceptSet(ConceptService conceptService) {

        setup(conceptService, EmrApiConstants.EMR_CONCEPT_SOURCE_NAME,
                "radiologyReportSetConcept", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_REPORT_SET,
                "accessionNumberConcept", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_ACCESSION_NUMBER,
                "reportBodyConcept", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_REPORT_BODY,
                "reportTypeConcept", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_REPORT_TYPE,
                "procedureConcept", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_PROCEDURE);

    }

    public Obs buildRadiologyReportObsGroup(RadiologyReport radiologyReport) {

        Obs radiologyReportSet = new Obs();
        radiologyReportSet.setConcept(radiologyReportSetConcept);
        radiologyReportSet.setOrder(radiologyReport.getAssociatedRadiologyOrder());

        if (StringUtils.isNotBlank(radiologyReport.getAccessionNumber())) {
            Obs accessionNumber = new Obs();
            accessionNumber.setConcept(accessionNumberConcept);
            accessionNumber.setValueText(radiologyReport.getAccessionNumber());
            radiologyReportSet.addGroupMember(accessionNumber);
        }

        if (StringUtils.isNotBlank(radiologyReport.getReportBody())) {
            Obs reportBody = new Obs();
            reportBody.setConcept(reportBodyConcept);
            reportBody.setValueText(radiologyReport.getReportBody());
            radiologyReportSet.addGroupMember(reportBody);
        }

        if (radiologyReport.getReportType() != null) {
            Obs reportType = new Obs();
            reportType.setConcept(reportTypeConcept);
            reportType.setValueCoded(radiologyReport.getReportType());
            radiologyReportSet.addGroupMember(reportType);
        }

        if (radiologyReport.getProcedure() != null) {
            Obs procedure = new Obs();
            procedure.setConcept(procedureConcept);
            procedure.setValueCoded(radiologyReport.getProcedure());
            radiologyReportSet.addGroupMember(procedure);
        }

        return radiologyReportSet;
    }

    public String getAccessionNumberFromObsGroup(Obs obsGroup) {
        Obs accessionNumberObs = getMemberObsByConcept(obsGroup, getAccessionNumberConcept());
        return accessionNumberObs != null ? accessionNumberObs.getValueText() : null;
    }

    public String getReportBodyFromObsGroup(Obs obsGroup) {
        Obs reportBodyObs = getMemberObsByConcept(obsGroup, getReportBodyConcept());
        return reportBodyObs != null ? reportBodyObs.getValueText() : null;
    }

    public Concept getReportTypeFromObsGroup(Obs obsGroup) {
        Obs reportTypeObs = getMemberObsByConcept(obsGroup, getReportTypeConcept());
        return reportTypeObs != null ? reportTypeObs.getValueCoded() : null;
    }

    public Concept getProcedureFromObsGroup(Obs obsGroup) {
        Obs procedureObs = getMemberObsByConcept(obsGroup, getProcedureConcept());
        return procedureObs != null ? procedureObs.getValueCoded() : null;
    }

    public String getAccessionNumberFromEncounter(Encounter encounter) {
        Obs obsGroup = getObsGroupFromEncounter(encounter);
        return obsGroup != null && obsGroup.getGroupMembers() != null ? getAccessionNumberFromObsGroup(obsGroup) : null;
    }

    public String getReportBodyFromEncounter(Encounter encounter) {
        Obs obsGroup = getObsGroupFromEncounter(encounter);
        return obsGroup != null && obsGroup.getGroupMembers() != null ? getReportBodyFromObsGroup(obsGroup) : null;
    }

    public Concept getReportTypeFromEncounter(Encounter encounter) {
        Obs obsGroup = getObsGroupFromEncounter(encounter);
        return obsGroup != null && obsGroup.getGroupMembers() != null ? getReportTypeFromObsGroup(obsGroup) : null;
    }

    public Concept getProcedureFromEncounter(Encounter encounter) {
        Obs obsGroup = getObsGroupFromEncounter(encounter);
        return obsGroup != null && obsGroup.getGroupMembers() != null ? getProcedureFromObsGroup(obsGroup) : null;
    }

    public Obs getObsGroupFromEncounter(Encounter encounter) {
        for (Obs obs : encounter.getObsAtTopLevel(false)) {
            if (obs.getConcept().equals(getRadiologyReportSetConcept())) {
                return obs;
            }
        }
        return null;
    }

    private Obs getMemberObsByConcept(Obs obsGroup, Concept concept) {
        for (Obs obs : obsGroup.getGroupMembers()) {
            if (obs.getConcept().equals(concept)) {
                return obs;
            }
        }
        return null;
    }

    public Concept getRadiologyReportSetConcept() {
        return radiologyReportSetConcept;
    }

    public void setRadiologyReportSetConcept(Concept radiologyReportSetConcept) {
        this.radiologyReportSetConcept = radiologyReportSetConcept;
    }

    public Concept getAccessionNumberConcept() {
        return accessionNumberConcept;
    }

    public void setAccessionNumberConcept(Concept accessionNumberConcept) {
        this.accessionNumberConcept = accessionNumberConcept;
    }

    public Concept getReportBodyConcept() {
        return reportBodyConcept;
    }

    public void setReportBodyConcept(Concept reportBodyConcept) {
        this.reportBodyConcept = reportBodyConcept;
    }

    public Concept getReportTypeConcept() {
        return reportTypeConcept;
    }

    public void setReportTypeConcept(Concept reportTypeConcept) {
        this.reportTypeConcept = reportTypeConcept;
    }

    public Concept getProcedureConcept() {
        return procedureConcept;
    }

    public void setProcedureConcept(Concept procedureConcept) {
        this.procedureConcept = procedureConcept;
    }
}
