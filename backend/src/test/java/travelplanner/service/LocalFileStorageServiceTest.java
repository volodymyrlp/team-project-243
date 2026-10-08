package travelplanner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import travelplanner.exception.InvalidFileException;

class LocalFileStorageServiceTest {

    @TempDir
    private Path tempDir;

    private LocalFileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new LocalFileStorageService(tempDir.toString());
    }

    @Test
    void uploadFile_Success_WithExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "vacation.png",
                "image/png",
                "image-content".getBytes()
        );

        String result = fileStorageService.uploadFile(file);

        assertNotNull(result);
        assertTrue(result.startsWith("/uploads/"));
        assertTrue(result.endsWith(".png"));

        String filename = result.substring("/uploads/".length());
        Path uploadedPath = tempDir.resolve(filename);
        assertTrue(Files.exists(uploadedPath));
    }

    @Test
    void uploadFile_Success_WithoutExtension() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "vacationfile",
                "application/octet-stream",
                "raw-content".getBytes()
        );

        String result = fileStorageService.uploadFile(file);

        assertNotNull(result);
        assertTrue(result.startsWith("/uploads/"));
        assertFalse(result.contains("."));

        String filename = result.substring("/uploads/".length());
        Path uploadedPath = tempDir.resolve(filename);
        assertTrue(Files.exists(uploadedPath));
    }

    @Test
    void uploadFile_Success_NullOriginalFilename() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                null,
                "image/jpeg",
                "jpeg-content".getBytes()
        );

        String result = fileStorageService.uploadFile(file);

        assertNotNull(result);
        assertTrue(result.startsWith("/uploads/"));

        String filename = result.substring("/uploads/".length());
        Path uploadedPath = tempDir.resolve(filename);
        assertTrue(Files.exists(uploadedPath));
    }

    @Test
    void uploadFile_Failure_EmptyFile_ThrowsInvalidFileException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        InvalidFileException ex = assertThrows(
                InvalidFileException.class,
                () -> fileStorageService.uploadFile(emptyFile)
        );

        assertEquals("Uploaded file is empty", ex.getMessage());
    }

    @Test
    void uploadFile_Failure_IoException_ThrowsInvalidFileException() throws Exception {
        MultipartFile mockFile = mock(MultipartFile.class);
        when(mockFile.isEmpty()).thenReturn(false);
        when(mockFile.getOriginalFilename()).thenReturn("photo.jpg");
        when(mockFile.getInputStream()).thenThrow(new IOException("Simulated disk error"));

        InvalidFileException ex = assertThrows(
                InvalidFileException.class,
                () -> fileStorageService.uploadFile(mockFile)
        );

        assertTrue(ex.getMessage().contains("Could not store file"));
        assertTrue(ex.getMessage().contains("Simulated disk error"));
    }
}
