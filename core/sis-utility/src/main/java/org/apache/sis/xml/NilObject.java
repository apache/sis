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
package org.apache.sis.xml;


/**
 * A marker interface for nil XML elements providing an explanation about why the information is absent.
 * GeoAPI getter methods usually return a {@code null} value when no information is available for
 * a given attribute. However it is possible to specify why an information is absent, in which case
 * the corresponding getter method will rather return an instance of this {@code NilObject} interface.
 * The information may be absent for various reasons, including the attribute being inapplicable in the metadata context
 * ({@link NilReason#INAPPLICABLE}), the information probably exists but is unknown to the data provider
 * ({@link NilReason#UNKNOWN UNKNOW}), the information may not exist at all ({@link NilReason#MISSING
 * MISSING}) or can not be divulged ({@link NilReason#WITHHELD WITHHELD}).
 *
 * <p>Nil objects appear most frequently in XML documents since if a mandatory ISO 19115 attribute
 * is absent, then the ISO 19139 standard requires us to said why it is so. The following example
 * shows a {@code CI_Citation} fragment with an ordinary {@code CI_Series} element on the left side,
 * and an unknown {@code CI_Series} element on the right side:</p>
 *
 * <table class="sis">
 * <caption>Example of missing object</caption>
 * <tr>
 *   <th>Normal {@code Series} element</th>
 *   <th>Unknown {@code Series} element</th>
 * </tr><tr><td>
 * {@preformat xml
 *   <gmd:CI_Citation>
 *     <gmd:series>
 *       <gmd:CI_Series>
 *         <!-- Some content here -->
 *       </gmd:CI_Series>
 *     </gmd:series>
 *   </gmd:CI_Citation>
 * }
 * </td><td>
 * {@preformat xml
 *   <gmd:CI_Citation>
 *     <gmd:series nilReason="unknown"/>
 *   </gmd:CI_Citation>
 * }
 * </td></tr></table>
 *
 * If the {@code CI_Series} element was completely omitted, then {@link org.opengis.metadata.citation.Citation#getSeries()}
 * method would return {@code null} in Apache SIS implementation. But since a {@code nilReason} is provided,
 * then the SIS implementation of {@code getSeries()} will rather return an object which implement
 * both the {@code Series} and the {@code NilObject} interfaces, and the {@link #getNilReason()} method
 * on that instance will return the {@link NilReason#UNKNOWN} constant.
 *
 * <div class="section">Instantiation</div>
 * Instances of {@code NilObject} are created by first fetching the reason why the information
 * is missing, then invoking {@link NilReason#createNilObject(Class)}. The following example
 * instantiates a {@code Citation} object which is nil because the information are missing:
 *
 * {@preformat java
 *     Citation nil = NilReason.MISSING.createNilObject(Citation.class);
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3
 * @version 0.3
 * @module
 *
 * @see NilReason#createNilObject(Class)
 * @see org.apache.sis.util.Numbers#valueOfNil(Class)
 */
public interface NilObject {
    /**
     * Returns the reason why this object contains no information.
     *
     * @return The reason why this object contains no information.
     *
     * @see NilReason#forObject(Object)
     */
    NilReason getNilReason();
}
