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

package org.openmrs.module.radiologyapp.importer;

import lombok.Data;

import java.util.Date;

/**
 * Describes a final or correction radiology report produced in an external system (e.g. a PACS), to be imported against
 * an existing order
 */
@Data
public class RadiologyReportImportRequest {

    private String accessionNumber;

    private Date reportDate;

    // either "final" or "correction"
    private String reportType;

    private String reportBody;

    // nullable
    private String radiologistProviderUuid;

    // nullable; used only if the study has to be created
    private Date datePerformed;

    private String patientIdentifierTypeUuid;

    private String patientIdentifier;
}
