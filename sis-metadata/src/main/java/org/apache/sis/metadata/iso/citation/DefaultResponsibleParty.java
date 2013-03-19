/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.metadata.iso.citation;

import org.opengis.metadata.citation.Contact;
import org.opengis.metadata.citation.ResponsibleParty;
import org.opengis.metadata.citation.Role;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.ISOMetadata;

public class DefaultResponsibleParty extends ISOMetadata implements ResponsibleParty {

    private String individualName;

    private InternationalString organisationName;

    private InternationalString positionName;

    private Contact contactInfo;

    private Role role;

    @Override
    public synchronized String getIndividualName() {
        return individualName;
    }

    @Override
    public synchronized InternationalString getOrganisationName() {
        return organisationName;
    }

    @Override
    public synchronized InternationalString getPositionName() {
        return positionName;
    }

    @Override
    public synchronized Contact getContactInfo() {
        return contactInfo;
    }

    @Override
    public synchronized Role getRole() {
        return role;
    }
}
