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

package org.openmrs.module.radiologyapp.fragment.controller;

import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.appui.AppUiConstants;
import org.openmrs.module.appui.UiSessionContext;
import org.openmrs.module.radiologyapp.RadiologyRequisition;
import org.openmrs.module.radiologyapp.RadiologyService;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.BindParams;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.action.FailureResult;
import org.openmrs.ui.framework.fragment.action.FragmentActionResult;
import org.openmrs.ui.framework.fragment.action.SuccessResult;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

public class RadiologyRequisitionFragmentController {

    public FragmentActionResult orderRadiology(@BindParams RadiologyRequisition requisition,
                                               @RequestParam("modality") String modality,
                                               UiSessionContext uiSessionContext,
                                               @SpringBean RadiologyService radiologyService,
                                               @SpringBean("messageSourceService") MessageSourceService messageSourceService,
                                               UiUtils ui, HttpServletRequest request) {

        if (requisition.getStudies().size() == 0) {
            throw new IllegalArgumentException(ui.message("radiologyapp.order.noStudiesSelected"));
        }

        // set provider and location if not specified
        if (requisition.getRequestedBy() == null) {
            requisition.setRequestedBy(uiSessionContext.getCurrentProvider());
        }
        if (requisition.getRequestedFrom() == null) {
            requisition.setRequestedFrom(uiSessionContext.getSessionLocation());
        }

        try {
            radiologyService.placeRadiologyRequisition(requisition);
        }
        catch (Exception e) {
            // TODO make this more user-friendly (but we never should get here)
            return new FailureResult(e.getLocalizedMessage());
        }

        request.getSession().setAttribute(AppUiConstants.SESSION_ATTRIBUTE_INFO_MESSAGE,
                messageSourceService.getMessage("radiologyapp.task.order." + modality.toUpperCase() + ".success"));

        request.getSession().setAttribute(AppUiConstants.SESSION_ATTRIBUTE_TOAST_MESSAGE, "true");

        return new SuccessResult();
    }

}
