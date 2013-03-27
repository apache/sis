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

import java.util.List;
import javax.measure.unit.Unit;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.opengis.metadata.quality.QuantitativeResult;
import org.opengis.util.InternationalString;
import org.opengis.util.Record;
import org.opengis.util.RecordType;


/**
 * Information about the value (or set of values) obtained from applying a data quality measure.
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @since   0.3 (derived from geotk-2.1)
 * @version 0.3
 * @module
 */
@XmlType(name = "DQ_QuantitativeResult_Type", propOrder = {
    "valueType",
    "valueUnit",
    "errorStatistic"
})
@XmlRootElement(name = "DQ_QuantitativeResult")
public class DefaultQuantitativeResult extends AbstractResult implements QuantitativeResult {
    /**
     * Serial number for compatibility with different versions.
     */
    private static final long serialVersionUID = 1230713599561236060L;

    /**
     * Quantitative value or values, content determined by the evaluation procedure used.
     */
    private List<Record> values;

    /**
     * Value type for reporting a data quality result, or {@code null} if none.
     */
    private RecordType valueType;

    /**
     * Value unit for reporting a data quality result, or {@code null} if none.
     */
    private Unit<?> valueUnit;

    /**
     * Statistical method used to determine the value, or {@code null} if none.
     */
    private InternationalString errorStatistic;

    /**
     * Constructs an initially empty quantitative result.
     */
    public DefaultQuantitativeResult() {
    }

    /**
     * Returns a SIS metadata implementation with the same values than the given arbitrary
     * implementation. If the given object is {@code null}, then this method returns {@code null}.
     * Otherwise if the given object is already a SIS implementation, then the given object is
     * returned unchanged. Otherwise a new SIS implementation is created and initialized to the
     * property values of the given object, using a <cite>shallow</cite> copy operation
     * (i.e. properties are not cloned).
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultQuantitativeResult castOrCopy(final QuantitativeResult object) {
        if (object == null || object instanceof DefaultQuantitativeResult) {
            return (DefaultQuantitativeResult) object;
        }
        final DefaultQuantitativeResult copy = new DefaultQuantitativeResult();
        copy.shallowCopy(object);
        return copy;
    }

    /**
     * Returns the quantitative value or values, content determined by the evaluation procedure used.
     */
    @Override
//  @XmlElement(name = "value", required = true) // TODO
    public synchronized List<Record> getValues() {
        return values = nonNullList(values, Record.class);
    }

    /**
     * Sets the quantitative value or values, content determined by the evaluation procedure used.
     *
     * @param newValues The new values.
     */
    public synchronized void setValues(final List<Record> newValues) {
        values = writeList(newValues, values, Record.class);
    }

    /**
     * Return the value type for reporting a data quality result, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "valueType")
    public synchronized RecordType getValueType()  {
        return valueType;
    }

    /**
     * Sets the value type for reporting a data quality result, or {@code null} if none.
     *
     * @param newValue The new value type.
     */
    public synchronized void setValueType(final RecordType newValue) {
        checkWritePermission();
        valueType = newValue;
    }

    /**
     * Returns the value unit for reporting a data quality result, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "valueUnit", required = true)
    public synchronized Unit<?> getValueUnit()  {
        return valueUnit;
    }

    /**
     * Sets the value unit for reporting a data quality result, or {@code null} if none.
     *
     * @param newValue The new value unit.
     */
    public synchronized void setValueUnit(final Unit<?> newValue) {
        checkWritePermission();
        valueUnit = newValue;
    }

    /**
     * Returns the statistical method used to determine the value, or {@code null} if none.
     */
    @Override
    @XmlElement(name = "errorStatistic")
    public synchronized InternationalString getErrorStatistic()  {
        return errorStatistic;
    }

    /**
     * Sets the statistical method used to determine the value, or {@code null} if none.
     *
     * @param newValue The new error statistic.
     */
    public synchronized void setErrorStatistic(final InternationalString newValue) {
        checkWritePermission();
        errorStatistic = newValue;
    }
}
