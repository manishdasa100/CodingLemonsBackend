package com.codinglemonsbackend.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.codinglemonsbackend.Properties.S3Properties;
import com.codinglemonsbackend.Repository.UserProfileRepository;

/**
 * The upload and its rollback used to spell the S3 key out separately, and the two spellings
 * drifted: uploads went to "users/..." while the rollback deleted "profile-picture/...". Nothing
 * failed loudly - the delete simply matched no object and every failed upload stayed in the bucket
 * forever. Only a test that compares the two keys catches that.
 */
class ProfilePictureRollbackTest {

    private static final String BUCKET = "codinglemons-assets";

    @Test
    void rollbackDeletesTheObjectTheUploadActuallyWrote() {
        S3Service s3Service = mock(S3Service.class);
        S3Properties s3Properties = mock(S3Properties.class);
        UserProfileRepository userProfileRepository = mock(UserProfileRepository.class);
        when(s3Properties.getBucket()).thenReturn(BUCKET);

        // The database write is what fails, so the picture is already in the bucket by then.
        doThrow(new IllegalStateException("mongo is down"))
                .when(userProfileRepository).updateUserProfile(anyString(), anyMap());

        UserProfileService service = new UserProfileService(
                userProfileRepository, null, s3Service, s3Properties, null, null, null, null);

        assertThrows(IllegalStateException.class,
                () -> service.uploadUserProfilePicture("ada", new byte[] { 1, 2, 3 }));

        ArgumentCaptor<String> uploadedKey = ArgumentCaptor.forClass(String.class);
        verify(s3Service).putObject(eq(BUCKET), uploadedKey.capture(), any());

        ArgumentCaptor<String> deletedKey = ArgumentCaptor.forClass(String.class);
        verify(s3Service).deleteObject(eq(BUCKET), deletedKey.capture());

        assertEquals(uploadedKey.getValue(), deletedKey.getValue(),
                "the rollback deleted a different key than the upload wrote, so the object leaked");
    }
}
