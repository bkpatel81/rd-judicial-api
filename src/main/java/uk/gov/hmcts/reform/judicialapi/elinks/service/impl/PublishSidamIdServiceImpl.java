package uk.gov.hmcts.reform.judicialapi.elinks.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.judicialapi.elinks.configuration.ElinkEmailConfiguration;
import uk.gov.hmcts.reform.judicialapi.elinks.exception.ElinksException;
import uk.gov.hmcts.reform.judicialapi.elinks.response.SchedulerJobStatusResponse;
import uk.gov.hmcts.reform.judicialapi.elinks.service.IEmailService;
import uk.gov.hmcts.reform.judicialapi.elinks.service.PublishSidamIdService;
import uk.gov.hmcts.reform.judicialapi.elinks.service.dto.Email;
import uk.gov.hmcts.reform.judicialapi.elinks.servicebus.ElinkTopicPublisher;
import uk.gov.hmcts.reform.judicialapi.elinks.util.RefDataConstants;

import java.util.List;
import java.util.Optional;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.JobStatus.FAILED;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.JobStatus.IN_PROGRESS;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.JobStatus.SUCCESS;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.RefDataConstants.CONTENT_TYPE_PLAIN;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.RefDataElinksConstants.DATABASE_FETCH_ERROR;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.RefDataElinksConstants.JOB_DETAILS_UPDATE_ERROR;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.SqlContants.GET_DISTINCT_SIDAM_ID;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.SqlContants.SELECT_JOB_STATUS_SQL;
import static uk.gov.hmcts.reform.judicialapi.elinks.util.SqlContants.UPDATE_JOB_SQL;


@Slf4j
@Component
public class PublishSidamIdServiceImpl implements PublishSidamIdService {

    private static final String ZERO = "0";

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Value("${logging-component-name}")
    String logComponentName;

    @Autowired
    ElinkTopicPublisher elinkTopicPublisher;

    @Autowired
    IEmailService emailService;

    @Autowired
    ElinkEmailConfiguration emailConfiguration;

    @Value("${launchdarkly.sdk.environment}")
    String environment;

    private int sidamIdcount;

    public ResponseEntity<SchedulerJobStatusResponse> publishSidamIdToAsb() {

        //Get the job details from dataload_schedular_job table
        Pair<String, String> jobDetails;
        try {
            jobDetails = getJobDetails(SELECT_JOB_STATUS_SQL);
        } catch (Exception ex) {
            throw new ElinksException(HttpStatus.BAD_REQUEST, DATABASE_FETCH_ERROR, ex.getMessage());
        }

        // Get all sidam id's from the judicial_user_profile table
        List<String> sidamIds = jdbcTemplate.query(GET_DISTINCT_SIDAM_ID, RefDataConstants.ROW_MAPPER);

        sidamIdcount = sidamIds.size();

        log.info("{}::Total SIDAM Id count from JUD_Database: {}", logComponentName, sidamIdcount);
        if (isEmpty(sidamIds)) {
            log.warn("{}:: No Sidam id exists in JRD for publishing in ASB for JOB id: {} ",
                logComponentName, jobDetails.getLeft());
            updateAsbStatus(jobDetails.getLeft(), SUCCESS.getStatus());
        }

        publishMessage(jobDetails.getRight(), sidamIds, jobDetails.getLeft());
        jobDetails = getJobDetails(SELECT_JOB_STATUS_SQL);
        log.info("{}:: completed Publish SidamId to ASB with JOB Id: {}  ",
            logComponentName, jobDetails.getLeft());

        SchedulerJobStatusResponse response = SchedulerJobStatusResponse.builder().id(jobDetails.getLeft())
            .jobStatus(jobDetails.getRight()).sidamIdsCount(sidamIdcount).statusCode(HttpStatus.OK.value()).build();

        return ResponseEntity.status(HttpStatus.OK).body(response);

    }

    private Pair<String, String> getJobDetails(String jobStatusQuery) {
        Optional<Pair<String, String>> pair = getElinkJobStatus(jobStatusQuery);

        final String jobId = pair.map(Pair::getLeft).orElse(ZERO);
        final String jobStatus = pair.map(Pair::getRight).orElse(EMPTY);
        return Pair.of(jobId, jobStatus);
    }

    private Optional<Pair<String, String>> getElinkJobStatus(String jobStatusQuery) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(jobStatusQuery, (resultSet, i) ->
                Pair.of(resultSet.getString(1), resultSet.getString(2))));
        } catch (EmptyResultDataAccessException ex) {
            log.info("No record found in table dataload_schedular_job");
            return Optional.empty();
        }
    }


    public void publishMessage(String status, List<String> sidamIds, String jobId) {
        try {
            if ((IN_PROGRESS.getStatus().equals(status)) && isNotEmpty(sidamIds)) {
                //Publish or retry Message in ASB
                log.info("{}:: Publishing/Retrying JRD messages in ASB for Job Id {}", logComponentName, jobId);
                elinkTopicPublisher.sendMessage(sidamIds, jobId);
                updateAsbStatus(jobId, SUCCESS.getStatus());
                log.info("{}:: Updated Total distinct Sidam Ids to ASB: {}", logComponentName, sidamIdcount);
            }
        } catch (Exception ex) {
            log.error("ASB Failure Root cause - {}", ex.getMessage());
            ElinkEmailConfiguration.MailTypeConfig mailTypeConfig = emailConfiguration.getMailTypes().get("asb");
            final String logMessage = String.format(mailTypeConfig.getSubject(), jobId);
            log.error("{}:: {}", logComponentName, logMessage);
            updateAsbStatus(jobId, FAILED.getStatus());
            if (mailTypeConfig.isEnabled()) {
                Email email = Email.builder()
                    .contentType(CONTENT_TYPE_PLAIN)
                    .from(mailTypeConfig.getFrom())
                    .to(mailTypeConfig.getTo())
                    .messageBody(String.format(mailTypeConfig.getBody(), jobId))
                    .subject(String.format(mailTypeConfig.getSubject(), environment))
                    .build();
                emailService.sendEmail(email);
            }
            throw ex;
        }
    }

    private void updateAsbStatus(String jobId,String jobStatus) {
        //Update elinks DB with Publishing Status
        try {
            jdbcTemplate.update(UPDATE_JOB_SQL, jobStatus, Integer.valueOf(jobId));
        } catch (Exception ex) {
            throw new ElinksException(HttpStatus.FORBIDDEN, JOB_DETAILS_UPDATE_ERROR, JOB_DETAILS_UPDATE_ERROR);
        }

    }
}
