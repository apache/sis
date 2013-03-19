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
package org.apache.sis.metadata.iso.lineage;

import java.util.Collection;
import org.opengis.metadata.lineage.Lineage;
import org.opengis.metadata.lineage.ProcessStep;
import org.opengis.metadata.lineage.Source;
import org.opengis.util.InternationalString;
import org.apache.sis.metadata.iso.ISOMetadata;


public class DefaultLineage extends ISOMetadata implements Lineage {

    private InternationalString statement;

    private Collection<ProcessStep> processSteps;

    private Collection<Source> sources;

    @Override
    public synchronized InternationalString getStatement() {
        return statement;
    }

    @Override
    public synchronized Collection<ProcessStep> getProcessSteps() {
        return processSteps;
    }

    @Override
    public synchronized Collection<Source> getSources() {
        return sources;
    }
}
