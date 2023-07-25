package uk.gov.hmcts.reform.judicialapi.elinks.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.judicialapi.elinks.controller.request.AppointmentsRequest;
import uk.gov.hmcts.reform.judicialapi.elinks.controller.request.AuthorisationsRequest;
import uk.gov.hmcts.reform.judicialapi.elinks.controller.request.PaginationRequest;
import uk.gov.hmcts.reform.judicialapi.elinks.controller.request.PeopleRequest;
import uk.gov.hmcts.reform.judicialapi.elinks.controller.request.ResultsRequest;
import uk.gov.hmcts.reform.judicialapi.elinks.controller.request.RoleRequest;
import uk.gov.hmcts.reform.judicialapi.elinks.domain.BaseLocation;
import uk.gov.hmcts.reform.judicialapi.elinks.domain.ElinkDataExceptionRecords;
import uk.gov.hmcts.reform.judicialapi.elinks.domain.ElinkDataSchedularAudit;
import uk.gov.hmcts.reform.judicialapi.elinks.domain.LocationMapping;
import uk.gov.hmcts.reform.judicialapi.elinks.exception.ElinksException;
import uk.gov.hmcts.reform.judicialapi.elinks.feign.ElinksFeignClient;
import uk.gov.hmcts.reform.judicialapi.elinks.repository.AppointmentsRepository;
import uk.gov.hmcts.reform.judicialapi.elinks.repository.AuthorisationsRepository;
import uk.gov.hmcts.reform.judicialapi.elinks.repository.BaseLocationRepository;
import uk.gov.hmcts.reform.judicialapi.elinks.repository.DataloadSchedularAuditRepository;
import uk.gov.hmcts.reform.judicialapi.elinks.repository.ElinkDataExceptionRepository;
import uk.gov.hmcts.reform.judicialapi.elinks.repository.ElinkSchedularAuditRepository;
import uk.gov.hmcts.reform.judicialapi.elinks.repository.JudicialRoleTypeRepository;
import uk.gov.hmcts.reform.judicialapi.elinks.repository.LocationMapppingRepository;
import uk.gov.hmcts.reform.judicialapi.elinks.repository.LocationRepository;
import uk.gov.hmcts.reform.judicialapi.elinks.repository.ProfileRepository;
import uk.gov.hmcts.reform.judicialapi.elinks.response.ElinkPeopleWrapperResponse;
import uk.gov.hmcts.reform.judicialapi.elinks.util.CommonUtil;
import uk.gov.hmcts.reform.judicialapi.elinks.util.ElinkDataExceptionHelper;
import uk.gov.hmcts.reform.judicialapi.elinks.util.ElinkDataIngestionSchedularAudit;
import uk.gov.hmcts.reform.judicialapi.elinks.util.RefDataElinksConstants;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.Charset.defaultCharset;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.RefDataElinksConstants.DATA_UPDATE_ERROR;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.RefDataElinksConstants.ELINKS_ACCESS_ERROR;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.RefDataElinksConstants.ELINKS_ERROR_RESPONSE_BAD_REQUEST;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.RefDataElinksConstants.ELINKS_ERROR_RESPONSE_FORBIDDEN;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.RefDataElinksConstants.ELINKS_ERROR_RESPONSE_NOT_FOUND;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.RefDataElinksConstants.ELINKS_ERROR_RESPONSE_UNAUTHORIZED;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.RefDataElinksConstants.PEOPLEAPI;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.RefDataElinksConstants.PEOPLE_DATA_LOAD_SUCCESS;

@ExtendWith(MockitoExtension.class)
class ElinksPeopleServiceImplTest {

    @Spy
    ElinksFeignClient elinksFeignClient;

    @Spy
    private BaseLocationRepository baseLocationRepository;

    @Spy
    private AppointmentsRepository appointmentsRepository;

    @Spy
    private AuthorisationsRepository authorisationsRepository;

    @Spy
    private ProfileRepository profileRepository;

    @Spy
    private JudicialRoleTypeRepository judicialRoleTypeRepository;

    @Spy
    private LocationMapppingRepository locationMapppingRepository;

    @Spy
    private LocationRepository locationRepository;

    @Spy
    private DataloadSchedularAuditRepository dataloadSchedularAuditRepository;

    @Spy
    private ElinkDataIngestionSchedularAudit elinkDataIngestionSchedularAudit;

    @Mock
    ElinkDataExceptionHelper elinkDataExceptionHelper;

    @Mock
    private ElinkDataExceptionRepository elinkDataExceptionRepository;

    @Spy
    private ElinkSchedularAuditRepository elinkSchedularAuditRepository;

