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
package org.apache.sis.metadata.iso.constraint;

import java.util.Collection;
import org.opengis.metadata.constraint.LegalConstraints;
import org.opengis.metadata.constraint.Restriction;
import org.opengis.util.InternationalString;

public class DefaultLegalConstraints extends DefaultConstraints implements LegalConstraints {

    private Collection<Restriction> accessConstraints;

    private Collection<Restriction> useConstraints;

    private Collection<InternationalString> otherConstraints;

    @Override
    public synchronized Collection<Restriction> getAccessConstraints() {
        return accessConstraints;
    }

    @Override
    public synchronized Collection<Restriction> getUseConstraints() {
        return useConstraints;
    }

    @Override
    public synchronized Collection<InternationalString> getOtherConstraints() {
        return otherConstraints;
    }
}
