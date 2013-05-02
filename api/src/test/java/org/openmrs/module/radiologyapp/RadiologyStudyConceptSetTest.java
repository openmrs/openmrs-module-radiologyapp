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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptMapType;
import org.openmrs.ConceptSource;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.api.context.Context;
import org.openmrs.module.emr.EmrConstants;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Context.class)
public class RadiologyStudyConceptSetTest extends BaseConceptSetTest {

    private Concept radiologyStudySetConcept;
    private Concept accessionNumberConcept;
    private Concept imagesAvailableConcept;
    private Concept procedureConcept;

    private ConceptService conceptService;
    private ConceptDatatype booleanType;
    private Concept trueConcept = new Concept();
    private Concept falseConcept = new Concept();

    @Before
    public void setUp() throws Exception {

        trueConcept.setId(1000);
        falseConcept.setId(1001);

        mockStatic(Context.class);
        conceptService = mock(ConceptService.class);
        when(Context.getConceptService()).thenReturn(conceptService);
        when(conceptService.getTrueConcept()).thenReturn(trueConcept);
        when(conceptService.getFalseConcept()).thenReturn(falseConcept);

        sameAs = new ConceptMapType();
        emrConceptSource = new ConceptSource();
        emrConceptSource.setName(EmrConstants.EMR_CONCEPT_SOURCE_NAME);

        booleanType = mock(ConceptDatatype.class);
        when(booleanType.isBoolean()).thenReturn(true);

        radiologyStudySetConcept = setupConcept(conceptService, "Radiology Study Set", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_STUDY_SET);
        accessionNumberConcept = setupConcept(conceptService, "Accession Number", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_ACCESSION_NUMBER);
        imagesAvailableConcept = setupConcept(conceptService, "Images Available", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_IMAGES_AVAILABLE);
        imagesAvailableConcept.setDatatype(booleanType);
        procedureConcept = setupConcept(conceptService, "Procedure", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_PROCEDURE);

        radiologyStudySetConcept.addSetMember(accessionNumberConcept);
        radiologyStudySetConcept.addSetMember(imagesAvailableConcept);
        radiologyStudySetConcept.addSetMember(procedureConcept);

    }

    @Test
    public void testConstructor() throws Exception {

        RadiologyStudyConceptSet radiologyStudyConceptSet = new RadiologyStudyConceptSet(conceptService);
        assertThat(radiologyStudyConceptSet.getRadiologyStudySetConcept(), is(radiologyStudySetConcept));
        assertThat(radiologyStudyConceptSet.getAccessionNumberConcept(), is(accessionNumberConcept));
        assertThat(radiologyStudyConceptSet.getImagesAvailableConcept(), is(imagesAvailableConcept));
        assertThat(radiologyStudyConceptSet.getProcedureConcept(), is(procedureConcept));

    }

    @Test
    public void shouldCreateObsGroupFromRadiologyStudy() {

        RadiologyOrder radiologyOrder = new RadiologyOrder();
        String accessionNumber = "12345";
        radiologyOrder.setAccessionNumber(accessionNumber);
        Concept procedure = new Concept();

        RadiologyStudy radiologyStudy = new RadiologyStudy();
        radiologyStudy.setAssociatedRadiologyOrder(radiologyOrder);
        radiologyStudy.setAccessionNumber(accessionNumber);
        radiologyStudy.setImagesAvailable(true);
        radiologyStudy.setProcedure(procedure);

        RadiologyStudyConceptSet radiologyStudyConceptSet = new RadiologyStudyConceptSet(conceptService);
        Obs radiologyStudyObsSet = radiologyStudyConceptSet.buildRadiologyStudyObsGroup(radiologyStudy);

        assertThat(radiologyStudyObsSet.getGroupMembers().size(), is(3));
        assertThat(radiologyStudyObsSet.getOrder().getAccessionNumber(), is(accessionNumber));

        Obs accessionNumberObs = null;
        Obs procedureObs = null;
        Obs imagesAvailableObs = null;

        for (Obs obs : radiologyStudyObsSet.getGroupMembers()) {
            if (obs.getConcept().equals(accessionNumberConcept)) {
                accessionNumberObs = obs;
            }
            if (obs.getConcept().equals(procedureConcept)) {
                procedureObs  = obs;
            }
            if (obs.getConcept().equals(imagesAvailableConcept)) {
                imagesAvailableObs = obs;
            }
        }

        assertNotNull(accessionNumberObs);
        assertNotNull(procedureObs);
        assertNotNull(imagesAvailableObs);

        assertThat(accessionNumberObs.getValueText(), is("12345"));
        assertThat(procedureObs.getValueCoded(), is(procedure));
        assertThat(imagesAvailableObs.getValueAsBoolean(), is(true));

    }

    @Test
    public void shouldNotCreateObsForAccessionNumberAndImagesAvailableIfNoValue() {

        Concept procedure = new Concept();

        RadiologyStudy radiologyStudy = new RadiologyStudy();
        radiologyStudy.setProcedure(procedure);

        RadiologyStudyConceptSet radiologyStudyConceptSet = new RadiologyStudyConceptSet(conceptService);
        Obs radiologyStudyObsSet = radiologyStudyConceptSet.buildRadiologyStudyObsGroup(radiologyStudy);

        assertThat(radiologyStudyObsSet.getGroupMembers().size(), is(1));

        Obs procedureObs = radiologyStudyObsSet.getGroupMembers().iterator().next();

        assertNotNull(procedureObs);
        assertThat(procedureObs.getValueCoded(), is(procedure));
    }


