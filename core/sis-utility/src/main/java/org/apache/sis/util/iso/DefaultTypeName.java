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
 * <div class="section">Mapping Java classes to type names</div>
 * It is sometime useful to establish a mapping between {@link Class} and {@code TypeName}.
 * When an UML identifier from an OGC standard exists for a given {@code Class}, Apache SIS
 * uses that identifier prefixed by the {@code "OGC"} namespace.
 * Note that this is <strong>not</strong> a standard practice.
 * A more standard practice would be to use the <cite>definition identifiers in OGC namespace</cite>
 * (last column in the table below), but the set of data type identifiers defined by OGC is currently
 * small and is sometime not an exact match.
 *
 * <table class="sis">
 *   <caption>Mapping from Java classes to type names (non-exhaustive list)</caption>
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
 * {@link DefaultNameFactory#toTypeName(Class)} or {@link #toClass()} instead than parsing the name.
 *
 *
 * <div class="section">Immutability and thread safety</div>
 * This class is immutable and thus inherently thread-safe if the {@link NameSpace} and {@link CharSequence}
 * arguments given to the constructor are also immutable. Subclasses shall make sure that any overridden methods
 * remain safe to call from multiple threads and do not change any public {@code TypeName} state.
 *
 * @author  Guilhem Legal (Geomatys)
 * @author  Cédric Briançon (Geomatys)
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
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
     * The value class to be returned by {@link #toClass()}, or {@code null} if not yet computed.
     * {@link Void#TYPE} is used as a sentinel value meaning explicit {@code null}.
     *
     * <p>This value is only computed. We do not allow the user to explicitely specify it, because we
     * need that {@code DefaultTypeName}s having identical name also have the same {@code valueClass}.
     * This is necessary {@link DefaultNameFactory#pool} cache integrity. Users who want to explicitely
     * specify their own value class can override {@link #toClass()} instead.</p>
     *
     * @see #setValueClass(NameSpace, String, Class)
     * @see #toClass()
     */
    private transient Class<?> valueClass;

    /**
     * Constructs a type name from the given character sequence. The argument are given unchanged to the
     * {@linkplain DefaultLocalName#DefaultLocalName(NameSpace,CharSequence) super-class constructor}.
     *
     * @param scope The scope of this name, or {@code null} for a global scope.
     * @param name  The local name (never {@code null}).
     *
     * @see DefaultNameFactory#createTypeName(NameSpace, CharSequence)
     */
    protected DefaultTypeName(final NameSpace scope, final CharSequence name) {
        super(scope, name);
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
     * Sets {@link #valueClass} to the given value, only if the scope and the name of this {@code TypeName}
     * are equal to the given values. The check for scope and name is a protection against renaming that user
     * could apply if they subclass {@link DefaultNameFactory}. If the user performed such renaming, then the
     * value class may be wrong, so we will ignore the given value class and let {@link #toClass()} computes
     * the class itself.
     */
    final void setValueClass(final NameSpace scope, final String name, final Class<?> valueClass) {
        if (scope == super.scope() && name.equals(super.toString())) {
            this.valueClass = valueClass;
        }
    }

    /**
     * Returns the Java class associated to this type name.
     * The default implementation parses this name in different ways depending on the {@linkplain #scope() scope}:
     *
     * <ul>
     *   <li>If the scope is {@code "OGC"}, then:
     *     <ul>
     *       <li>If the name is {@code "CharacterString"}, {@code "Integer"}, {@code "Real"} or other recognized names
     *           (see {@linkplain DefaultTypeName class javadoc}), then the corresponding class is returned.</li>
     *       <li>Otherwise {@link UnknownNameException} is thrown.</li>
     *     </ul>
     *   </li>
     *   <li>Else if the scope is {@code "class"}, then:
     *     <ul>
     *       <li>If the name is accepted by {@link Class#forName(String)}, then that class is returned.</li>
     *       <li>Otherwise {@link UnknownNameException} is thrown.</li>
     *     </ul>
     *   </li>
     *   <li>Else if the scope {@linkplain DefaultNameSpace#isGlobal() is global}, then:
     *     <ul>
     *       <li>If the name is one of the names recognized in {@code "OGC"} scope (see above),
     *           then the corresponding class is returned.</li>
     *       <li>Otherwise {@code null} is returned. No exception is thrown because names in the global namespace
     *           could be anything, so we can not be sure that the given name was wrong.</li>
     *     </ul>
     *   </li>
     *   <li>Otherwise {@code null} is returned, since this method can not check the validity of names in other
     *       namespaces.</li>
     * </ul>
     *
     * @return The Java class associated to this {@code TypeName},
     *         or {@code null} if there is no mapping from this name to a Java class.
     * @throws UnknownNameException if a mapping from this name to a Java class was expected to exist
     *         (typically because of the {@linkplain #scope() scope}) but the operation failed.
     *
     * @see Names#toClass(TypeName)
     * @see DefaultNameFactory#toTypeName(Class)
     *
     * @since 0.5
     */
    public Class<?> toClass() throws UnknownNameException {
        /*
         * No synchronization, because it is not a problem if two threads compute the same value concurrently.
         * No volatile field neither, because instances of Class are safely published (well, I presume...).
         */
        Class<?> c = valueClass;
        if (c == Void.TYPE) {
            return null;
        }
        if (c == null) {
            /*
             * Invoke super.foo() instead than this.foo() because we do not want to invoke any overridden method.
             * This is for ensuring that two TypeNames constructed with the same name will map to the same class.
             * See 'valueClass' javadoc for more information.
             */
            try {
                c = TypeNames.toClass(TypeNames.namespace(super.scope()), super.toString());
            } catch (ClassNotFoundException e) {
                throw new UnknownNameException(TypeNames.unknown(super.toFullyQualifiedName()), e);
            }
            if (c == null) {
                throw new UnknownNameException(TypeNames.unknown(super.toFullyQualifiedName()));
            }
            valueClass = c;
        }
        return (c != Void.TYPE) ? c : null;
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
     * the {@link #name} field will be set by JAXB during unmarshalling.
     */
    private DefaultTypeName() {
    }
}
