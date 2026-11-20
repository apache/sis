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
package org.apache.sis.storage.hdf.base;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.hdf.btree.BTreeV1;
import org.apache.sis.storage.hdf.btree.BTreeV2;
import org.apache.sis.storage.hdf.heap.FractalHeap;
import org.apache.sis.storage.hdf.heap.GlobalHeap;
import org.apache.sis.storage.hdf.heap.LocalHeap;
import org.apache.sis.storage.hdf.internal.StringUtilities;
import org.apache.sis.storage.hdf.io.HDF5DataInput;

/**
 * Base class of all HDF-5 structures which are read from the file.
 * Subclasses populate their fields from the binary content in {@link #read(HDF5DataInput)}.
 *
 * @author Johann Sorel (Geomatys)
 */
public abstract class IOStructure {

    /** Creates an empty structure, filled by {@link #read(HDF5DataInput)}. */
    public IOStructure() {
    }

    /**
     * Reads this structure from the current channel position.
     *
     * @param channel channel positioned on the structure.
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the content is invalid or unsupported.
     */
    public abstract void read(HDF5DataInput channel) throws IOException, DataStoreException;

    @Override
    public String toString() {
        return reflectionToString(this);
    }

    /**
     * Reads the structure at the current channel position, its type being identified by its 4 bytes signature.
     *
     * @param channel channel positioned on the structure signature.
     * @return the structure read.
     * @throws IOException if an I/O error occurred.
     * @throws DataStoreException if the signature is unknown or the content is invalid.
     */
    public static IOStructure loadIdentifiedObject(HDF5DataInput channel) throws IOException, DataStoreException{
        channel.mark();
        final byte[] signature = channel.readNBytes(4);
        channel.reset();
        final IOStructure structure;
        if (Arrays.equals(FreeSpaceManager.SIGNATURE, signature)) {
            structure = new FreeSpaceManager();
        } else if (Arrays.equals(FreeSpaceSectionList.SIGNATURE, signature)) {
            structure = new FreeSpaceSectionList();
        } else if (Arrays.equals(ObjectHeaderV2.SIGNATURE, signature)) {
            structure = new ObjectHeaderV2();
        } else if (Arrays.equals(SharedMessageRecordList.SIGNATURE, signature)) {
            structure = new SharedMessageRecordList();
        } else if (Arrays.equals(SharedObjectHeaderMessageTable.SIGNATURE, signature)) {
            structure = new SharedObjectHeaderMessageTable();
        } else if (Arrays.equals(SuperBlock.SIGNATURE, signature)) {
            structure = new SuperBlock();
        } else if (Arrays.equals(SymbolTableNode.SIGNATURE, signature)) {
            structure = new SymbolTableNode();
        } else if (Arrays.equals(BTreeV1.SIGNATURE, signature)) {
            structure = new BTreeV1();
        } else if (Arrays.equals(BTreeV2.SIGNATURE, signature)) {
            structure = new BTreeV2();
        } else if (Arrays.equals(FractalHeap.SIGNATURE, signature)) {
            structure = new FractalHeap();
        } else if (Arrays.equals(GlobalHeap.SIGNATURE, signature)) {
            structure = new GlobalHeap();
        } else if (Arrays.equals(LocalHeap.SIGNATURE, signature)) {
            structure = new LocalHeap();
        } else {
            throw new DataStoreException("Unknown structure for signature " + Arrays.toString(signature));
        }

        structure.read(channel);
        return structure;
    }

    /**
     * Formats all non-static fields (including inherited ones) of the given object as a tree, using reflection.
     *
     * @param object object to format.
     * @return tree representation of the object fields.
     */
    public static String reflectionToString(Object object) {
        final List<String> list = new ArrayList<>();
        final List<Field> fields = new ArrayList<>();
        getAllFields(fields, object.getClass());
        for (Field f : fields) {
            if (!java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                f.setAccessible(true);
                String name = f.getName();
                String value = "<null>";
                try {
                    Object v = f.get(object);
                    if (v != null) {
                        if (v instanceof CharSequence) {
                            value = "\"" + v + "\"";
                            value = value.replace('\n', ' ');
                        } else if (v.getClass().isArray()) {
                            final Object[] a = new Object[Array.getLength(v)];
                            for(int i=0;i<a.length;i++) {
                                a[i] = Array.get(v, i);
                            }
                            value = Arrays.toString(a);
                        } else if (v instanceof Iterable ite) {
                            final Iterator iterator = ite.iterator();
                            final List<String> lst = new ArrayList<>();
                            int i = 0;
                            while (iterator.hasNext()) {
                                Object c = iterator.next();
                                lst.add(i + "." + c.toString());
                                i++;
                            }
                            value = StringUtilities.toStringTree("", lst);
                        } else {
                            value = String.valueOf(v);
                        }
                    }
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    value = "<" + ex.getMessage() + ">";
                }
                list.add(name + "=" + value);
            }
        }

        String name = object.getClass().getSimpleName();
        return StringUtilities.toStringTree(name, list);
    }

    /**
     * Collects the fields declared by the given type and all its super classes.
     *
     * @param fields list where to add the fields.
     * @param type class to inspect.
     * @return the given {@code fields} list.
     */
    public static List<Field> getAllFields(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            getAllFields(fields, type.getSuperclass());
        }

        return fields;
    }
}
