package org.openmrs.module.radiologyapp.web.controller;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.api.context.UserContext;
import org.openmrs.messagesource.MessageSourceService;
import org.openmrs.module.radiologyapp.RadiologyService;
import org.openmrs.module.radiologyapp.importer.RadiologyImportResult;
import org.openmrs.module.radiologyapp.importer.RadiologyReportImportRequest;
import org.openmrs.module.radiologyapp.importer.RadiologyStudyImportRequest;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class RadiologyImportRestControllerTest {

    private RadiologyImportRestController controller = new RadiologyImportRestController();

    private RadiologyService radiologyService;

    private UserContext userContext;

    @Before
    public void setUp() {
        radiologyService = mock(RadiologyService.class);
        userContext = mock(UserContext.class);
        when(userContext.hasPrivilege(PrivilegeConstants.ADD_ENCOUNTERS)).thenReturn(true);
        Context.setUserContext(userContext);
        // ConversionUtil looks up registered converters; there are none in a unit test, so it falls back to its defaults
        ServiceContext.getInstance().setApplicationContext(mock(ApplicationContext.class));
        // used by Context.requirePrivilege to build its error message
        ServiceContext.getInstance().setMessageSourceService(mock(MessageSourceService.class));
        ServiceContext.getInstance().setService(RadiologyService.class, radiologyService);
    }

    @After
    public void tearDown() {
        Context.clearUserContext();
    }

    private Map<String, Object> studyBody(String datePerformed) {
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("accessionNumber", "ACC-1");
        body.put("datePerformed", datePerformed);
        body.put("imagesAvailable", true);
        body.put("technicianProviderUuid", "tech-uuid");
        body.put("patientIdentifierTypeUuid", "type-uuid");
        body.put("patientIdentifier", "ID-1");
        return body;
    }

    @Test
    public void postStudy_shouldPassParsedRequestToServiceAndReturnOutcome() {
        RadiologyImportResult result = RadiologyImportResult.created("enc-uuid");
        when(radiologyService.importRadiologyStudy(any(RadiologyStudyImportRequest.class))).thenReturn(result);

        Object response = controller.postStudy(studyBody("2026-11-10T21:21:19.000+00:00"));

        org.mockito.ArgumentCaptor<RadiologyStudyImportRequest> captor = org.mockito.ArgumentCaptor
                .forClass(RadiologyStudyImportRequest.class);
        verify(radiologyService).importRadiologyStudy(captor.capture());
        RadiologyStudyImportRequest request = captor.getValue();
        assertThat(request.getAccessionNumber(), is("ACC-1"));
        assertThat(request.getDatePerformed(), is(Date.from(Instant.parse("2026-11-10T21:21:19Z"))));
        assertThat(request.getImagesAvailable(), is(true));
        assertThat(request.getTechnicianProviderUuid(), is("tech-uuid"));
        assertThat(request.getPatientIdentifierTypeUuid(), is("type-uuid"));
        assertThat(request.getPatientIdentifier(), is("ID-1"));

        Map<?, ?> map = (Map<?, ?>) response;
        assertThat(map.get("outcome"), is((Object) "CREATED"));
        assertThat(map.get("encounterUuid"), is((Object) "enc-uuid"));
        assertThat(map.get("studyCreated"), is((Object) false));
    }

    @Test
    public void postReport_shouldPassParsedRequestToService() {
        when(radiologyService.importRadiologyReport(any(RadiologyReportImportRequest.class)))
                .thenReturn(RadiologyImportResult.created("enc-uuid"));
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("accessionNumber", "ACC-1");
        body.put("reportDate", "2026-11-11T08:00:00+01:00");
        body.put("reportType", "final");
        body.put("reportBody", "Normal");
        body.put("patientIdentifierTypeUuid", "type-uuid");
        body.put("patientIdentifier", "ID-1");

        controller.postReport(body);

        org.mockito.ArgumentCaptor<RadiologyReportImportRequest> captor = org.mockito.ArgumentCaptor
                .forClass(RadiologyReportImportRequest.class);
        verify(radiologyService).importRadiologyReport(captor.capture());
        assertThat(captor.getValue().getReportDate(), is(Date.from(Instant.parse("2026-11-11T07:00:00Z"))));
        assertThat(captor.getValue().getDatePerformed() == null, is(true));
        assertThat(captor.getValue().getReportBody(), is("Normal"));
    }

    @Test
    public void postReport_shouldReturn400ForIllegalArgument() {
        when(radiologyService.importRadiologyReport(any(RadiologyReportImportRequest.class)))
                .thenThrow(new IllegalArgumentException("bad report"));
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("reportDate", "2026-11-11T08:00:00+01:00");

        try {
            controller.postReport(body);
            fail("expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            ResponseEntity<Map<String, String>> response = controller.handleIllegalArgument(e);
            assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
            assertThat(response.getBody().get("error"), is("bad report"));
        }
    }

    @Test
    public void postStudy_shouldReturn400ForBadDate() {
        try {
            controller.postStudy(studyBody("yesterday"));
            fail("expected IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            ResponseEntity<Map<String, String>> response = controller.handleIllegalArgument(e);
            assertThat(response.getStatusCode(), is(HttpStatus.BAD_REQUEST));
            assertThat(response.getBody().get("error"), is(notNullValue()));
        }
        verifyNoInteractions(radiologyService);
    }

    @Test
    public void postStudy_shouldRequireAddEncountersPrivilege() {
        when(userContext.hasPrivilege(PrivilegeConstants.ADD_ENCOUNTERS)).thenReturn(false);

        try {
            controller.postStudy(studyBody("2026-11-10T21:21:19.000+00:00"));
            fail("expected APIAuthenticationException");
        }
        catch (APIAuthenticationException e) {
            // expected
        }
        verify(radiologyService, never()).importRadiologyStudy(any(RadiologyStudyImportRequest.class));
    }
}
