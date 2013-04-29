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
import org.openmrs.ConceptMap;
import org.openmrs.ConceptMapType;
import org.openmrs.ConceptName;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.ConceptSource;
import org.openmrs.api.ConceptService;

import java.util.Locale;

import static org.mockito.Mockito.when;

public abstract class BaseConceptSetTest {

    protected ConceptMapType sameAs;
    protected ConceptSource emrConceptSource;

    protected Concept setupConcept(ConceptService mockConceptService, String name, String mappingCode) {
        Concept concept = new Concept();
        concept.addName(new ConceptName(name, Locale.ENGLISH));
        concept.addConceptMapping(new ConceptMap(new ConceptReferenceTerm(emrConceptSource, mappingCode, null), sameAs));
        when(mockConceptService.getConceptByMapping(mappingCode, emrConceptSource.getName())).thenReturn(concept);
        return concept;
    }

}
