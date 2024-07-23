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
package org.apache.sis.storage.gimi.isobmff;

import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.apache.sis.io.stream.ChannelDataInput;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class Box {

    /**
     * Offset of the box in the file.
     */
    public long boxOffset;
    /**
     * Offset of the box payload in the file.
     */
    public long payloadOffset;
    /**
     * Size in bytes of the box, 0 if it extends to the end of the file.
     */
    public long size;
    /**
     * FourCC box identifier.
     * if type is uuid only then uuid field is defined.
     */
    public String type;
    /**
     * Universal Unique Identifier, can be null.
     */
    public UUID uuid;

    /**
     * Box may have children boxes.
     */
    private List<Box> children;

    /**
     * Read box payload, may be values or children boxes.
     *
     * @param cdi to read from, channel position is undefined after this operation
     * @throws java.io.IOException
     */
    public final void readPayload(ChannelDataInput cdi) throws IOException {
        if (isContainer()) {
            getChildren(cdi);
        } else {
            cdi.seek(payloadOffset);
            readProperties(cdi);
            if (cdi.getStreamPosition() != boxOffset + size) {
                throw new IOException("Incorrect offset after reading " + this.getClass().getSimpleName() + " properties, box end has not been reached, position : " + cdi.getStreamPosition() + " expected : " + (boxOffset + size));
            }
        }
    }

    /**
     * Read properties
     *
     * @param cdi to read from, channel position is undefined after this operation
     * @throws java.io.IOException
     */
    protected void readProperties(ChannelDataInput cdi) throws IOException {
        //skip to box end
        cdi.seek(boxOffset + size);
    }

    public boolean isContainer() {
        return false;
    }

    /**
     *
     * @param cdi to read from, channel position is undefined after this operation
     * @return list of children boxes
     * @throws IOException
     */
    public synchronized final List<Box> getChildren(ChannelDataInput cdi) throws IOException {
        if (children != null) return children;
        if (isContainer()) {
            cdi.seek(payloadOffset);
            final List<Box> children = new ArrayList<>();
            if (size == 0) {
                //go to file end
                try {
                    while (true) {
                        final Box box = ISOBMFFReader.readBox(cdi);
                        cdi.seek(box.boxOffset + box.size);
                        children.add(box);
                    }
                } catch (EOFException ex) {
                    //expected
                }
            } else {
                while (cdi.getStreamPosition() < boxOffset+size) {
                    final Box box = ISOBMFFReader.readBox(cdi);
                    cdi.seek(box.boxOffset + box.size);
                    children.add(box);
                }
            }

            this.children = Collections.unmodifiableList(children);
        } else {
            children = Collections.EMPTY_LIST;
        }
        return children;
    }

    public Box getChild(String fourCC, String uuid, ChannelDataInput cdi) throws IOException {
        List<Box> children = getChildren(cdi);
        for (Box b : children) {
            if (b.type.equals(fourCC)) {
                if (uuid != null && !uuid.equals(b.uuid.toString())) {
                    continue;
                }
                return b;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(this instanceof FullBox ? " - FullBox" : " - Box");
        sb.append("(").append(type);
        sb.append(",offset:").append(boxOffset);
        sb.append(",size:").append(size);
        if (uuid != null) sb.append(",uuid:").append(uuid);
        if (this instanceof FullBox fb) {
            sb.append(",version: ").append(fb.version);
            sb.append(",flags: ").append(fb.flags);
        }
        sb.append(")");

        final String p = propertiesToString();
        if (!p.isBlank()) sb.append("\n  ").append(p.replaceAll("\n", "\n  "));
        if (isContainer()) {
            sb.append(" CONTAINER");

            final List<Box> children = this.children;
            if (children != null) {
                return toStringTree(sb.toString(), children);
            } else {
                sb.append(" - not loaded");
            }
        }
        return sb.toString();
    }

    /**
     * @return this box subclass properties as string
     */
    protected String propertiesToString() {
        return beanToString(this);
    }

    /**
     * Returns a graphical representation of the specified objects. This representation can be
     * printed to the {@linkplain System#out standard output stream} (for example) if it uses
     * a monospaced font and supports unicode.
     *
     * @param  root  The root name of the tree to format.
     * @param  objects The objects to format as root children.
     * @return A string representation of the tree.
     */
    public static String toStringTree(String root, final Iterable<?> objects) {
        final StringBuilder sb = new StringBuilder();
        if (root != null) {
            sb.append(root);
        }
        if (objects != null) {
            final Iterator<?> ite = objects.iterator();
            while (ite.hasNext()) {
                sb.append('\n');
                final Object next = ite.next();
                final boolean last = !ite.hasNext();
                sb.append(last ? "\u2514\u2500 " : "\u251C\u2500 ");

                final String[] parts = String.valueOf(next).split("\n");
                sb.append(parts[0]);
                for (int k=1;k<parts.length;k++) {
                    sb.append('\n');
                    sb.append(last ? ' ' : '\u2502');
                    sb.append("  ");
                    sb.append(parts[k]);
                }
            }
        }
        return sb.toString();
    }

    public static int fourCCtoInt(String fourcc) {
        return (fourcc.charAt(0) << 24) |
               (fourcc.charAt(1) << 16) |
               (fourcc.charAt(2) << 8) |
               (fourcc.charAt(3));
    }

    public static String intToFourCC(int value) {
        final StringBuilder sb = new StringBuilder();
        sb.append((char)((value>>>24) & 0xFF));
        sb.append((char)((value>>>16) & 0xFF));
        sb.append((char)((value>>>8) & 0xFF));
        sb.append((char)((value) & 0xFF));
        return sb.toString();
    }

    public static String beanToString(Object obj) {
        final Class<? extends Object> clazz = obj.getClass();
        if (!(clazz == Box.class || clazz == FullBox.class)) {
            final StringBuilder sb = new StringBuilder();
            final Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (!Modifier.isPublic(field.getModifiers())) continue;
                if (!sb.isEmpty()) sb.append('\n');
                sb.append(field.getName()).append(" : ");
                try {
                    Object value = field.get(obj);
                    if (value != null && value.getClass().isArray()) {
                        final Class<?> componentType = value.getClass().getComponentType();
                        int length = Array.getLength(value);
                        if (value instanceof boolean[]) {
                            sb.append(Arrays.toString((boolean[])value));
                        } else if (value instanceof byte[]) {
                            sb.append(Arrays.toString((byte[])value));
                        } else if (value instanceof int[]) {
                            sb.append(Arrays.toString((int[])value));
                        } else if (value instanceof double[]) {
                            sb.append(Arrays.toString((double[])value));
                        } else if (value instanceof String[]) {
                            sb.append(Arrays.toString((String[])value));
                        } else {
                            if (length > 0) {
                                for (int i = 0; i < length && i < 20; i++) {
                                    String str = String.valueOf(Array.get(value, i));
                                    sb.append("\n [").append(i).append("]:").append(str.replaceAll("\n", "\n     "));
                                }
                                if (length >= 20) sb.append("\n [...").append(length).append("]: ... more values...");
                            }
                        }

                    } else {
                        sb.append(value);
                    }
                } catch (IllegalArgumentException | IllegalAccessException ex) {
                    sb.append("- can not acces value");
                }
            }
            return sb.toString();
        }
        return "";
    }
}
