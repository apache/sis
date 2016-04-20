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
package org.apache.sis.internal.referencing;

import java.util.Locale;
import java.io.Serializable;
import java.io.ObjectStreamException;
import org.apache.sis.util.iso.AbstractInternationalString;
import org.apache.sis.util.resources.Messages;


/**
 * Comments telling whether a parameter value use the same sign or the opposite sign for the inverse operation.
 * Those comments are used for encoding the {@code PARAM_SIGN_REVERSAL} boolean value in the
 * {@code [Coordinate_Operation Parameter Usage]} table of the EPSG dataset.
 *
 * <p>This approach may change in any future SIS version.</p>
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @since   0.7
 * @version 0.7
 * @module
 *
 * @see org.apache.sis.internal.referencing.provider.AbstractProvider#isInvertible()
 */
public final class SignReversalComment extends AbstractInternationalString implements Serializable {
    /**
     * For cross-version compatibility.
     */
    private static final long serialVersionUID = -2171813880302865442L;

    /**
     * Remark telling that an inverse operation uses the same sign for the parameter value.
     */
    public static final SignReversalComment SAME = new SignReversalComment(false);

    /**
     * Remark telling that an inverse operation uses the parameter value with opposite sign.
     */
    public static final SignReversalComment OPPOSITE = new SignReversalComment(true);

    /**
     * Whether the inverse operation use a parameter value of opposite sign or same sign.
     */
    private final boolean opposite;

    /**
     * Constructor for the {@link #SAME} and {@link #OPPOSITE} constants only.
     */
    private SignReversalComment(final boolean r) {
        opposite = r;
    }

    /**
     * Returns a human-readable text for this constant.
     *
     * @param  locale  the desired locale, or {@code null}.
     * @return a human-readable text in the given locale if possible.
     */
    @Override
    public String toString(final Locale locale) {
        return Messages.getResources(locale).getString(opposite
                ? Messages.Keys.InverseOperationUsesOppositeSign
                : Messages.Keys.InverseOperationUsesSameSign);
    }

    /**
     * Invokes on deserialization for returning the canonical constant.
     */
    private Object readResolve() throws ObjectStreamException {
        return opposite ? OPPOSITE : SAME;
    }
}