    @InjectMocks
    private ElinksPeopleServiceImpl elinksPeopleServiceImpl;

    @Mock
    private ElinksPeopleDeleteServiceimpl elinksPeopleDeleteServiceimpl;

    private ResultsRequest result1;

    private ResultsRequest result2;

    private PaginationRequest pagination;

    private PeopleRequest elinksApiResponseFirstHit;

    private PeopleRequest elinksApiResponseSecondHit;

    JdbcTemplate jdbcTemplate =  mock(JdbcTemplate.class);

    @Spy
    CommonUtil commonUtil;

    @BeforeEach
    void setUP() {

        ReflectionTestUtils.setField(elinksPeopleServiceImpl, "threadPauseTime",
                "2000");
        ReflectionTestUtils.setField(elinksPeopleServiceImpl, "threadRetriggerPauseTime",
            "1000");
        ReflectionTestUtils.setField(elinksPeopleServiceImpl, "lastUpdated",
                "Thu Jan 01 00:00:00 GMT 2015");
        ReflectionTestUtils.setField(elinksPeopleServiceImpl, "page",
                "1");


        pagination = PaginationRequest.builder()
                .results(1)
                .pages(1).currentPage(1).resultsPerPage(3).morePages(true).build();

        AppointmentsRequest appointmentsRequest1 = AppointmentsRequest.builder()
                .baseLocationId("baselocId").circuit("circuit").location("location")
                .isPrincipleAppointment(true).startDate("1991-12-19").endDate("2022-12-20")
                .roleName("appointment").contractType("type").type("Courts").build();
        AppointmentsRequest appointmentsRequest2 = AppointmentsRequest.builder()
                .baseLocationId("baselocId").circuit("circuit").location("location")
                .isPrincipleAppointment(true).startDate("1991-12-19").endDate("2022-12-20")
                .roleName("appointment").contractType("type").type("Tribunals").build();
        List<AppointmentsRequest> appointmentsRequests = Arrays.asList(appointmentsRequest1,appointmentsRequest2);

        AuthorisationsRequest authorisation1 = AuthorisationsRequest.builder().jurisdiction("juristriction")
                .ticket("lowerlevel").startDate("1991-12-19")
                .endDate("2022-12-20").ticketCode("ticketId").build();
        AuthorisationsRequest authorisation2 = AuthorisationsRequest.builder().jurisdiction("juristriction")
                .ticket("lowerlevel").startDate("1991-12-19")
                .endDate("2022-12-20").ticketCode("ticketId").build();
        RoleRequest roleRequestOne = RoleRequest.builder().judiciaryRoleId("427").name("name")
            .startDate("1991-12-19T00:00:00.000Z").endDate("2024-12-20T00:00:00.000Z").build();
        RoleRequest roleRequestTwo = RoleRequest.builder().judiciaryRoleId("427").name("name")
            .startDate("1991-12-19T00:00:00.000Z")
            .endDate("2024-12-20T00:00:00.000Z").build();
        List<AuthorisationsRequest> authorisations = Arrays.asList(authorisation1,authorisation2);



        result1 = ResultsRequest.builder().personalCode("1234").knownAs("knownas").fullName("fullName")
                .surname("surname").postNominals("postNOmi").email("email").lastWorkingDate("2022-12-20")
                .objectId("objectId").initials("initials").appointmentsRequests(appointmentsRequests)
                .authorisationsRequests(authorisations).judiciaryRoles(List.of(roleRequestOne,roleRequestTwo)).build();

        result2 = ResultsRequest.builder().personalCode("12345").knownAs("knownas").fullName("fullName")
                .surname("surname").postNominals("postNOmi").email("email").lastWorkingDate("2022-12-20")
                .objectId("objectId").initials("initials").appointmentsRequests(appointmentsRequests)
                .authorisationsRequests(authorisations).judiciaryRoles(List.of(roleRequestOne,roleRequestTwo)).build();

        List<ResultsRequest> results = Arrays.asList(result1,result2);

        elinksApiResponseFirstHit = PeopleRequest.builder().resultsRequests(results).pagination(pagination).build();


        PaginationRequest paginationFalse = PaginationRequest.builder()
                .results(1)
                .pages(1).currentPage(1).resultsPerPage(3).morePages(false).build();
        elinksApiResponseSecondHit = PeopleRequest.builder().resultsRequests(results).pagination(paginationFalse)
                .build();
    }

