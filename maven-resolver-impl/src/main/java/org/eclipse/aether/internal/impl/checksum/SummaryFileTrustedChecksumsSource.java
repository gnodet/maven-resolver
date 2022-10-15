/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.eclipse.aether.internal.impl.checksum;

import static java.util.Objects.requireNonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.MultiRuntimeException;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.util.artifact.ArtifactIdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Compact file {@link FileTrustedChecksumsSourceSupport} implementation that use specified directory as base
 * directory, where it expects a "summary" file named as "checksums.${checksumExt}" for each checksum algorithm, and
 * file format is artifact ID and checksum separated by space per line. The format supports comments "#" (hash) and
 * empty lines (both are ignored).
 * <p>
 * The source may be configured to be "origin aware", in that case it will factor in origin repository ID as well into
 * file name (for example "checksums-central.sha1").
 * <p>
 * The checksums file once loaded are cached in session, so in-flight file changes during lifecycle of session are NOT
 * noticed.
 * <p>
 * The name of this implementation is "summary-file".
 *
 * @see ArtifactIdUtils#toId(Artifact)
 * @since TBD
 */
@Singleton
@Named(SummaryFileTrustedChecksumsSource.NAME)
public final class SummaryFileTrustedChecksumsSource extends FileTrustedChecksumsSourceSupport {
    public static final String NAME = "summary-file";

    private static final String CHECKSUMS_FILE_PREFIX = "checksums";

    private static final String CHECKSUMS_CACHE_KEY = SummaryFileTrustedChecksumsSource.class.getName() + ".checksums";

    private static final String RECORDING_KEY = SummaryFileTrustedChecksumsSource.class.getName() + ".recording";

    private static final Logger LOGGER = LoggerFactory.getLogger(SummaryFileTrustedChecksumsSource.class);

