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
package org.apache.sis.style.sld1;

import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import org.apache.sis.style.se1.Description;


/**
 * Root element of a Styled Layer Descriptor document.
 * A {@code StyledLayerDescriptor} describes the rendering of one or several layers,
 * either by reference to a named style ({@link NamedLayer}) or by providing the styling
 * inline ({@link UserLayer}).
 *
 * @author  Johann Sorel (Geomatys)
 */
public class StyledLayerDescriptor extends SLDElement {
    /**
     * Version number of the Styled Layer Descriptor Implementation Specification standard currently implemented.
     * This version number may change in future Apache SIS releases if new standards are published.
     * The current value is {@value}.
     */
    public static final String VERSION = "1.1.0";

    /**
     * Version number of the Styled Layer Descriptor standard used.
     * This value should not be changed, unless this descriptor has been unmarshalled
     * from a XML document that specifies a different version number.
     *
     * @see #getVersion()
     */
    protected String version;

    /**
     * Name for this styled layer descriptor, or {@code null} if none.
     *
     * @see #getName()
     * @see #setName(String)
     */
    protected String name;

    /**
     * Information for user interfaces, or {@code null} if none.
     *
     * @see #getDescription()
     * @see #setDescription(Description)
     */
    protected Description<?> description;

    /**
     * References to external SLD documents to import, as a live list.
     *
     * @see #libraries()
     */
    private List<UseSLDLibrary> libraries;

    /**
     * The named or user layers described by this document, as a live list.
     *
     * @see #layers()
     */
    private List<Layer> layers;

    /**
     * Creates an initially empty styled layer descriptor.
     */
    public StyledLayerDescriptor() {
        version   = VERSION;
        libraries = new ArrayList<>();
        layers    = new ArrayList<>();
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public StyledLayerDescriptor(final StyledLayerDescriptor source) {
        version     = source.version;
        name        = source.name;
        description = source.description;
        libraries   = new ArrayList<>(source.libraries);
        layers      = new ArrayList<>(source.layers);
    }

    /**
     * Returns the version number of the Styled Layer Descriptor standard used.
     * This is fixed to {@value #VERSION} in current Apache SIS release.
     * This value cannot be changed, unless this descriptor has been unmarshalled
     * from a XML document that specifies a different version number.
     *
     * @return version number of the Styled Layer Descriptor standard used.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the name for this styled layer descriptor.
     * This can be any string that uniquely identifies this descriptor.
     * It is not meant to be human-friendly. For a human-friendly label,
     * see the {@linkplain Description#getTitle() title} instead.
     *
     * @return a name for this styled layer descriptor.
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Sets a name for this styled layer descriptor.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new name for this descriptor, or {@code null} if none.
     */
    public void setName(final String value) {
        name = value;
    }

    /**
     * Returns the description of this styled layer descriptor.
     * The returned object is <em>live</em>:
     * changes in the returned instance will be reflected in this descriptor, and conversely.
     *
     * @return information for user interfaces.
     */
    public Optional<Description<?>> getDescription() {
        return Optional.ofNullable(description);
    }

    /**
     * Sets a description of this styled layer descriptor.
     * The given instance is stored by reference, it is not cloned.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new information for user interfaces, or {@code null} if none.
     */
    public void setDescription(final Description<?> value) {
        description = value;
    }

    /**
     * Returns the references to external SLD documents to import.
     * Libraries are used for resolving named layers and named styles
     * which are not known to the map server.
     *
     * <p>The returned collection is <em>live</em>:
     * changes in that collection are reflected into this object, and conversely.</p>
     *
     * @return references to external SLD documents, as a live collection.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<UseSLDLibrary> libraries() {
        return libraries;
    }

    /**
     * Returns the named or user layers described by this document.
     * Order matter: the first item in a list will be the
     * first item plotted and hence appears on the bottom.
     *
     * <p>The returned collection is <em>live</em>:
     * changes in that collection are reflected into this object, and conversely.</p>
     *
     * @return ordered list of layers, as a live collection.
     */
    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    public List<Layer> layers() {
        return layers;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {version, name, description, libraries, layers};
    }

    /**
     * Returns a deep clone of this object. All style elements are cloned,
     * but filters, features and online resources are not.
     *
     * @return deep clone of all style elements.
     */
    @Override
    public StyledLayerDescriptor clone() {
        final var clone = (StyledLayerDescriptor) super.clone();
        clone.selfClone();
        return clone;
    }

    /**
     * Clones the mutable style fields of this element.
     */
    private void selfClone() {
        if (description != null) description = description.clone();
        libraries = new ArrayList<>(libraries);
        libraries.replaceAll(UseSLDLibrary::clone);
        layers = new ArrayList<>(layers);
        layers.replaceAll(Layer::clone);
    }
}
