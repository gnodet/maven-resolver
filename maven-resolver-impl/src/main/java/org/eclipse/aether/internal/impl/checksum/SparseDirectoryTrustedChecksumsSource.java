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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.internal.impl.LocalPathComposer;
import org.eclipse.aether.repository.ArtifactRepository;
import org.eclipse.aether.spi.connector.checksum.ChecksumAlgorithmFactory;
import org.eclipse.aether.spi.io.FileProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sparse file {@link FileTrustedChecksumsSourceSupport} implementation that use specified directory as base directory,
 * where it expects artifacts checksums on standard Maven2 "local" layout. This implementation uses Artifact coordinates
 * solely to form path from basedir, pretty much as Maven local repository does.
 * <p>
 * The source may be configured to be "origin aware", in that case it will factor in origin repository ID as well into
 * base directory name (for example ".checksums/central/...").
 * <p>
 * The name of this implementation is "sparse-directory".
 *
 * @see LocalPathComposer
 * @since TBD
 */
@Singleton
@Named(SparseDirectoryTrustedChecksumsSource.NAME)
public final class SparseDirectoryTrustedChecksumsSource extends FileTrustedChecksumsSourceSupport {
    public static final String NAME = "sparse-directory";

    private static final Logger LOGGER = LoggerFactory.getLogger(SparseDirectoryTrustedChecksumsSource.class);

    private final FileProcessor fileProcessor;

    private final LocalPathComposer localPathComposer;

    @Inject
    public SparseDirectoryTrustedChecksumsSource(FileProcessor fileProcessor, LocalPathComposer localPathComposer) {
        super(NAME);
        this.fileProcessor = requireNonNull(fileProcessor);
        this.localPathComposer = requireNonNull(localPathComposer);
    }

    @Override
    protected Map<String, String> performLookup(
            RepositorySystemSession session,
            Path basedir,
            Artifact artifact,
            ArtifactRepository artifactRepository,
            List<ChecksumAlgorithmFactory> checksumAlgorithmFactories) {
        final boolean originAware = isOriginAware(session);
        final HashMap<String, String> checksums = new HashMap<>();
        for (ChecksumAlgorithmFactory checksumAlgorithmFactory : checksumAlgorithmFactories) {
            Path checksumPath = basedir.resolve(
                    calculateArtifactPath(originAware, artifact, artifactRepository, checksumAlgorithmFactory));

            if (!Files.isRegularFile(checksumPath)) {
                LOGGER.debug(
                        "Artifact '{}' trusted checksum '{}' not found on path '{}'",
                        artifact,
                        checksumAlgorithmFactory.getName(),
                        checksumPath);
                continue;
            }

            try {
                String checksum = fileProcessor.readChecksum(checksumPath.toFile());
                if (checksum != null) {
                    checksums.put(checksumAlgorithmFactory.getName(), checksum);
                }
            } catch (IOException e) {
                // unexpected, log, skip
                LOGGER.warn("Could not read artifact '{}' trusted checksum on path '{}'", artifact, checksumPath, e);
            }
        }
        return checksums;
    }

    @Override
    protected SparseDirectoryWriter getWriter(RepositorySystemSession session, Path basedir) {
        return new SparseDirectoryWriter(basedir, isOriginAware(session));
    }

    private String calculateArtifactPath(
            boolean originAware,
            Artifact artifact,
            ArtifactRepository artifactRepository,
            ChecksumAlgorithmFactory checksumAlgorithmFactory) {
        final String prefix;
        if (originAware) {
            prefix = artifactRepository.getId() + "/";
        } else {
            prefix = "";
        }

        return prefix + localPathComposer.getPathForArtifact(artifact, false) + "."
                + checksumAlgorithmFactory.getFileExtension();
    }

    private class SparseDirectoryWriter implements Writer {
        private final Path basedir;

        private final boolean originAware;

        private SparseDirectoryWriter(Path basedir, boolean originAware) {
            this.basedir = basedir;
            this.originAware = originAware;
        }

        @Override
        public void addTrustedArtifactChecksums(
                Artifact artifact,
                ArtifactRepository artifactRepository,
                List<ChecksumAlgorithmFactory> checksumAlgorithmFactories,
                Map<String, String> trustedArtifactChecksums)
                throws IOException {
            for (ChecksumAlgorithmFactory checksumAlgorithmFactory : checksumAlgorithmFactories) {
                String checksum = requireNonNull(trustedArtifactChecksums.get(checksumAlgorithmFactory.getName()));
                Path checksumPath = basedir.resolve(
                        calculateArtifactPath(originAware, artifact, artifactRepository, checksumAlgorithmFactory));
                Files.createDirectories(checksumPath.getParent());
                Files.write(checksumPath, checksum.getBytes(StandardCharsets.UTF_8));
            }
        }

        @Override
        public void close() {
            // nop
        }
    }
}