// -*- coding: utf-8 -*-
package com.modularmc.ten.integration.jei;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contract test for JEI plugin SPI discovery descriptor.
 * <p>
 * JEI 29 discovers plugins via {@code java.util.ServiceLoader} mechanism:
 * a file named {@code META-INF/services/mezz.jei.api.IModPlugin} must exist
 * in the classpath resources with the fully-qualified class name of the
 * {@link mezz.jei.api.IModPlugin} implementation. The {@code @JeiPlugin}
 * annotation alone is <b>not sufficient</b> for JEI 29's SPI-based discovery.
 * <p>
 * These tests verify the presence, content, and well-formedness of the
 * SPI service descriptor in the client resource source set.
 */
class JeiPluginDiscoveryContractTest {

    // ═══════════════════════════════════════════════════════════════════
    // Paths
    // ═══════════════════════════════════════════════════════════════════

    private static final String EXPECTED_FQCN =
            "com.modularmc.ten.integration.jei.TENJeiPlugin";

    /**
     * The SPI service descriptor path in the client resource source set.
     * {@code src/client/resources/} is the default resources directory for
     * the {@code client} source set by Gradle convention.
     */
    private static final File SPI_DESCRIPTOR = new File(
            "src/client/resources/META-INF/services/mezz.jei.api.IModPlugin");

