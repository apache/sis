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
package org.apache.sis.style.se1;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import jakarta.xml.bind.Unmarshaller;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlAttribute;
import org.opengis.util.GenericName;
import org.apache.sis.util.collection.CodeListSet;

// Branch-dependent imports
import org.opengis.feature.Feature;
import org.opengis.filter.ResourceId;
import org.opengis.style.SemanticType;


/**
 * Defines the styling that is to be applied to a single feature type.
 *
 * <!-- Following list of authors contains credits to OGC GeoAPI 2 contributors. -->
 * @author  Johann Sorel (Geomatys)
 * @author  Chris Dillard (SYS Technologies)
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.5
 * @since   1.5
 */
@XmlType(name = "FeatureTypeStyleType", propOrder = {
    "name",
    "description",
    "featureTypeName",
//  "semanticTypeIdentifiers",
    "rules"
})
@XmlRootElement(name = "FeatureTypeStyle")
public class FeatureTypeStyle extends StyleElement {
    /**
     * Version number of the Symbology Encoding standard used.
     * This value should not be changed, unless this style has been unmarshalled
     * from a XML document that specifies a different version number.
     *
     * @see #getVersion()
     */
    @XmlAttribute
    protected String version;

    /**
     * Name for this style, or {@code null} if none.
     *
     * @see #getName()
     * @see #setName(String)
     */
    @XmlElement(name = "Name")
    protected String name;

    /**
     * Information for user interfaces, or {@code null} if none.
     *
     * @see #getDescription()
     * @see #setDescription(Description)
     */
    @XmlElement(name = "Description")
    protected Description description;

    /**
     * Identification of feature instances on which to apply the style, or {@code null} if none.
     *
     * @see #getFeatureInstanceIDs()
     * @see #setFeatureInstanceIDs(ResourceId)
     */
    protected ResourceId<? super Feature> featureInstanceIDs;

    /**
     * Name of the feature type that this style is meant to act upon, or {@code null} if none.
     *
     * @see #getFeatureTypeName()
     * @see #setFeatureTypeName(GenericName)
     */
    @XmlElement(name = "FeatureTypeName")
    protected GenericName featureTypeName;

    /**
     * Types of geometry that this style is meant to act upon, as a live collection.
     * May be empty but never null.
     *
     * @see #semanticTypeIdentifiers()
     */
//  @XmlElement(name = "SemanticTypeIdentifier")
    private CodeListSet<SemanticType> semanticTypeIdentifiers;

    /**
     * List of rules.
     *
     * <h4>Online rules</h4>
     * A XML document may contain links to rules instead of a full definitions.
     * Such {@code se:OnlineResource} elements are handled at marshalling time.
     * When reading a XML document, the rule links are resolved automatically.
     * When writing a XML document, some rules may be replaced by online resources
     * if {@link Rule#getOnlineSource()} is provided.
     *
     * @todo JAXB adapter for handling the online case is not yet written.
     *
     * @see #rules()
     */
    @XmlElement(name = "Rule")
    private List<Rule<Feature>> rules;

    /**
     * Invoked by JAXB before unmarshalling this mark.
     * Avoid giving the false impression that the XML document contained a version string.
     */
    private void beforeUnmarshal(Unmarshaller caller, Object parent) {
        version = "unspecified";
    }

