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
package org.apache.sis.util.iso;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import net.jcip.annotations.Immutable;
import org.opengis.util.MemberName;
import org.opengis.util.NameSpace;
import org.opengis.util.TypeName;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;


/**
 * The name to identify a member of a {@linkplain org.opengis.util.Record record}.
 * {@code DefaultMemberName} can be instantiated by any of the following methods:
 *
 * <ul>
 *   <li>{@link DefaultNameFactory#createMemberName(NameSpace, CharSequence, TypeName)}</li>
 * </ul>
 *
 * @author  Guilhem Legal (Geomatys)
 * @since   0.3 (derived from geotk-3.17)
 * @version 0.3
 * @module
 */
@Immutable
@XmlRootElement(name = "MemberName")
public class DefaultMemberName extends DefaultLocalName implements MemberName {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 6252686806895124457L;

    /**
     * The type of the data associated with the record member.
     */
    @XmlElement(required = true)
    private final TypeName attributeType;

    /**
     * Empty constructor to be used by JAXB only. Despite its "final" declaration,
     * the {@link #attributeType} field will be set by JAXB during unmarshalling.
     */
    private DefaultMemberName() {
        attributeType = null;
    }

    /**
     * Constructs a member name from the given character sequence and attribute type.
     *
     * @param scope The scope of this name, or {@code null} for a global scope.
     * @param name  The local name (never {@code null}).
     * @param attributeType The type of the data associated with the record member (can not be {@code null}).
     */
    protected DefaultMemberName(final NameSpace scope, final CharSequence name, final TypeName attributeType) {
        super(scope, name);
        ensureNonNull("attributeType", attributeType);
        this.attributeType = attributeType;
    }

    /**
     * Returns the type of the data associated with the record member.
     */
    @Override
    public TypeName getAttributeType() {
        return attributeType;
    }
}