    @Test
    void loadPeopleWhenAuditEntryPresentPartialSuccess() throws JsonProcessingException {

        ElinkDataSchedularAudit schedularAudit = new ElinkDataSchedularAudit();
        schedularAudit.setStatus(RefDataElinksConstants.JobStatus.PARTIAL_SUCCESS.getStatus());
        schedularAudit.setId(1);
        schedularAudit.setApiName(PEOPLEAPI);
        schedularAudit.setSchedulerName("testschedulername");
        schedularAudit.setSchedulerEndTime(LocalDateTime.now());
        schedularAudit.setSchedulerStartTime(LocalDateTime.now());

        ElinkDataExceptionRecords record = new ElinkDataExceptionRecords();
        record.setId(1L);
        record.setErrorDescription("Test Error Description");
        record.setKey("testKey");
        record.setTableName("test table name");
        record.setFieldInError("testfieldInError");
        record.setSchedulerName("testbaselocationscheduler");
        record.setRowId("0");
        record.setSchedulerStartTime(LocalDateTime.now());
        record.setUpdatedTimeStamp(LocalDateTime.now());

        when(elinkSchedularAuditRepository.save(any())).thenReturn(schedularAudit);
        when(elinkDataExceptionRepository.save(any())).thenReturn(record);
        when(locationRepository.fetchRegionIdfromCftRegionDescEn(any())).thenReturn("1");
        when(baseLocationRepository.fetchParentId(any())).thenReturn("1234");

        BaseLocation location = new BaseLocation();
        location.setBaseLocationId("Baselocid");
        location.setName("ABC");


        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(elinksApiResponseFirstHit);
        String body2 = mapper.writeValueAsString(elinksApiResponseSecondHit);

        when(elinksFeignClient.getPeopleDetials(any(), any(), any(),
                Boolean.parseBoolean(any()))).thenReturn(Response.builder()
                        .request(mock(Request.class)).body(body, defaultCharset()).status(200).build())
                .thenReturn(Response.builder().request(mock(Request.class))
                        .body(body2, defaultCharset()).status(200).build());

        ResponseEntity<ElinkPeopleWrapperResponse> response = elinksPeopleServiceImpl.updatePeople();
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertThat(response.getBody().getMessage()).isEqualTo(PEOPLE_DATA_LOAD_SUCCESS);

        ElinkDataExceptionRecords result = elinkDataExceptionRepository.save(record);
        ElinkDataSchedularAudit resultAudit = elinkSchedularAuditRepository.save(schedularAudit);

        assertThat(resultAudit.getStatus()).isEqualTo(schedularAudit.getStatus());
        assertThat(result.getId()).isEqualTo(record.getId());
        assertThat(result.getErrorDescription()).isEqualTo(record.getErrorDescription());



        verify(elinkDataExceptionRepository, times(1)).save(any());
    }

    @Test
    void loadPeopleWhenAuditEntryPresentPartialSuccessFor429() throws JsonProcessingException {

        ElinkDataSchedularAudit schedularAudit = new ElinkDataSchedularAudit();
        schedularAudit.setStatus(RefDataElinksConstants.JobStatus.PARTIAL_SUCCESS.getStatus());
        schedularAudit.setId(1);
        schedularAudit.setApiName(PEOPLEAPI);
        schedularAudit.setSchedulerName("testschedulername");
        schedularAudit.setSchedulerEndTime(LocalDateTime.now());
        schedularAudit.setSchedulerStartTime(LocalDateTime.now());

        ElinkDataExceptionRecords record = new ElinkDataExceptionRecords();
        record.setId(1L);
        record.setErrorDescription("Test Error Description");
        record.setKey("testKey");
        record.setTableName("test table name");
        record.setFieldInError("testfieldInError");
        record.setSchedulerName("testbaselocationscheduler");
        record.setRowId("0");
        record.setSchedulerStartTime(LocalDateTime.now());
        record.setUpdatedTimeStamp(LocalDateTime.now());

        when(elinkSchedularAuditRepository.save(any())).thenReturn(schedularAudit);
        when(elinkDataExceptionRepository.save(any())).thenReturn(record);
        when(locationRepository.fetchRegionIdfromCftRegionDescEn(any())).thenReturn("1");
        when(baseLocationRepository.fetchParentId(any())).thenReturn("1234");

        BaseLocation location = new BaseLocation();
        location.setBaseLocationId("Baselocid");
        location.setName("ABC");


        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(elinksApiResponseFirstHit);
        String body2 = mapper.writeValueAsString(elinksApiResponseSecondHit);

        when(elinksFeignClient.getPeopleDetials(any(), any(), any(),
            Boolean.parseBoolean(any()))).thenReturn(Response.builder()
                .request(mock(Request.class)).body(body, defaultCharset()).status(429).build())
            .thenReturn(Response.builder().request(mock(Request.class))
                .body(body2, defaultCharset()).status(200).build());

        ResponseEntity<ElinkPeopleWrapperResponse> response = elinksPeopleServiceImpl.updatePeople();
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertThat(response.getBody().getMessage()).isEqualTo(PEOPLE_DATA_LOAD_SUCCESS);

        ElinkDataExceptionRecords result = elinkDataExceptionRepository.save(record);
        ElinkDataSchedularAudit resultAudit = elinkSchedularAuditRepository.save(schedularAudit);

        assertThat(resultAudit.getStatus()).isEqualTo(schedularAudit.getStatus());
        assertThat(result.getId()).isEqualTo(record.getId());
        assertThat(result.getErrorDescription()).isEqualTo(record.getErrorDescription());



        verify(elinkDataExceptionRepository, times(1)).save(any());
    }

