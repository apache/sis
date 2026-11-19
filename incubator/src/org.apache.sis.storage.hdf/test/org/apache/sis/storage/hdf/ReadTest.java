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
package org.apache.sis.storage.hdf;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import org.apache.sis.coverage.grid.GridExtent;
import org.apache.sis.storage.DataStoreException;
import org.apache.sis.storage.DataStores;
import org.apache.sis.util.collection.TableColumn;
import org.apache.sis.util.collection.TreeTable;
import org.apache.sis.storage.hdf.internal.DatasetNode;
import org.apache.sis.storage.hdf.datatype.Chars;
import org.apache.sis.storage.hdf.datatype.Compound;
import org.apache.sis.storage.hdf.datatype.FixedPoint;
import org.apache.sis.storage.hdf.datatype.FloatingPoint;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Johann Sorel (Geomatys)
 */
public class ReadTest {

    private static final double TOLERANCE = 0.0;

    @Test
    public void testCompound2D() throws DataStoreException, IOException {

        URL url = ReadTest.class.getResource("test_data.h5");
        try (HDF5Store store = (HDF5Store) DataStores.open(url)) {
//            System.out.println(store.toString());

            { // 1D floating point
                final DatasetNode dataset = (DatasetNode) store.getRootGroup().resolve("array_1D");
                assertTrue(dataset.getDataType() instanceof FloatingPoint);
                assertArrayEquals(new int[]{50}, dataset.getDataspace().getDimensionSizes());
                double[] values;

                //read all
                values = (double[]) dataset.read(null);
                assertEquals(50, values.length);
                assertArrayEquals(new double[]{0.0,0.1,0.2,0.3,0.4,0.5}, Arrays.copyOf(values, 6), TOLERANCE);

                //read part
                values = (double[]) dataset.readMembers(new GridExtent(null, new long[]{5}, new long[]{10}, false), 0);
                assertEquals(5, values.length);
                assertArrayEquals(new double[]{0.5,0.6,0.7,0.8,0.9}, values, TOLERANCE);
            }

            { // 2D floating point
                final DatasetNode dataset = (DatasetNode) store.getRootGroup().resolve("array_2D");
                assertTrue(dataset.getDataType() instanceof FixedPoint);
                assertArrayEquals(new int[]{20,20}, dataset.getDataspace().getDimensionSizes());
                int[][] values;

                //read all
                values = (int[][]) dataset.read(null);
                assertEquals(20, values.length);
                assertEquals(20, values[19].length);

                //read part
                values = (int[][]) dataset.read(new GridExtent(null, new long[]{5,12}, new long[]{8,14}, false));
                assertArrayEquals(new int[]{10512,10513}, values[0]);
                assertArrayEquals(new int[]{10612,10613}, values[1]);
                assertArrayEquals(new int[]{10712,10713}, values[2]);
            }

            { // 3D floating point
                final DatasetNode dataset = (DatasetNode) store.getRootGroup().resolve("array_3D");
                assertTrue(dataset.getDataType() instanceof FixedPoint);
                assertArrayEquals(new int[]{20,20,20}, dataset.getDataspace().getDimensionSizes());
                int[][][] values;

                //read all
                values = (int[][][]) dataset.read(null);
                assertEquals(20, values.length);
                assertEquals(20, values[19].length);
                assertEquals(20, values[10][19].length);

                //read part
                values = (int[][][]) dataset.read(new GridExtent(null, new long[]{5,12,17}, new long[]{8,14,19}, false));
                assertArrayEquals(new int[]{1051217,1051218}, values[0][0]);
                assertArrayEquals(new int[]{1061217,1061218}, values[1][0]);
                assertArrayEquals(new int[]{1071217,1071218}, values[2][0]);
                assertArrayEquals(new int[]{1051317,1051318}, values[0][1]);
                assertArrayEquals(new int[]{1061317,1061318}, values[1][1]);
                assertArrayEquals(new int[]{1071317,1071318}, values[2][1]);
            }

            { // 1D floating point
                final DatasetNode dataset = (DatasetNode) store.getRootGroup().resolve("table");
                assertTrue(dataset.getDataType() instanceof Compound);
                Compound cmpd = (Compound) dataset.getDataType();
                assertEquals(Object[].class, cmpd.getValueClass());
                assertEquals(4, cmpd.members.length);
                assertTrue(cmpd.members[0].memberType instanceof FixedPoint);
                assertTrue(cmpd.members[1].memberType instanceof FloatingPoint);
                assertTrue(cmpd.members[2].memberType instanceof FixedPoint);
                assertTrue(cmpd.members[3].memberType instanceof Chars);
                assertEquals("index", cmpd.members[0].name);
                assertEquals("index_divided_by_ten", cmpd.members[1].name);
                assertEquals("index_multiplied_by_ten", cmpd.members[2].name);
                assertEquals("status", cmpd.members[3].name);
                assertArrayEquals(new int[]{50}, dataset.getDataspace().getDimensionSizes());
                Object[][] values;

                //read all
                values = (Object[][]) dataset.read(null);
                assertEquals(50, values.length);
                assertArrayEquals(new Object[]{(byte)0, 0.0f,  0, "OK"}, values[0]);
                assertArrayEquals(new Object[]{(byte)1, 0.1f, 10, "OK"}, values[1]);
                assertArrayEquals(new Object[]{(byte)2, 0.2f, 20, "OK"}, values[2]);
                assertArrayEquals(new Object[]{(byte)3, 0.3f, 30, "OK"}, values[3]);
                assertArrayEquals(new Object[]{(byte)4, 0.4f, 40, "OK"}, values[4]);

                //read part
                values = (Object[][]) dataset.read(new GridExtent(null, new long[]{5}, new long[]{10}, false));
                assertEquals(5, values.length);
                assertArrayEquals(new Object[]{(byte)5, 0.5f, 50, "OK"}, values[0]);
                assertArrayEquals(new Object[]{(byte)6, 0.6f, 60, "OK"}, values[1]);
                assertArrayEquals(new Object[]{(byte)7, 0.7f, 70, "OK"}, values[2]);
                assertArrayEquals(new Object[]{(byte)8, 0.8f, 80, "OK"}, values[3]);
                assertArrayEquals(new Object[]{(byte)9, 0.9f, 90, "OK"}, values[4]);

                //read part with compound index
                values = (Object[][]) dataset.readMembers(new GridExtent(null, new long[]{5}, new long[]{10}, false), 1,3);
                assertEquals(5, values.length);
                assertArrayEquals(new Object[]{0.5f, "OK"}, values[0]);
                assertArrayEquals(new Object[]{0.6f, "OK"}, values[1]);
                assertArrayEquals(new Object[]{0.7f, "OK"}, values[2]);
                assertArrayEquals(new Object[]{0.8f, "OK"}, values[3]);
                assertArrayEquals(new Object[]{0.9f, "OK"}, values[4]);
            }

            { // 2D floating point
                final DatasetNode dataset = (DatasetNode) store.getRootGroup().resolve("table_2D");
                assertTrue(dataset.getDataType() instanceof Compound);
                Compound cmpd = (Compound) dataset.getDataType();
                assertEquals(Object[].class, cmpd.getValueClass());
                assertEquals(2, cmpd.members.length);
                assertTrue(cmpd.members[0].memberType instanceof FixedPoint);
                assertTrue(cmpd.members[1].memberType instanceof FixedPoint);
                assertEquals("j", cmpd.members[0].name);
                assertEquals("i", cmpd.members[1].name);
                assertArrayEquals(new int[]{20,20}, dataset.getDataspace().getDimensionSizes());
                Object[][][] values;

                //read all
                values = (Object[][][]) dataset.read(null);
                assertEquals(20, values.length);
                assertEquals(20, values[19].length);
                assertArrayEquals(new Object[]{(byte)0, (byte)0}, values[0][0]);
                assertArrayEquals(new Object[]{(byte)1, (byte)0}, values[1][0]);
                assertArrayEquals(new Object[]{(byte)0, (byte)1}, values[0][1]);
                assertArrayEquals(new Object[]{(byte)19, (byte)0}, values[19][0]);
                assertArrayEquals(new Object[]{(byte)0, (byte)19}, values[0][19]);
                assertArrayEquals(new Object[]{(byte)19, (byte)19}, values[19][19]);

                //read part
                values = (Object[][][]) dataset.read(new GridExtent(null, new long[]{5,12}, new long[]{8,14}, false));
                assertArrayEquals(new Object[]{(byte)5, (byte)12}, values[0][0]);
                assertArrayEquals(new Object[]{(byte)6, (byte)12}, values[1][0]);
                assertArrayEquals(new Object[]{(byte)7, (byte)12}, values[2][0]);
                assertArrayEquals(new Object[]{(byte)5, (byte)13}, values[0][1]);
                assertArrayEquals(new Object[]{(byte)6, (byte)13}, values[1][1]);
                assertArrayEquals(new Object[]{(byte)7, (byte)13}, values[2][1]);
            }

            { // 3D floating point
                final DatasetNode dataset = (DatasetNode) store.getRootGroup().resolve("table_3D");
                assertTrue(dataset.getDataType() instanceof Compound);
                Compound cmpd = (Compound) dataset.getDataType();
                assertEquals(Object[].class, cmpd.getValueClass());
                assertEquals(3, cmpd.members.length);
                assertTrue(cmpd.members[0].memberType instanceof FixedPoint);
                assertTrue(cmpd.members[1].memberType instanceof FixedPoint);
                assertTrue(cmpd.members[2].memberType instanceof FixedPoint);
                assertEquals("k", cmpd.members[0].name);
                assertEquals("j", cmpd.members[1].name);
                assertEquals("i", cmpd.members[2].name);
                assertArrayEquals(new int[]{20,20,20}, dataset.getDataspace().getDimensionSizes());
                Object[][][][] values;

                //read all
                values = (Object[][][][]) dataset.read(null);
                assertEquals(20, values.length);
                assertEquals(20, values[19].length);
                assertEquals(20, values[10][19].length);
                for (int x = 0; x < values.length; x++) {
                    for (int y = 0; y < values.length; y++) {
                        for (int z = 0; z < values.length; z++) {
                            assertEquals((byte)x, values[x][y][z][0]);
                            assertEquals((byte)y, values[x][y][z][1]);
                            assertEquals((byte)z, values[x][y][z][2]);
                        }
                    }
                }

                //read part
                values = (Object[][][][]) dataset.read(new GridExtent(null, new long[]{5,12,17}, new long[]{8,14,19}, false));
                assertArrayEquals(new Object[]{(byte)5, (byte)12, (byte)17}, values[0][0][0]);
                assertArrayEquals(new Object[]{(byte)6, (byte)12, (byte)17}, values[1][0][0]);
                assertArrayEquals(new Object[]{(byte)7, (byte)12, (byte)17}, values[2][0][0]);
                assertArrayEquals(new Object[]{(byte)5, (byte)13, (byte)17}, values[0][1][0]);
                assertArrayEquals(new Object[]{(byte)6, (byte)13, (byte)17}, values[1][1][0]);
                assertArrayEquals(new Object[]{(byte)7, (byte)13, (byte)17}, values[2][1][0]);
                assertArrayEquals(new Object[]{(byte)5, (byte)12, (byte)18}, values[0][0][1]);
                assertArrayEquals(new Object[]{(byte)6, (byte)12, (byte)18}, values[1][0][1]);
                assertArrayEquals(new Object[]{(byte)7, (byte)12, (byte)18}, values[2][0][1]);
                assertArrayEquals(new Object[]{(byte)5, (byte)13, (byte)18}, values[0][1][1]);
                assertArrayEquals(new Object[]{(byte)6, (byte)13, (byte)18}, values[1][1][1]);
                assertArrayEquals(new Object[]{(byte)7, (byte)13, (byte)18}, values[2][1][1]);
            }

        }

    }

