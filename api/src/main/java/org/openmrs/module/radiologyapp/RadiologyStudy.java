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
import org.openmrs.Patient;
import org.openmrs.Provider;

import java.util.Date;

public class RadiologyStudy {

    private RadiologyOrder associatedRadiologyOrder;

    private String accessionNumber;

    private Concept procedure;

    private Patient patient;

    private Provider technician;

    private Date datePerformed;

    private Location studyLocation;

    private Boolean imagesAvailable;

    public RadiologyOrder getAssociatedRadiologyOrder() {
        return associatedRadiologyOrder;
    }

    public void setAssociatedRadiologyOrder(RadiologyOrder associatedRadiologyOrder) {
        this.associatedRadiologyOrder = associatedRadiologyOrder;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public Concept getProcedure() {
        return procedure;
    }

    public void setProcedure(Concept procedure) {
        this.procedure = procedure;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Provider getTechnician() {
        return technician;
    }

    public void setTechnician(Provider technician) {
        this.technician = technician;
    }

    public Date getDatePerformed() {
        return datePerformed;
    }

    public void setDatePerformed(Date datePerformed) {
        this.datePerformed = datePerformed;
    }

    public Location getStudyLocation() {
        return studyLocation;
    }

    public void setStudyLocation(Location studyLocation) {
        this.studyLocation = studyLocation;
    }

    public Boolean isImagesAvailable() {
        return imagesAvailable;
    }

    public void setImagesAvailable(Boolean imagesAvailable) {
        this.imagesAvailable = imagesAvailable;
    }

}
