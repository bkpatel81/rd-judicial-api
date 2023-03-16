package uk.gov.hmcts.reform.judicialapi.elinks.service.impl;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Assertions;
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
import uk.gov.hmcts.reform.judicialapi.elinks.exception.ElinksException;
import uk.gov.hmcts.reform.judicialapi.elinks.feign.ElinksFeignClient;
import uk.gov.hmcts.reform.judicialapi.elinks.repository.BaseLocationRepository;
import uk.gov.hmcts.reform.judicialapi.elinks.repository.LocationRepository;
import uk.gov.hmcts.reform.judicialapi.elinks.response.ElinkLocationResponse;
import uk.gov.hmcts.reform.judicialapi.elinks.response.ElinkLocationWrapperResponse;
import uk.gov.hmcts.reform.judicialapi.elinks.response.LocationResponse;
import uk.gov.hmcts.reform.judicialapi.elinks.util.ElinkDataIngestionSchedularAudit;

import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.Charset.defaultCharset;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.RefDataElinksConstants.ELINKS_ACCESS_ERROR;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.RefDataElinksConstants.ELINKS_DATA_STORE_ERROR;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.RefDataElinksConstants.ELINKS_ERROR_RESPONSE_BAD_REQUEST;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.RefDataElinksConstants.ELINKS_ERROR_RESPONSE_FORBIDDEN;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.RefDataElinksConstants.ELINKS_ERROR_RESPONSE_NOT_FOUND;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.RefDataElinksConstants.ELINKS_ERROR_RESPONSE_TOO_MANY_REQUESTS;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.RefDataElinksConstants.ELINKS_ERROR_RESPONSE_UNAUTHORIZED;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.RefDataElinksConstants.LOCATION_DATA_LOAD_SUCCESS;


@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"AbbreviationAsWordInName","MemberName"})
class ELinksServiceImplTest {

    @Mock
    BaseLocationRepository baseLocationRepository;

    @Mock
    LocationRepository locationRepository;

    @Spy
    ElinksFeignClient elinksFeignClient;

    @Spy
    private ElinkDataIngestionSchedularAudit elinkDataIngestionSchedularAudit;

    @InjectMocks
    private ELinksServiceImpl eLinksServiceImpl;

    @Test
    void elinksService_load_location_should_return_sucess_msg_with_status_200() throws JsonProcessingException {

        List<LocationResponse> locations = getLocationResponseData();


        ElinkLocationResponse elinkLocationResponse = new ElinkLocationResponse();
        elinkLocationResponse.setResults(locations);


        ObjectMapper mapper = new ObjectMapper();

        String body = mapper.writeValueAsString(elinkLocationResponse);


        when(elinksFeignClient.getLocationDetails()).thenReturn(Response.builder()
                .request(mock(Request.class)).body(body, defaultCharset()).status(HttpStatus.OK.value()).build());

        when(locationRepository.saveAll(anyList())).thenReturn(new ArrayList<>());

        ResponseEntity<ElinkLocationWrapperResponse> responseEntity = eLinksServiceImpl.retrieveLocation();

        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
        assertThat(responseEntity.getBody().getMessage()).contains(LOCATION_DATA_LOAD_SUCCESS);

    }


    @Test
    void elinksService_load_location_should_return_elinksException_when_DataAccessException()
            throws JsonProcessingException {

        List<LocationResponse> locations = getLocationResponseData();


        ElinkLocationResponse elinkLocationResponse = new ElinkLocationResponse();
        elinkLocationResponse.setResults(locations);


        ObjectMapper mapper = new ObjectMapper();

        String body = mapper.writeValueAsString(elinkLocationResponse);

        DataAccessException dataAccessException = mock(DataAccessException.class);

        when(elinksFeignClient.getLocationDetails()).thenReturn(Response.builder()
                .request(mock(Request.class)).body(body, defaultCharset()).status(HttpStatus.OK.value()).build());

        when(locationRepository.saveAll(anyList())).thenThrow(dataAccessException);
        ElinksException thrown = Assertions.assertThrows(ElinksException.class, () -> {
            ResponseEntity<ElinkLocationWrapperResponse> responseEntity = eLinksServiceImpl.retrieveLocation();
        });

        assertThat(thrown.getStatus().value()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());

        assertThat(thrown.getErrorMessage()).contains(ELINKS_DATA_STORE_ERROR);
        assertThat(thrown.getErrorDescription()).contains(ELINKS_DATA_STORE_ERROR);


    }

