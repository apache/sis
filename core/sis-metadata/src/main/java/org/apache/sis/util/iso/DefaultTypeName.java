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

import java.util.Objects;
import java.util.Optional;
import java.lang.reflect.Type;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.TypeName;
import org.opengis.util.NameSpace;
import org.apache.sis.util.UnknownNameException;


/**
 * The name of an attribute type associated to a {@linkplain DefaultMemberName member name}.
 * {@code DefaultTypeName} can be instantiated by any of the following methods:
 *
 * <ul>
 *   <li>{@link DefaultNameFactory#createTypeName(NameSpace, CharSequence)}</li>
 *   <li>{@link DefaultNameFactory#toTypeName(Class)}</li>
 * </ul>
 *
 * <h2>Mapping Java classes to type names</h2>
 * A bidirectional mapping is defined between {@code TypeName} and Java {@link Class}.
 * When an UML identifier from an OGC standard exists for a given {@code Class},
 * Apache SIS uses that identifier prefixed by the {@code "OGC"} namespace.
 * Note that this is <strong>not</strong> a standard practice.
 * A more standard practice would be to use the
 * <a href="https://schemas.opengis.net/definitions/1.1.0/dataType.xml">data type URN standard values</a>
 * (third column in the table below), but the set of data type identifiers defined by OGC is currently
 * small and is sometimes not an exact match.
 *
 * <table class="sis">
 *   <caption>Mapping from Java classes to type names (non-exhaustive list)</caption>
 *   <tr>
 *     <th>Java class</th>
 *     <th>Scoped type name</th>
 *     <th class="sep">Data type URN standard values</th>
 *     <th>URL in Web Services</th>
 *   </tr><tr>
 *     <td>{@link org.opengis.util.InternationalString}</td>
 *     <td>{@code OGC:FreeText}</td>
 *     <td class="sep"></td>
 *     <td></td>
 *   </tr><tr>
 *     <td>{@link java.lang.String}</td>
 *     <td>{@code OGC:CharacterString}</td>
 *     <td class="sep">{@code urn:ogc:def:dataType:OGC::string}</td>
 *     <td>{@code http://www.w3.org/2001/XMLSchema#string}</td>
 *   </tr><tr>
 *     <td>{@link java.net.URI}</td>
 *     <td>{@code OGC:URI}</td>
 *     <td class="sep">{@code urn:ogc:def:dataType:OGC::anyURI}</td>
 *     <td></td>
 *   </tr><tr>
 *     <td>{@link java.lang.Boolean}</td>
 *     <td>{@code OGC:Boolean}</td>
 *     <td class="sep">{@code urn:ogc:def:dataType:OGC::boolean}</td>
 *     <td>{@code http://www.w3.org/2001/XMLSchema#boolean}</td>
 *   </tr><tr>
 *     <td>{@link java.lang.Integer}</td>
 *     <td>{@code OGC:Integer}</td>
 *     <td class="sep">{@code urn:ogc:def:dataType:OGC::nonNegativeInteger}</td>
 *     <td>{@code http://www.w3.org/2001/XMLSchema#integer}</td>
 *   </tr><tr>
 *     <td>{@link java.math.BigDecimal}</td>
 *     <td>{@code OGC:Decimal}</td>
 *     <td class="sep"></td>
 *     <td>{@code http://www.w3.org/2001/XMLSchema#decimal}</td>
 *   </tr><tr>
 *     <td>{@link java.lang.Double}</td>
 *     <td>{@code OGC:Real}</td>
 *     <td class="sep"></td>
 *     <td>{@code http://www.w3.org/2001/XMLSchema#double}</td>
 *   </tr><tr>
 *     <td>{@link java.lang.Float}</td>
 *     <td>{@code OGC:Real}</td>
 *     <td class="sep"></td>
 *     <td>{@code http://www.w3.org/2001/XMLSchema#float}</td>
 *   </tr><tr>
 *     <td>{@link java.util.Date}</td>
 *     <td>{@code OGC:DateTime}</td>
 *     <td class="sep"></td>
 *     <td></td>
 *   </tr><tr>
 *     <td>{@link java.util.Locale}</td>
 *     <td>{@code OGC:PT_Locale}</td>
 *     <td class="sep"></td>
 *     <td></td>
 *   </tr><tr>
 *     <td>{@link org.opengis.metadata.Metadata}</td>
 *     <td>{@code OGC:MD_Metadata}</td>
 *     <td class="sep"></td>
 *     <td></td>
 *   </tr><tr>
 *     <td>Unknown Java class</td>
 *     <td>{@code class:}&lt;the class name&gt;</td>
 *     <td class="sep"></td>
 *     <td></td>
 *   </tr>
 * </table>
 *
 * The mapping defined by Apache SIS may change in any future version depending on standardization progress.
 * To protect against such changes, users are encouraged to rely on methods or constructors like
 * {@link #toJavaType()} or {@link DefaultNameFactory#toTypeName(Class)} instead of parsing the name.
 *
 *
 * <h2>Immutability and thread safety</h2>
 * This class is immutable and thus inherently thread-safe if the {@link NameSpace} and {@link CharSequence}
 * arguments given to the constructor are also immutable. Subclasses shall make sure that any overridden methods
 * remain safe to call from multiple threads and do not change any public {@code TypeName} state.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.4
 *
 * @see DefaultMemberName
 * @see DefaultNameFactory
 *
 * @since 0.3
 */