    // ═══════════════════════════════════════════════════════════════════
    // 1. SPI descriptor existence
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class SpiDescriptorExistence {

        @Test
        void spiDescriptorFile_exists() {
            assertTrue(SPI_DESCRIPTOR.isFile(),
                    "SPI service descriptor must exist at: "
                            + "src/client/resources/META-INF/services/mezz.jei.api.IModPlugin"
                            + "\n  JEI 29 requires this file for plugin discovery;"
                            + " @JeiPlugin annotation alone is insufficient.");
        }

        @Test
        void spiDescriptorFile_notEmpty() throws Exception {
            assertTrue(SPI_DESCRIPTOR.isFile(),
                    "SPI descriptor must exist before checking content");
            String content = Files.readString(SPI_DESCRIPTOR.toPath(), StandardCharsets.UTF_8);
            assertFalse(content.isBlank(),
                    "SPI descriptor must not be blank — it must contain the plugin FQCN");
        }

        @Test
        void spiDescriptorFile_notOnlyWhitespace() throws Exception {
            assertTrue(SPI_DESCRIPTOR.isFile(),
                    "SPI descriptor must exist before checking content");
            String content = Files.readString(SPI_DESCRIPTOR.toPath(), StandardCharsets.UTF_8);
            String trimmed = content.trim();
            assertFalse(trimmed.isEmpty(),
                    "SPI descriptor must contain non-whitespace content");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 2. SPI descriptor content — exact FQCN match
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class SpiDescriptorContent {

        @Test
        void spiDescriptor_containsExactFqcn() throws Exception {
            assertTrue(SPI_DESCRIPTOR.isFile(),
                    "SPI descriptor must exist before checking content");
            String content = Files.readString(SPI_DESCRIPTOR.toPath(), StandardCharsets.UTF_8);
            String trimmed = content.trim();

            assertEquals(EXPECTED_FQCN, trimmed,
                    "SPI descriptor must contain exactly the FQCN of TENJeiPlugin"
                            + "\n  Expected: " + EXPECTED_FQCN
                            + "\n  Actual:   [" + trimmed + "]");
        }

        @Test
        void spiDescriptor_hasNoLeadingWhitespace() throws Exception {
            assertTrue(SPI_DESCRIPTOR.isFile(),
                    "SPI descriptor must exist before checking content");
            String content = Files.readString(SPI_DESCRIPTOR.toPath(), StandardCharsets.UTF_8);
            assertFalse(content.startsWith(" ") || content.startsWith("\t"),
                    "SPI descriptor must not have leading whitespace");
        }

        @Test
        void spiDescriptor_hasTrailingNewlineOnly() throws Exception {
            assertTrue(SPI_DESCRIPTOR.isFile(),
                    "SPI descriptor must exist before checking content");
            String content = Files.readString(SPI_DESCRIPTOR.toPath(), StandardCharsets.UTF_8);
            // Should be "FQCN\n" — exactly one trailing newline, nothing else
            String expected = EXPECTED_FQCN + "\n";
            assertEquals(expected, content,
                    "SPI descriptor must be exactly 'FQCN\\n' (FQCN + single trailing newline)"
                            + "\n  Expected: [" + expected + "]"
                            + "\n  Actual:   [" + content + "]");
        }

        @Test
        void spiDescriptor_noExtraLinesAfterFqcn() throws Exception {
            assertTrue(SPI_DESCRIPTOR.isFile(),
                    "SPI descriptor must exist before checking content");
            List<String> lines = Files.readAllLines(SPI_DESCRIPTOR.toPath(), StandardCharsets.UTF_8);
            // Filter out empty trailing lines (the file should have exactly 1 content line)
            long nonEmptyLines = lines.stream()
                    .filter(l -> !l.isEmpty())
                    .count();
            assertEquals(1, nonEmptyLines,
                    "SPI descriptor must contain exactly one non-empty line (the FQCN)");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 3. SPI descriptor — single provider, valid package
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class SpiDescriptorProvider {

        @Test
        void spiDescriptor_hasExactlyOneProvider() throws Exception {
            assertTrue(SPI_DESCRIPTOR.isFile(),
                    "SPI descriptor must exist before checking content");
            List<String> lines = Files.readAllLines(SPI_DESCRIPTOR.toPath(), StandardCharsets.UTF_8);
            long providerLines = lines.stream()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                    .count();
            assertEquals(1, providerLines,
                    "SPI descriptor must list exactly one provider (our TENJeiPlugin)");
        }

        @Test
        void spiDescriptor_noCommentsOrPreamble() throws Exception {
            assertTrue(SPI_DESCRIPTOR.isFile(),
                    "SPI descriptor must exist before checking content");
            List<String> lines = Files.readAllLines(SPI_DESCRIPTOR.toPath(), StandardCharsets.UTF_8);
            boolean hasComment = lines.stream().anyMatch(l -> l.trim().startsWith("#"));
            assertFalse(hasComment,
                    "SPI descriptor must not contain comments (JEI SPI parser reads every line as a class name)");
        }

        @Test
        void spiDescriptor_fqcnMatchesJeiPluginClass() throws Exception {
            assertTrue(SPI_DESCRIPTOR.isFile(),
                    "SPI descriptor must exist before checking content");
            String content = Files.readString(SPI_DESCRIPTOR.toPath(), StandardCharsets.UTF_8);
            String trimmed = content.trim();

            // Verify the FQCN does not contain typos, wrong packages, or encoding artifacts
            assertTrue(trimmed.startsWith("com.modularmc.ten"),
                    "FQCN must start with com.modularmc.ten");
            assertTrue(trimmed.endsWith("TENJeiPlugin"),
                    "FQCN must end with TENJeiPlugin");
            assertFalse(trimmed.contains(" "),
                    "FQCN must not contain spaces");
            assertFalse(trimmed.contains("\t"),
                    "FQCN must not contain tabs");
            assertFalse(trimmed.contains(".."),
                    "FQCN must not contain double dots");
            assertFalse(trimmed.contains("/"),
                    "FQCN must use dots (not slashes) as package separators");
            assertFalse(trimmed.contains("\\"),
                    "FQCN must use dots (not backslashes) as package separators");
            assertFalse(trimmed.contains("\u0000"),
                    "FQCN must not contain null bytes");
        }

        @Test
        void spiDescriptor_notHtmlOrBinaryFile() throws Exception {
            assertTrue(SPI_DESCRIPTOR.isFile(),
                    "SPI descriptor must exist before checking content");
            byte[] bytes = Files.readAllBytes(SPI_DESCRIPTOR.toPath());
            // Service file is plain ASCII (Java FQCN)
            boolean allAscii = true;
            for (byte b : bytes) {
                if (b < 0 || b > 127) {
                    allAscii = false;
                    break;
                }
            }
            assertTrue(allAscii,
                    "SPI descriptor must be plain ASCII (Java FQCN + newline only)");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 4. Run resource output — SPI must be in clientExtra resources dir
    // ═══════════════════════════════════════════════════════════════════
    // This guards against ModDevGradle's run classpath not including
    // build/resources/client.  By verifying the SPI descriptor exists
    // in the clientExtra output (the actual run sourceSet), we ensure
    // JEI ServiceLoader can discover TENJeiPlugin at dev runtime.
    // Before the sourceSet fix (RED): processClientExtraResources is
    // NO-SOURCE and this test fails.
    // After the sourceSet fix (GREEN): processClientExtraResources copies
    // the SPI descriptor and this test passes.

    @Nested
    class SpiDescriptorInRunOutput {

        /** The path where processClientExtraResources should put the SPI. */
        private static final File CLIENTEXTRA_SPI = new File(
                "build/resources/clientExtra/META-INF/services/mezz.jei.api.IModPlugin");

        @Test
        void spiDescriptor_existsInClientExtraResources() {
            assertTrue(CLIENTEXTRA_SPI.isFile(),
                    "SPI descriptor must exist in clientExtra resources output: "
                            + CLIENTEXTRA_SPI
                            + "\n  runClient uses clientExtra as its sourceSet;"
                            + " without this file JEI cannot discover TENJeiPlugin.");
        }

        @Test
        void spiDescriptor_contentInClientExtraResources() throws Exception {
            assertTrue(CLIENTEXTRA_SPI.isFile(),
                    "SPI descriptor must exist before checking content");
            String content = Files.readString(CLIENTEXTRA_SPI.toPath(), StandardCharsets.UTF_8);
            assertEquals("com.modularmc.ten.integration.jei.TENJeiPlugin\n", content,
                    "clientExtra SPI descriptor content must match expected FQCN + newline");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 5. Client-only — descriptor must NOT be in main resources
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class SpiDescriptorClientOnly {

        @Test
        void spiDescriptor_notInMainResources() {
            var mainResourcesSp = new File(
                    "src/main/resources/META-INF/services/mezz.jei.api.IModPlugin");
            assertFalse(mainResourcesSp.exists(),
                    "SPI descriptor must NOT be in main resources"
                            + "\n  Client-only classes (TENJeiPlugin) would cause"
                            + " ClassNotFoundException on dedicated server");
        }

        @Test
        void spiDescriptor_notInGeneratedResources() {
            var generatedResourcesSp = new File(
                    "src/generated/resources/META-INF/services/mezz.jei.api.IModPlugin");
            assertFalse(generatedResourcesSp.exists(),
                    "SPI descriptor must NOT be in generated resources");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 6. Cross-reference — the target class exists in client source set
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    class TargetClassExistence {

        @Test
        void tenJeiPluginClass_exists() {
            var pluginSource = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/TENJeiPlugin.java");
            assertTrue(pluginSource.isFile(),
                    "TENJeiPlugin.java must exist in client source set");
        }

        @Test
        void tenJeiPluginClass_implementsIModPlugin() throws Exception {
            var pluginSource = new File(
                    "src/client/java/com/modularmc/ten/integration/jei/TENJeiPlugin.java");
            assertTrue(pluginSource.isFile());
            String content = Files.readString(pluginSource.toPath());

            assertTrue(content.contains("implements IModPlugin"),
                    "TENJeiPlugin must implement IModPlugin");
            assertTrue(content.contains("@JeiPlugin"),
                    "TENJeiPlugin must have @JeiPlugin annotation");
        }

        @Test
        void spiDescriptorPackage_matchesPluginPackage() throws Exception {
            assertTrue(SPI_DESCRIPTOR.isFile(),
                    "SPI descriptor must exist before cross-reference");
            String fqcn = Files.readString(SPI_DESCRIPTOR.toPath(), StandardCharsets.UTF_8).trim();
            // Derive the expected source path from the FQCN
            String expectedSourcePath = "src/client/java/"
                    + fqcn.replace('.', '/') + ".java";
            var expectedSourceFile = new File(expectedSourcePath);
            assertTrue(expectedSourceFile.isFile(),
                    "SPI descriptor FQCN must resolve to an existing source file"
                            + "\n  FQCN:        " + fqcn
                            + "\n  Source path: " + expectedSourcePath);
        }
    }
}
