package uk.gov.hmcts.reform.judicialapi.controller.advice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.AccessDeniedException;
import javax.validation.ConstraintViolationException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.client.HttpStatusCodeException;

@RunWith(MockitoJUnitRunner.class)
public class ExceptionMapperUnitTest {

    @InjectMocks
    private ExceptionMapper exceptionMapper;

    @Test
    public void should_handle_empty_result_exception() {
        EmptyResultDataAccessException emptyResultDataAccessException = mock(EmptyResultDataAccessException.class);

        ResponseEntity<Object> responseEntity = exceptionMapper.handleEmptyResultDataAccessException(emptyResultDataAccessException);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Test
    public void should_handle_resource_not_found_exception() {
        ResourceNotFoundException resourceNotFoundException = mock(ResourceNotFoundException.class);

        ResponseEntity<Object> responseEntity = exceptionMapper.handleResourceNotFoundException(resourceNotFoundException);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Test
    public void should_handle_illegal_argument_exception() {
        IllegalArgumentException exception = mock(IllegalArgumentException.class);

        ResponseEntity<Object> responseEntity = exceptionMapper.handleIllegalArgumentException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void should_handle_http_message_not_readable_exception() {
        HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);

        ResponseEntity<Object> responseEntity = exceptionMapper.httpMessageNotReadableExceptionError(exception);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void should_handle_http_media_type_not_supported_exception() {
        IllegalArgumentException exception = mock(IllegalArgumentException.class);

        ResponseEntity<Object> responseEntity = exceptionMapper.handleHttpMediaTypeNotSupported(exception);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void should_handle_forbidden_error_exception() {
        AccessDeniedException exception = mock(AccessDeniedException.class);

        ResponseEntity<Object> responseEntity = exceptionMapper.handleForbiddenException(exception);

        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
    }

    @Test
    public void should_handle_http_status_code_exception() {
        HttpStatusCodeException exception = mock(HttpStatusCodeException.class);
        HttpStatus httpStatus = mock(HttpStatus.class);

        when(exception.getStatusCode()).thenReturn(httpStatus);

        ResponseEntity<Object> responseEntity = exceptionMapper.handleHttpStatusException(exception);
        assertNotNull(responseEntity.getStatusCode());
    }

    @Test
    public void should_handle_exception() {
        Exception exception = mock(Exception.class);

        ResponseEntity<Object> responseEntity = exceptionMapper.handleException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
    }

    @Test
    public void should_handle_method_not_valid_exception() {
        MethodArgumentNotValidException methodArgumentNotValidException = mock(MethodArgumentNotValidException.class);

        ResponseEntity<Object> responseEntity = exceptionMapper.annotationDrivenValidationError(methodArgumentNotValidException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void should_handle_invalid_request_exception() {
        InvalidRequest invalidRequestException = mock(InvalidRequest.class);

        ResponseEntity<Object> responseEntity = exceptionMapper.customValidationError(invalidRequestException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void should_handle_duplicate_key_exception() {
        DuplicateKeyException duplicateKeyException = mock(DuplicateKeyException.class);

        ResponseEntity<Object> responseEntity = exceptionMapper.duplicateKeyException(duplicateKeyException);

        assertEquals(HttpStatus.CONFLICT, responseEntity.getStatusCode());
    }

    @Test
    public void should_handle_constraint_violation_exception() {
        ConstraintViolationException constraintViolationException = mock(ConstraintViolationException.class);

        ResponseEntity<Object> responseEntity = exceptionMapper.constraintViolationError(constraintViolationException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Test
    public void should_handle_data_integrity_violation_exception() {
        DataIntegrityViolationException dataIntegrityViolationException = mock(DataIntegrityViolationException.class);

        ResponseEntity<Object> responseEntity = exceptionMapper.dataIntegrityViolationError(dataIntegrityViolationException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    private ResponseEntity<Object> generateCustomDataIntegrityViolationResponseMessageForSpecificCause(String errorCause) {
        DataIntegrityViolationException dataIntegrityViolationException = mock(DataIntegrityViolationException.class);

        Throwable throwable = mock(Throwable.class);
        Throwable throwable1 = mock(Throwable.class);

        when(dataIntegrityViolationException.getCause()).thenReturn(throwable);
        when(dataIntegrityViolationException.getCause().getCause()).thenReturn(throwable1);
        when(dataIntegrityViolationException.getCause().getCause().getMessage()).thenReturn(errorCause);

        return exceptionMapper.dataIntegrityViolationError(dataIntegrityViolationException);
    }
}
