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
package org.apache.sis.storage.ogcjson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Objects;
import org.apache.sis.storage.json.DataTransferObject;

/**
 * Link
 */
@JsonPropertyOrder({
    Link.JSON_PROPERTY_HREF,
    Link.JSON_PROPERTY_REL,
    Link.JSON_PROPERTY_TYPE,
    Link.JSON_PROPERTY_TEMPLATED,
    Link.JSON_PROPERTY_VAR_BASE,
    Link.JSON_PROPERTY_HREFLANG,
    Link.JSON_PROPERTY_TITLE,
    Link.JSON_PROPERTY_LENGTH
})
public class Link extends DataTransferObject {

    public static final String JSON_PROPERTY_HREF = "href";
    private String href;

    public static final String JSON_PROPERTY_REL = "rel";
    private String rel;

    public static final String JSON_PROPERTY_TYPE = "type";
    private String type;

    //Note: this attribute is present in OGCAPI Tiles
    public static final String JSON_PROPERTY_TEMPLATED = "templated";
    private Boolean templated;

    //Note: this attribute is present in OGCAPI Tiles
    public static final String JSON_PROPERTY_VAR_BASE = "varBase";
    private String varBase;

    public static final String JSON_PROPERTY_HREFLANG = "hreflang";
    private String hreflang;

    public static final String JSON_PROPERTY_TITLE = "title";
    private String title;

    public static final String JSON_PROPERTY_LENGTH = "length";
    private Integer length;

    public Link() {
    }

    public Link(String href, String rel, String type, String hreflang, String title, Integer length) {
        this.href = href;
        this.rel = rel;
        this.type = type;
        this.hreflang = hreflang;
        this.title = title;
        this.length = length;
    }

    public Link href(String href) {
        this.href = href;
        return this;
    }

    /**
     * Get href
     *
     * @return href
     */
    @JsonProperty(JSON_PROPERTY_HREF)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public String getHref() {
        return href;
    }

    @JsonProperty(JSON_PROPERTY_HREF)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setHref(String href) {
        this.href = href;
    }

    public Link rel(String rel) {
        this.rel = rel;
        return this;
    }

    /**
     * Get rel
     *
     * @return rel
     */
    @JsonProperty(JSON_PROPERTY_REL)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public String getRel() {
        return rel;
    }

    @JsonProperty(JSON_PROPERTY_REL)
    @JsonInclude(value = JsonInclude.Include.ALWAYS)
    public void setRel(String rel) {
        this.rel = rel;
    }

    public Link type(String type) {
        this.type = type;
        return this;
    }

    /**
     * Get type
     *
     * @return type
     */
    @JsonProperty(JSON_PROPERTY_TYPE)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public String getType() {
        return type;
    }

    @JsonProperty(JSON_PROPERTY_TYPE)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setType(String type) {
        this.type = type;
    }

    public Link templated(Boolean templated) {
        this.templated = templated;
        return this;
    }

    /**
     * This flag set to true if the link is a URL template.
     * @return templated
     */
    @JsonProperty(JSON_PROPERTY_TEMPLATED)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public Boolean getTemplated() {
        return templated;
    }


    @JsonProperty(JSON_PROPERTY_TEMPLATED)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setTemplated(Boolean templated) {
        this.templated = templated;
    }

    public Link varBase(String varBase) {
        this.varBase = varBase;
        return this;
    }

    /**
     * A base path to retrieve semantic information about the variables used in URL template.
     * @return varBase
     */
    @JsonProperty(JSON_PROPERTY_VAR_BASE)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public String getVarBase() {
        return varBase;
    }

    @JsonProperty(JSON_PROPERTY_VAR_BASE)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setVarBase(String varBase) {
        this.varBase = varBase;
    }

    public Link hreflang(String hreflang) {
        this.hreflang = hreflang;
        return this;
    }

    /**
     * Get hreflang
     *
     * @return hreflang
     */
    @JsonProperty(JSON_PROPERTY_HREFLANG)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public String getHreflang() {
        return hreflang;
    }

    @JsonProperty(JSON_PROPERTY_HREFLANG)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setHreflang(String hreflang) {
        this.hreflang = hreflang;
    }

    public Link title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Get title
     *
     * @return title
     */
    @JsonProperty(JSON_PROPERTY_TITLE)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public String getTitle() {
        return title;
    }

    @JsonProperty(JSON_PROPERTY_TITLE)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setTitle(String title) {
        this.title = title;
    }

    public Link length(Integer length) {
        this.length = length;
        return this;
    }

    /**
     * Get length
     *
     * @return length
     */
    @JsonProperty(JSON_PROPERTY_LENGTH)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public Integer getLength() {
        return length;
    }

    @JsonProperty(JSON_PROPERTY_LENGTH)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public void setLength(Integer length) {
        this.length = length;
    }

    /**
     * Return true if this link object is equal to o.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Link link = (Link) o;
        return Objects.equals(this.href, link.href)
                && Objects.equals(this.rel, link.rel)
                && Objects.equals(this.type, link.type)
                && Objects.equals(this.templated, link.templated)
                && Objects.equals(this.varBase, link.varBase)
                && Objects.equals(this.hreflang, link.hreflang)
                && Objects.equals(this.title, link.title)
                && Objects.equals(this.length, link.length);
    }

    @Override
    public int hashCode() {
        return Objects.hash(href, rel, type, templated, varBase, hreflang, title, length);
    }

}
