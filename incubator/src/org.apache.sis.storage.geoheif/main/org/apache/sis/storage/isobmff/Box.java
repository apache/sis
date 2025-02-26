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

import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


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
     * Keep reader reference for reading when needed
     * Use it in a sync block.
     */
    protected Reader reader;
    private boolean loaded;

    /**
     *
     * @param reader to read from, channel position is undefined after this operation
     */
    public void setLoader(Reader reader) {
        this.reader = reader;
    }

    /**
     * Read box payload, may be values or children boxes.
     *
     * @throws java.io.IOException
     */
    public synchronized final void readPayload() throws IOException {
        if (loaded) return;
        loaded = true;
        synchronized (reader) {
            if (isContainer()) {
                readChildren(reader);
            } else {
                reader.channel.seek(payloadOffset);
                readProperties(reader);
                if (reader.channel.getStreamPosition() != boxOffset + size) {
                    throw new IOException("Incorrect offset after reading " + this.getClass().getSimpleName() + " properties, box end has not been reached, position : " + reader.channel.getStreamPosition() + " expected : " + (boxOffset + size));
                }
            }
        }
    }

    /**
     * Read properties
     *
     * @param cdi to read from, channel position is undefined after this operation
     * @throws java.io.IOException
     */
    protected void readProperties(Reader reader) throws IOException {
        //skip to box end
        reader.channel.seek(boxOffset + size);
    }

    private void readChildren(Reader reader) throws IOException {
        if (!isContainer()) throw new IOException("Box is not a container.");
        if (children != null) return;

        reader.channel.seek(payloadOffset);
        final List<Box> children = new ArrayList<>();
        if (size == 0) {
            //go to file end
            try {
                while (true) {
                    final Box box = reader.readBox();
                    reader.channel.seek(box.boxOffset + box.size);
                    children.add(box);
                    if (box.size == 0) break; //last box
                }
            } catch (EOFException ex) {
                //expected
            }
        } else {
            while (reader.channel.getStreamPosition() < boxOffset+size) {
                final Box box = reader.readBox();
                reader.channel.seek(box.boxOffset + box.size);
                children.add(box);
                if (box.size == 0) break; //last box
            }
        }
        this.children = Collections.unmodifiableList(children);
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
    public final List<Box> getChildren() throws IOException {
        readPayload();
        return children;
    }

    public Box getChild(String fourCC, String uuid) throws IOException {
        List<Box> children = getChildren();
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
            try {
                final List<Box> children = getChildren();
                return TreeNode.toStringTree(sb.toString(), children);
            } catch (IOException ex) {
                sb.append(" - ERROR loading boxes");
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
        Class<? extends Object> clazz = obj.getClass();
        if (!(clazz == Box.class || clazz == FullBox.class)) {

            final List<Field> fields = new ArrayList<>();
            while (!(clazz == Box.class || clazz == FullBox.class) && clazz != null) {
                fields.addAll(0, Arrays.asList(clazz.getDeclaredFields()));
                clazz = clazz.getSuperclass();
            }

            final StringBuilder sb = new StringBuilder();
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                if (!Modifier.isPublic(field.getModifiers())) continue;
                if (!sb.isEmpty()) sb.append('\n');
                sb.append(field.getName()).append(" : ");
                try {
                    Object value = field.get(obj);
                    if (value instanceof Collection) {
                        value = ((Collection) value).toArray();
                    }

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
                                int maxCount = 2000000000;
                                for (int i = 0; i < length && i < maxCount; i++) {
                                    String str = String.valueOf(Array.get(value, i));
                                    sb.append("\n [").append(i).append("]:").append(str.replaceAll("\n", "\n     "));
                                }
                                if (length >= maxCount) sb.append("\n [...").append(length).append("]: ... more values...");
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
