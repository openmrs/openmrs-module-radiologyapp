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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.emr.EmrContext;
import org.openmrs.module.emr.order.EmrOrderService;
import org.openmrs.module.emrapi.EmrApiProperties;
import org.openmrs.module.emrapi.db.EmrApiDAO;
import org.openmrs.module.emrapi.visit.VisitDomainWrapper;
import org.openmrs.module.radiologyapp.comparator.RadiologyStudyByDateComparator;
import org.openmrs.module.radiologyapp.db.RadiologyOrderDAO;
import org.openmrs.module.radiologyapp.exception.RadiologyAPIException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class RadiologyServiceImpl  extends BaseOpenmrsService implements RadiologyService {

    private static final Log log = LogFactory.getLog(RadiologyServiceImpl.class);

    private EmrApiProperties emrApiProperties;

    private RadiologyProperties radiologyProperties;

    private EmrOrderService emrOrderService;

    private EncounterService encounterService;

    private ConceptService conceptService;

    private RadiologyOrderDAO radiologyOrderDAO;

    private EmrApiDAO emrApiDAO;

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

    @Transactional
    @Override
    public Encounter saveRadiologyReport(RadiologyReport radiologyReport) {

        validate(radiologyReport);

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

    @Transactional
    @Override
    public Encounter saveRadiologyStudy(RadiologyStudy radiologyStudy) {

        validate(radiologyStudy);

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

    @Transactional(readOnly = true)
    @Override
    public RadiologyOrder getRadiologyOrderByAccessionNumber(String accessionNumber) {
        return radiologyOrderDAO.getRadiologyOrderByAccessionNumber(accessionNumber);
    }

    @Transactional(readOnly = true)
    @Override
    public RadiologyStudy getRadiologyStudyByAccessionNumber(String accessionNumber) {

        List<Encounter> radiologyStudyEncounters =
                emrApiDAO.getEncountersByObsValueText(new RadiologyStudyConceptSet(conceptService).getAccessionNumberConcept(),
                accessionNumber, radiologyProperties.getRadiologyStudyEncounterType(), false);

        if (radiologyStudyEncounters == null || radiologyStudyEncounters.size() == 0) {
            return null;
        }

        // note that also the API should prevent two radiology study encounters with the same accession number from being created,
        // if we do encounter this issue, we log an error, but we don't throw an exception and instead just return the first study
        if (radiologyStudyEncounters.size() > 1) {
            log.error("More than one Radiology Study Encounter with accession number " + accessionNumber);
        }

        return convertEncounterToRadiologyStudy(radiologyStudyEncounters.get(0));
    }

    @Transactional(readOnly = true)
    @Override
    public List<RadiologyStudy> getRadiologyStudiesForPatient(Patient patient) {

        // first fetch all the radiology study encounters for this patient
        List<Encounter> encounters = encounterService.getEncounters(patient, null, null, null, null,
                Collections.singletonList(radiologyProperties.getRadiologyStudyEncounterType()),
                null, null, null, false);

        // return an empty list if no matching encounters
        if (encounters == null || encounters.size() == 0) {
            return new ArrayList<RadiologyStudy>();
        }

        List<RadiologyStudy> radiologyStudies = new ArrayList<RadiologyStudy>();

        for (Encounter encounter : encounters) {
            radiologyStudies.add(convertEncounterToRadiologyStudy(encounter));
        }

        Collections.sort(radiologyStudies, new RadiologyStudyByDateComparator());
        return radiologyStudies;
    }

    private RadiologyStudy convertEncounterToRadiologyStudy(Encounter encounter) {

        RadiologyStudy radiologyStudy = new RadiologyStudy();
        radiologyStudy.setPatient(encounter.getPatient());
        radiologyStudy.setDatePerformed(encounter.getEncounterDatetime());
        radiologyStudy.setStudyLocation(encounter.getLocation());

        Set<Provider> technicians = encounter.getProvidersByRole(radiologyProperties.getRadiologyTechnicianEncounterRole());
        if (technicians != null && !technicians.isEmpty()) {
            if (technicians.size() > 1) {
                log.warn("Multiple technicians listed for radiology encounter encounter " + encounter);
            }
            radiologyStudy.setTechnician(technicians.iterator().next());
        }

        RadiologyStudyConceptSet radiologyStudyConceptSet = new RadiologyStudyConceptSet(conceptService);
        radiologyStudy.setProcedure(radiologyStudyConceptSet.getProcedureFromEncounter(encounter));
        radiologyStudy.setImagesAvailable(radiologyStudyConceptSet.getImagesAvailableFromEncounter(encounter));
        radiologyStudy.setAccessionNumber(radiologyStudyConceptSet.getAccessionNumberFromEncounter(encounter));

        return radiologyStudy;
    }

    private void validate(RadiologyReport radiologyReport) {

        // TODO: perhaps move these into an external validator?

        if (StringUtils.isBlank(radiologyReport.getAccessionNumber())) {
            throw new RadiologyAPIException("Accession number must be specified when saving Radiology Report. Patient: "
                    + radiologyReport.getPatient() + ", Procedure: " + radiologyReport.getProcedure());
        }

        if (radiologyReport.getReportDate() == null) {
            throw new RadiologyAPIException("Date performed must be specified when saving Radiology Report. Patient: "
                    + radiologyReport.getPatient() + ", Accession Number: " + radiologyReport.getAccessionNumber());
        }

        if (radiologyReport.getPatient() == null) {
            throw new RadiologyAPIException("Patient must be specified when saving Radiology Report. Accession Number: "
                    + radiologyReport.getAccessionNumber());
        }

    }

    private void validate(RadiologyStudy radiologyStudy) {

        // TODO: perhaps move these into an external validator?

        if (StringUtils.isBlank(radiologyStudy.getAccessionNumber())) {
            throw new RadiologyAPIException("Accession number must be specified when saving Radiology Study. Patient: "
                    + radiologyStudy.getPatient() + ", Procedure: " + radiologyStudy.getProcedure());
        }

        if (radiologyStudy.getDatePerformed() == null) {
            throw new RadiologyAPIException("Date performed must be specified when saving Radiology Study. Patient: "
                    + radiologyStudy.getPatient() + ", Accession Number: " + radiologyStudy.getAccessionNumber());
        }

        if (radiologyStudy.getPatient() == null) {
            throw new RadiologyAPIException("Patient must be specified when saving Radiology Study. Accession Number: "
                    + radiologyStudy.getAccessionNumber());
        }

        // make sure no existing study with the same accession number
        List<Encounter> radiologyStudyEncounters = emrApiDAO.getEncountersByObsValueText(new RadiologyStudyConceptSet(conceptService).getAccessionNumberConcept(),
                radiologyStudy.getAccessionNumber(), radiologyProperties.getRadiologyStudyEncounterType(), false);

        if (radiologyStudyEncounters != null && radiologyStudyEncounters.size() > 0) {
            throw new RadiologyAPIException("A Radiology Study already exists with accession number " + radiologyStudy.getAccessionNumber());
        }

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

    public void setEmrApiDAO(EmrApiDAO emrApiDAO) {
        this.emrApiDAO = emrApiDAO;
    }
}
