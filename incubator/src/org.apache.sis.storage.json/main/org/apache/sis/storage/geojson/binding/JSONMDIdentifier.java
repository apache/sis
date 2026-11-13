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
package org.apache.sis.storage.geojson.binding;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.sis.metadata.iso.DefaultIdentifier;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.Identifier;

/**
 * Binding of the {@code MD_Identifier} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#MD_Identifier</a>
 */
@JsonPropertyOrder({
    JSONMDIdentifier.PROPERTY_AUTHORITY,
    JSONMDIdentifier.PROPERTY_CODE,
    JSONMDIdentifier.PROPERTY_CODE_SPACE,
    JSONMDIdentifier.PROPERTY_VERSION,
    JSONMDIdentifier.PROPERTY_DESCRIPTION
})
public final class JSONMDIdentifier extends DataTransferObject {

    public static final String PROPERTY_AUTHORITY = "authority";
    public static final String PROPERTY_CODE = "code";
    public static final String PROPERTY_CODE_SPACE = "codeSpace";
    public static final String PROPERTY_VERSION = "version";
    public static final String PROPERTY_DESCRIPTION = "description";

    @JsonProperty(PROPERTY_AUTHORITY)
    private JSONMDCitation authority;
    @JsonProperty(PROPERTY_CODE)
    private String code;
    @JsonProperty(PROPERTY_CODE_SPACE)
    private String codeSpace;
    @JsonProperty(PROPERTY_VERSION)
    private String version;
    @JsonProperty(PROPERTY_DESCRIPTION)
    private String description;

    public JSONMDIdentifier() {
    }

    public JSONMDIdentifier(final Identifier source, final JSONMDEncoder encoder) {
        authority   = (source.getAuthority() == null) ? null : new JSONMDCitation(source.getAuthority(), encoder);
        code        = source.getCode();
        codeSpace   = source.getCodeSpace();
        version     = source.getVersion();
        description = encoder.text(source.getDescription());
    }

    public JSONMDCitation getAuthority() {
        return authority;
    }

    public void setAuthority(final JSONMDCitation authority) {
        this.authority = authority;
    }

    public String getCode() {
        return code;
    }

    public void setCode(final String code) {
        this.code = code;
    }

    public String getCodeSpace() {
        return codeSpace;
    }

    public void setCodeSpace(final String codeSpace) {
        this.codeSpace = codeSpace;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public DefaultIdentifier toMetadata(final JSONMDDecoder decoder) {
        final DefaultIdentifier target = new DefaultIdentifier();
        if (authority != null) {
            target.setAuthority(authority.toMetadata(decoder));
        }
        target.setCode(code);
        target.setCodeSpace(codeSpace);
        target.setVersion(version);
        target.setDescription(decoder.text(description));
        return target;
    }
}