    @Test
    void loadPeopleWhenAuditEntryPresentSuccess() throws JsonProcessingException {

        LocalDateTime dateTime = LocalDateTime.now();
        when(dataloadSchedularAuditRepository.findLatestSchedularEndTime()).thenReturn(dateTime);

        when(locationRepository.fetchRegionIdfromCftRegionDescEn(any())).thenReturn("1");
        LocationMapping locationMapping = LocationMapping.builder()
            .serviceCode("BHA1")
            .epimmsId("1234").build();
        BaseLocation location = new BaseLocation();
        location.setBaseLocationId("12345");
        location.setName("ABC");
        when(locationMapppingRepository.fetchEpimmsIdfromLocationId(any())).thenReturn("2344");
        when(baseLocationRepository.fetchParentId(any())).thenReturn("1234");
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(elinksApiResponseFirstHit);
        String body2 = mapper.writeValueAsString(elinksApiResponseSecondHit);

        when(elinksFeignClient.getPeopleDetials(any(), any(), any(),
                 Boolean.parseBoolean(any()))).thenReturn(Response.builder()
                .request(mock(Request.class)).body(body, defaultCharset()).status(200).build())
                .thenReturn(Response.builder().request(mock(Request.class))
                        .body(body2, defaultCharset()).status(200).build());

        ResponseEntity<ElinkPeopleWrapperResponse> response = elinksPeopleServiceImpl.updatePeople();
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertThat(response.getBody().getMessage()).isEqualTo(PEOPLE_DATA_LOAD_SUCCESS);

        verify(elinksFeignClient, times(2)).getPeopleDetials(any(), any(), any(),
                Boolean.parseBoolean(any()));
        verify(profileRepository, atLeastOnce()).save(any());

        verify(appointmentsRepository, times(8)).save(any());
        verify(judicialRoleTypeRepository, atLeastOnce()).save(any());
        verify(authorisationsRepository, atLeastOnce()).save(any());
    }

    @Test
    void loadPeopleWhenAuditEntryNotPresentSuccess() throws JsonProcessingException {
        when(dataloadSchedularAuditRepository.findLatestSchedularEndTime()).thenReturn(null);
        BaseLocation location = new BaseLocation();
        location.setBaseLocationId("12345");
        location.setName("ABC");
        LocationMapping locationMapping = LocationMapping.builder()
            .serviceCode("BHA1")
            .epimmsId("1234").build();
        when(locationRepository.fetchRegionIdfromCftRegionDescEn(any())).thenReturn("1");
        when(baseLocationRepository.fetchParentId(any())).thenReturn("1234");
        ObjectMapper mapper = new ObjectMapper();
        when(locationMapppingRepository.fetchEpimmsIdfromLocationId(any())).thenReturn("234");
        String body = mapper.writeValueAsString(elinksApiResponseFirstHit);
        String body2 = mapper.writeValueAsString(elinksApiResponseSecondHit);

        when(elinksFeignClient.getPeopleDetials(any(), any(), any(),
                Boolean.parseBoolean(any()))).thenReturn(Response.builder()
                        .request(mock(Request.class)).body(body, defaultCharset()).status(200).build())
                .thenReturn(Response.builder().request(mock(Request.class))
                        .body(body2, defaultCharset()).status(200).build());

        ResponseEntity<ElinkPeopleWrapperResponse> response = elinksPeopleServiceImpl.updatePeople();
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertThat(response.getBody().getMessage()).isEqualTo(PEOPLE_DATA_LOAD_SUCCESS);


        verify(elinksFeignClient, times(2)).getPeopleDetials(any(), any(), any(),
                Boolean.parseBoolean(any()));
        verify(profileRepository, times(4)).save(any());

        verify(appointmentsRepository, atLeastOnce()).save(any());

        verify(authorisationsRepository, atLeastOnce()).save(any());


    }

