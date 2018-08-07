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
package org.apache.sis.test.mock;

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.sis.internal.xml.LegacyNamespaces;
import org.apache.sis.internal.jaxb.gco.Multiplicity;
import org.opengis.util.LocalName;


/**
 * Partial implementation of {@code FC_FeatureAttribute} with only a few properties for testing purposes.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   1.0
 * @module
 */
@XmlType(name = "FC_FeatureAttribute_Type", namespace = LegacyNamespaces.GFC)
@XmlRootElement(name = "FC_FeatureAttribute", namespace = LegacyNamespaces.GFC)
public final class FeatureAttributeMock {
    /**
     * The name of the attribute.
     */
    @XmlElement(name = "memberName", required = true)
    public LocalName memberName;

    /**
     * Number of occurrences allowed.
     */
    @XmlElement(name = "cardinality", required = true)
    public Multiplicity cardinality;
}