    @Test
    void elinksService_load_location_should_return_elinksException_when_FeignException()
            throws JsonProcessingException {

        List<LocationResponse> locations = getLocationResponseData();


        ElinkLocationResponse elinkLocationResponse = new ElinkLocationResponse();
        elinkLocationResponse.setResults(locations);

        FeignException feignExceptionMock = Mockito.mock(FeignException.class);


        ObjectMapper mapper = new ObjectMapper();

        String body = mapper.writeValueAsString(elinkLocationResponse);

        when(elinksFeignClient.getLocationDetails()).thenThrow(feignExceptionMock);


        ElinksException thrown = Assertions.assertThrows(ElinksException.class, () -> {
            ResponseEntity<ElinkLocationWrapperResponse> responseEntity = eLinksServiceImpl.retrieveLocation();
        });

        assertThat(thrown.getStatus().value()).isEqualTo(HttpStatus.FORBIDDEN.value());

        assertThat(thrown.getErrorMessage()).contains(ELINKS_ACCESS_ERROR);
        assertThat(thrown.getErrorDescription()).contains(ELINKS_ACCESS_ERROR);


    }

    @Test
    void elinksService_load_location_should_return_elinksException_when_http_BAD_REQUEST()
            throws JsonProcessingException {

        List<LocationResponse> locations = getLocationResponseData();


        ElinkLocationResponse elinkLocationResponse = new ElinkLocationResponse();
        elinkLocationResponse.setResults(locations);

        FeignException feignExceptionMock = Mockito.mock(FeignException.class);


        ObjectMapper mapper = new ObjectMapper();

        String body = mapper.writeValueAsString(elinkLocationResponse);


        when(elinksFeignClient.getLocationDetails()).thenReturn(Response.builder()
                .request(mock(Request.class))
                .body("", defaultCharset()).status(HttpStatus.BAD_REQUEST.value()).build());



        ElinksException thrown = Assertions.assertThrows(ElinksException.class, () -> {
            ResponseEntity<ElinkLocationWrapperResponse> responseEntity = eLinksServiceImpl.retrieveLocation();
        });

        assertThat(thrown.getStatus().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());

        assertThat(thrown.getErrorMessage()).contains(ELINKS_ERROR_RESPONSE_BAD_REQUEST);
        assertThat(thrown.getErrorDescription()).contains(ELINKS_ERROR_RESPONSE_BAD_REQUEST);


    }


    @Test
    void elinksService_load_location_should_return_elinksException_when_http_UNAUTHORIZED()
            throws JsonProcessingException {

        List<LocationResponse> locations = getLocationResponseData();


        ElinkLocationResponse elinkLocationResponse = new ElinkLocationResponse();
        elinkLocationResponse.setResults(locations);

        FeignException feignExceptionMock = Mockito.mock(FeignException.class);


        ObjectMapper mapper = new ObjectMapper();

        String body = mapper.writeValueAsString(elinkLocationResponse);


        when(elinksFeignClient.getLocationDetails()).thenReturn(Response.builder()
                .request(mock(Request.class))
                .body("", defaultCharset()).status(HttpStatus.UNAUTHORIZED.value()).build());



        ElinksException thrown = Assertions.assertThrows(ElinksException.class, () -> {
            ResponseEntity<ElinkLocationWrapperResponse> responseEntity = eLinksServiceImpl.retrieveLocation();
        });

        assertThat(thrown.getStatus().value()).isEqualTo(HttpStatus.UNAUTHORIZED.value());

        assertThat(thrown.getErrorMessage()).contains(ELINKS_ERROR_RESPONSE_UNAUTHORIZED);
        assertThat(thrown.getErrorDescription()).contains(ELINKS_ERROR_RESPONSE_UNAUTHORIZED);


    }