    @Test
    void loadPeopleWithPartialSuccess() throws JsonProcessingException {

        when(dataloadSchedularAuditRepository.findLatestSchedularEndTime()).thenReturn(null);
        BaseLocation location = new BaseLocation();
        location.setBaseLocationId("12345");
        location.setName("ABC");
        LocationMapping locationMapping = LocationMapping.builder()
            .serviceCode("BHA1")
            .epimmsId("1234").build();
        when(locationRepository.fetchRegionIdfromCftRegionDescEn(any())).thenReturn(null);
        when(baseLocationRepository.fetchParentId(any())).thenReturn("1234");
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(elinksApiResponseFirstHit);
        String body2 = mapper.writeValueAsString(elinksApiResponseSecondHit);

        when(elinksFeignClient.getPeopleDetials(any(), any(), any(),
            Boolean.parseBoolean(any()))).thenReturn(Response.builder()
                .request(mock(Request.class)).body(body, defaultCharset()).status(200).build())
            .thenReturn(Response.builder().request(mock(Request.class))
                .body(body2, defaultCharset()).status(200).build());

        ResponseEntity<ElinkPeopleWrapperResponse> response = elinksPeopleServiceImpl.updatePeople();
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertThat(response.getBody().getMessage()).isEqualTo(PEOPLE_DATA_LOAD_SUCCESS);


        verify(elinksFeignClient, times(2)).getPeopleDetials(any(), any(), any(),
            Boolean.parseBoolean(any()));
        verify(profileRepository, times(4)).save(any());

        verify(authorisationsRepository, atLeastOnce()).save(any());


    }

    @Test
    void loadPeopleWithPartialSuccessWithInvalidRoleNames() throws JsonProcessingException {

        when(dataloadSchedularAuditRepository.findLatestSchedularEndTime()).thenReturn(null);
        BaseLocation location = new BaseLocation();
        location.setBaseLocationId("12345");
        location.setName("ABC");
        when(locationRepository.fetchRegionIdfromCftRegionDescEn(any())).thenReturn("1");
        when(baseLocationRepository.fetchParentId(any())).thenReturn("1234");
        ObjectMapper mapper = new ObjectMapper();
        AppointmentsRequest appointmentsRequestNew = AppointmentsRequest.builder()
            .baseLocationId("baselocId").circuit("circuit").location("location")
            .isPrincipleAppointment(true).startDate("1991-12-19").endDate("2022-12-20")
            .roleName("CRTS TRIB - RS Admin User").contractType("type").type("Tribunals").build();
        PaginationRequest paginationNew = PaginationRequest.builder()
            .results(1)
            .pages(1).currentPage(1).resultsPerPage(3).morePages(false).build();
        elinksApiResponseFirstHit.setPagination(paginationNew);
        elinksApiResponseFirstHit.getResultsRequests().get(0).setAppointmentsRequests(List.of(appointmentsRequestNew));
        String body = mapper.writeValueAsString(elinksApiResponseFirstHit);

        when(elinksFeignClient.getPeopleDetials(any(), any(), any(),
            Boolean.parseBoolean(any()))).thenReturn(Response.builder()
                .request(mock(Request.class)).body(body, defaultCharset()).status(200).build());


        ResponseEntity<ElinkPeopleWrapperResponse> response = elinksPeopleServiceImpl.updatePeople();
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertThat(response.getBody().getMessage()).isEqualTo(PEOPLE_DATA_LOAD_SUCCESS);


        verify(elinksFeignClient, times(1)).getPeopleDetials(any(), any(), any(),
            Boolean.parseBoolean(any()));
        verify(profileRepository, times(2)).save(any());

        verify(authorisationsRepository, atLeastOnce()).save(any());


    }

    @Test
    void load_people_should_return_elinksException_when_DataAccessException_while_connecting_to_Audit_table() {

        DataAccessException dataAccessException = mock(DataAccessException.class);
        when(dataloadSchedularAuditRepository.findLatestSchedularEndTime()).thenThrow(dataAccessException);
        ElinksException thrown = Assertions.assertThrows(ElinksException.class, () -> {
            ResponseEntity<ElinkPeopleWrapperResponse> responseEntity = elinksPeopleServiceImpl.updatePeople();
        });
        assertThat(thrown.getStatus().value()).isEqualTo(HttpStatus.NOT_ACCEPTABLE.value());
        assertThat(thrown.getErrorMessage()).contains(DATA_UPDATE_ERROR);
        assertThat(thrown.getErrorDescription()).contains(DATA_UPDATE_ERROR);
    }


