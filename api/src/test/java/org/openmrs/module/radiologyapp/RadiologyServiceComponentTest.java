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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.LocationService;
import org.openmrs.api.OrderService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.emr.EmrContext;
import org.openmrs.module.emrapi.EmrApiProperties;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RadiologyServiceComponentTest extends BaseModuleContextSensitiveTest {

    @Autowired
    @Qualifier("radiologyService")
    RadiologyService radiologyService;

    @Autowired
    @Qualifier("patientService")
    private PatientService patientService;

    @Autowired
    @Qualifier("orderService")
    private OrderService orderService;

    @Autowired
    @Qualifier("conceptService")
    private ConceptService conceptService;

    @Autowired
    @Qualifier("locationService")
    private LocationService locationService;

    @Autowired
    @Qualifier("encounterService")
    private EncounterService encounterService;

    @Autowired
    @Qualifier("emrApiProperties")
    private EmrApiProperties emrApiProperties;

    @Autowired
    @Qualifier("radiologyProperties")
    private RadiologyProperties radiologyProperties;

    @Before
    public void beforeAllTests() throws Exception {
        executeDataSet("radiologyServiceComponentTestDataset.xml");
    }

    @Test
    public void testThatServiceIsConfiguredCorrectly() {
        Assert.assertNotNull("Couldn't autowire RadiologyService", radiologyService);
        Assert.assertNotNull("Couldn't get RadiologyService from Context", Context.getService(RadiologyService.class));
    }

    @Test
    public void shouldPlaceARadiologyRequisition() {

        Patient patient = patientService.getPatient(6);

        // sanity check
        Assert.assertEquals(0, encounterService.getEncountersByPatient(patient).size());

        EmrContext emrContext = mock(EmrContext.class);
        when(emrContext.getSessionLocation()).thenReturn(locationService.getLocation(1));
        when(emrContext.getActiveVisit()).thenReturn(null);

        RadiologyRequisition requisition = new RadiologyRequisition();

        requisition.setPatient(patient);
        requisition.setStudies(Collections.singleton(conceptService.getConcept(18)));
        requisition.setUrgency(Order.Urgency.STAT);

        radiologyService.placeRadiologyRequisition(emrContext, requisition);

        List<Encounter> encounters = encounterService.getEncountersByPatient(patient);
        Assert.assertEquals(1, encounters.size());

        Set<Order> orders = encounters.get(0).getOrders();
        Assert.assertEquals(1, orders.size());

    }

    @Test
    public void shouldRetrieveOrderByAccessionNumber() {

        Patient patient = patientService.getPatient(6);

        EmrContext emrContext = mock(EmrContext.class);
        when(emrContext.getSessionLocation()).thenReturn(locationService.getLocation(1));
        when(emrContext.getActiveVisit()).thenReturn(null);

        RadiologyRequisition requisition = new RadiologyRequisition();

        requisition.setPatient(patient);
        requisition.setStudies(Collections.singleton(conceptService.getConcept(18)));
        requisition.setUrgency(Order.Urgency.STAT);

        radiologyService.placeRadiologyRequisition(emrContext, requisition);

        List<Encounter> encounters = encounterService.getEncountersByPatient(patient);
        Set<Order> orders = encounters.get(0).getOrders();

        String accessionNumber = orders.iterator().next().getAccessionNumber();

        RadiologyOrder radiologyOrder = radiologyService.getRadiologyOrderByAccessionNumber(accessionNumber);
        Assert.assertNotNull(radiologyOrder);
        Assert.assertEquals(conceptService.getConcept(18), radiologyOrder.getConcept());
        Assert.assertEquals(Order.Urgency.STAT, radiologyOrder.getUrgency());
    }

    @Test
    public void shouldSaveARadiologyStudy() {

        Date timeOfStudy = new Date();

        // use patient demo database
        Patient patient = patientService.getPatient(6);

        // from radiologyServiceComponentTestDataset.xml
        Concept procedure = conceptService.getConcept(1001);
        RadiologyOrder radiologyOrder = radiologyService.getRadiologyOrderByAccessionNumber("12345");

        RadiologyStudy radiologyStudy = new RadiologyStudy();
        radiologyStudy.setPatient(patient);
        radiologyStudy.setProcedure(procedure);
        radiologyStudy.setImagesAvailable(true);
        radiologyStudy.setAccessionNumber("12345");
        radiologyStudy.setAssociatedRadiologyOrder(radiologyOrder);
        radiologyStudy.setDatePerformed(timeOfStudy);

        Encounter encounter = radiologyService.saveRadiologyStudy(radiologyStudy);

        assertThat(encounter.getPatient(), is(patient));
        assertThat(encounter.getEncounterType(), is(radiologyProperties.getRadiologyStudyEncounterType()));
        assertThat(encounter.getEncounterDatetime(), is(timeOfStudy));
        assertThat(encounter.getLocation(), is(emrApiProperties.getUnknownLocation()));
        assertThat(encounter.getProvidersByRole(radiologyProperties.getRadiologyTechnicianEncounterRole()).size(), is(1));
        assertThat(encounter.getProvidersByRole(radiologyProperties.getRadiologyTechnicianEncounterRole()).iterator().next(), is(emrApiProperties.getUnknownProvider()));

        assertThat(encounter.getObsAtTopLevel(false).size(), is(1));

        Obs radiologyStudyObsSet = encounter.getObsAtTopLevel(false).iterator().next();


        assertThat(radiologyStudyObsSet.getGroupMembers().size(), is(3));
        assertThat(radiologyStudyObsSet.getOrder().getAccessionNumber(), is("12345"));

        Obs accessionNumberObs = null;
        Obs procedureObs = null;
        Obs imagesAvailableObs = null;

        // hack, just reference the concepts by their ids in the test dataset
        for (Obs obs : radiologyStudyObsSet.getGroupMembers()) {
            if (obs.getConcept().getId() == 1004) {
                accessionNumberObs = obs;
            }
            if (obs.getConcept().getId() == 1003) {
                procedureObs  = obs;
            }
            if (obs.getConcept().getId() == 1005) {
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
    public void shouldSaveARadiologyReport() {

        Date timeOfStudy = new Date();

        // use patient demo database
        Patient patient = patientService.getPatient(6);

        // from radiologyServiceComponentTestDataset.xml
        Concept procedure = conceptService.getConcept(1001);
        Concept reportType = conceptService.getConcept(1009);
        RadiologyOrder radiologyOrder = radiologyService.getRadiologyOrderByAccessionNumber("12345");

        RadiologyReport radiologyReport = new RadiologyReport();
        radiologyReport.setPatient(patient);
        radiologyReport.setProcedure(procedure);
        radiologyReport.setReportType(reportType);
        radiologyReport.setReportBody("Some test report");
        radiologyReport.setAccessionNumber("12345");
        radiologyReport.setAssociatedRadiologyOrder(radiologyOrder);
        radiologyReport.setReportDate(timeOfStudy) ;

        Encounter encounter = radiologyService.saveRadiologyReport(radiologyReport);

        assertThat(encounter.getPatient(), is(patient));
        assertThat(encounter.getEncounterType(), is(radiologyProperties.getRadiologyReportEncounterType()));
        assertThat(encounter.getEncounterDatetime(), is(timeOfStudy));
        assertThat(encounter.getLocation(), is(emrApiProperties.getUnknownLocation()));
        assertThat(encounter.getProvidersByRole(radiologyProperties.getPrincipalResultsInterpreterEncounterRole()).size(), is(1));
        assertThat(encounter.getProvidersByRole(radiologyProperties.getPrincipalResultsInterpreterEncounterRole()).iterator().next(), is(emrApiProperties.getUnknownProvider()));

        assertThat(encounter.getObsAtTopLevel(false).size(), is(1));

        Obs radiologyStudyObsSet = encounter.getObsAtTopLevel(false).iterator().next();


        assertThat(radiologyStudyObsSet.getGroupMembers().size(), is(4));
        assertThat(radiologyStudyObsSet.getOrder().getAccessionNumber(), is("12345"));

        Obs accessionNumberObs = null;
        Obs procedureObs = null;
        Obs reportBodyObs = null;
        Obs reportTypeObs = null;

        // hack, just reference the concepts by their ids in the test dataset
        for (Obs obs : radiologyStudyObsSet.getGroupMembers()) {
            if (obs.getConcept().getId() == 1004) {
                accessionNumberObs = obs;
            }
            if (obs.getConcept().getId() == 1003) {
                procedureObs  = obs;
            }
            if (obs.getConcept().getId() == 1007) {
                reportTypeObs = obs;
            }
            if (obs.getConcept().getId() == 1008) {
                reportBodyObs = obs;
            }
        }

        assertNotNull(accessionNumberObs);
        assertNotNull(procedureObs);
        assertNotNull(reportTypeObs);
        assertNotNull(reportBodyObs);

        assertThat(accessionNumberObs.getValueText(), is("12345"));
        assertThat(procedureObs.getValueCoded(), is(procedure));
        assertThat(reportBodyObs.getValueText(), is("Some test report"));
        assertThat(reportTypeObs.getValueCoded(), is(reportType));

    }

}