    @Test
    void elinksService_load_location_should_return_elinksException_when_http_FORBIDDEN()
            throws JsonProcessingException {

        List<LocationResponse> locations = getLocationResponseData();


        ElinkLocationResponse elinkLocationResponse = new ElinkLocationResponse();
        elinkLocationResponse.setResults(locations);

        FeignException feignExceptionMock = Mockito.mock(FeignException.class);


        ObjectMapper mapper = new ObjectMapper();

        String body = mapper.writeValueAsString(elinkLocationResponse);


        when(elinksFeignClient.getLocationDetails()).thenReturn(Response.builder()
                .request(mock(Request.class)).body("", defaultCharset()).status(HttpStatus.FORBIDDEN.value()).build());



        ElinksException thrown = Assertions.assertThrows(ElinksException.class, () -> {
            ResponseEntity<ElinkLocationWrapperResponse> responseEntity = eLinksServiceImpl.retrieveLocation();
        });

        assertThat(thrown.getStatus().value()).isEqualTo(HttpStatus.FORBIDDEN.value());

        assertThat(thrown.getErrorMessage()).contains(ELINKS_ERROR_RESPONSE_FORBIDDEN);
        assertThat(thrown.getErrorDescription()).contains(ELINKS_ERROR_RESPONSE_FORBIDDEN);


    }

    @Test
    void elinksService_load_location_should_return_elinksException_when_http_NOT_FOUND()
            throws JsonProcessingException {

        List<LocationResponse> locations = getLocationResponseData();


        ElinkLocationResponse elinkLocationResponse = new ElinkLocationResponse();
        elinkLocationResponse.setResults(locations);

        FeignException feignExceptionMock = Mockito.mock(FeignException.class);


        ObjectMapper mapper = new ObjectMapper();

        String body = mapper.writeValueAsString(elinkLocationResponse);


        when(elinksFeignClient.getLocationDetails()).thenReturn(Response.builder()
                .request(mock(Request.class)).body("", defaultCharset()).status(HttpStatus.NOT_FOUND.value()).build());



        ElinksException thrown = Assertions.assertThrows(ElinksException.class, () -> {
            ResponseEntity<ElinkLocationWrapperResponse> responseEntity = eLinksServiceImpl.retrieveLocation();
        });

        assertThat(thrown.getStatus().value()).isEqualTo(HttpStatus.NOT_FOUND.value());

        assertThat(thrown.getErrorMessage()).contains(ELINKS_ERROR_RESPONSE_NOT_FOUND);
        assertThat(thrown.getErrorDescription()).contains(ELINKS_ERROR_RESPONSE_NOT_FOUND);


    }

    @Test
    void elinksService_load_location_should_return_elinksException_when_http_TOO_MANY_REQUESTS()
            throws JsonProcessingException {

        List<LocationResponse> locations = getLocationResponseData();


        ElinkLocationResponse elinkLocationResponse = new ElinkLocationResponse();
        elinkLocationResponse.setResults(locations);

        FeignException feignExceptionMock = Mockito.mock(FeignException.class);


        ObjectMapper mapper = new ObjectMapper();

        String body = mapper.writeValueAsString(elinkLocationResponse);


        when(elinksFeignClient.getLocationDetails()).thenReturn(Response.builder()
                .request(mock(Request.class))
                .body("", defaultCharset()).status(HttpStatus.TOO_MANY_REQUESTS.value()).build());



        ElinksException thrown = Assertions.assertThrows(ElinksException.class, () -> {
            ResponseEntity<ElinkLocationWrapperResponse> responseEntity = eLinksServiceImpl.retrieveLocation();
        });

        assertThat(thrown.getStatus().value()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());

        assertThat(thrown.getErrorMessage()).contains(ELINKS_ERROR_RESPONSE_TOO_MANY_REQUESTS);
        assertThat(thrown.getErrorDescription()).contains(ELINKS_ERROR_RESPONSE_TOO_MANY_REQUESTS);


    }

    private List<LocationResponse> getLocationResponseData() {


        LocationResponse locationOne = new LocationResponse();
        locationOne.setId("1");
        locationOne.setName("National");
        locationOne.setCreatedAt("2022-10-03T15:28:19Z");
        locationOne.setUpdatedAt("2022-10-03T15:28:19Z");


        List<LocationResponse> locations = new ArrayList<>();

        locations.add(locationOne);

        return locations;

    }

}