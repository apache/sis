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
package org.apache.sis.metadata.iso.quality;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Iterator;
import jakarta.xml.bind.JAXBException;
import org.opengis.util.Type;
import org.opengis.util.RecordType;
import org.opengis.util.MemberName;
import org.opengis.metadata.quality.Element;
import org.opengis.metadata.quality.QuantitativeResult;
import org.apache.sis.xml.XML;
import org.apache.sis.xml.internal.shared.LegacyNamespaces;
import org.apache.sis.util.SimpleInternationalString;
import org.apache.sis.util.iso.DefaultRecord;
import org.apache.sis.util.iso.DefaultRecordType;
import org.apache.sis.util.internal.shared.Constants;
import org.apache.sis.metadata.internal.Resources;

// Test dependencies
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.apache.sis.test.Assertions.assertSingleton;
import org.apache.sis.test.TestCase;
import org.apache.sis.util.iso.DefaultRecordSchemaTest;


/**
 * Tests {@link DefaultQuantitativeResult}.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @author  Guilhem Legal (Geomatys)
 */
@SuppressWarnings("exports")
public final class DefaultQuantitativeResultTest extends TestCase {
    /**
     * Creates a new test case.
     */
    public DefaultQuantitativeResultTest() {
    }

    /**
     * Tests {@link DefaultQuantitativeResult#isEmpty()}. The {@code isEmpty()} method needs a special check
     * for the deprecated {@code "errorStatistic"} property because, contrarily to other deprecated properties,
     * that one has no replacement. Consequently, no non-deprecated property is set as a result of redirection.
     * Because by default {@code isEmpty()} ignores deprecated properties,
     * it can cause {@link DefaultQuantitativeResult} to be wrongly considered as empty.
     */
    @Test
    @SuppressWarnings("deprecation")
    public void testIsEmpty() {
        final var r = new DefaultQuantitativeResult();
        assertTrue(r.isEmpty());
        r.setErrorStatistic(new SimpleInternationalString("a description"));
        assertFalse(r.isEmpty());
    }

    /**
     * Creates a {@code DefaultQuantitativeResult} instance wrapped in an element.
     * The returned element is as below:
     *
     * <pre class="text">
     *   Quantitative attribute accuracy
     *     ├─Measure
     *     │   └─Name of measure…………………… Some quality flag
     *     └─Quantitative result
     *         ├─Value……………………………………………… The quality is okay
     *         └─Value record type……………… CharacterSequence</pre>
     */
    @SuppressWarnings("deprecation")
    private static Element createResultInsideElement() {
        /*
         * The `RecordType` constructor invoked at unmarshalling time sets the name
         * to the hard-coded "Multiline record" string. We need to use the same name.
         */
        final RecordType recordType = DefaultRecordSchemaTest.createRecordType(
                Constants.SIS,
                Resources.formatInternational(Resources.Keys.MultilineRecord),
                Map.of("Result of quality measurement", String.class));
        /*
         * The `Record` constructor invoked at unmarshalling time sets the type
         * to the hard-coded "Single text" value. We need to use the same type.
         */
        final RecordType singleText = DefaultRecordType.SINGLE_STRING;
        final var record = new DefaultRecord(singleText);
        record.set(assertSingleton(singleText.getMembers()), "The quality is okay");
        /*
         * Record type and record value are set independently in two properties.
         * In current implementation, `record.type` is not equal to `recordType`.
         */
        // assertEquals(recordType, record.getRecordType());    // Limitation of current implementation.
        final var result = new DefaultQuantitativeResult();
        result.setValues(List.of(record));
        result.setValueType(recordType);
        /*
         * Opportunistically test the redirection implemented in deprecated methods.
         */
        final var element = new DefaultQuantitativeAttributeAccuracy();
        element.setNamesOfMeasure(Set.of(new SimpleInternationalString("Some quality flag")));
        element.setResults(Set.of(result));
        return element;
    }

    /**
     * Tests unmarshalling of an XML element containing result records.
     *
     * @throws JAXBException if an error occurred during unmarshalling.
     */
    @Test
    public void testUnmarshallingLegacy() throws JAXBException {
        final String xml =  // Following XML shall match the object built by `createResultInsideElement()`.
                "<gmd:DQ_QuantitativeAttributeAccuracy xmlns:gmd=\"" + LegacyNamespaces.GMD + '"'
                                                   + " xmlns:gco=\"" + LegacyNamespaces.GCO + "\">\n" +
                "  <gmd:nameOfMeasure>\n" +
                "    <gco:CharacterString>Some quality flag</gco:CharacterString>\n" +
                "  </gmd:nameOfMeasure>\n" +
                "  <gmd:result>\n" +
                "    <gmd:DQ_QuantitativeResult>\n" +
                "      <gmd:value>\n" +
                "        <gco:Record>The quality is okay</gco:Record>\n" +
                "      </gmd:value>\n" +
                "      <gmd:valueType>\n" +
                "        <gco:RecordType>Result of quality measurement</gco:RecordType>\n" +
                "      </gmd:valueType>\n" +
                "    </gmd:DQ_QuantitativeResult>\n" +
                "  </gmd:result>\n" +
                "</gmd:DQ_QuantitativeAttributeAccuracy>";

        final Element unmarshalled = (Element) XML.unmarshal(xml);
        final Element programmatic = createResultInsideElement();
        /*
         * Before to compare the two `Element`, compare some individual components.
         * The intent is to identify which metadata is not equal in case of test failure.
         */
        final QuantitativeResult   uResult = assertInstanceOf(QuantitativeResult.class, assertSingleton(unmarshalled.getResults()));
        final QuantitativeResult   pResult = assertInstanceOf(QuantitativeResult.class, assertSingleton(programmatic.getResults()));
        final RecordType           uType   = uResult.getValueType();
        final RecordType           pType   = pResult.getValueType();
        final Map<MemberName,Type> uFields = uType.getMemberTypes();
        final Map<MemberName,Type> pFields = pType.getMemberTypes();
        final Iterator<MemberName> uIter   = uFields.keySet().iterator();
        final Iterator<MemberName> pIter   = pFields.keySet().iterator();
        assertEquals(uFields.size(), pFields.size());
        while (uIter.hasNext() | pIter.hasNext()) {
            final MemberName uName = uIter.next();
            final MemberName pName = pIter.next();
            assertEquals(uName.scope(), pName.scope());
            assertEquals(uName, pName);
            assertEquals(uFields.get(uName), pFields.get(pName));
        }
        assertEquals(uFields,             pFields);
        assertEquals(uType.getMembers(),  pType.getMembers());
        assertEquals(uType.getTypeName(), pType.getTypeName());
        assertEquals(uType,               pType);
        assertEquals(uResult,             pResult);
        assertEquals(unmarshalled, programmatic);
    }
}
