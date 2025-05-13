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
import org.openmrs.module.emrapi.EmrApiConstants;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
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
@PowerMockIgnore("javax.management.*")
public class RadiologyStudyConceptSetTest extends BaseConceptSetTest {

    private Concept radiologyStudySetConcept;
    private Concept orderNumberConcept;
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
        emrConceptSource.setName(EmrApiConstants.EMR_CONCEPT_SOURCE_NAME);

        booleanType = mock(ConceptDatatype.class);
        when(booleanType.isBoolean()).thenReturn(true);

        radiologyStudySetConcept = setupConcept(conceptService, "Radiology Study Set", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_STUDY_SET);
        orderNumberConcept = setupConcept(conceptService, "Order Number", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_ORDER_NUMBER);
        imagesAvailableConcept = setupConcept(conceptService, "Images Available", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_IMAGES_AVAILABLE);
        imagesAvailableConcept.setDatatype(booleanType);
        procedureConcept = setupConcept(conceptService, "Procedure", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_PROCEDURE);

        radiologyStudySetConcept.addSetMember(orderNumberConcept);
        radiologyStudySetConcept.addSetMember(imagesAvailableConcept);
        radiologyStudySetConcept.addSetMember(procedureConcept);

    }

    @Test
    public void testConstructor() throws Exception {

        RadiologyStudyConceptSet radiologyStudyConceptSet = new RadiologyStudyConceptSet(conceptService);
        assertThat(radiologyStudyConceptSet.getRadiologyStudySetConcept(), is(radiologyStudySetConcept));
        assertThat(radiologyStudyConceptSet.getOrderNumberConcept(), is(orderNumberConcept));
        assertThat(radiologyStudyConceptSet.getImagesAvailableConcept(), is(imagesAvailableConcept));
        assertThat(radiologyStudyConceptSet.getProcedureConcept(), is(procedureConcept));

    }

    @Test
    public void shouldCreateObsGroupFromRadiologyStudy() {

        String orderNumber = "12345";

        RadiologyOrder radiologyOrder = mock(RadiologyOrder.class);
        when(radiologyOrder.getOrderNumber()).thenReturn(orderNumber);

        Concept procedure = new Concept();

        RadiologyStudy radiologyStudy = new RadiologyStudy();
        radiologyStudy.setAssociatedRadiologyOrder(radiologyOrder);
        radiologyStudy.setOrderNumber(orderNumber);
        radiologyStudy.setImagesAvailable(true);
        radiologyStudy.setProcedure(procedure);

        RadiologyStudyConceptSet radiologyStudyConceptSet = new RadiologyStudyConceptSet(conceptService);
        Obs radiologyStudyObsSet = radiologyStudyConceptSet.buildRadiologyStudyObsGroup(radiologyStudy);

        assertThat(radiologyStudyObsSet.getGroupMembers().size(), is(3));
        assertThat(radiologyStudyObsSet.getOrder().getOrderNumber(), is(orderNumber));

        Obs orderNumberObs = null;
        Obs procedureObs = null;
        Obs imagesAvailableObs = null;

        for (Obs obs : radiologyStudyObsSet.getGroupMembers()) {
            if (obs.getConcept().equals(orderNumberConcept)) {
                orderNumberObs = obs;
            }
            if (obs.getConcept().equals(procedureConcept)) {
                procedureObs  = obs;
            }
            if (obs.getConcept().equals(imagesAvailableConcept)) {
                imagesAvailableObs = obs;
            }
        }

        assertNotNull(orderNumberObs);
        assertNotNull(procedureObs);
        assertNotNull(imagesAvailableObs);

        assertThat(orderNumberObs.getValueText(), is("12345"));
        assertThat(procedureObs.getValueCoded(), is(procedure));
        assertThat(imagesAvailableObs.getValueAsBoolean(), is(true));

    }

    @Test
    public void shouldNotCreateObsForOrderNumberAndImagesAvailableIfNoValue() {

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
    public void shouldFetchOrderNumberFromObsGroup() {
        RadiologyStudyConceptSet radiologyStudyConceptSet = new RadiologyStudyConceptSet(conceptService);
        Obs obsGroup = createObsGroup();
        assertThat(radiologyStudyConceptSet.getOrderNumberFromObsGroup(obsGroup), is("123"));
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
    public void shouldFetchOrderNumberFromEncounter() {
        RadiologyStudyConceptSet radiologyStudyConceptSet = new RadiologyStudyConceptSet(conceptService);
        Encounter encounter = createEncounter();
        assertThat(radiologyStudyConceptSet.getOrderNumberFromEncounter(encounter), is("123"));
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
    public void shouldReturnNullIfNoOrderNumberObs() {
        RadiologyStudyConceptSet radiologyStudyConceptSet = new RadiologyStudyConceptSet(conceptService);
        Encounter encounter = createEncounterWithoutOrderNumberAndImagesAvailable();
        assertNull(radiologyStudyConceptSet.getOrderNumberFromEncounter(encounter));
    }

    @Test
    public void shouldReturnNullIfNoImagesAvailableObs() {
        RadiologyStudyConceptSet radiologyStudyConceptSet = new RadiologyStudyConceptSet(conceptService);
        Encounter encounter = createEncounterWithoutOrderNumberAndImagesAvailable();
        assertNull(radiologyStudyConceptSet.getImagesAvailableFromEncounter(encounter));
    }

    @Test
    public void shouldReturnNullIfNoObsGroup() {
        RadiologyStudyConceptSet radiologyStudyConceptSet = new RadiologyStudyConceptSet(conceptService);
        Encounter encounter = new Encounter();
        assertNull(radiologyStudyConceptSet.getOrderNumberFromEncounter(encounter));
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

        Obs orderNumber = new Obs();
        orderNumber.setConcept(orderNumberConcept);
        orderNumber.setValueText("123");
        obsGroup.addGroupMember(orderNumber);

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

    private Encounter createEncounterWithoutOrderNumberAndImagesAvailable() {
        Encounter encounter = new Encounter();
        encounter.addObs(createObsGroupWithoutOrderNumberAndImagesAvailable());
        return encounter;
    }

    private Obs createObsGroupWithoutOrderNumberAndImagesAvailable() {

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


