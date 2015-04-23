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
import org.openmrs.Concept;
import org.openmrs.ConceptMapType;
import org.openmrs.ConceptSource;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.api.ConceptService;
import org.openmrs.module.emrapi.EmrApiConstants;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RadiologyReportConceptSetTest extends BaseConceptSetTest {

    private Concept radiologyReportSetConcept;
    private Concept orderNumberConcept;
    private Concept reportBodyConcept;
    private Concept reportTypeConcept;
    private Concept procedureConcept;

    private Concept reportTypeFinalConcept = new Concept();

    private ConceptService conceptService;

    @Before
    public void setup() throws Exception {

        conceptService = mock(ConceptService.class);

        sameAs = new ConceptMapType();
        emrConceptSource = new ConceptSource();
        emrConceptSource.setName(EmrApiConstants.EMR_CONCEPT_SOURCE_NAME);

        radiologyReportSetConcept = setupConcept(conceptService, "Radiology Report Set", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_REPORT_SET);
        orderNumberConcept = setupConcept(conceptService, "order Number", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_ORDER_NUMBER);
        reportBodyConcept = setupConcept(conceptService, "Report Body", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_REPORT_BODY);
        reportTypeConcept = setupConcept(conceptService, "Report Type", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_REPORT_TYPE);;
        procedureConcept = setupConcept(conceptService, "Procedure", RadiologyConstants.CONCEPT_CODE_RADIOLOGY_PROCEDURE);

        radiologyReportSetConcept.addSetMember(orderNumberConcept);
        radiologyReportSetConcept.addSetMember(reportBodyConcept);
        radiologyReportSetConcept.addSetMember(reportTypeConcept);
        radiologyReportSetConcept.addSetMember(procedureConcept);

    }

    @Test
    public void testConstructor() throws Exception {

        RadiologyReportConceptSet radiologyReportConceptSet = new RadiologyReportConceptSet(conceptService);
        assertThat(radiologyReportConceptSet.getRadiologyReportSetConcept(), is(radiologyReportSetConcept));
        assertThat(radiologyReportConceptSet.getOrderNumberConcept(), is(orderNumberConcept));
        assertThat(radiologyReportConceptSet.getReportBodyConcept(), is(reportBodyConcept));
        assertThat(radiologyReportConceptSet.getReportTypeConcept(), is(reportTypeConcept));
        assertThat(radiologyReportConceptSet.getProcedureConcept(), is(procedureConcept));

    }

    @Test
    public void shouldCreateObsGroupFromRadiologyReport() {

        String orderNumber = "12345";

        RadiologyOrder radiologyOrder = mock(RadiologyOrder.class);
        when(radiologyOrder.getOrderNumber()).thenReturn(orderNumber);

        Concept procedure = new Concept();

        RadiologyReport radiologyReport = new RadiologyReport();
        radiologyReport.setAssociatedRadiologyOrder(radiologyOrder);
        radiologyReport.setOrderNumber(orderNumber);
        radiologyReport.setReportBody("Some report body");
        radiologyReport.setReportType(reportTypeFinalConcept);
        radiologyReport.setProcedure(procedure);

        RadiologyReportConceptSet radiologyReportConceptSet = new RadiologyReportConceptSet(conceptService);
        Obs radiologyReportObsSet = radiologyReportConceptSet.buildRadiologyReportObsGroup(radiologyReport);

        assertThat(radiologyReportObsSet.getGroupMembers().size(), is(4));
        assertThat(radiologyReportObsSet.getOrder().getOrderNumber(), is(orderNumber));

        Obs orderNumberObs = null;
        Obs procedureObs = null;
        Obs reportBodyObs = null;
        Obs reportTypeObs = null;

        for (Obs obs : radiologyReportObsSet.getGroupMembers()) {
            if (obs.getConcept().equals(orderNumberConcept)) {
                orderNumberObs = obs;
            }
            if (obs.getConcept().equals(procedureConcept)) {
                procedureObs  = obs;
            }
            if (obs.getConcept().equals(reportBodyConcept)) {
                reportBodyObs = obs;
            }
            if (obs.getConcept().equals(reportTypeConcept))
                reportTypeObs = obs;
        }

        assertNotNull(orderNumberObs);
        assertNotNull(procedureObs);
        assertNotNull(reportBodyObs);
        assertNotNull(reportTypeObs);

        assertThat(orderNumberObs.getValueText(), is("12345"));
        assertThat(procedureObs.getValueCoded(), is(procedure));
        assertThat(reportBodyObs.getValueText(), is("Some report body"));
        assertThat(reportTypeObs.getValueCoded(), is(reportTypeFinalConcept));

    }

    @Test
    public void shouldNotCreateObsForOrderNumberAndTypeIfNotSpecified() {

        Concept procedure = new Concept();

        RadiologyReport radiologyReport = new RadiologyReport();
        radiologyReport.setReportBody("Some report body");
        radiologyReport.setProcedure(procedure);

        RadiologyReportConceptSet radiologyReportConceptSet = new RadiologyReportConceptSet(conceptService);
        Obs radiologyReportObsSet = radiologyReportConceptSet.buildRadiologyReportObsGroup(radiologyReport);

        assertThat(radiologyReportObsSet.getGroupMembers().size(), is(2));

        Obs procedureObs = null;
        Obs reportBodyObs = null;

        for (Obs obs : radiologyReportObsSet.getGroupMembers()) {
            if (obs.getConcept().equals(procedureConcept)) {
                procedureObs  = obs;
            }
            if (obs.getConcept().equals(reportBodyConcept)) {
                reportBodyObs = obs;
            }
        }

        assertNotNull(procedureObs);
        assertNotNull(reportBodyObs);

        assertThat(procedureObs.getValueCoded(), is(procedure));
        assertThat(reportBodyObs.getValueText(), is("Some report body"));

    }

    @Test
    public void shouldFetchOrderNumberFromObsGroup() {
        RadiologyReportConceptSet radiologyReportConceptSet = new RadiologyReportConceptSet(conceptService);
        Obs obsGroup = createObsGroup();
        assertThat(radiologyReportConceptSet.getOrderNumberFromObsGroup(obsGroup), is("123"));
    }

    @Test
    public void shouldFetchProcedureFromObsGroup() {
        RadiologyReportConceptSet radiologyReportConceptSet = new RadiologyReportConceptSet(conceptService);
        Obs obsGroup = createObsGroup();
        assertThat(radiologyReportConceptSet.getProcedureFromObsGroup(obsGroup).getId(), is(321));
    }

    @Test
    public void shouldFetchReportTypeFromObsGroup() {
        RadiologyReportConceptSet radiologyReportConceptSet = new RadiologyReportConceptSet(conceptService);
        Obs obsGroup = createObsGroup();
        assertThat(radiologyReportConceptSet.getReportTypeFromObsGroup(obsGroup).getId(), is(345));
    }

    @Test
    public void shouldFetchReportBodyFromObsGroup() {
        RadiologyReportConceptSet radiologyReportConceptSet = new RadiologyReportConceptSet(conceptService);
        Obs obsGroup = createObsGroup();
        assertThat(radiologyReportConceptSet.getReportBodyFromObsGroup(obsGroup), is("Some report"));
    }

    @Test
    public void shouldFetchObsGroupFromEncounter() {
        RadiologyReportConceptSet radiologyStudyConceptSet = new RadiologyReportConceptSet(conceptService);
        Encounter encounter = createEncounter();
        assertThat(radiologyStudyConceptSet.getObsGroupFromEncounter(encounter).getId(), is(222));
    }

    @Test
    public void shouldFetchOrderNumberFromEncounter() {
        RadiologyReportConceptSet radiologyReportConceptSet = new RadiologyReportConceptSet(conceptService);
        Encounter encounter = createEncounter();
        assertThat(radiologyReportConceptSet.getOrderNumberFromEncounter(encounter), is("123"));
    }

    @Test
    public void shouldFetchProcedureFromEncounter() {
        RadiologyReportConceptSet radiologyReportConceptSet = new RadiologyReportConceptSet(conceptService);
        Encounter encounter = createEncounter();
        assertThat(radiologyReportConceptSet.getProcedureFromEncounter(encounter).getId(), is(321));
    }

    @Test
    public void shouldFetchReportTypeFromObsEncounter() {
        RadiologyReportConceptSet radiologyReportConceptSet = new RadiologyReportConceptSet(conceptService);
        Encounter encounter = createEncounter();
        assertThat(radiologyReportConceptSet.getReportTypeFromEncounter(encounter).getId(), is(345));
    }

    @Test
    public void shouldFetchReportBodyFromEncounter() {
        RadiologyReportConceptSet radiologyReportConceptSet = new RadiologyReportConceptSet(conceptService);
        Encounter encounter = createEncounter();
        assertThat(radiologyReportConceptSet.getReportBodyFromEncounter(encounter), is("Some report"));
    }

    @Test
    public void shouldReturnNullForAllObs() {
        RadiologyReportConceptSet radiologyReportConceptSet = new RadiologyReportConceptSet(conceptService);
        Encounter encounter = createEncounterWithObsGroupWithNoObs();
        assertNull(radiologyReportConceptSet.getOrderNumberFromEncounter(encounter));
        assertNull(radiologyReportConceptSet.getProcedureFromEncounter(encounter));
        assertNull(radiologyReportConceptSet.getReportBodyFromEncounter(encounter));
        assertNull(radiologyReportConceptSet.getReportTypeFromEncounter(encounter));
    }

    @Test
    public void shouldReturnNullForIfNoObsGroup() {
        RadiologyReportConceptSet radiologyReportConceptSet = new RadiologyReportConceptSet(conceptService);
        Encounter encounter = new Encounter();
        assertNull(radiologyReportConceptSet.getOrderNumberFromEncounter(encounter));
        assertNull(radiologyReportConceptSet.getProcedureFromEncounter(encounter));
        assertNull(radiologyReportConceptSet.getReportBodyFromEncounter(encounter));
        assertNull(radiologyReportConceptSet.getReportTypeFromEncounter(encounter));
    }

    private Encounter createEncounter() {
        Encounter encounter = new Encounter();
        encounter.addObs(createObsGroup());
        return encounter;
    }

    private Obs createObsGroup() {

        Obs obsGroup = new Obs();
        obsGroup.setId(222);
        obsGroup.setConcept(radiologyReportSetConcept);

        Obs orderNumber = new Obs();
        orderNumber.setConcept(orderNumberConcept);
        orderNumber.setValueText("123");
        obsGroup.addGroupMember(orderNumber);

        Concept procedure = new Concept();
        procedure.setId(321);
        Obs procedureObs = new Obs();
        procedureObs.setConcept(procedureConcept);
        procedureObs.setValueCoded(procedure);
        obsGroup.addGroupMember(procedureObs);

        Concept reportType = new Concept();
        reportType.setId(345);
        Obs reportTypeObs = new Obs();
        reportTypeObs.setConcept(reportTypeConcept);
        reportTypeObs.setValueCoded(reportType);
        obsGroup.addGroupMember(reportTypeObs);

        Obs reportBody = new Obs();
        reportBody.setConcept(reportBodyConcept);
        reportBody.setValueText("Some report");
        obsGroup.addGroupMember(reportBody);

        return obsGroup;
    }

    private Encounter createEncounterWithObsGroupWithNoObs() {
        Encounter encounter = new Encounter();
        encounter.addObs(createObsGroupWithNoObs());
        return encounter;
    }

    private Obs createObsGroupWithNoObs() {

        Obs obsGroup = new Obs();
        obsGroup.setId(222);
        obsGroup.setConcept(radiologyReportSetConcept);

        return obsGroup;
    }
}