    @Test
    public void shouldFetchAccessionNumberFromObsGroup() {
        RadiologyStudyConceptSet radiologyStudyConceptSet = new RadiologyStudyConceptSet(conceptService);
        Obs obsGroup = createObsGroup();
        assertThat(radiologyStudyConceptSet.getAccessionNumberFromObsGroup(obsGroup), is("123"));
    }

    @Test
    public void shouldFetchProcedureFromObsGroup() {
        RadiologyStudyConceptSet radiologyStudyConceptSet = new RadiologyStudyConceptSet(conceptService);
        Obs obsGroup = createObsGroup();
        assertThat(radiologyStudyConceptSet.getProcedureFromObsGroup(obsGroup).getId(), is(321));
    }

    @Test
    public void shouldFetchImagesAvailableFromObsGroup() {
        RadiologyStudyConceptSet radiologyStudyConceptSet = new RadiologyStudyConceptSet(conceptService);
        Obs obsGroup = createObsGroup();
        assertThat(radiologyStudyConceptSet.getImagesAvailableFromObsGroup(obsGroup), is(true));
    }

    @Test
    public void shouldFetchObsGroupFromEncounter() {
        RadiologyStudyConceptSet radiologyStudyConceptSet = new RadiologyStudyConceptSet(conceptService);
        Encounter encounter = createEncounter();
        assertThat(radiologyStudyConceptSet.getObsGroupFromEncounter(encounter).getId(), is(222));
    }

    @Test
    public void shouldFetchAccessionNumberFromEncounter() {
        RadiologyStudyConceptSet radiologyStudyConceptSet = new RadiologyStudyConceptSet(conceptService);
        Encounter encounter = createEncounter();
        assertThat(radiologyStudyConceptSet.getAccessionNumberFromEncounter(encounter), is("123"));
    }

    @Test
    public void shouldFetchProcedureFromEncounter() {
        RadiologyStudyConceptSet radiologyStudyConceptSet = new RadiologyStudyConceptSet(conceptService);
        Encounter encounter = createEncounter();
        assertThat(radiologyStudyConceptSet.getProcedureFromEncounter(encounter).getId(), is(321));
    }

    @Test
    public void shouldFetchImagesAvailableFromEncounter() {
        RadiologyStudyConceptSet radiologyStudyConceptSet = new RadiologyStudyConceptSet(conceptService);
        Encounter encounter = createEncounter();
        assertThat(radiologyStudyConceptSet.getImagesAvailableFromEncounter(encounter), is(true));
    }

    @Test
    public void shouldReturnNullIfNoAccessionNumberObs() {
        RadiologyStudyConceptSet radiologyStudyConceptSet = new RadiologyStudyConceptSet(conceptService);
        Encounter encounter = createEncounterWithoutAccessionNumberAndImagesAvailable();
        assertNull(radiologyStudyConceptSet.getAccessionNumberFromEncounter(encounter));
    }

    @Test
    public void shouldReturnNullIfNoImagesAvailableObs() {
        RadiologyStudyConceptSet radiologyStudyConceptSet = new RadiologyStudyConceptSet(conceptService);
        Encounter encounter = createEncounterWithoutAccessionNumberAndImagesAvailable();
        assertNull(radiologyStudyConceptSet.getImagesAvailableFromEncounter(encounter));
    }

    @Test
    public void shouldReturnNullIfNoObsGroup() {
        RadiologyStudyConceptSet radiologyStudyConceptSet = new RadiologyStudyConceptSet(conceptService);
        Encounter encounter = new Encounter();
        assertNull(radiologyStudyConceptSet.getAccessionNumberFromEncounter(encounter));
        assertNull(radiologyStudyConceptSet.getImagesAvailableFromEncounter(encounter));
        assertNull(radiologyStudyConceptSet.getProcedureFromEncounter(encounter));
    }

    private Encounter createEncounter() {
        Encounter encounter = new Encounter();
        encounter.addObs(createObsGroup());
        return encounter;
    }

    private Obs createObsGroup() {

        Obs obsGroup = new Obs();
        obsGroup.setId(222);
        obsGroup.setConcept(radiologyStudySetConcept);

        Obs accessionNumber = new Obs();
        accessionNumber.setConcept(accessionNumberConcept);
        accessionNumber.setValueText("123");
        obsGroup.addGroupMember(accessionNumber);

        Obs imagesAvailable = new Obs();
        imagesAvailable.setConcept(imagesAvailableConcept);
        imagesAvailable.setValueCoded(trueConcept);
        obsGroup.addGroupMember(imagesAvailable);

        Concept procedure = new Concept();
        procedure.setId(321);
        Obs procedureObs = new Obs();
        procedureObs.setConcept(procedureConcept);
        procedureObs.setValueCoded(procedure);
        obsGroup.addGroupMember(procedureObs);

        return obsGroup;

    }

    private Encounter createEncounterWithoutAccessionNumberAndImagesAvailable() {
        Encounter encounter = new Encounter();
        encounter.addObs(createObsGroupWithoutAccessionNumberAndImagesAvailable());
        return encounter;
    }

    private Obs createObsGroupWithoutAccessionNumberAndImagesAvailable() {

        Obs obsGroup = new Obs();
        obsGroup.setId(222);
        obsGroup.setConcept(radiologyStudySetConcept);

        Concept procedure = new Concept();
        procedure.setId(321);
        Obs procedureObs = new Obs();
        procedureObs.setConcept(procedureConcept);
        procedureObs.setValueCoded(procedure);
        obsGroup.addGroupMember(procedureObs);

        return obsGroup;
    }

}


