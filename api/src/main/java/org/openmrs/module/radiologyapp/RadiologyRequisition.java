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

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Set;

import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.Visit;

/**
 * Requisition of one or more radiology studies (XRay, CT, Ultrasound), with associated metadata about the entire group
 */
public class RadiologyRequisition {

    private Patient patient;

    private Provider requestedBy;

    private Location requestedFrom;

    private Date requestedOn;

    private String clinicalHistory;

    private Order.Urgency urgency;

    private Location examLocation;

    private Set<Concept> studies = new LinkedHashSet<Concept>();

    private Visit visit;

    private Double creatinineLevel;

    private Date creatinineTestDate;

    public Patient getPatient() {
        return patient;
    }

    public Provider getRequestedBy() {
        return requestedBy;
    }

    public Location getRequestedFrom() {
        return requestedFrom;
    }

    public Date getRequestedOn() {
        return requestedOn;
    }

    public String getClinicalHistory() {
        return clinicalHistory;
    }

    public Order.Urgency getUrgency() {
        return urgency;
    }

    public Location getExamLocation() {
        return examLocation;
    }

    public Set<Concept> getStudies() {
        return studies;
    }

    public Visit getVisit() {
        return visit;
    }

    public void setStudies(Set<Concept> studies) {
        this.studies = studies;
    }

    public void setRequestedBy(Provider requestedBy) {
        this.requestedBy = requestedBy;
    }

    public void setRequestedFrom(Location requestedFrom) {
        this.requestedFrom = requestedFrom;
    }

    public void setRequestedOn(Date requestedOn) {
        this.requestedOn = requestedOn;
    }

    public void setClinicalHistory(String clinicalHistory) {
        this.clinicalHistory = clinicalHistory;
    }

    public void addStudy(Concept orderable) {
        studies.add(orderable);
    }

    public void setUrgency(Order.Urgency urgency) {
        this.urgency = urgency;
    }

    public void setExamLocation(Location examLocation) {
        this.examLocation = examLocation;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public void setVisit(Visit visit) {
        this.visit = visit;
    }

    public Double getCreatinineLevel() {
        return creatinineLevel;
    }

    public void setCreatinineLevel(Double creatinineLevel) {
        this.creatinineLevel = creatinineLevel;
    }

    public Date getCreatinineTestDate() {
        return creatinineTestDate;
    }

    public void setCreatinineTestDate(Date creatinineTestDate) {
        this.creatinineTestDate = creatinineTestDate;
    }
}
