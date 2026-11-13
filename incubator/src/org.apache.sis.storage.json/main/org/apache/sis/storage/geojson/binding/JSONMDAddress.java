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
import java.util.ArrayList;
import java.util.List;
import org.apache.sis.metadata.iso.citation.DefaultAddress;
import org.apache.sis.storage.json.DataTransferObject;
import org.opengis.metadata.citation.Address;

/**
 * Binding of the {@code CI_Address} JSON type.
 *
 * @author Johann Sorel (Geomatys)
 * @see <a href="https://schemas.isotc211.org/json/19115/-4/mdj/1.0.0/mdj.json">mdj.json#CI_Address</a>
 */
@JsonPropertyOrder({
    JSONMDAddress.PROPERTY_DELIVERY_POINT,
    JSONMDAddress.PROPERTY_CITY,
    JSONMDAddress.PROPERTY_ADMINISTRATIVE_AREA,
    JSONMDAddress.PROPERTY_POSTAL_CODE,
    JSONMDAddress.PROPERTY_COUNTRY,
    JSONMDAddress.PROPERTY_ELECTRONIC_MAIL_ADDRESS
})
public final class JSONMDAddress extends DataTransferObject {

    public static final String PROPERTY_DELIVERY_POINT = "deliveryPoint";
    public static final String PROPERTY_CITY = "city";
    public static final String PROPERTY_ADMINISTRATIVE_AREA = "administrativeArea";
    public static final String PROPERTY_POSTAL_CODE = "postalCode";
    public static final String PROPERTY_COUNTRY = "country";
    public static final String PROPERTY_ELECTRONIC_MAIL_ADDRESS = "electronicMailAddress";

    @JsonProperty(PROPERTY_DELIVERY_POINT)
    private List<String> deliveryPoint;
    @JsonProperty(PROPERTY_CITY)
    private String city;
    @JsonProperty(PROPERTY_ADMINISTRATIVE_AREA)
    private String administrativeArea;
    @JsonProperty(PROPERTY_POSTAL_CODE)
    private String postalCode;
    @JsonProperty(PROPERTY_COUNTRY)
    private String country;
    @JsonProperty(PROPERTY_ELECTRONIC_MAIL_ADDRESS)
    private List<String> electronicMailAddress;

    public JSONMDAddress() {
    }

    public JSONMDAddress(final Address source, final JSONMDEncoder encoder) {
        deliveryPoint         = encoder.texts(source.getDeliveryPoints());
        city                  = encoder.text(source.getCity());
        administrativeArea    = encoder.text(source.getAdministrativeArea());
        postalCode            = source.getPostalCode();
        country               = encoder.text(source.getCountry());
        electronicMailAddress = encoder.list(source.getElectronicMailAddresses(), (String e) -> e);
    }

    public List<String> getDeliveryPoint() {
        return deliveryPoint;
    }

    public void setDeliveryPoint(final List<String> deliveryPoint) {
        this.deliveryPoint = deliveryPoint;
    }

    public String getCity() {
        return city;
    }

    public void setCity(final String city) {
        this.city = city;
    }

    public String getAdministrativeArea() {
        return administrativeArea;
    }

    public void setAdministrativeArea(final String administrativeArea) {
        this.administrativeArea = administrativeArea;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(final String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(final String country) {
        this.country = country;
    }

    public List<String> getElectronicMailAddress() {
        return electronicMailAddress;
    }

    public void setElectronicMailAddress(final List<String> electronicMailAddress) {
        this.electronicMailAddress = electronicMailAddress;
    }

    public DefaultAddress toMetadata(final JSONMDDecoder decoder) {
        final DefaultAddress target = new DefaultAddress();
        target.setDeliveryPoints(decoder.texts(deliveryPoint));
        target.setCity(decoder.text(city));
        target.setAdministrativeArea(decoder.text(administrativeArea));
        target.setPostalCode(postalCode);
        target.setCountry(decoder.text(country));
        if (electronicMailAddress != null) {
            target.setElectronicMailAddresses(new ArrayList<>(electronicMailAddress));
        }
        return target;
    }
}
