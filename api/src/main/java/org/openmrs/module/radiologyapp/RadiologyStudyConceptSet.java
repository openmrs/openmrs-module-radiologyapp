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
import org.openmrs.module.emr.EmrConstants;
import org.openmrs.module.emrapi.descriptor.ConceptSetDescriptor;

public class RadiologyStudyConceptSet extends ConceptSetDescriptor {

    private Concept radiologyStudySetConcept;

    private Concept accessionNumberConcept;

    private Concept imagesAvailableConcept;

    private Concept procedureConcept;

    public RadiologyStudyConceptSet(ConceptService conceptService) {

        setup(conceptService, EmrConstants.EMR_CONCEPT_SOURCE_NAME,
                "radiologyStudySetConcept", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_STUDY_SET,
                "accessionNumberConcept", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_ACCESSION_NUMBER,
                "imagesAvailableConcept", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_IMAGES_AVAILABLE,
                "procedureConcept", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_PROCEDURE);

    }

    public Obs buildRadiologyStudyObsGroup(RadiologyStudy radiologyStudy) {

        Obs radiologyStudySet = new Obs();
        radiologyStudySet.setConcept(radiologyStudySetConcept);
        radiologyStudySet.setOrder(radiologyStudy.getAssociatedRadiologyOrder());

        if (StringUtils.isNotBlank(radiologyStudy.getAccessionNumber())) {
            Obs accessionNumber = buildObsFor(accessionNumberConcept, radiologyStudy.getAccessionNumber());
            radiologyStudySet.addGroupMember(accessionNumber);
        }

        if (radiologyStudy.isImagesAvailable() != null) {
            Obs imagesAvailable = new Obs();
            imagesAvailable.setConcept(imagesAvailableConcept);
            imagesAvailable.setValueBoolean(radiologyStudy.isImagesAvailable());
            radiologyStudySet.addGroupMember(imagesAvailable);
        }

        if (radiologyStudy.getProcedure() != null) {
            Obs procedure = new Obs();
            procedure.setConcept(procedureConcept);
            procedure.setValueCoded(radiologyStudy.getProcedure());
            radiologyStudySet.addGroupMember(procedure);
        }

        return radiologyStudySet;

    }

    public String getAccessionNumberFromObsGroup(Obs obsGroup) {
        Obs accessionNumberObs = getMemberObsByConcept(obsGroup, getAccessionNumberConcept());
        return accessionNumberObs != null ? accessionNumberObs.getValueText() : null;
    }

    public Concept getProcedureFromObsGroup(Obs obsGroup) {
        Obs procedureObs = getMemberObsByConcept(obsGroup, getProcedureConcept());
        return procedureObs != null ? procedureObs.getValueCoded() : null;
    }

    public Boolean getImagesAvailableFromObsGroup(Obs obsGroup) {
        Obs imagesAvailableObs = getMemberObsByConcept(obsGroup, getImagesAvailableConcept());
        return imagesAvailableObs != null ? imagesAvailableObs.getValueBoolean() : null;
    }

    public String getAccessionNumberFromEncounter(Encounter encounter) {
        Obs obsGroup = getObsGroupFromEncounter(encounter);
        return obsGroup != null && obsGroup.getGroupMembers() != null ? getAccessionNumberFromObsGroup(obsGroup) : null;
    }

    public Concept getProcedureFromEncounter(Encounter encounter) {
        Obs obsGroup = getObsGroupFromEncounter(encounter);
        return obsGroup != null && obsGroup.getGroupMembers() != null  ? getProcedureFromObsGroup(obsGroup) : null;
    }

    public Boolean getImagesAvailableFromEncounter(Encounter encounter) {
        Obs obsGroup = getObsGroupFromEncounter(encounter);
        return obsGroup != null && obsGroup.getGroupMembers() != null  ? getImagesAvailableFromObsGroup(obsGroup) : null;
    }

    public Obs getObsGroupFromEncounter(Encounter encounter) {
        for (Obs obs : encounter.getObsAtTopLevel(false)) {
            if (obs.getConcept().equals(getRadiologyStudySetConcept())) {
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


    public Concept getRadiologyStudySetConcept() {
        return radiologyStudySetConcept;
    }

    public void setRadiologyStudySetConcept(Concept radiologyStudySetConcept) {
        this.radiologyStudySetConcept = radiologyStudySetConcept;
    }

    public Concept getAccessionNumberConcept() {
        return accessionNumberConcept;
    }

    public void setAccessionNumberConcept(Concept accessionNumberConcept) {
        this.accessionNumberConcept = accessionNumberConcept;
    }

    public Concept getImagesAvailableConcept() {
        return imagesAvailableConcept;
    }

    public void setImagesAvailableConcept(Concept imagesAvailableConcept) {
        this.imagesAvailableConcept = imagesAvailableConcept;
    }

    public Concept getProcedureConcept() {
        return procedureConcept;
    }

    public void setProcedureConcept(Concept procedureConcept) {
        this.procedureConcept = procedureConcept;
    }
}