    public SummaryFileTrustedChecksumsSource() {
        super(NAME);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map<String, String> performLookup(
            RepositorySystemSession session,
            Path basedir,
            Artifact artifact,
            ArtifactRepository artifactRepository,
            List<ChecksumAlgorithmFactory> checksumAlgorithmFactories) {
        final boolean originAware = isOriginAware(session);
        final ConcurrentHashMap<String, ConcurrentHashMap<String, String>> basedirProvidedChecksums =
                (ConcurrentHashMap<String, ConcurrentHashMap<String, String>>)
                        session.getData().computeIfAbsent(CHECKSUMS_CACHE_KEY, ConcurrentHashMap::new);

        final HashMap<String, String> checksums = new HashMap<>();
        for (ChecksumAlgorithmFactory checksumAlgorithmFactory : checksumAlgorithmFactories) {
            ConcurrentHashMap<String, String> algorithmChecksums = basedirProvidedChecksums.computeIfAbsent(
                    checksumAlgorithmFactory.getName(),
                    algName ->
                            loadProvidedChecksums(basedir, originAware, artifactRepository, checksumAlgorithmFactory));
            String checksum = algorithmChecksums.get(ArtifactIdUtils.toId(artifact));
            if (checksum != null) {
                checksums.put(checksumAlgorithmFactory.getName(), checksum);
            }
        }
        return checksums;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected SummaryFileWriter getWriter(RepositorySystemSession session, Path basedir) {
        ConcurrentHashMap<Path, Set<String>> recordedLines =
                (ConcurrentHashMap<Path, Set<String>>) session.getData().computeIfAbsent(RECORDING_KEY, () -> {
                    session.addOnCloseHandler(this::saveSessionRecordedLines);
                    return new ConcurrentHashMap<>();
                });
        return new SummaryFileWriter(basedir, isOriginAware(session), recordedLines);
    }

    private ConcurrentHashMap<String, String> loadProvidedChecksums(
            Path basedir,
            boolean originAware,
            ArtifactRepository artifactRepository,
            ChecksumAlgorithmFactory checksumAlgorithmFactory) {
        Path checksumsFile =
                basedir.resolve(calculateSummaryPath(originAware, artifactRepository, checksumAlgorithmFactory));
        ConcurrentHashMap<String, String> result = new ConcurrentHashMap<>();
        if (Files.isReadable(checksumsFile)) {
            try (BufferedReader reader = Files.newBufferedReader(checksumsFile, StandardCharsets.UTF_8)) {
                LOGGER.debug(
                        "Loading {} trusted checksums for remote repository {} from '{}'",
                        checksumAlgorithmFactory.getName(),
                        artifactRepository.getId(),
                        checksumsFile);
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("#") && !line.isEmpty()) {
                        String[] parts = line.split(" ", 2);
                        if (parts.length == 2) {
                            String newChecksum = parts[1];
                            String oldChecksum = result.put(parts[0], newChecksum);
                            if (oldChecksum != null) {
                                if (Objects.equals(oldChecksum, newChecksum)) {
                                    LOGGER.warn(
                                            "Checksums file '{}' contains duplicate checksums for artifact {}: {}",
                                            checksumsFile,
                                            parts[0],
                                            oldChecksum);
                                } else {
                                    LOGGER.warn(
                                            "Checksums file '{}' contains different checksums for artifact {}: "
                                                    + "old '{}' replaced by new '{}'",
                                            checksumsFile,
                                            parts[0],
                                            oldChecksum,
                                            newChecksum);
                                }
                            }
                        } else {
                            LOGGER.warn("Checksums file '{}' ignored malformed line '{}'", checksumsFile, line);
                        }
                    }
                }
                LOGGER.info(
                        "Loaded {} {} trusted checksums for remote repository {}",
                        result.size(),
                        checksumAlgorithmFactory.getName(),
                        artifactRepository.getId());
            } catch (NoSuchFileException e) {
                // strange: we tested for it above, still, we should not fail
                LOGGER.debug(
                        "The {} trusted checksums for remote repository {} not exist at '{}'",
                        checksumAlgorithmFactory.getName(),
                        artifactRepository.getId(),
                        checksumsFile);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        } else {
            LOGGER.debug(
                    "The {} trusted checksums for remote repository {} not exist at '{}'",
                    checksumAlgorithmFactory.getName(),
                    artifactRepository.getId(),
                    checksumsFile);
        }

        return result;
    }

    private String calculateSummaryPath(
            boolean originAware,
            ArtifactRepository artifactRepository,
            ChecksumAlgorithmFactory checksumAlgorithmFactory) {
        final String fileName;
        if (originAware) {
            fileName = CHECKSUMS_FILE_PREFIX + "-" + artifactRepository.getId();
        } else {
            fileName = CHECKSUMS_FILE_PREFIX;
        }
        return fileName + "." + checksumAlgorithmFactory.getFileExtension();
    }

    private class SummaryFileWriter implements Writer {
        private final Path basedir;

        private final boolean originAware;

        private final ConcurrentHashMap<Path, Set<String>> recordedLines;

        private SummaryFileWriter(
                Path basedir, boolean originAware, ConcurrentHashMap<Path, Set<String>> recordedLines) {
            this.basedir = basedir;
            this.originAware = originAware;
            this.recordedLines = recordedLines;
        }

        @Override
        public void addTrustedArtifactChecksums(
                Artifact artifact,
                ArtifactRepository artifactRepository,
                List<ChecksumAlgorithmFactory> checksumAlgorithmFactories,
                Map<String, String> trustedArtifactChecksums) {
            for (ChecksumAlgorithmFactory checksumAlgorithmFactory : checksumAlgorithmFactories) {
                String checksum = requireNonNull(trustedArtifactChecksums.get(checksumAlgorithmFactory.getName()));
                String summaryLine = ArtifactIdUtils.toId(artifact) + " " + checksum;
                Path summaryPath = basedir.resolve(
                        calculateSummaryPath(originAware, artifactRepository, checksumAlgorithmFactory));

                recordedLines
                        .computeIfAbsent(summaryPath, p -> Collections.synchronizedSet(new TreeSet<>()))
                        .add(summaryLine);
            }
        }

        @Override
        public void close() {
            // nop
        }
    }

    @SuppressWarnings("unchecked")
    private void saveSessionRecordedLines(RepositorySystemSession session) {
        Map<Path, Set<String>> recordedLines =
                (Map<Path, Set<String>>) session.getData().get(RECORDING_KEY);
        if (recordedLines == null || recordedLines.isEmpty()) {
            return;
        }

        ArrayList<Exception> exceptions = new ArrayList<>();
        for (Map.Entry<Path, Set<String>> entry : recordedLines.entrySet()) {
            Path file = entry.getKey();
            Set<String> lines = entry.getValue();
            if (!lines.isEmpty()) {
                LOGGER.info("Saving {} checksums to '{}'", lines.size(), file);
                try {
                    Files.createDirectories(file.getParent());
                    Files.write(file, lines);
                } catch (IOException e) {
                    exceptions.add(e);
                }
            }
        }
        MultiRuntimeException.mayThrow("session save checksums failure", exceptions);
    }
}