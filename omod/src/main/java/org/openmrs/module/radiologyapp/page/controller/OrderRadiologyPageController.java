package org.openmrs.module.radiologyapp.page.controller;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.Visit;
import org.openmrs.api.LocationService;
import org.openmrs.api.ProviderService;
import org.openmrs.api.context.Context;
import org.openmrs.module.appframework.feature.FeatureToggleProperties;
import org.openmrs.module.appui.UiSessionContext;
import org.openmrs.module.coreapps.CoreAppsProperties;
import org.openmrs.module.emrapi.EmrApiProperties;
import org.openmrs.module.emrapi.adt.AdtService;
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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class OrderRadiologyPageController {

    public void controller(@RequestParam("visitId") Visit visit,
                           @RequestParam("patientId") Patient patient,
                           @RequestParam("modality") String modality,
                           @RequestParam(value = "returnUrl", required = false) String returnUrl,
                           @SpringBean("radiologyProperties") RadiologyProperties radiologyProperties,
                           @SpringBean("providerService") ProviderService providerService,
                           @SpringBean("locationService") LocationService locationService,
                           @SpringBean("emrApiProperties") EmrApiProperties emrApiProperties,
                           @SpringBean("coreAppsProperties") CoreAppsProperties coreAppsProperties,
                           @SpringBean("featureToggles") FeatureToggleProperties featureToggles,
                           @SpringBean("adtService") AdtService adtService,
                           UiSessionContext uiSessionContext,
                           UiUtils ui,
                           PageModel model) {

        model.addAttribute("dashboardUrl", coreAppsProperties.getDashboardUrl());
        model.addAttribute("returnUrl", returnUrl);

        // default to x-ray
        if (StringUtils.isBlank(modality)) {
            modality = RadiologyConstants.XRAY_MODALITY_CODE;
        }

        VisitDomainWrapper visitWrapper = new VisitDomainWrapper(visit);

        VisitDomainWrapper activeVisit = null;
        try {
            Location visitLocation = adtService.getLocationThatSupportsVisits(uiSessionContext.getSessionLocation());
            activeVisit = adtService.getActiveVisit(patient, visitLocation);
        }
        catch (IllegalArgumentException e) {
            // don't fail hard if location doesn't support visits
        }
        model.addAttribute("activeVisit", activeVisit);

        // TODO better handle the case where this is multiple providers for a single user
        Collection<Provider> providers = Context.getProviderService().getProvidersByPerson(Context.getAuthenticatedUser().getPerson());
        model.addAttribute("currentProvider", providers.iterator().next());

        model.addAttribute("xrayModalityCode", RadiologyConstants.XRAY_MODALITY_CODE);
        model.addAttribute("ctScanModalityCode", RadiologyConstants.CT_SCAN_MODALITY_CODE);

        model.addAttribute("portableLocations", ui.toJson(getPortableLocations(locationService, emrApiProperties, ui)));
        model.addAttribute("patient", patient);
        model.addAttribute("modality", modality.toUpperCase());
        model.addAttribute("providers", getProviders(providerService));
        model.addAttribute("visit", visitWrapper.getVisit());

        Date defaultOrderDate = visitWrapper.isActive() ? new Date() : visitWrapper.getStartDatetime(); // active visit, default order date = now, otherwise equals start date of visit
        // note that the underlying date widget takes care of stripping out the time component for us
        model.addAttribute("minOrderDate", visitWrapper.getStartDatetime());
        model.addAttribute("maxOrderDate", visitWrapper.getEncounterStopDateRange());
        model.addAttribute("defaultOrderDate",defaultOrderDate);

        model.addAttribute("minCreatinineTestDate", new DateTime(defaultOrderDate).minusMonths(1).toDate());
        model.addAttribute("maxCreatinineTestDate", defaultOrderDate);
        model.addAttribute("defaultCreatinineTestDate", defaultOrderDate);

        model.addAttribute("leadRadiologyTechName", radiologyProperties.getLeadRadiologyTechName());
        model.addAttribute("leadRadiologyTechContactInfo", radiologyProperties.getLeadRadiologyTechContactInfo());

        // used to determine if we need to collect creatinine level
        List<Integer> contrastOrderables = null; // don't need separate this out once we remove feature toggle
        if (radiologyProperties.getContrastOrderablesConcept() != null) {
            contrastOrderables = getContrastOrderables(radiologyProperties.getContrastOrderablesConcept());
            model.addAttribute("contrastStudies", ui.toJson(contrastOrderables));
        }
        else {
            model.addAttribute("contrastStudies","");
        }

        if (modality.equalsIgnoreCase(RadiologyConstants.XRAY_MODALITY_CODE)) {
            model.addAttribute("orderables", ui.toJson(getOrderables(radiologyProperties.getXrayOrderablesConcept(), Context.getLocale())));
        }
        else if (modality.equalsIgnoreCase(RadiologyConstants.CT_SCAN_MODALITY_CODE)) {

            List<SimpleObject> ctScanOrderables = getOrderables(radiologyProperties.getCTScanOrderablesConcept(), Context.getLocale());

            // remove this block once radiology contrasts have been toggled on
            if (!featureToggles.isFeatureEnabled("radiologyContrastStudies"))  {
                removeContrastOrderables(ctScanOrderables, contrastOrderables);
            }

            model.addAttribute("orderables", ui.toJson(ctScanOrderables));
        }
        else if (modality.equalsIgnoreCase(RadiologyConstants.ULTRASOUND_MODALITY_CODE)) {
            model.addAttribute("orderables", ui.toJson(getOrderables(radiologyProperties.getUltrasoundOrderablesConcept(), Context.getLocale())));
        }
        else {
            throw new IllegalArgumentException("Invalid Modality: " + modality);
        }


    }

    private List<SimpleObject> getPortableLocations(LocationService locationService, EmrApiProperties emrApiProperties, final UiUtils ui) {
        List<SimpleObject> items = new ArrayList<SimpleObject>();
        List<Location> locations = locationService.getLocationsByTag(emrApiProperties.getSupportsLoginLocationTag()); // TODO: is login locations really the right thing here?

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

    private List<Integer> getContrastOrderables(Concept contrastOrderablesSet)  {
        // we only need concept ids for the contrast set
        List<Integer> items = new ArrayList<Integer>();
        for (Concept concept : contrastOrderablesSet.getSetMembers()) {
            items.add(concept.getId());
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

    // temporary feature toggle method to remove contrast orderables as an option
    private void removeContrastOrderables(List<SimpleObject> ctScanOrderables, List<Integer> contrastOrderables) {

        if (contrastOrderables == null) {
            return;
        }

        Iterator<SimpleObject> i = ctScanOrderables.iterator();

        while (i.hasNext()) {
            SimpleObject ctScanOrderable = i.next();
            if (contrastOrderables.contains(Integer.valueOf(ctScanOrderable.get("value").toString()))) {
                i.remove();
            }
        }

    }
}
