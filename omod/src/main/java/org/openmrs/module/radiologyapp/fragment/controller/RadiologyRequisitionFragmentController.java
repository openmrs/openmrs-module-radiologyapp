package org.openmrs.module.radiologyapp.fragment.controller;/*
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


import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.emr.EmrConstants;
import org.openmrs.module.emr.EmrContext;
import org.openmrs.module.radiologyapp.RadiologyRequisition;
import org.openmrs.module.radiologyapp.RadiologyService;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.BindParams;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.action.FragmentActionResult;
import org.openmrs.ui.framework.fragment.action.SuccessResult;

import javax.servlet.http.HttpServletRequest;

public class RadiologyRequisitionFragmentController {

    public FragmentActionResult orderXray(@BindParams RadiologyRequisition requisition,
                                          EmrContext emrContext,
                                          @SpringBean RadiologyService radiologyService,
                                          @SpringBean("messageSourceService") MessageSourceService messageSourceService,
                                          UiUtils ui, HttpServletRequest request) {
        if (requisition.getStudies().size() == 0) {
            throw new IllegalArgumentException(ui.message("radiologyapp.orderXray.noStudiesSelected"));
        }

        radiologyService.placeRadiologyRequisition(emrContext, requisition);

        request.getSession().setAttribute(EmrConstants.SESSION_ATTRIBUTE_INFO_MESSAGE,
                messageSourceService.getMessage("radiologyapp.task.orderXray.success"));

        request.getSession().setAttribute(EmrConstants.SESSION_ATTRIBUTE_TOAST_MESSAGE, "true");

        return new SuccessResult();
    }

}