@XmlType(name = "TypeName_Type")
@XmlRootElement(name = "TypeName")
public class DefaultTypeName extends DefaultLocalName implements TypeName {
    /**
     * Serial number for inter-operability with different versions.
     */
    private static final long serialVersionUID = 7571710679743017926L;

    /**
     * The value returned by {@link #toJavaType()}, or {@code null} if none.
     * This is usually a {@link Class}, which is serializable.
     */
    @SuppressWarnings("serial")         // Not statically typed as Serializable.
    private final Type javaType;

    /**
     * Constructs a type name from the given character sequence and infers automatically a Java type.
     * The scope and name arguments are given unchanged to the
     * {@linkplain DefaultLocalName#DefaultLocalName(NameSpace,CharSequence) super-class constructor}.
     * Then the Java type is inferred in a way that depends on the specified scope:
     *
     * <ul>
     *   <li>If the scope is {@code "OGC"}, then:
     *     <ul>
     *       <li>If the name is {@code "CharacterString"}, {@code "Integer"}, {@code "Real"} or other recognized names
     *           (see {@linkplain DefaultTypeName class javadoc}),
     *           then the corresponding Java class is associated to this type name.</li>
     *       <li>Otherwise {@link UnknownNameException} is thrown.</li>
     *     </ul>
     *   </li>
     *   <li>Else if the scope is {@code "class"}, then:
     *     <ul>
     *       <li>If the name is accepted by {@link Class#forName(String)},
     *           then that Java class is associated to this type name.</li>
     *       <li>Otherwise {@link UnknownNameException} is thrown.</li>
     *     </ul>
     *   </li>
     *   <li>Else if the scope {@linkplain DefaultNameSpace#isGlobal() is global}, then:
     *     <ul>
     *       <li>If the name is one of the names recognized in {@code "OGC"} scope (see above),
     *           then the corresponding class is associated to this type name.</li>
     *       <li>Otherwise no Java class is associated to this type name.
     *           No exception is thrown because names in the global namespace could be anything;
     *           this constructor cannot know if the given name was wrong.</li>
     *     </ul>
     *   </li>
     *   <li>Otherwise no Java class is associated to this type name,
     *       because this method cannot check the validity of names in other namespaces.</li>
     * </ul>
     *
     * @param  scope  the scope of this name, or {@code null} for a global scope.
     * @param  name   the local name (never {@code null}).
     * @throws UnknownNameException if a mapping from this name to a Java class was expected to exist
     *         (because the specified scope is "OGC" or "class") but the associated Java class cannot be found.
     *
     * @see DefaultNameFactory#createTypeName(NameSpace, CharSequence)
     */
    protected DefaultTypeName(final NameSpace scope, final CharSequence name) throws UnknownNameException {
        super(scope, name);
        try {
            javaType = TypeNames.toClass(TypeNames.namespace(scope), super.toString());
        } catch (ClassNotFoundException e) {
            throw new UnknownNameException(TypeNames.unknown(super.toFullyQualifiedName()), e);
        }
        if (javaType == Void.TYPE) {
            throw new UnknownNameException(TypeNames.unknown(super.toFullyQualifiedName()));
        }
    }

    /**
     * Constructs a type name from the given character sequence and explicit Java type.
     * The scope and name arguments are given unchanged to the
     * {@linkplain DefaultLocalName#DefaultLocalName(NameSpace,CharSequence) super-class constructor}.
     *
     * @param scope     the scope of this name, or {@code null} for a global scope.
     * @param name      the local name (never {@code null}).
     * @param javaType  the value type to be returned by {@link #toJavaType()}, or {@code null} if none.
     *
     * @see DefaultNameFactory#createTypeName(NameSpace, CharSequence, Type)
     *
     * @since 1.3
     */
    protected DefaultTypeName(final NameSpace scope, final CharSequence name, final Type javaType) {
        super(scope, name);
        this.javaType = javaType;
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
     * @param  object  the object to get as a SIS implementation, or {@code null} if none.
     * @return a SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     *
     * @since 0.5
     */
    public static DefaultTypeName castOrCopy(final TypeName object) {
        if (object == null || object instanceof DefaultTypeName) {
            return (DefaultTypeName) object;
        }
        return new DefaultTypeName(object.scope(), object.toInternationalString(), object.toJavaType().orElse(null));
    }

    /**
     * Returns the Java type represented by this name.
     * This is the type either specified explicitly at construction time or inferred from the type name.
     *
     * @return the Java type (usually a {@link Class}) for this type name.
     *
     * @see Names#toClass(TypeName)
     *
     * @since 1.3
     */
    @Override
    public Optional<Type> toJavaType() {
        return Optional.ofNullable(javaType);
    }

    /**
     * Compares this type name with the specified object for equality.
     *
     * @param  object  the object to compare with this type for equality.
     * @return {@code true} if the given object is equal to this name.
     */
    @Override
    public boolean equals(final Object object) {
        if (super.equals(object)) {
            return Objects.equals(javaType, ((DefaultTypeName) object).javaType);
        }
        return false;
    }

    /**
     * Invoked by {@link #hashCode()} for computing the hash code value when first needed.
     */
    @Override
    int computeHashCode() {
        return super.computeHashCode() ^ Objects.hashCode(javaType);
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
     * Empty constructor to be used by JAXB only. Despite its `final` declaration,
     * the {@link #name} field will be set by JAXB during unmarshalling.
     */
    private DefaultTypeName() {
        javaType = null;
    }
}
