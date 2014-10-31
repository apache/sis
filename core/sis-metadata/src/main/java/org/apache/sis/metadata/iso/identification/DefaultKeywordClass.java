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
package org.apache.sis.metadata.iso.identification;

import java.net.URI;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.util.InternationalString;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.identification.KeywordClass;
import org.apache.sis.metadata.iso.ISOMetadata;
import org.apache.sis.util.iso.Types;


/**
 * Specification of a class to categorize keywords in a domain-specific vocabulary
 * that has a binding to a formal ontology.
 *
 * <p><b>Limitations:</b></p>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.5
 * @version 0.5
 * @module
 */
@XmlType(name = "MD_KeywordClass_Type", propOrder = {
    "className",
    "conceptIdentifier",
    "ontology"
})
@XmlRootElement(name = "MD_KeywordClass")
public class DefaultKeywordClass extends ISOMetadata implements KeywordClass {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 5353835680916000713L;

    /**
     * A character string to label the keyword category in natural language.
     */
    private InternationalString className;

    /**
     * URI of concept in the ontology specified by the {@linkplain #getOntology() ontology} citation.
     */
    private URI conceptIdentifier;

    /**
     * Reference that binds the keyword class to a formal conceptualization of a knowledge domain.
     */
    private Citation ontology;

    /**
     * Constructs an initially empty keyword class.
     */
    public DefaultKeywordClass() {
        super();
    }

    /**
     * Creates keyword class initialized to the given key name and ontology.
     *
     * @param className A character string to label the keyword category in natural language.
     * @param ontology  Reference that binds the keyword class to a formal conceptualization of a knowledge domain.
     */
    public DefaultKeywordClass(final CharSequence className, final Citation ontology) {
        this.className = Types.toInternationalString(className);
        this.ontology  = ontology;
    }

    /**
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(KeywordClass)
     */
    public DefaultKeywordClass(final KeywordClass object) {
        super(object);
        if (object != null) {
            className         = object.getClassName();
            conceptIdentifier = object.getConceptIdentifier();
            ontology          = object.getOntology();
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultKeywordClass}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultKeywordClass} instance is created using the
     *       {@linkplain #DefaultKeywordClass(KeywordClass) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultKeywordClass castOrCopy(final KeywordClass object) {
        if (object == null || object instanceof DefaultKeywordClass) {
            return (DefaultKeywordClass) object;
        }
        return new DefaultKeywordClass(object);
    }

    /**
     * Returns a label for the keyword category in natural language.
     *
     * @return The keyword category in natural language.
     */
    @Override
    @XmlElement(name = "className", required = true)
    public InternationalString getClassName() {
        return className;
    }

    /**
     * Sets a label for the keyword category in natural language.
     *
     * @param newValue The new keyword category in natural language.
     */
    public void setClassName(final InternationalString newValue) {
        checkWritePermission();
        className = newValue;
    }

    /**
     * Returns the URI of concept in the ontology specified by the {@linkplain #getOntology() ontology} citation.
     *
     * @return URI of concept in the ontology, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "conceptIdentifier")
    public URI getConceptIdentifier() {
        return conceptIdentifier;
    }

    /**
     * Sets the URI of concept in the ontology specified by the {@linkplain #getOntology() ontology} citation.
     *
     * @param newValue The new URI of concept in the ontology.
     */
    public void setConceptIdentifier(final URI newValue) {
        checkWritePermission();
        conceptIdentifier = newValue;
    }

    /**
     * Returns a reference that binds the keyword class to a formal conceptualization of a knowledge domain.
     *
     * @return A reference that binds the keyword class to a formal conceptualization.
     */
    @Override
    @XmlElement(name = "ontology", required = true)
    public Citation getOntology() {
        return ontology;
    }

    /**
     * Sets a reference that binds the keyword class to a formal conceptualization of a knowledge domain.
     *
     * @param newValue The new reference that binds the keyword class to a formal conceptualization.
     */
    public void setOntology(final Citation newValue) {
        checkWritePermission();
        ontology = newValue;
    }
}
