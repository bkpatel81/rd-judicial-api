package uk.gov.hmcts.reform.judicialapi.elinks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.judicialapi.elinks.domain.UserProfile;
import uk.gov.hmcts.reform.judicialapi.elinks.repository.ProfileRepository;
import uk.gov.hmcts.reform.judicialapi.elinks.response.ElinkLeaversWrapperResponse;
import uk.gov.hmcts.reform.judicialapi.elinks.util.ElinksEnabledIntegrationTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


class LeaversIntegrationTest extends ElinksEnabledIntegrationTest {

    @Autowired
    private ProfileRepository profileRepository;

    @BeforeEach
    void setUp() {

    }

    @DisplayName("Elinks Leavers endpoint status verification")
    @Test
    void getLeaversUserProfile() {

        Map<String, Object> response = elinksReferenceDataClient.getLeavers();
        assertThat(response).containsEntry("http_status", "200 OK");
        ElinkLeaversWrapperResponse profiles = (ElinkLeaversWrapperResponse)response.get("body");
        assertEquals("Leavers Data Loaded Successfully", profiles.getMessage());
    }

    @DisplayName("Elinks Leavers to JRD user profile verification")
    @Test
    void verifyLeaversJrdUserProfile() {
        Map<String, Object> response = elinksReferenceDataClient.getPeoples();
        Map<String, Object> leaversResponse = elinksReferenceDataClient.getLeavers();
        assertThat(leaversResponse).containsEntry("http_status", "200 OK");
        ElinkLeaversWrapperResponse profiles = (ElinkLeaversWrapperResponse)leaversResponse.get("body");
        assertEquals("Leavers Data Loaded Successfully", profiles.getMessage());

        List<UserProfile> userprofile = profileRepository.findAll();

        assertEquals(1, userprofile.size());
        assertEquals("0049931063", userprofile.get(0).getPersonalCode());
        assertEquals("2021-02-24", userprofile.get(0).getLastWorkingDate().toString());
        assertEquals(false, userprofile.get(0).getActiveFlag());
        assertEquals("552da697-4b3d-4aed-9c22-1e903b70aead", userprofile.get(0).getObjectId());

    }
}