    @Test
    void load_people_should_return_elinksException_when_ElinksApi_Failure() {

        when(dataloadSchedularAuditRepository.findLatestSchedularEndTime()).thenReturn(null);
        FeignException feignExceptionMock = Mockito.mock(FeignException.class);
        when(elinksFeignClient.getPeopleDetials(any(), any(), any(),
                Boolean.parseBoolean(any()))).thenThrow(feignExceptionMock);

        ElinksException thrown = Assertions.assertThrows(ElinksException.class, () -> {
            ResponseEntity<ElinkPeopleWrapperResponse> responseEntity = elinksPeopleServiceImpl.updatePeople();
        });
        assertThat(thrown.getStatus().value()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(thrown.getErrorMessage()).contains(ELINKS_ACCESS_ERROR);
        assertThat(thrown.getErrorDescription()).contains(ELINKS_ACCESS_ERROR);
    }

    @Test
    void load_people_should_return_elinksException_when_ElinksApi_Response_is_unknown_Format()
            throws JsonProcessingException {

        String body = "{\"test\":\"test\"}";
        when(dataloadSchedularAuditRepository.findLatestSchedularEndTime()).thenReturn(null);

        when(elinksFeignClient.getPeopleDetials(any(), any(), any(),
                Boolean.parseBoolean(any()))).thenReturn(Response.builder()
                .request(mock(Request.class)).body(body, defaultCharset()).status(200).build());

        ElinksException thrown = Assertions.assertThrows(ElinksException.class, () -> {
            ResponseEntity<ElinkPeopleWrapperResponse> responseEntity = elinksPeopleServiceImpl.updatePeople();
        });
        assertThat(thrown.getStatus().value()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(thrown.getErrorMessage()).contains(ELINKS_ACCESS_ERROR);
        assertThat(thrown.getErrorDescription()).contains(ELINKS_ACCESS_ERROR);

    }

    @Test
    void load_people_should_return_elinksException_when_ElinksApi_Response_does_not_have_results()
            throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        elinksApiResponseFirstHit.setResultsRequests(null);
        String body = mapper.writeValueAsString(elinksApiResponseFirstHit);

        when(dataloadSchedularAuditRepository.findLatestSchedularEndTime()).thenReturn(LocalDateTime.now());

        when(elinksFeignClient.getPeopleDetials(any(), any(), any(),
                Boolean.parseBoolean(any()))).thenReturn(Response.builder()
                        .request(mock(Request.class)).body(body, defaultCharset()).status(200).build());

        ElinksException thrown = Assertions.assertThrows(ElinksException.class, () -> {
            ResponseEntity<ElinkPeopleWrapperResponse> responseEntity = elinksPeopleServiceImpl.updatePeople();
        });
        assertThat(thrown.getStatus().value()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(thrown.getErrorMessage()).contains(ELINKS_ACCESS_ERROR);
        assertThat(thrown.getErrorDescription()).contains(ELINKS_ACCESS_ERROR);

    }

    @Test
    void load_people_should_return_elinksException_when_ElinksApi_Response_does_not_have_pagination()
            throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        elinksApiResponseFirstHit.setPagination(null);
        String body = mapper.writeValueAsString(elinksApiResponseFirstHit);

        when(dataloadSchedularAuditRepository.findLatestSchedularEndTime()).thenReturn(LocalDateTime.now());

        when(elinksFeignClient.getPeopleDetials(any(), any(), any(),
                Boolean.parseBoolean(any()))).thenReturn(Response.builder()
                .request(mock(Request.class)).body(body, defaultCharset()).status(200).build());

        ElinksException thrown = Assertions.assertThrows(ElinksException.class, () -> {
            ResponseEntity<ElinkPeopleWrapperResponse> responseEntity = elinksPeopleServiceImpl.updatePeople();
        });
        assertThat(thrown.getStatus().value()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(thrown.getErrorMessage()).contains(ELINKS_ACCESS_ERROR);
        assertThat(thrown.getErrorDescription()).contains(ELINKS_ACCESS_ERROR);
    }

    @Test
    void load_people_should_return_elinksException_when_updating_peopleDb()
            throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        PaginationRequest paginationNew = PaginationRequest.builder()
            .results(1)
            .pages(1).currentPage(1).resultsPerPage(3).morePages(false).build();
        elinksApiResponseFirstHit.setPagination(paginationNew);
        String body = mapper.writeValueAsString(elinksApiResponseFirstHit);



        when(dataloadSchedularAuditRepository.findLatestSchedularEndTime()).thenReturn(LocalDateTime.now());

        when(elinksFeignClient.getPeopleDetials(any(), any(), any(),
                Boolean.parseBoolean(any()))).thenReturn(Response.builder()
                .request(mock(Request.class)).body(body, defaultCharset()).status(200).build());

        DataAccessException dataAccessException = mock(DataAccessException.class);
        when(profileRepository.save(any())).thenThrow(dataAccessException);



        ResponseEntity<ElinkPeopleWrapperResponse> responseEntity = elinksPeopleServiceImpl.updatePeople();
        verify(elinkDataExceptionHelper,atLeastOnce()).auditException(any(),any(),any(),any(),any(),any(),any());
    }


    @Test
    void load_people_should_return_elinksException_when_updating_appointmentsDb()
            throws JsonProcessingException {

        LocationMapping locationMapping = LocationMapping.builder()
            .serviceCode("BHA1")
            .epimmsId("1234").build();
        ObjectMapper mapper = new ObjectMapper();
        PaginationRequest paginationNew = PaginationRequest.builder()
            .results(1)
            .pages(1).currentPage(1).resultsPerPage(3).morePages(false).build();
        elinksApiResponseFirstHit.setPagination(paginationNew);
        when(locationRepository.fetchRegionIdfromCftRegionDescEn(any())).thenReturn("1");
        String body = mapper.writeValueAsString(elinksApiResponseFirstHit);
        when(locationMapppingRepository.fetchEpimmsIdfromLocationId(any())).thenReturn("234");
        when(dataloadSchedularAuditRepository.findLatestSchedularEndTime()).thenReturn(LocalDateTime.now());

        DataAccessException dataAccessException = mock(DataAccessException.class);
        when(appointmentsRepository.save(any())).thenThrow(dataAccessException);
        when(elinksFeignClient.getPeopleDetials(any(), any(), any(),
                Boolean.parseBoolean(any()))).thenReturn(Response.builder()
                .request(mock(Request.class)).body(body, defaultCharset()).status(200).build());


        ResponseEntity<ElinkPeopleWrapperResponse> responseEntity = elinksPeopleServiceImpl.updatePeople();
        verify(elinkDataExceptionHelper,atLeastOnce()).auditException(any(),any(),any(),any(),any(),any(),any());
    }

    @Test
    void load_people_should_return_elinksException_when_updating_authorisationsDb()
            throws JsonProcessingException {

        PaginationRequest paginationNew = PaginationRequest.builder()
            .results(1)
            .pages(1).currentPage(1).resultsPerPage(3).morePages(false).build();
        LocationMapping locationMapping = LocationMapping.builder()
            .serviceCode("BHA1")
            .epimmsId("1234").build();
        elinksApiResponseFirstHit.setPagination(paginationNew);
        when(locationRepository.fetchRegionIdfromCftRegionDescEn(any())).thenReturn("1");
        when(locationMapppingRepository.fetchEpimmsIdfromLocationId(any())).thenReturn("234");
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(elinksApiResponseFirstHit);
        DataAccessException dataAccessException = mock(DataAccessException.class);
        when(authorisationsRepository.save(any())).thenThrow(dataAccessException);
        when(dataloadSchedularAuditRepository.findLatestSchedularEndTime()).thenReturn(LocalDateTime.now());

        when(elinksFeignClient.getPeopleDetials(any(), any(), any(),
                Boolean.parseBoolean(any()))).thenReturn(Response.builder()
                .request(mock(Request.class)).body(body, defaultCharset()).status(200).build());

        ResponseEntity<ElinkPeopleWrapperResponse> responseEntity = elinksPeopleServiceImpl.updatePeople();
        verify(elinkDataExceptionHelper,atLeastOnce()).auditException(any(),any(),any(),any(),any(),any(),any());
    }

    @Test
    void load_people_should_return_elinksException_when_updating_JudicialRoleTypeDb()
        throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        PaginationRequest paginationNew = PaginationRequest.builder()
            .results(1)
            .pages(1).currentPage(1).resultsPerPage(3).morePages(false).build();
        LocationMapping locationMapping = LocationMapping.builder()
            .serviceCode("BHA1")
            .epimmsId("1234").build();
        elinksApiResponseFirstHit.setPagination(paginationNew);
        when(locationRepository.fetchRegionIdfromCftRegionDescEn(any())).thenReturn("1");
        String body = mapper.writeValueAsString(elinksApiResponseFirstHit);
        when(locationMapppingRepository.fetchEpimmsIdfromLocationId(any())).thenReturn("234");
        when(dataloadSchedularAuditRepository.findLatestSchedularEndTime()).thenReturn(LocalDateTime.now());

        when(elinksFeignClient.getPeopleDetials(any(), any(), any(),
            Boolean.parseBoolean(any()))).thenReturn(Response.builder()
            .request(mock(Request.class)).body(body, defaultCharset()).status(200).build());

        ResponseEntity<ElinkPeopleWrapperResponse> responseEntity = elinksPeopleServiceImpl.updatePeople();
        verify(elinkDataExceptionHelper,atLeastOnce()).auditException(any(),any(),any(),any(),any(),any(),any());
    }

