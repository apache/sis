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
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.TypeName;
import org.opengis.util.NameSpace;
import org.apache.sis.util.resources.Errors;


/**
 * The name of an attribute type associated to a {@linkplain DefaultMemberName member name}.
 * {@code DefaultTypeName} can be instantiated by any of the following methods:
 *
 * <ul>
 *   <li>{@link DefaultNameFactory#createTypeName(NameSpace, CharSequence)}</li>
 *   <li>{@link DefaultNameFactory#toTypeName(Class)}</li>
 * </ul>
 *
 * {@section Mapping Java classes to type names}
 * It is sometime useful to establish a mapping between {@link Class} and {@code TypeName}.
 * When an UML identifier from an OGC standard exists for a given {@code Class}, Apache SIS
 * uses that identifier prefixed by the {@code "OGC"} namespace.
 * Note that this is <strong>not</strong> a standard practice.
 * A more standard practice would be to use the <cite>definition identifiers in OGC namespace</cite>
 * (last column in the table below), but the set of data type identifiers defined by OGC is currently
 * small and is sometime not an exact match.
 *
 * <table class="sis">
 *   <caption>Mapping from Java classes to type names (non exhaustive list)</caption>
 *   <tr><th>Java class</th>                                   <th>Type name (unofficial)</th>      <th>Definition identifier in OGC namespace</th></tr>
 *   <tr><td>{@link org.opengis.util.InternationalString}</td> <td>{@code OGC:FreeText}</td>        <td></td></tr>
 *   <tr><td>{@link java.lang.String}</td>                     <td>{@code OGC:CharacterString}</td> <td>urn:ogc:def:dataType:OGC::string</td></tr>
 *   <tr><td>{@link java.net.URI}</td>                         <td>{@code OGC:URI}</td>             <td>urn:ogc:def:dataType:OGC::anyURI</td></tr>
 *   <tr><td>{@link java.lang.Boolean}</td>                    <td>{@code OGC:Boolean}</td>         <td>urn:ogc:def:dataType:OGC::boolean</td></tr>
 *   <tr><td>{@link java.lang.Integer}</td>                    <td>{@code OGC:Integer}</td>         <td>urn:ogc:def:dataType:OGC::nonNegativeInteger</td></tr>
 *   <tr><td>{@link java.lang.Double}</td>                     <td>{@code OGC:Real}</td>            <td></td></tr>
 *   <tr><td>{@link java.util.Date}</td>                       <td>{@code OGC:DateTime}</td>        <td></td></tr>
 *   <tr><td>{@link java.util.Locale}</td>                     <td>{@code OGC:PT_Locale}</td>       <td></td></tr>
 *   <tr><td>{@link org.opengis.metadata.Metadata}</td>        <td>{@code OGC:MD_Metadata}</td>     <td></td></tr>
 *   <tr><td>Unknown Java class</td>                           <td>{@code class:}&lt;the class name&gt;</td><td></td></tr>
 * </table>
 *
 * The mapping defined by Apache SIS may change in any future version depending on standardization progress.
 * To protect against such changes, users are encouraged to rely on methods or constructors like
 * {@link DefaultNameFactory#toTypeName(Class)} or {@link #getValueClass()} instead than parsing the name.
 *
 *
 * {@section Immutability and thread safety}
 * This class is immutable and thus inherently thread-safe if the {@link NameSpace} and {@link CharSequence}
 * arguments given to the constructor are also immutable. Subclasses shall make sure that any overridden methods
 * remain safe to call from multiple threads and do not change any public {@code TypeName} state.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.00)
 * @version 0.5
 * @module
 *
 * @see DefaultMemberName
 * @see DefaultNameFactory
 */
@XmlType(name = "TypeName_Type")
@XmlRootElement(name = "TypeName")
public class DefaultTypeName extends DefaultLocalName implements TypeName {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7182126541436753582L;

    /**
     * The value class to be returned by {@link #getValueClass()}, or {@code null} if not yet computed.
     * This field is serialized because it may be specified at construction time.
     * {@link Void#TYPE} is used as a sentinel value meaning explicit {@code null}.
     */
    private Class<?> valueClass;

    /**
     * Empty constructor to be used by JAXB only. Despite its "final" declaration,
     * the {@link #name} field will be set by JAXB during unmarshalling.
     */
    private DefaultTypeName() {
    }

    /**
     * Constructs a type name from the given character sequence. The argument are given unchanged
     * to the {@linkplain DefaultLocalName#DefaultLocalName(NameSpace,CharSequence) super-class
     * constructor}.
     *
     * @param scope The scope of this name, or {@code null} for a global scope.
     * @param name  The local name (never {@code null}).
     */
    protected DefaultTypeName(final NameSpace scope, final CharSequence name) {
        super(scope, name);
    }

    /**
     * Constructs a type name from the given character sequence and value class.
     * The given value class is stored for retrieval by {@link #getValueClass()}.
     *
     * @param scope      The scope of this name, or {@code null} for a global scope.
     * @param name       The local name (never {@code null}).
     * @param valueClass The Java class associated to this {@code TypeName}, or {@code null}.
     *
     * @see DefaultNameFactory#toTypeName(Class)
     *
     * @since 0.5
     */
    protected DefaultTypeName(final NameSpace scope, final CharSequence name, Class<?> valueClass) {
        super(scope, name);
        if (valueClass == null) {
            valueClass = Void.TYPE;
        } else if (valueClass == Void.TYPE) {
            throw new IllegalArgumentException(Errors.format(Errors.Keys.IllegalArgumentValue_2, "valueClass", "void"));
        }
        this.valueClass = valueClass;
    }

    /**
     * Returns a SIS type name implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of {@code DefaultTypeName},
     *       then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultTypeName} instance is created
     *       with the same values than the given name.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     *
     * @since 0.5
     */
    public static DefaultTypeName castOrCopy(final TypeName object) {
        if (object == null || object instanceof DefaultTypeName) {
            return (DefaultTypeName) object;
        }
        return new DefaultTypeName(object.scope(), object.toInternationalString());
    }

    /**
     * Returns the Java class associated to this type name. If a Java class has been explicitely specified to the
     * {@linkplain #DefaultTypeName(NameSpace, CharSequence, Class) constructor}, then that class is returned
     * (note that it may be explicitely {@code null}). Otherwise if this {@code TypeName} is as documented in
     * the <cite>Mapping Java classes to type names</cite> section of class javadoc, then the corresponding
     * Java class is returned. Otherwise this method returns {@code null}.
     *
     * @return The Java class associated to this {@code TypeName}, or {@code null} if none.
     *
     * @since 0.5
     */
    public Class<?> getValueClass() {
        if (valueClass == Void.TYPE) {
            return null;
        }
        if (valueClass == null) {
            // TODO: infer here.
        }
        return valueClass;
    }
}
