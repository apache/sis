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
 * <p><b>Limitations:</b></p>
 * <ul>
 *   <li>Instances of this class are not synchronized for multi-threading.
 *       Synchronization, if needed, is caller's responsibility.</li>
 *   <li>Serialized objects of this class are not guaranteed to be compatible with future Apache SIS releases.
 *       Serialization support is appropriate for short term storage or RMI between applications running the
 *       same version of Apache SIS. For long term storage, use {@link org.apache.sis.xml.XML} instead.</li>
 * </ul>
 *
 * @author  Martin Desruisseaux (IRD, Geomatys)
 * @author  Touraïvane (IRD)
 * @since   0.3
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
    private static final long serialVersionUID = -403671810118461829L;

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
     * Constructs a new instance initialized with the values from the specified metadata object.
     * This is a <cite>shallow</cite> copy constructor, since the other metadata contained in the
     * given object are not recursively copied.
     *
     * @param object The metadata to copy values from, or {@code null} if none.
     *
     * @see #castOrCopy(QuantitativeResult)
     */
    public DefaultQuantitativeResult(final QuantitativeResult object) {
        super(object);
        if (object != null) {
            valueType      = object.getValueType();
            valueUnit      = object.getValueUnit();
            errorStatistic = object.getErrorStatistic();
            values         = copyList(object.getValues(), Record.class);
        }
    }

    /**
     * Returns a SIS metadata implementation with the values of the given arbitrary implementation.
     * This method performs the first applicable action in the following choices:
     *
     * <ul>
     *   <li>If the given object is {@code null}, then this method returns {@code null}.</li>
     *   <li>Otherwise if the given object is already an instance of
     *       {@code DefaultQuantitativeResult}, then it is returned unchanged.</li>
     *   <li>Otherwise a new {@code DefaultQuantitativeResult} instance is created using the
     *       {@linkplain #DefaultQuantitativeResult(QuantitativeResult) copy constructor}
     *       and returned. Note that this is a <cite>shallow</cite> copy operation, since the other
     *       metadata contained in the given object are not recursively copied.</li>
     * </ul>
     *
     * @param  object The object to get as a SIS implementation, or {@code null} if none.
     * @return A SIS implementation containing the values of the given object (may be the
     *         given object itself), or {@code null} if the argument was null.
     */
    public static DefaultQuantitativeResult castOrCopy(final QuantitativeResult object) {
        if (object == null || object instanceof DefaultQuantitativeResult) {
            return (DefaultQuantitativeResult) object;
        }
        return new DefaultQuantitativeResult(object);
    }

    /**
     * Returns the quantitative value or values, content determined by the evaluation procedure used.
     *
     * @return Quantitative value or values.
     */
    @Override
//  @XmlElement(name = "value", required = true) // TODO
    public List<Record> getValues() {
        return values = nonNullList(values, Record.class);
    }

    /**
     * Sets the quantitative value or values, content determined by the evaluation procedure used.
     *
     * @param newValues The new values.
     */
    public void setValues(final List<? extends Record> newValues) {
        values = writeList(newValues, values, Record.class);
    }

    /**
     * Return the value type for reporting a data quality result.
     *
     * <div class="section">Default value</div>
     * If no type has been set but all {@linkplain #getValues() values} are of the same type,
     * then this method defaults to that type. Otherwise this method returns {@code null}.
     *
     * @return Value type for reporting a data quality result, or {@code null}.
     */
    @Override
    @XmlElement(name = "valueType")
    public RecordType getValueType()  {
        RecordType type = valueType;
        if (type == null && values != null) {
            for (final Record value : values) {
                if (value != null) {
                    final RecordType t = value.getRecordType();
                    if (t == null) {
                        return null;
                    } else if (type == null) {
                        type = t;
                    } else if (type != t) {
                        return null;
                    }
                }
            }
        }
        return type;
    }

    /**
     * Sets the value type for reporting a data quality result.
     * A {@code null} value restores the default value documented in {@link #getValueType()}.
     *
     * @param newValue The new value type.
     */
    public void setValueType(final RecordType newValue) {
        checkWritePermission();
        valueType = newValue;
    }

    /**
     * Returns the value unit for reporting a data quality result.
     *
     * @return Value unit for reporting a data quality result, or {@code null}.
     */
    @Override
    @XmlElement(name = "valueUnit", required = true)
    public Unit<?> getValueUnit()  {
        return valueUnit;
    }

    /**
     * Sets the value unit for reporting a data quality result.
     *
     * @param newValue The new value unit.
     */
    public void setValueUnit(final Unit<?> newValue) {
        checkWritePermission();
        valueUnit = newValue;
    }

    /**
     * Returns the statistical method used to determine the value.
     *
     * @return Statistical method used to determine the value, or {@code null}.
     */
    @Override
    @XmlElement(name = "errorStatistic")
    public InternationalString getErrorStatistic()  {
        return errorStatistic;
    }

    /**
     * Sets the statistical method used to determine the value, or {@code null} if none.
     *
     * @param newValue The new error statistic.
     */
    public void setErrorStatistic(final InternationalString newValue) {
        checkWritePermission();
        errorStatistic = newValue;
    }
}
