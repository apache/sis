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

import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.MemberName;
import org.opengis.util.NameSpace;
import org.opengis.util.TypeName;

import static org.apache.sis.util.ArgumentChecks.ensureNonNull;

// Branch-dependent imports
import org.apache.sis.internal.jdk7.Objects;


/**
 * The name to identify a member of a {@linkplain org.opengis.util.Record record}.
 * {@code DefaultMemberName} can be instantiated by any of the following methods:
 *
 * <ul>
 *   <li>{@link DefaultNameFactory#createMemberName(NameSpace, CharSequence, TypeName)}</li>
 *   <li>Similar static convenience method in {@link Names}.</li>
 * </ul>
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus inherently thread-safe if the {@link NameSpace}, {@link CharSequence} and
 * {@link TypeName} arguments given to the constructor are also immutable. Subclasses shall make sure that any
 * overridden methods remain safe to call from multiple threads and do not change any public {@code MemberName}
 * state.
 *
 * @author  Guilhem Legal (Geomatys)
 * @since   0.3
 * @version 0.5
 * @module
 *
 * @see DefaultTypeName
 * @see DefaultNameFactory
 * @see DefaultRecordType
 */
@XmlType(name = "MemberName_Type")
@XmlRootElement(name = "MemberName")
public class DefaultMemberName extends DefaultLocalName implements MemberName {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7307683415489715298L;

    /**
     * The type of the data associated with the record member.
     */
    @XmlElement(required = true)
    private final TypeName attributeType;

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
     * Returns a SIS member name implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of {@code DefaultMemberName},
     *       then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultMemberName} instance is created
     *       with the same values than the given name.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     *
     * @since 0.5
     */
    public static DefaultMemberName castOrCopy(final MemberName object) {
        if (object == null || object instanceof DefaultMemberName) {
            return (DefaultMemberName) object;
        }
        return new DefaultMemberName(object.scope(), object.toInternationalString(), object.getAttributeType());
    }

    /**
     * Returns the type of the data associated with the record member.
     *
     * @return The type of the data associated with the record member.
     */
    @Override
    public TypeName getAttributeType() {
        return attributeType;
    }

    /**
     * Compares this member name with the specified object for equality.
     *
     * @param object The object to compare with this name for equality.
     * @return {@code true} if the given object is equal to this name.
     */
    @Override
    public boolean equals(final Object object) {
        return super.equals(object) && Objects.equals(attributeType, ((DefaultMemberName) object).attributeType);
    }

    /**
     * Invoked by {@link #hashCode()} for computing the hash code value when first needed.
     */
    @Override
    final int computeHashCode() {
        return super.computeHashCode() + Objects.hashCode(attributeType);
    }




    //////////////////////////////////////////////////////////////////////////////////////////////////
    ////////                                                                                  ////////
    ////////                               XML support with JAXB                              ////////
    ////////                                                                                  ////////
    ////////        The following methods are invoked by JAXB using reflection (even if       ////////
    ////////        they are private) or are helpers for other methods invoked by JAXB.       ////////
    ////////        Those methods can be safely removed if Geographic Markup Language         ////////
    ////////        (GML) support is not needed.                                              ////////
    ////////                                                                                  ////////
    //////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Empty constructor to be used by JAXB only. Despite its "final" declaration,
     * the {@link #attributeType} field will be set by JAXB during unmarshalling.
     */
    private DefaultMemberName() {
        attributeType = null;
    }
}
