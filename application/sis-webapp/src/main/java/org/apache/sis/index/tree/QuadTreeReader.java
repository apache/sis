/**
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

//JDK imports
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

//SIS imports
import org.apache.sis.geometry.DirectPosition2D;

/**
 * Class to reload the quad tree index from file.
 *
 */
public class QuadTreeReader {

  /**
   * Loads the quad tree index from file.
   *
   * @param directory
   *          the directory where the index files are located
   * @param treeConfigFile
   *          the name of the tree configuration file
   * @param nodeFile
   *          the name of the root node file
   * @return fully loaded QuadTree
   */
  public static QuadTree readFromFile(final String directory,
      final String treeConfigFile, final String nodeFile) {
    QuadTree tree = new QuadTree();
    readConfigFromFile(tree, directory, treeConfigFile);
    readFromFile(tree, tree.getRoot(), directory, nodeFile);
    return tree;
  }

  /**
   * Read the quad tree configuration from file.
   *
   *  @param tree
   *           the quad tree
   *  @param directory
   *           the directory where the configuration file is located
   *  @param treeConfigFile
   *           the name of the tree configuration file
   */
  private static void readConfigFromFile(QuadTree tree, String directory,
      String treeConfigFile) {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(directory
          + treeConfigFile));
      String line = "";
      while ((line = reader.readLine()) != null) {
        String[] tokens = line.split(";");
        int capacity = Integer.parseInt(tokens[1]);
        int depth = Integer.parseInt(tokens[3]);
        tree.setCapacity(capacity);
        tree.setDepth(depth);
      }
    } catch (FileNotFoundException e1) {
      e1.printStackTrace();
    } catch (NumberFormatException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Read the quad tree index from file.
   *
   * @param tree
   *          the quad tree
   * @param parent
   *          the quad tree parent node
   * @param directory
   *          the directory where the index files are located
   * @param filename
   *          the name of the parent node file
   */
  private static void readFromFile(final QuadTree tree,
      final QuadTreeNode parent, final String directory, final String filename) {
    try {
      BufferedReader reader = new BufferedReader(new FileReader(directory
          + filename));
      String line = "";
      while ((line = reader.readLine()) != null) {
        String[] tokens = line.split(":");
        Quadrant quadrant = Quadrant.getQuadrant(Integer.parseInt(tokens[0]));
        String nodetype = tokens[1];
        int id = Integer.parseInt(tokens[2]);

        if (nodetype.equals("GRAY")) {
          parent.setChild(new QuadTreeNode(NodeType.GRAY, id), quadrant);
          tree.setNodeSize(tree.getNodeSize() + 1);
        } else {
          int capacity = Integer.parseInt(tokens[3]);
          parent.setChild(new QuadTreeNode(id, capacity), quadrant);
          for (int i = 4; i < tokens.length; i++) {
            String[] dataTokens = tokens[i].split(";");
            double lat = Double.parseDouble(dataTokens[0]);
            double lon = Double.parseDouble(dataTokens[1]);
            parent.getChild(quadrant).addData(
                new GeoRSSData(dataTokens[2], new DirectPosition2D(lon, lat)));
            tree.setSize(tree.getSize() + 1);
          }
          tree.setNodeSize(tree.getNodeSize() + 1);
        }

      }
      reader.close();

      if (parent.getChild(Quadrant.NW) != null
          && parent.getChild(Quadrant.NW).getNodeType() == NodeType.GRAY) {
        readFromFile(tree, parent.getChild(Quadrant.NW), directory, "node_"
            + parent.getChild(Quadrant.NW).getId() + ".txt");
      }

      if (parent.getChild(Quadrant.NE) != null
          && parent.getChild(Quadrant.NE).getNodeType() == NodeType.GRAY) {
        readFromFile(tree, parent.getChild(Quadrant.NE), directory, "node_"
            + parent.getChild(Quadrant.NE).getId() + ".txt");
      }

      if (parent.getChild(Quadrant.SW) != null
          && parent.getChild(Quadrant.SW).getNodeType() == NodeType.GRAY) {
        readFromFile(tree, parent.getChild(Quadrant.SW), directory, "node_"
            + parent.getChild(Quadrant.SW).getId() + ".txt");
      }

      if (parent.getChild(Quadrant.SE) != null
          && parent.getChild(Quadrant.SE).getNodeType() == NodeType.GRAY) {
        readFromFile(tree, parent.getChild(Quadrant.SE), directory, "node_"
            + parent.getChild(Quadrant.SE).getId() + ".txt");
      }

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (NumberFormatException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
