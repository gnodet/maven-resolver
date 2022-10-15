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
package org.eclipse.aether.impl;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.installation.InstallRequest;
import org.eclipse.aether.installation.InstallResult;
import org.eclipse.aether.installation.InstallationException;

/**
 * Publishes artifacts to the local repository.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @provisional This type is provisional and can be changed, moved or removed without prior notice.
 */
public interface Installer {

    /**
     * Installs a collection of artifacts and their accompanying metadata to the local repository.
     *
     * @param session The repository session, must not be {@code null}.
     * @param request The installation request, must not be {@code null}.
     * @return The installation result, never {@code null}.
     * @throws InstallationException If any artifact/metadata from the request could not be installed.
     * @see RepositorySystem#install(RepositorySystemSession, InstallRequest)
     * @see MetadataGeneratorFactory#newInstance(RepositorySystemSession, InstallRequest)
     */
    InstallResult install( RepositorySystemSession session, InstallRequest request )
            throws InstallationException;

}