    /**
     * Explores the file using only the public API ({@link Group}, {@link Dataset}, {@link Node}),
     * as a user outside this module would do.
     *
     * @throws DataStoreException if an error occurred while reading the file.
     */
    @Test
    public void testPublicAPI() throws DataStoreException {
        URL url = ReadTest.class.getResource("test_data.h5");
        try (HDF5Store store = (HDF5Store) DataStores.open(url)) {
            final Group root = store.getRootGroup();
            assertNull(root.getParent());
            assertTrue(root.components().stream().anyMatch((n) -> "array_2D".equals(n.getName())));
            assertNull(root.resolve("no_such_node"));
            assertNull(root.resolve("array_2D/not_a_group"));

            final Dataset dataset = assertInstanceOf(Dataset.class, root.resolve("array_2D"));
            assertSame(root, dataset.getParent());
            assertSame(dataset, root.findNode(dataset.getAddress()));
            assertEquals(int.class, dataset.getValueClass());
            final GridExtent extent = dataset.getExtent();
            assertEquals(2, extent.getDimension());
            assertEquals(20, extent.getSize(0));
            assertEquals(20, extent.getSize(1));

            final int[][] values = (int[][]) dataset.read(new GridExtent(null, new long[]{5,12}, new long[]{8,14}, false));
            assertArrayEquals(new int[]{10512,10513}, values[0]);
            assertArrayEquals(new int[]{10712,10713}, values[2]);
        }
    }

