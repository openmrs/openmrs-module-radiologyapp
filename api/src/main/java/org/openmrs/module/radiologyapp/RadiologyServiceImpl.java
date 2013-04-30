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

import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.emr.EmrContext;
import org.openmrs.module.emr.EmrProperties;
import org.openmrs.module.emr.order.EmrOrderService;
import org.openmrs.module.emrapi.EmrApiProperties;
import org.openmrs.module.emrapi.visit.VisitDomainWrapper;
import org.openmrs.module.radiologyapp.db.RadiologyOrderDAO;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

public class RadiologyServiceImpl  extends BaseOpenmrsService implements RadiologyService {

    private EmrApiProperties emrApiProperties;

    private RadiologyProperties radiologyProperties;

    private EmrOrderService emrOrderService;

    private EncounterService encounterService;

    private ConceptService conceptService;

    private RadiologyOrderDAO radiologyOrderDAO;

    @Transactional
    @Override
    public Encounter placeRadiologyRequisition(EmrContext emrContext, RadiologyRequisition requisition) {
        Encounter encounter = new Encounter();
        encounter.setEncounterType(radiologyProperties.getRadiologyOrderEncounterType());
        encounter.setProvider(emrApiProperties.getOrderingProviderEncounterRole(), requisition.getRequestedBy());
        encounter.setPatient(requisition.getPatient());
        encounter.setLocation(emrContext.getSessionLocation());
        VisitDomainWrapper activeVisitSummary = emrContext.getActiveVisit();
        if (activeVisitSummary != null) {
            encounter.setVisit(activeVisitSummary.getVisit());
        }

        Date currentDatetime = new Date();
        encounter.setEncounterDatetime(currentDatetime);
        encounter.setDateCreated(currentDatetime);

        for (Concept study : requisition.getStudies()) {
            RadiologyOrder order = new RadiologyOrder();
            order.setExamLocation(requisition.getExamLocation());
            order.setClinicalHistory(requisition.getClinicalHistory());
            order.setConcept(study);
            order.setUrgency(requisition.getUrgency());
            order.setStartDate(new Date());
            order.setOrderType(radiologyProperties.getRadiologyTestOrderType());
            order.setPatient(requisition.getPatient());
            encounter.addOrder(order);
        }

        // since accession numbers are determined by primary key, we need to first save the encounter
        encounterService.saveEncounter(encounter);
        assignAccessionNumbersToOrders(encounter);
        return encounterService.saveEncounter(encounter);
    }

    private void assignAccessionNumbersToOrders(Encounter encounter) {
        for (Order order : encounter.getOrders()) {
            emrOrderService.ensureAccessionNumberAssignedTo(order);
        }
    }

    @Override
    public Encounter saveRadiologyReport(RadiologyReport radiologyReport) {

        // TODO: add a validator
        // TODO: or just validate that there is an accession number, report date?

        Encounter encounter = new Encounter();
        encounter.setEncounterType(radiologyProperties.getRadiologyReportEncounterType());
        encounter.setEncounterDatetime(radiologyReport.getReportDate());
        encounter.setLocation(radiologyReport.getReportLocation() != null ?
                radiologyReport.getReportLocation() : emrApiProperties.getUnknownLocation());
        encounter.setPatient(radiologyReport.getPatient());
        encounter.addProvider(radiologyProperties.getPrincipalResultsInterpreterEncounterRole(),
                radiologyReport.getPrincipalResultsInterpreter() != null ?  radiologyReport.getPrincipalResultsInterpreter() : emrApiProperties.getUnknownProvider());

        RadiologyReportConceptSet radiologyReportConceptSet = new RadiologyReportConceptSet(conceptService);
        encounter.addObs(radiologyReportConceptSet.buildRadiologyReportObsGroup(radiologyReport));

        return encounterService.saveEncounter(encounter);
    }

    @Override
    public Encounter saveRadiologyStudy(RadiologyStudy radiologyStudy) {

        // TODO: add a validator?
        // TODO: or just validate that there is an accession number, report date?

        Encounter encounter = new Encounter();
        encounter.setEncounterType(radiologyProperties.getRadiologyStudyEncounterType());
        encounter.setEncounterDatetime(radiologyStudy.getDatePerformed());
        encounter.setLocation(radiologyStudy.getStudyLocation() != null ?
                radiologyStudy.getStudyLocation() : emrApiProperties.getUnknownLocation());
        encounter.setPatient(radiologyStudy.getPatient());
        encounter.addProvider(radiologyProperties.getRadiologyTechnicianEncounterRole(),
                radiologyStudy.getTechnician() != null ? radiologyStudy.getTechnician() : emrApiProperties.getUnknownProvider());

        RadiologyStudyConceptSet radiologyStudyConceptSet = new RadiologyStudyConceptSet(conceptService);
        encounter.addObs(radiologyStudyConceptSet.buildRadiologyStudyObsGroup(radiologyStudy));

        return encounterService.saveEncounter(encounter);
    }

    @Override
    public RadiologyOrder getRadiologyOrderByAccessionNumber(String accessionNumber) {
        return radiologyOrderDAO.getRadiologyOrderByAccessionNumber(accessionNumber);
    }

    public void setEmrApiProperties(EmrApiProperties emrApiProperties) {
        this.emrApiProperties = emrApiProperties;
    }

    public void setRadiologyProperties(RadiologyProperties radiologyProperties) {
        this.radiologyProperties = radiologyProperties;
    }

    public void setEncounterService(EncounterService encounterService) {
        this.encounterService = encounterService;
    }

    public void setEmrOrderService(EmrOrderService emrOrderService) {
        this.emrOrderService = emrOrderService;
    }

    public void setConceptService(ConceptService conceptService) {
        this.conceptService = conceptService;
    }

    public void setRadiologyOrderDAO(RadiologyOrderDAO radiologyOrderDAO) {
        this.radiologyOrderDAO = radiologyOrderDAO;
    }
}
