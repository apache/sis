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

import java.util.Optional;
import org.opengis.metadata.citation.OnlineResource;


/**
 * Reference to a remote Web Feature Service or Web Coverage Service providing
 * the data to be styled by a {@link UserLayer}.
 *
 * @author  Johann Sorel (Geomatys)
 */
public class RemoteOWS extends LayerSource {
    /**
     * Type of the remote service, such as {@code "WFS"} or {@code "WCS"}, or {@code null} if none.
     *
     * @see #getService()
     * @see #setService(String)
     */
    protected String service;

    /**
     * Location of the remote service, or {@code null} if none.
     *
     * @see #getOnlineResource()
     * @see #setOnlineResource(OnlineResource)
     */
    protected OnlineResource onlineResource;

    /**
     * Creates an initially empty remote service reference.
     * Callers should set the {@linkplain #setService(String) service type}
     * and the {@linkplain #setOnlineResource(OnlineResource) online resource}
     * after construction, since those properties are mandatory.
     */
    public RemoteOWS() {
    }

    /**
     * Creates a shallow copy of the given object.
     * For a deep copy, see {@link #clone()} instead.
     *
     * @param  source  the object to copy.
     */
    public RemoteOWS(final RemoteOWS source) {
        service        = source.service;
        onlineResource = source.onlineResource;
    }

    /**
     * Returns the type of the remote service.
     * The values allowed by the Styled Layer Descriptor standard are
     * {@code "WFS"} (Web Feature Service) and {@code "WCS"} (Web Coverage Service).
     *
     * @return type of the remote service.
     */
    public Optional<String> getService() {
        return Optional.ofNullable(service);
    }

    /**
     * Sets the type of the remote service.
     * The values allowed by the Styled Layer Descriptor standard are {@code "WFS"} and {@code "WCS"}.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new type of the remote service, or {@code null} if none.
     */
    public void setService(final String value) {
        service = value;
    }

    /**
     * Returns the location of the remote service.
     *
     * @return location of the remote service.
     */
    public Optional<OnlineResource> getOnlineResource() {
        return Optional.ofNullable(onlineResource);
    }

    /**
     * Sets the location of the remote service.
     * If this method is never invoked, then the default value is absence.
     *
     * @param  value  new location of the remote service, or {@code null} if none.
     */
    public void setOnlineResource(final OnlineResource value) {
        onlineResource = value;
    }

    /**
     * Returns all properties contained in this class.
     * This is used for {@link #equals(Object)} and {@link #hashCode()} implementations.
     */
    @Override
    final Object[] properties() {
        return new Object[] {service, onlineResource};
    }

    /**
     * Returns a clone of this object.
     * The online resource is not cloned.
     *
     * @return a clone of this element.
     */
    @Override
    public RemoteOWS clone() {
        return (RemoteOWS) super.clone();
    }
}
