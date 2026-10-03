package com.codinglemonsbackend.Service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;

import com.codinglemonsbackend.Dto.SupportedLanguage;

/**
 * The archive is what Judge0 unpacks next to the driver. A wrapping directory or a wrong entry
 * name leaves the driver's include/import unresolved, which surfaces as a confusing compile
 * error rather than anything pointing back here - so assert the layout directly.
 */
class Judge0SolutionArchiveTest {

    private static final String USER_CODE = """
            class Solution {
            public:
                int climbStairs(int n) { return n; }
            };
            """;

    private record Entry(String name, byte[] content) {}

    private static List<Entry> unzip(String base64Archive) throws Exception {
        List<Entry> entries = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(
                new ByteArrayInputStream(Base64.getDecoder().decode(base64Archive)))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                entries.add(new Entry(entry.getName(), zip.readAllBytes()));
            }
        }
        return entries;
    }

    private static String encoded(String plain) {
        return Base64.getEncoder().encodeToString(plain.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void archiveHoldsExactlyTheSolutionFileAtTheRoot() throws Exception {
        record Case(SupportedLanguage language, String expectedEntry) {}
        List<Case> cases = List.of(
                new Case(SupportedLanguage.CPP, "solution.cpp"),
                new Case(SupportedLanguage.PYTHON, "solution.py"),
                new Case(SupportedLanguage.JAVA, "Solution.java"));

        for (Case testCase : cases) {
            List<Entry> entries = unzip(
                    Judge0ExecutionServiceImpl.solutionArchive(testCase.language(), encoded(USER_CODE)));

            assertEquals(1, entries.size(), testCase.language() + " archive must hold exactly one file");
            assertEquals(testCase.expectedEntry(), entries.get(0).name());
            assertEquals(-1, entries.get(0).name().indexOf('/'),
                    testCase.language() + " archive must not wrap the solution in a directory");
        }
    }

    @Test
    void archiveRoundTripsTheUserCodeByteForByte() throws Exception {
        List<Entry> entries = unzip(
                Judge0ExecutionServiceImpl.solutionArchive(SupportedLanguage.CPP, encoded(USER_CODE)));

        assertArrayEquals(USER_CODE.getBytes(StandardCharsets.UTF_8), entries.get(0).content());
    }

    @Test
    void unicodeUserCodeSurvivesTheRoundTrip() throws Exception {
        String withUnicode = "# éà中文 🚀\nclass Solution: pass\n";

        List<Entry> entries = unzip(
                Judge0ExecutionServiceImpl.solutionArchive(SupportedLanguage.PYTHON, encoded(withUnicode)));

        assertEquals(withUnicode, new String(entries.get(0).content(), StandardCharsets.UTF_8));
    }

    @Test
    void base64OutputCarriesNoLineBreaks() {
        // getMimeEncoder would wrap at 76 chars and corrupt the archive Judge0 receives.
        String archive = Judge0ExecutionServiceImpl.solutionArchive(
                SupportedLanguage.JAVA, encoded(USER_CODE.repeat(40)));

        assertEquals(-1, archive.indexOf('\n'));
        assertEquals(-1, archive.indexOf('\r'));
    }

    @Test
    void lineWrappedBase64FromClientsIsAccepted() throws Exception {
        // openssl base64 and Python's base64.encodebytes both wrap; the NSJAIL path never decodes,
        // so rejecting these here would make the two executors disagree about valid input.
        String wrapped = Base64.getMimeEncoder().encodeToString(USER_CODE.getBytes(StandardCharsets.UTF_8));

        List<Entry> entries = unzip(Judge0ExecutionServiceImpl.solutionArchive(SupportedLanguage.CPP, wrapped));

        assertArrayEquals(USER_CODE.getBytes(StandardCharsets.UTF_8), entries.get(0).content());
    }

    @Test
    void malformedUserCodeFailsLoudly() {
        assertThrows(IllegalArgumentException.class,
                () -> Judge0ExecutionServiceImpl.solutionArchive(SupportedLanguage.CPP, "not-base64!!"));
    }
}
