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
package org.apache.sis.index.tree;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Class to save the quad tree index from file.
 *
 * <div class="warning"><b>Note on future work:</b> this class may change in
 * incompatible way in a future Apache SIS release, or may be replaced by new
 * API.</div>
 */
public final class QuadTreeWriter {
    private QuadTreeWriter() {
    }

    /**
     * Writes the entire quad tree index to file with each node in saved in a
     * separate file.
     *
     * @param tree
     *            the quad tree
     * @param directory
     *            the directory where the index file is located
     * @throws IOException if an I/O error occurred.
     */
    public static void writeTreeToFile(QuadTree tree, String directory) throws IOException {
        createIdxDir(directory);
        writeTreeConfigsToFile(tree, directory);
        writeNodeToFile(tree.getRoot(), directory);
    }

    /**
     * Creating quad tree index file.
     *
     * @param directory
     *            the directory where the index file is located
     */
    private static void createIdxDir(String directory) {
        File dir = new File(directory);
        if (!dir.exists()) {
            System.out.println("[INFO] Creating qtree idx dir: [" + directory + "]");
            new File(directory).mkdirs();
        }
    }

    /**
     * Write quad tree configurations to file.
     *
     * @param tree
     *            the quad tree
     * @param directory
     *            the directory where the configurations file is located
     */
    private static void writeTreeConfigsToFile(QuadTree tree, String directory) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(directory + "tree_config.txt"))) {
            writer.write("capacity;" + tree.getCapacity() + ";depth;" + tree.getDepth());
            writer.newLine();
            writer.close();
        }
    }

    /**
     * Write children of the node to index file.
     *
     * @param node
     *            the quad tree node
     * @param quadrant
     *            specified quadrant
     * @param writer
     *            the BufferedWriter
     * @param directory
     *            the directory where the index file is located
     * @param checkIfParent
     *            if true, checks if the node's child is not null and is a parent
     *            if false, only checks if the node's child is not null
     */
    private static void writeChildrenToFile(QuadTreeNode node, Quadrant quadrant, BufferedWriter writer, String directory, boolean checkIfParent) throws IOException {
        if (checkIfParent){
            if (node.getChild(quadrant) != null && node.getChild(quadrant).getNodeType() == NodeType.GRAY) {
                writeNodeToFile(node.getChild(quadrant), directory);
            }
        } else {
            if (node.getChild(quadrant) != null) {
                writer.write(getQuadTreeDataString(quadrant, node.getChild(quadrant)));
                writer.newLine();
            }
        }
    }

    /**
     * Write quad tree node to index file if node is a parent.
     *
     * @param node
     *            the quad tree node
     * @param directory
     *            the directory where the index file is located
     */
    private static void writeNodeToFile(QuadTreeNode node, String directory) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(directory + "node_" + node.getId() + ".txt"))){
            if (node.getNodeType() == NodeType.GRAY) {
                writeChildrenToFile(node, Quadrant.NW, writer, directory, false);
                writeChildrenToFile(node, Quadrant.NE, writer, directory, false);
                writeChildrenToFile(node, Quadrant.SW, writer, directory, false);
                writeChildrenToFile(node, Quadrant.SE, writer, directory, false);
            }
        }
        if (node.getNodeType() == NodeType.GRAY) {
            writeChildrenToFile(node, Quadrant.NW, null, directory, true);
            writeChildrenToFile(node, Quadrant.NE, null, directory, true);
            writeChildrenToFile(node, Quadrant.SW, null, directory, true);
            writeChildrenToFile(node, Quadrant.SE, null, directory, true);
        }
    }

    /**
     * Get the quad tree data string
     *
     * @param quadrant
     *            specified quadrant
     * @param node
     *            the quad tree node
     * @return quad tree data string
     */
    private static String getQuadTreeDataString(Quadrant quadrant, final QuadTreeNode node) {
        StringBuilder str = new StringBuilder();
        str.append(quadrant.index());
        str.append(':');
        str.append(node.getNodeType().toString());
        str.append(':');
        str.append(node.getId());
        str.append(':');
        str.append(node.getCapacity());
        str.append(':');
        QuadTreeData[] data = node.getData();
        for (int i = 0; i < node.getCount(); i++) {
            str.append(data[i].getLatLon().y);
            str.append(';');
            str.append(data[i].getLatLon().x);
            str.append(';');
            str.append(data[i].getFileName());
            str.append(':');
        }
        return str.substring(0, str.length() - 1);
    }
}
