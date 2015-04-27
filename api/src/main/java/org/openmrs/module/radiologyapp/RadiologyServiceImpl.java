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
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.OrderContext;
import org.openmrs.api.OrderService;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.emrapi.EmrApiProperties;
import org.openmrs.module.emrapi.adt.exception.EncounterDateAfterVisitStopDateException;
import org.openmrs.module.emrapi.adt.exception.EncounterDateBeforeVisitStartDateException;
import org.openmrs.module.emrapi.db.EmrEncounterDAO;
import org.openmrs.module.emrapi.encounter.EncounterDomainWrapper;
import org.openmrs.module.radiologyapp.comparator.RadiologyReportByDataComparator;
import org.openmrs.module.radiologyapp.comparator.RadiologyStudyByDateComparator;
import org.openmrs.module.radiologyapp.db.RadiologyOrderDAO;
import org.openmrs.module.radiologyapp.exception.RadiologyAPIException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RadiologyServiceImpl  extends BaseOpenmrsService implements RadiologyService {

    private static final Log log = LogFactory.getLog(RadiologyServiceImpl.class);

    private EmrApiProperties emrApiProperties;

    private RadiologyProperties radiologyProperties;

    private EncounterService encounterService;

    private ConceptService conceptService;

    private OrderService orderService;

    private RadiologyOrderDAO radiologyOrderDAO;

    private EmrEncounterDAO emrEncounterDAO;

    @Transactional
    @Override
    public Encounter placeRadiologyRequisition(RadiologyRequisition requisition)
        throws EncounterDateBeforeVisitStartDateException, EncounterDateAfterVisitStopDateException {

        Encounter encounter = new Encounter();
        encounter.setEncounterType(radiologyProperties.getRadiologyOrderEncounterType());
        encounter.setProvider(emrApiProperties.getOrderingProviderEncounterRole(), requisition.getRequestedBy());
        encounter.setPatient(requisition.getPatient());
        encounter.setLocation(requisition.getRequestedFrom());
        encounter.setEncounterDatetime(requisition.getRequestedOn() != null ? requisition.getRequestedOn() : new Date());

        // add creatinine level if it has been specified
        if (requisition.getCreatinineLevel() != null) {
            Obs creatinineLevel = new Obs();
            creatinineLevel.setConcept(radiologyProperties.getCreatinineLevelConcept());
            creatinineLevel.setValueNumeric(requisition.getCreatinineLevel());
            if (requisition.getCreatinineTestDate() != null) {
                creatinineLevel.setObsDatetime(requisition.getCreatinineTestDate());
            }
            encounter.addObs(creatinineLevel);
        }

        // save the encounter
        encounterService.saveEncounter(encounter);

        if (requisition.getVisit() != null) {
            new EncounterDomainWrapper(encounter).attachToVisit(requisition.getVisit());
        }

        OrderContext orderContext = new OrderContext();
        orderContext.setOrderType(radiologyProperties.getRadiologyTestOrderType());

        // now add the orders
        for (Concept study : requisition.getStudies()) {
            RadiologyOrder order = new RadiologyOrder();
            order.setExamLocation(requisition.getExamLocation());
            order.setClinicalHistory(requisition.getClinicalHistory());
            order.setConcept(study);
            order.setUrgency(requisition.getUrgency());
            order.setDateActivated(encounter.getEncounterDatetime());  // note that the attachToVisit method may have altered this date to match the visit
            order.setOrderType(radiologyProperties.getRadiologyTestOrderType());
            order.setCareSetting(radiologyProperties.getRadiologyCareSetting());  // currently only a single care setting support, defined by emr.radiologyCareSetting global property
            order.setPatient(requisition.getPatient());
            order.setOrderer(requisition.getRequestedBy());
            encounter.addOrder(order);
            orderService.saveOrder(order, orderContext);
        }

        return encounter;
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
    // we synchronize this method so that we can verify that we never create two studies with the same order number
    public synchronized Encounter saveRadiologyStudy(RadiologyStudy radiologyStudy) {

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
    public RadiologyOrder getRadiologyOrderByOrderNumber(String orderNumber) {
        return radiologyOrderDAO.getRadiologyOrderByOrderNumber(orderNumber);
    }

    @Transactional(readOnly = true)
    @Override
    public RadiologyStudy getRadiologyStudyByOrderNumber(String orderNumber) {

        RadiologyStudy radiologyStudy = null;

        // first search for any radiology study encounters
        List<Encounter> radiologyStudyEncounters =
                emrEncounterDAO.getEncountersByObsValueText(new RadiologyStudyConceptSet(conceptService).getOrderNumberConcept(),
                        orderNumber, radiologyProperties.getRadiologyStudyEncounterType(), false);

        if (radiologyStudyEncounters != null && radiologyStudyEncounters.size() > 0) {

            // note that also the API should prevent two radiology study encounters with the same order number from being created,
            // if we do encounter this issue, we log an error, but we don't throw an exception and instead just return the first study
            if (radiologyStudyEncounters.size() > 1) {
                log.error("More than one Radiology Study Encounter with order number " + orderNumber);
            }

            radiologyStudy = convertEncounterToRadiologyStudy(radiologyStudyEncounters.get(0));
        }
        else {

            // if we don't find an actual radiology study encounter, see if we can derive information from any reports
            // with the same order number
            List<RadiologyReport> radiologyReports = getRadiologyReportsByOrderNumber(orderNumber);

            if (radiologyReports != null && radiologyReports.size() > 0) {
                radiologyStudy = deriveRadiologyStudyFromRadiologyReports(radiologyReports);
            }
        }

        return radiologyStudy;
    }

    @Transactional(readOnly = true)
    @Override
    public List<RadiologyReport> getRadiologyReportsByOrderNumber(String orderNumber) {

        List<Encounter> radiologyReportEncounters =
                emrEncounterDAO.getEncountersByObsValueText(new RadiologyReportConceptSet(conceptService).getOrderNumberConcept(),
                        orderNumber, radiologyProperties.getRadiologyReportEncounterType(), false);

        List<RadiologyReport> radiologyReports = new ArrayList<RadiologyReport>();

        if (radiologyReportEncounters != null) {
            for (Encounter radiologyReportEncounter : radiologyReportEncounters) {
                radiologyReports.add(convertEncounterToRadiologyReport(radiologyReportEncounter));
            }
        }

        Collections.sort(radiologyReports, new RadiologyReportByDataComparator());
        return radiologyReports;
    }

    @Transactional(readOnly = true)
    @Override
    public List<RadiologyStudy> getRadiologyStudiesForPatient(Patient patient) {

        RadiologyReportConceptSet radiologyReportConceptSet = new RadiologyReportConceptSet(conceptService);

        // first fetch all the radiology study encounters for this patient
        List<Encounter> radiologyStudyEncounters = encounterService.getEncounters(patient, null, null, null, null,
                Collections.singletonList(radiologyProperties.getRadiologyStudyEncounterType()),
                null, null, null, false);

        List<RadiologyStudy> radiologyStudies = new ArrayList<RadiologyStudy>();
        Set<String> orderNumbersOfExistingRadiologyStudyEncounters = new HashSet<String>();

        if (radiologyStudyEncounters != null) {
            for (Encounter encounter : radiologyStudyEncounters) {
                RadiologyStudy radiologyStudy = convertEncounterToRadiologyStudy(encounter);
                radiologyStudies.add(radiologyStudy);
                orderNumbersOfExistingRadiologyStudyEncounters.add(radiologyStudy.getOrderNumber());
            }
        }

        // now find any "orphaned" reports" and make transient radiology studies to represent them
        List<Encounter> radiologyReportEncounters = encounterService.getEncounters(patient, null, null, null, null,
                Collections.singletonList(radiologyProperties.getRadiologyReportEncounterType()),
                null, null, null, false);

        Map<String, List<RadiologyReport>> radiologyReportsByOrderNumber = new HashMap<String, List<RadiologyReport>>();

        for (Encounter radiologyReportEncounter : radiologyReportEncounters) {
            String orderNumber = radiologyReportConceptSet.getOrderNumberFromEncounter(radiologyReportEncounter);

            if (!orderNumbersOfExistingRadiologyStudyEncounters.contains(orderNumber)) {
                if (!radiologyReportsByOrderNumber.containsKey(orderNumber)) {
                    radiologyReportsByOrderNumber.put(orderNumber, new ArrayList<RadiologyReport>());
                }
                radiologyReportsByOrderNumber.get(orderNumber).add(convertEncounterToRadiologyReport(radiologyReportEncounter));
            }
        }

        for (List<RadiologyReport> radiologyReports : radiologyReportsByOrderNumber.values()) {
            radiologyStudies.add(deriveRadiologyStudyFromRadiologyReports(radiologyReports));
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
                log.warn("Multiple technicians listed for radiology study encounter " + encounter);
            }
            radiologyStudy.setTechnician(technicians.iterator().next());
        }

        RadiologyStudyConceptSet radiologyStudyConceptSet = new RadiologyStudyConceptSet(conceptService);
        radiologyStudy.setProcedure(radiologyStudyConceptSet.getProcedureFromEncounter(encounter));
        radiologyStudy.setImagesAvailable(radiologyStudyConceptSet.getImagesAvailableFromEncounter(encounter));
        radiologyStudy.setOrderNumber(radiologyStudyConceptSet.getOrderNumberFromEncounter(encounter));

        return radiologyStudy;
    }

    private RadiologyReport convertEncounterToRadiologyReport(Encounter encounter) {

        RadiologyReport radiologyReport = new RadiologyReport();
        radiologyReport.setPatient(encounter.getPatient());
        radiologyReport.setReportDate(encounter.getEncounterDatetime());
        radiologyReport.setReportLocation(encounter.getLocation());

        Set<Provider> resultsInterpreters = encounter.getProvidersByRole(radiologyProperties.getPrincipalResultsInterpreterEncounterRole());
        if (resultsInterpreters != null && !resultsInterpreters.isEmpty()) {
            if (resultsInterpreters.size() > 1) {
                log.warn("Multiple prinicipal results interpreters listed for radiology report encounter " + encounter);
            }
            radiologyReport.setPrincipalResultsInterpreter(resultsInterpreters.iterator().next());
        }

        RadiologyReportConceptSet radiologyReportConceptSet = new RadiologyReportConceptSet(conceptService);
        radiologyReport.setReportType(radiologyReportConceptSet.getReportTypeFromEncounter(encounter));
        radiologyReport.setReportBody(radiologyReportConceptSet.getReportBodyFromEncounter(encounter));
        radiologyReport.setOrderNumber(radiologyReportConceptSet.getOrderNumberFromEncounter(encounter));
        radiologyReport.setProcedure(radiologyReportConceptSet.getProcedureFromEncounter(encounter));

        return radiologyReport;

    }

    private RadiologyStudy deriveRadiologyStudyFromRadiologyReports(List<RadiologyReport> radiologyReports) {

        RadiologyStudy radiologyStudy = new RadiologyStudy();

        // just pull the data from the most recent report
        radiologyStudy.setProcedure(radiologyReports.get(0).getProcedure());
        radiologyStudy.setPatient(radiologyReports.get(0).getPatient());
        radiologyStudy.setOrderNumber(radiologyReports.get(0).getOrderNumber());
        radiologyStudy.setAssociatedRadiologyOrder(radiologyReports.get(0).getAssociatedRadiologyOrder());

        // set the date performed to the date of the earliest report
        // TODO: (a bit of a hack, not entirely accurate)
        radiologyStudy.setDatePerformed(radiologyReports.get(radiologyReports.size() - 1).getReportDate());

        return radiologyStudy;
    }

    private void validate(RadiologyReport radiologyReport) {

        // TODO: perhaps move these into an external validator?

        if (StringUtils.isBlank(radiologyReport.getOrderNumber())) {
            throw new RadiologyAPIException("order number must be specified when saving Radiology Report. Patient: "
                    + radiologyReport.getPatient() + ", Procedure: " + radiologyReport.getProcedure());
        }

        if (radiologyReport.getReportDate() == null) {
            throw new RadiologyAPIException("Date performed must be specified when saving Radiology Report. Patient: "
                    + radiologyReport.getPatient() + ", order Number: " + radiologyReport.getOrderNumber());
        }

        if (radiologyReport.getPatient() == null) {
            throw new RadiologyAPIException("Patient must be specified when saving Radiology Report. order Number: "
                    + radiologyReport.getOrderNumber());
        }

    }

    private void validate(RadiologyStudy radiologyStudy) {

        // TODO: perhaps move these into an external validator?

        if (StringUtils.isBlank(radiologyStudy.getOrderNumber())) {
            throw new RadiologyAPIException("order number must be specified when saving Radiology Study. Patient: "
                    + radiologyStudy.getPatient() + ", Procedure: " + radiologyStudy.getProcedure());
        }

        if (radiologyStudy.getDatePerformed() == null) {
            throw new RadiologyAPIException("Date performed must be specified when saving Radiology Study. Patient: "
                    + radiologyStudy.getPatient() + ", order Number: " + radiologyStudy.getOrderNumber());
        }

        if (radiologyStudy.getPatient() == null) {
            throw new RadiologyAPIException("Patient must be specified when saving Radiology Study. order Number: "
                    + radiologyStudy.getOrderNumber());
        }

        // make sure no existing study with the same order number
        List<Encounter> radiologyStudyEncounters = emrEncounterDAO.getEncountersByObsValueText(new RadiologyStudyConceptSet(conceptService).getOrderNumberConcept(),
                radiologyStudy.getOrderNumber(), radiologyProperties.getRadiologyStudyEncounterType(), false);

        if (radiologyStudyEncounters != null && radiologyStudyEncounters.size() > 0) {
            throw new RadiologyAPIException("A Radiology Study already exists with order number " + radiologyStudy.getOrderNumber());
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

    public void setConceptService(ConceptService conceptService) {
        this.conceptService = conceptService;
    }

    public void setOrderService(OrderService orderService) {
        this.orderService = orderService;
    }

    public void setRadiologyOrderDAO(RadiologyOrderDAO radiologyOrderDAO) {
        this.radiologyOrderDAO = radiologyOrderDAO;
    }

    public void setEmrEncounterDAO(EmrEncounterDAO emrEncounterDAO) {
        this.emrEncounterDAO = emrEncounterDAO;
    }
}
