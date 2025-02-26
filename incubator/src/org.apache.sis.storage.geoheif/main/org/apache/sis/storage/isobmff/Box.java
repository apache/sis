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
package org.apache.sis.storage.isobmff;

import java.time.Instant;
import java.util.UUID;
import org.apache.sis.storage.base.MetadataBuilder;


/**
 * Building block defined by a unique type identifier and a length in bytes.
 * All data in a GeoHEIF file are contained in boxes, there is no other data within the file.
 * A box may be a {@link ContainerBox} containing other boxes.
 *
 * <h4>Reading process</h4>
 * Each box in the input stream starts with an header declaring the size and the box type.
 * This header is parsed by the {@link Reader} for determining which {@code Box} class to instantiate.
 * The <dfn>box payload</dfn> is defined as all bytes in a box following the header.
 * The box payload shall be read by the {@code Box} subclass constructor.
 * Constructors do not need to read the payload fully, as remaining bytes will be automatically skipped.
 *
 * @author Johann Sorel (Geomatys)
 * @author Martin Desruisseaux (Geomatys)
 */
public abstract class Box extends TreeNode {
    /**
     * The epoch of dates in <abbr>ISO</abbr> <abbr>BMFF</abbr> files.
     * This is fixed to midnight, January 1, 1904 in UTC time.
     */
    protected static final Instant EPOCH = Instant.ofEpochMilli(-2082844800000L);

    /**
     * Creates a new box. The header, which contains the size and the box type, shall have been read by the caller.
     * Therefore, there is no more field to read in this constructor. The <dfn>box payload</dfn> (all bytes after
     * the header) shall be read by the subclass constructor. It is okay to read the payload only partially.
     */
    protected Box() {
    }

    /**
     * Returns the four-character type of this box. This is the value defined in the {@code BOXTYPE} constant
     * of each sub-class. If the value is {@link Extension#BOXTYPE}, then the {@link #extendedType()} method
     * shall return a non-null value.
     *
     * @return the four-character type of this box.
     *
     * @see #formatFourCC(int)
     */
    public abstract int type();

    /**
     * Returns the identifier of this box. For normative boxes defined by <abbr>ISO</abbr> 14496,
     * the identifier is derived by an algorithm defined in the <abbr>ISO</abbr> specification.
     * For all other boxes, this is the value of the {@code EXTENDED_TYPE} constant defined in
     * each {@link Extension} sub-class.
     *
     * @return the box identifier.
     */
    public UUID extendedType() {
        return new UUID((((long) type()) << Integer.SIZE) | 0x0011_0010, 0x8000_00AA00389B71L);
    }

    /**
     * Returns a unique key for the type of this box.
     * This is either {@link #type()} or {@link #extendedType()}.
     *
     * @return a unique key for the type of this box.
     */
    public Object typeKey() {
        return type();
    }

    /**
     * Returns a human-readable name for this node to shown in the tree.
     * This method adds the {@linkplain #typeKey() type} between parenthesis after the box name.
     *
     * @return Human-readable name of this node.
     */
    @Override
    public String typeName() {
        String name = super.typeName();
        Object type = typeKey();
        if (type instanceof Integer fourCC) {
            type = formatFourCC(fourCC);
        }
        if (type != null) {
            name = name + " (" + type + ')';
        }
        return name;
    }

    /**
     * Converts node properties to <abbr>ISO</abbr> 19115 metadata if applicable.
     * The default implementation does nothing.
     *
     * @param  builder  the builder where to set metadata information.
     */
    public void metadata(MetadataBuilder builder) {
    }
}
