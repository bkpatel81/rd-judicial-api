package uk.gov.hmcts.reform.judicialapi.elinks.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.judicialapi.elinks.domain.ElinkDataSchedularAudit;
import uk.gov.hmcts.reform.judicialapi.elinks.repository.ElinkSchedularAuditRepository;

import java.time.LocalDateTime;

import static java.util.Objects.nonNull;

@Component
@Slf4j
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class ElinkDataIngestionSchedularAudit {
    @Value("${loggingComponentName}")
    private String loggingComponentName;

    @Autowired
    private ElinkSchedularAuditRepository elinkSchedularAuditRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auditSchedulerStatus(String schedulerName, LocalDateTime schedulerStartTime,
                                     LocalDateTime schedulerEndTime, String status, String apiName) {

        ElinkDataSchedularAudit audit = new ElinkDataSchedularAudit();
        try {
            if (nonNull(schedulerEndTime)) {
                audit = elinkSchedularAuditRepository.findBySchedulerStartTime(schedulerStartTime);
                audit.setSchedulerEndTime(schedulerEndTime);
            }
            audit.setSchedulerName(schedulerName);
            audit.setSchedulerStartTime(schedulerStartTime);
            audit.setStatus(status);
            audit.setApiName(apiName);

            elinkSchedularAuditRepository.save(audit);
        } catch (Exception e) {
            log.error("{}:: Failure error Message {} in auditSchedulerStatus {}  ",
                loggingComponentName, e.getMessage(), schedulerName);
        }

    }

}