    @Test
    void load_people_should_return_elinksException_when_http_bad_request() {

        when(dataloadSchedularAuditRepository.findLatestSchedularEndTime()).thenReturn(LocalDateTime.now());

        when(elinksFeignClient.getPeopleDetials(any(), any(), any(),
                Boolean.parseBoolean(any()))).thenReturn(Response.builder()
                .request(mock(Request.class)).body("", defaultCharset()).status(HttpStatus.BAD_REQUEST.value())
                .build());

        ElinksException thrown = Assertions.assertThrows(ElinksException.class, () -> {
            ResponseEntity<ElinkPeopleWrapperResponse> responseEntity = elinksPeopleServiceImpl.updatePeople();
        });
        assertThat(thrown.getStatus().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(thrown.getErrorMessage()).contains(ELINKS_ERROR_RESPONSE_BAD_REQUEST);
        assertThat(thrown.getErrorDescription()).contains(ELINKS_ERROR_RESPONSE_BAD_REQUEST);

    }

    @Test
    void load_people_should_return_elinksException_when_http_unauthorised() {

        when(dataloadSchedularAuditRepository.findLatestSchedularEndTime()).thenReturn(LocalDateTime.now());

        when(elinksFeignClient.getPeopleDetials(any(), any(), any(),
                Boolean.parseBoolean(any()))).thenReturn(Response.builder()
                .request(mock(Request.class)).body("", defaultCharset()).status(HttpStatus.UNAUTHORIZED.value())
                .build());

        ElinksException thrown = Assertions.assertThrows(ElinksException.class, () -> {
            ResponseEntity<ElinkPeopleWrapperResponse> responseEntity = elinksPeopleServiceImpl.updatePeople();
        });
        assertThat(thrown.getStatus().value()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(thrown.getErrorMessage()).contains(ELINKS_ERROR_RESPONSE_UNAUTHORIZED);
        assertThat(thrown.getErrorDescription()).contains(ELINKS_ERROR_RESPONSE_UNAUTHORIZED);

    }

    @Test
    void load_people_should_return_elinksException_when_http_forbidden() {

        when(dataloadSchedularAuditRepository.findLatestSchedularEndTime()).thenReturn(LocalDateTime.now());

        when(elinksFeignClient.getPeopleDetials(any(), any(), any(),
                Boolean.parseBoolean(any()))).thenReturn(Response.builder()
                .request(mock(Request.class)).body("", defaultCharset()).status(HttpStatus.FORBIDDEN.value()).build());

        ElinksException thrown = Assertions.assertThrows(ElinksException.class, () -> {
            ResponseEntity<ElinkPeopleWrapperResponse> responseEntity = elinksPeopleServiceImpl.updatePeople();
        });
        assertThat(thrown.getStatus().value()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(thrown.getErrorMessage()).contains(ELINKS_ERROR_RESPONSE_FORBIDDEN);
        assertThat(thrown.getErrorDescription()).contains(ELINKS_ERROR_RESPONSE_FORBIDDEN);

    }

    @Test
    void load_people_should_return_elinksException_when_http_not_found() {

        when(dataloadSchedularAuditRepository.findLatestSchedularEndTime()).thenReturn(LocalDateTime.now());

        when(elinksFeignClient.getPeopleDetials(any(), any(), any(),
                Boolean.parseBoolean(any()))).thenReturn(Response.builder()
                .request(mock(Request.class)).body("", defaultCharset()).status(HttpStatus.NOT_FOUND.value())
                .build());

        ElinksException thrown = Assertions.assertThrows(ElinksException.class, () -> {
            ResponseEntity<ElinkPeopleWrapperResponse> responseEntity = elinksPeopleServiceImpl.updatePeople();
        });
        assertThat(thrown.getStatus().value()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(thrown.getErrorMessage()).contains(ELINKS_ERROR_RESPONSE_NOT_FOUND);
        assertThat(thrown.getErrorDescription()).contains(ELINKS_ERROR_RESPONSE_NOT_FOUND);

    }
}