    /**
     * Creates an initially empty feature type style.
     * Callers should set the following properties after construction:
     *
     * <ul>
     *   <li>Either the {@linkplain #setFeatureTypeName feature type name}
     *       or {@linkplain #semanticTypeIdentifiers() semantic type identifiers}, or both.</li>
     *   <li>At least one {@linkplain #rules() rule} should be added.</li>
     * </ul>
     */
    public FeatureTypeStyle() {
        version = VERSION;
        semanticTypeIdentifiers = new CodeListSet<>(SemanticType.class);
        rules = new ArrayList<>();
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public FeatureTypeStyle(final FeatureTypeStyle source) {
        super(source);
        version                 = source.version;
        name                    = source.name;
        description             = source.description;
        featureInstanceIDs      = source.featureInstanceIDs;
        featureTypeName         = source.featureTypeName;
        semanticTypeIdentifiers = source.semanticTypeIdentifiers.clone();
        rules                   = new ArrayList<>(source.rules);
    }

    /**
     * Returns the version number of the Symbology Encoding standard used.
     * This is fixed to {@value #VERSION} in current Apache SIS release.
     * This value cannot be changed, unless this style has been unmarshalled
     * from a XML document that specifies a different version number.
     *
     * @return version number of the Symbology Encoding standard used.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the name for this style.
     * This can be any string that uniquely identifies this style within a given canvas.
     * It is not meant to be human-friendly. For a human-friendly label,
     * see the {@linkplain Description#getTitle() title} instead.
     *
     * @return a name for this style.
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Sets a name for this style.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new name for this style.
     */
    public void setName(final String value) {
        name = value;
    }

    /**
     * Returns the description of this style.
     * The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this style, and conversely.
     *
     * @return information for user interfaces.
     */
    public Optional<Description> getDescription() {
        return Optional.ofNullable(description);
    }

    /**
     * Sets a description of this style.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new information for user interfaces, or {@code null} if none.
     */
    public void setDescription(final Description value) {
        description = value;
    }

    /**
     * Returns an identification of feature instances on which to apply the style.
     * This method enable the possibility to use a feature type style on a given list
     * of features only, instead of all instances of the feature type.
     *
     * @return identification of the feature instances.
     */
    public Optional<ResourceId<? super Feature>> getFeatureInstanceIDs() {
        return Optional.ofNullable(featureInstanceIDs);
    }

    /**
     * Sets an identification of feature instances on which to apply the style.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new identification of feature instances, or {@code null} if none.
     */
    public void setFeatureInstanceIDs(final ResourceId<? super Feature> value) {
        featureInstanceIDs = value;
    }

    /**
     * Returns the name of the feature type that this style is meant to act upon.
     * It is allowed to be null but only if the feature type can be inferred by other means,
     * for example from context or using {@link SemanticType} identifiers.
     *
     * @return name of the feature type that this style is meant to act upon.
     */
    public Optional<GenericName> getFeatureTypeName() {
        return Optional.ofNullable(featureTypeName);
    }

    /**
     * Sets the name of the feature type that this style is meant to act upon.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new name of the feature type, or {@code null} if none.
     */
    public void setFeatureTypeName(final GenericName value) {
        featureTypeName = value;
    }

    /**
     * Returns the most general types of geometry that this style is meant to act upon.
     * The syntax is currently undefined, but the following values are reserved to indicate
     * that the style applies to feature with default geometry of specific type:
     *
     * <ul>
     *   <li>{@code generic:point}</li>
     *   <li>{@code generic:line}</li>
     *   <li>{@code generic:polygon}</li>
     *   <li>{@code generic:text}</li>
     *   <li>{@code generic:raster}</li>
     *   <li>{@code generic:any}</li>
     * </ul>
     *
     * <p>The returned collection is <em>live</em>:
     * changes in that collection are reflected into this object, and conversely.</p>
     *
     * @return types of geometry that this style is meant to act upon, as a live collection.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public Set<SemanticType> semanticTypeIdentifiers() {
        return semanticTypeIdentifiers;
    }

    /**
     * Returns the list of rules contained by this style.
     * Order matter: the first item in a list will be the
     * first item plotted and hence appears on the bottom.
     *
     * <p>The returned collection is <em>live</em>:
     * changes in that collection are reflected into this object, and conversely.</p>
     *
     * @return ordered list of rules, as a live collection.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<Rule<Feature>> rules() {
        return rules;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {name, description, featureInstanceIDs, featureTypeName, semanticTypeIdentifiers, rules};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but expressions are not on the assumption that they are immutable.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public FeatureTypeStyle clone() {
        final var clone = (FeatureTypeStyle) super.clone();
        clone.selfClone();
        return clone;
    }

    /**
     * Clones the mutable style fields of this element.
     */
    @SuppressWarnings("unchecked")
    private void selfClone() {
        if (description != null) description = description.clone();
        semanticTypeIdentifiers = semanticTypeIdentifiers.clone();
        rules = new ArrayList<>(rules);
        rules.replaceAll(Rule::clone);
    }
}