    /**
     * Verifies that the native metadata expose the file structures as a tree.
     *
     * @throws DataStoreException if an error occurred while reading the file.
     */
    @Test
    public void testNativeMetadata() throws DataStoreException {
        URL url = ReadTest.class.getResource("test_data.h5");
        try (HDF5Store store = (HDF5Store) DataStores.open(url)) {
            final TreeTable tree = store.getNativeMetadata().orElseThrow();
            final TreeTable.Node root = tree.getRoot();
            assertEquals("HDF5", root.getValue(TableColumn.NAME));
            assertNotNull(child(root, "Superblock"));

            final TreeTable.Node group = child(root, store.getRootGroup().getName());
            assertEquals("Group", group.getValue(TableColumn.VALUE));
            final TreeTable.Node dataset = child(group, "array_2D");
            assertEquals("Dataset", dataset.getValue(TableColumn.VALUE));
            assertEquals("FixedPoint", child(dataset, "Datatype").getValue(TableColumn.VALUE));
            assertNotNull(child(dataset, "Dataspace"));
            assertNotNull(child(dataset, "Layout"));
        }
    }

    /**
     * Returns the child of the given tree node having the given name.
     *
     * @param  parent  the node where to search.
     * @param  name    name of the child to search.
     * @return the child, never {@code null}.
     */
    private static TreeTable.Node child(final TreeTable.Node parent, final String name) {
        for (TreeTable.Node child : parent.getChildren()) {
            if (name.equals(child.getValue(TableColumn.NAME))) {
                return child;
            }
        }
        return fail("No child named " + name + " under " + parent.getValue(TableColumn.NAME));
    }
}
