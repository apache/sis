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
 * A marker interface for empty XML elements. Note that an "nil" XML element may not be an
 * empty Java object, since the Java object can still be associated with {@link XLink} or
 * {@link NilReason} attributes. Those attributes are not part of ISO 19115, but may appear
 * in ISO 19139 XML documents like below:
 *
 * <blockquote><table class="sis" border="1"><tr>
 *   <th>Non-empty {@code Series} element</th>
 *   <th>Empty {@code Series} element</th>
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
 * </td></tr></table></blockquote>
 *
 * The reason why an object is empty can be obtained by the {@link #getNilReason()} method.
 *
 * {@section Instantiation}
 * The following example instantiates a {@link org.opengis.metadata.citation.Citation} object
 * which is empty because the information are missing:
 *
 * {@preformat java
 *     Citation nil = NilReason.MISSING.createNilObject(Citation.class);
 * }
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.3 (derived from geotk-3.18)
 * @version 0.3
 * @module
 *
 * @see NilReason#createNilObject(Class)
 * @see ObjectLinker#resolve(Class, NilReason)
 * @see org.apache.sis.util.Numbers#valueOfNil(Class)
 */
public interface NilObject {
    /**
     * Returns the reason why this object is empty.
     *
     * @return The reason why this object is empty.
     */
    NilReason getNilReason();
}
