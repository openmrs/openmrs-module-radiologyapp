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
import org.openmrs.Location;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.Provider;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Requisition of one or more radiology studies (XRay, CT, Ultrasound), with associated metadata about the entire group
 */
public class RadiologyRequisition {

    private Patient patient;

    private Provider requestedBy;

    private String clinicalHistory;

    private Order.Urgency urgency;

    private Location examLocation;

    private Set<Concept> studies = new LinkedHashSet<Concept>();

    public Patient getPatient() {
        return patient;
    }

    public Provider getRequestedBy() {
        return requestedBy;
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

    public void setStudies(Set<Concept> studies) {
        this.studies = studies;
    }

    public void setRequestedBy(Provider requestedBy) {
        this.requestedBy = requestedBy;
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
}
