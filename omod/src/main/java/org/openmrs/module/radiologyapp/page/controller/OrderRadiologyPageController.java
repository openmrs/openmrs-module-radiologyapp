package org.openmrs.module.radiologyapp.page.controller;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.Visit;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.emr.EmrContext;
import org.openmrs.module.emr.api.EmrService;
import org.openmrs.module.emrapi.visit.VisitDomainWrapper;
import org.openmrs.module.radiologyapp.RadiologyConstants;
import org.openmrs.module.radiologyapp.RadiologyProperties;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.openmrs.ui.util.ByFormattedObjectComparator;
import org.openmrs.util.ProviderByPersonNameComparator;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderRadiologyPageController {

    public void controller(@RequestParam("visitId") Visit visit,
                           @RequestParam("patientId") Patient patient,
                           @RequestParam("modality") String modality,
                           @SpringBean("radiologyProperties") RadiologyProperties radiologyProperties,
                           @SpringBean("emrService") EmrService emrService,
                           @SpringBean("providerService") ProviderService providerService,
                           EmrContext emrContext,
                           UiUtils ui,
                           PageModel model) {

        // default to x-ray
        if (StringUtils.isBlank(modality)) {
            modality = RadiologyConstants.XRAY_MODALITY_CODE;
        }

        VisitDomainWrapper visitWrapper = new VisitDomainWrapper(visit);

        // TODO better handle the case where this is multiple providers for a single user
        Collection<Provider> providers = Context.getProviderService().getProvidersByPerson(Context.getAuthenticatedUser().getPerson());
        model.addAttribute("currentProvider", providers.iterator().next());

        model.addAttribute("xrayModalityCode", RadiologyConstants.XRAY_MODALITY_CODE);
        model.addAttribute("ctScanModalityCode", RadiologyConstants.CT_SCAN_MODALITY_CODE);

        model.addAttribute("portableLocations", ui.toJson(getPortableLocations(emrService, ui)));
        model.addAttribute("patient", patient);
        model.addAttribute("modality", modality.toUpperCase());
        model.addAttribute("providers", getProviders(providerService));
        model.addAttribute("visit", visitWrapper.getVisit());

        // note that the underlying date widget takes care of stripping out the time component for us
        model.addAttribute("minOrderDate", visitWrapper.getStartDatetime());
        model.addAttribute("maxOrderDate", visitWrapper.getEncounterStopDateRange());
        model.addAttribute("defaultOrderDate", emrContext.getActiveVisit() != null && emrContext.getActiveVisit().equals(visitWrapper.getVisit()) ?
                        new Date() : visitWrapper.getStartDatetime());

        if (modality.equalsIgnoreCase(RadiologyConstants.XRAY_MODALITY_CODE)) {
            model.addAttribute("orderables", ui.toJson(getOrderables(radiologyProperties.getXrayOrderablesConcept(), Context.getLocale())));
        }
        else if (modality.equalsIgnoreCase(RadiologyConstants.CT_SCAN_MODALITY_CODE)) {
            model.addAttribute("orderables", ui.toJson(getOrderables(radiologyProperties.getCTScanOrderablesConcept(), Context.getLocale())));
        }
        else if (modality.equalsIgnoreCase(RadiologyConstants.ULTRASOUND_MODALITY_CODE)) {
            model.addAttribute("orderables", ui.toJson(getOrderables(radiologyProperties.getUltrasoundOrderablesConcept(), Context.getLocale())));
        }
        else {
            throw new IllegalArgumentException("Invalid Modality: " + modality);
        }
    }

    private List<SimpleObject> getPortableLocations(EmrService emrService, final UiUtils ui) {
        List<SimpleObject> items = new ArrayList<SimpleObject>();
        List<Location> locations = emrService.getLoginLocations(); // TODO: is login locations really the right thing here?

        // sort objects by localized name
        Collections.sort(locations, new ByFormattedObjectComparator(ui));

        for (Location location: locations) {
            SimpleObject item = new SimpleObject();
            item.put("value", location.getLocationId());
            item.put("label", ui.format(location));
            items.add(item);
        }
        return items;

    }

    private List<SimpleObject> getOrderables(Concept orderablesSet, Locale locale) {
        List<SimpleObject> items = new ArrayList<SimpleObject>();
        for (Concept concept : orderablesSet.getSetMembers()) {
            SimpleObject item = new SimpleObject();
            item.put("value", concept.getId());

            // TODO: this should really be fully specified name based on local
            item.put("label", concept.getName().getName());
            items.add(item);
        }
        return items;
    }

    private List<SimpleObject> getProviders(ProviderService providerService) {
        List<SimpleObject> items = new ArrayList<SimpleObject>();
        List<Provider> providers = providerService.getAllProviders(false);

        Collections.sort(providers, new ProviderByPersonNameComparator());

        for (Provider provider : providers) {
            SimpleObject item = new SimpleObject();
            item.put("value", provider.getProviderId());
            item.put("label", provider.getName());
            items.add(item);
        }

        return items;
    }
}
