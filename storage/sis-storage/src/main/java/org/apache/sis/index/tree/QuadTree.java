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
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

//SIS imports
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.Envelope2D;
import org.apache.sis.distance.DistanceUtils;
import org.apache.sis.distance.LatLonPointRadius;

/**
 * Implementation of Quad Tree Index. Insertion algorithm implemented based on
 * design of quad tree index in H. Samet, The Design and Analysis of Spatial
 * Data Structures. Massachusetts: Addison Wesley Publishing Company, 1989.
 *
 */
public class QuadTree {

  // assume map is shifted to be in positive coordinate
  private static final double EARTH_MIN_X = 0;
  private static final double EARTH_MIN_Y = 0;
  private static final double EARTH_MAX_X = 360;
  private static final double EARTH_MAX_Y = 180;
  private static final double EARTH_MID_X = (EARTH_MAX_X - EARTH_MIN_X) / 2;
  private static final double EARTH_MID_Y = (EARTH_MAX_Y - EARTH_MIN_Y) / 2;
  private static final double[] xf = new double[] { -0.25, 0.25, -0.25, 0.25 };
  private static final double[] yf = new double[] { 0.25, 0.25, -0.25, -0.25 };

  private QuadTreeNode root;
  private int size;
  private int nodeSize;

  private int maxDepth;
  private int capacity;

  /**
   * Creates a quad tree.
   *
   * @param capacity
   *          the capacity of each node in the quad tree
   * @param maxDepth
   *          the maximum depth of the tree
   */
  public QuadTree(int capacity, int maxDepth) {
    this.size = 0;
    this.nodeSize = 0;
    this.capacity = capacity;
    this.maxDepth = maxDepth;
    this.root = new QuadTreeNode(NodeType.GRAY, this.nodeSize);
  }

  /**
   * Creates a quad tree with 0 capacity and depth. Useful when user wants to
   * set the capacity and depth after quad tree construction.
   */
  public QuadTree() {
    this.size = 0;
    this.nodeSize = 0;
    this.capacity = 0;
    this.maxDepth = 0;
    this.root = new QuadTreeNode(NodeType.GRAY, this.nodeSize);
  }

  /**
   * Inserts the specified data into the quad tree.
   *
   * @param data
   *          specified data to be inserted
   * @return true if the data was inserted into the quad tree; false if data
   *         cannot be inserted because the capacity of the node has been
   *         exceeded and the depth of the tree will be exceeded if we insert
   *         this data
   */
  public boolean insert(QuadTreeData data) {
    if (insert(data, this.root)) {
      this.size++;
      return true;
    } else {
      return false;
    }
  }

  /**
   * Calculates the quadrant that the data lies in.
   *
   * @param data
   *          specifed data
   * @param x
   *          the x-midpoint of the current node
   * @param y
   *          the y-midpoint of the current node
   * @return the quadrant that the data lies in
   */
  private Quadrant compare(final QuadTreeData data, final double x,
      final double y) {
    if (data.getX() < x)
      if (data.getY() < y)
        return Quadrant.SW;
      else
        return Quadrant.NW;
    else if (data.getY() < y)
      return Quadrant.SE;
    else
      return Quadrant.NE;
  }

  /**
   * Inserts the data into the quad tree with the specified root.
   *
   * @param data
   *          data to be inserted
   * @param root
   *          root of quadtree
   * @return true if data was inserted, false otherwise
   */
  private boolean insert(final QuadTreeData data, final QuadTreeNode root) {
    int currentDepth = 0;

    QuadTreeNode r = root;
    double x = EARTH_MID_X;
    double y = EARTH_MID_Y;
    double lx = EARTH_MAX_X;
    double ly = EARTH_MAX_Y;

    QuadTreeNode u;
    QuadTreeNode t;
    Quadrant q, uq;
    t = r;
    q = compare(data, x, y);
    currentDepth++;
    while (t.getChild(q) != null
        && t.getChild(q).getNodeType() == NodeType.GRAY) {
      t = t.getChild(q);
      x = x + xf[q.index()] * lx;
      lx = lx / 2.0;
      y = y + yf[q.index()] * ly;
      ly = ly / 2.0;
      q = compare(data, x, y);
      currentDepth++;

      if (currentDepth > this.maxDepth)
        return false;
    }
    if (t.getChild(q) == null) {
      QuadTreeNode newlyCreated = new QuadTreeNode(++this.nodeSize,
          this.capacity);
      newlyCreated.addData(data);
      t.setChild(newlyCreated, q);
    } else {
      u = t.getChild(q);
      if (u.getCount() < this.capacity) {
        u.addData(data);
        return true;
      } else {
        QuadTreeData[] originalData = u.getData();

        if (!maxDepthExceeded(originalData, data, x, y, lx, ly, q, currentDepth)) {
          t.setChild(new QuadTreeNode(NodeType.GRAY, u.getId()), q);
          t = t.getChild(q);
          x = x + xf[q.index()] * lx;
          lx = lx / 2.0;

          y = y + yf[q.index()] * ly;
          ly = ly / 2.0;
          q = compare(data, x, y);
          currentDepth++;
          if (currentDepth > this.maxDepth)
            return false;
          while (isSimilarQuad(originalData, data, x, y, q)) {
            t.setChild(new QuadTreeNode(NodeType.GRAY, ++this.nodeSize), q);
            t = t.getChild(q);
            x = x + xf[q.index()] * lx;
            lx = lx / 2.0;

            y = y + yf[q.index()] * ly;
            ly = ly / 2.0;
            q = compare(data, x, y);
            currentDepth++;
            if (currentDepth > this.maxDepth)
              return false;
          }

          if (t.getChild(q) == null) {
            QuadTreeNode newlyCreated = new QuadTreeNode(++this.nodeSize,
                this.capacity);
            newlyCreated.addData(data);
            t.setChild(newlyCreated, q);
          } else {
            t.getChild(q).addData(data);
          }

          for (int i = 0; i < originalData.length; i++) {
            uq = compare(originalData[i], x, y);
            if (t.getChild(uq) == null) {
              QuadTreeNode newlyCreated = new QuadTreeNode(++this.nodeSize,
                  this.capacity);
              newlyCreated.addData(originalData[i]);
              t.setChild(newlyCreated, uq);
            } else {
              t.getChild(uq).addData(originalData[i]);
            }
          }
        } else {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Determines if insertion of the data will cause the depth of the quad tree
   * to be exceeded.
   *
   * @param originalData
   *          array containing the data that is already stored in the node
   * @param toBeAddedData
   *          data to be added to the node
   * @param originalX
   *          the x-midpoint of the node
   * @param originalY
   *          the y-midpoint of the node
   * @param originalLX
   *          the width of the node
   * @param originalLY
   *          the height of the node
   * @param originalQ
   *          the quadrant that all data currently lies in
   * @param depth
   *          the current depth
   * @return true if the depth will be exceeded, false otherwise
   */
  private boolean maxDepthExceeded(final QuadTreeData[] originalData,
      final QuadTreeData toBeAddedData, final double originalX,
      final double originalY, final double originalLX, final double originalLY,
      final Quadrant originalQ, final int depth) {
    int currentDepth = depth;
    double x = originalX;
    double lx = originalLX;
    double y = originalY;
    double ly = originalLY;
    Quadrant q = originalQ;

    x = x + xf[q.index()] * lx;
    lx = lx / 2.0;

    y = y + yf[q.index()] * ly;
    ly = ly / 2.0;
    q = compare(toBeAddedData, x, y);
    currentDepth++;
    if (currentDepth > this.maxDepth)
      return true;
    while (isSimilarQuad(originalData, toBeAddedData, x, y, q)) {
      x = x + xf[q.index()] * lx;
      lx = lx / 2.0;

      y = y + yf[q.index()] * ly;
      ly = ly / 2.0;
      q = compare(toBeAddedData, x, y);
      currentDepth++;
      if (currentDepth > this.maxDepth)
        return true;
    }
    return false;
  }

  /**
   * Returns true if all data (new and old) have to be stored in the same
   * quadrant of the node.
   *
   * @param originalData
   *          array of data already stored in the node
   * @param newNode
   *          the node in which data is stored
   * @param x
   *          the x midpoint of the node
   * @param y
   *          the y midpoint of the node
   * @param q
   *          the quadrant that the new data lies in
   * @return true if all data lies in the quadrant, false otherwise
   */
  private boolean isSimilarQuad(QuadTreeData[] originalData,
      QuadTreeData newNode, double x, double y, Quadrant q) {
    Quadrant quad = compare(originalData[0], x, y);
    if (q != quad)
      return false;
    for (int i = 1; i < originalData.length; i++) {
      if (compare(originalData[i], x, y) != quad)
        return false;
    }
    return true;
  }

  /**
   * Performs point radius search.
   *
   * @param point
   *          the center of the circular region
   * @param radiusKM
   *          the radius in kilometers
   * @return a list of QuadTreeData that are within the given radius from the
   *         point
   */
  public List<QuadTreeData> queryByPointRadius(final DirectPosition2D point,
      final double radiusKM) {
    LatLonPointRadius pr = new LatLonPointRadius(point, radiusKM);
    return queryByPointRadius(point, radiusKM, this.root,
        new Rectangle2D.Double(EARTH_MIN_X, EARTH_MIN_Y, EARTH_MAX_X,
            EARTH_MAX_Y), pr.getRectangularRegionApproximation(360));
  }

  /**
   * Performs point radius search.
   *
   * @param point
   *          the center of the circular region
   * @param radiusKM
   *          the radius in kilometers
   * @param node
   *          quad tree root node
   * @param nodeRegion
   *          Rectangle2D representing the circular node region
   * @param searchRegion
   *          Rectangle2D representing the circular search region
   * @return a list of QuadTreeData that are within the given radius from the
   *         point
   */
  private List<QuadTreeData> queryByPointRadius(final DirectPosition2D point,
      final double radiusKM, final QuadTreeNode node,
      final Rectangle2D nodeRegion, final Rectangle2D searchRegion) {
    List<QuadTreeData> matches = new ArrayList<QuadTreeData>();
    if (node == null) {
      return matches;
    } else if (node.getNodeType() != NodeType.GRAY) {
      if (node.getNodeType() == NodeType.WHITE)
        return matches;
      else {
        QuadTreeData[] data = node.getData();
        for (int i = 0; i < node.getCount(); i++) {
          if (DistanceUtils.getHaversineDistance(data[i].getLatLon().y, data[i]
              .getLatLon().x, point.y, point.x) <= radiusKM) {
            matches.add(data[i]);
          }
        }
        return matches;
      }

    } else {
      Rectangle2D swRectangle = new Rectangle2D.Double(nodeRegion.getX(),
          nodeRegion.getY(), nodeRegion.getWidth() / 2,
          nodeRegion.getHeight() / 2);
      Rectangle2D seRectangle = new Rectangle2D.Double(nodeRegion.getX()
          + nodeRegion.getWidth() / 2, nodeRegion.getY(),
          nodeRegion.getWidth() / 2, nodeRegion.getHeight() / 2);
      Rectangle2D nwRectangle = new Rectangle2D.Double(nodeRegion.getX(),
          nodeRegion.getY() + nodeRegion.getHeight() / 2,
          nodeRegion.getWidth() / 2, nodeRegion.getHeight() / 2);
      Rectangle2D neRectangle = new Rectangle2D.Double(nodeRegion.getX()
          + nodeRegion.getWidth() / 2, nodeRegion.getY()
          + nodeRegion.getHeight() / 2, nodeRegion.getWidth() / 2, nodeRegion
          .getHeight() / 2);

      if (swRectangle.intersects(searchRegion)) {
        List<QuadTreeData> swMatches = queryByPointRadius(point, radiusKM, node
            .getChild(Quadrant.SW), swRectangle, searchRegion);
        for (QuadTreeData q : swMatches) {
          matches.add(q);
        }
      }
      if (seRectangle.intersects(searchRegion)) {
        List<QuadTreeData> seMatches = queryByPointRadius(point, radiusKM, node
            .getChild(Quadrant.SE), seRectangle, searchRegion);
        for (QuadTreeData q : seMatches) {
          matches.add(q);
        }
      }
      if (nwRectangle.intersects(searchRegion)) {
        List<QuadTreeData> nwMatches = queryByPointRadius(point, radiusKM, node
            .getChild(Quadrant.NW), nwRectangle, searchRegion);
        for (QuadTreeData q : nwMatches) {
          matches.add(q);
        }
      }
      if (neRectangle.intersects(searchRegion)) {
        List<QuadTreeData> neMatches = queryByPointRadius(point, radiusKM, node
            .getChild(Quadrant.NE), neRectangle, searchRegion);
        for (QuadTreeData q : neMatches) {
          matches.add(q);
        }
      }
    }
    return matches;
  }

  /**
   * Performs bounding box search.
   *
   * @param searchRegion
   *          Envelope representing the rectangular search region
   * @return a list of QuadTreeData that are within the given radius from the
   *         point
   */
  public List<QuadTreeData> queryByBoundingBox(final Envelope2D searchRegion) {
    Rectangle2D.Double rectArray[] = searchRegion.toRectangles();
    for (final Rectangle2D.Double r : rectArray) {
        r.x += 180;
        r.y += 90;
    }
    if (rectArray.length == 1) {
      // traverse tree once because region does not cross dateline
      return queryByBoundingBox(rectArray[0]);

    } else if (rectArray.length == 2) {
      // traverse tree twice since region crosses dateline
      List<QuadTreeData> firstMatches = queryByBoundingBox(rectArray[0]);
      List<QuadTreeData> secondMatches = queryByBoundingBox(rectArray[1]);

      // merge two lists and return
      for (QuadTreeData q : secondMatches) {
        if (!firstMatches.contains(q)) {
          firstMatches.add(q);
        }
      }
      return firstMatches;
    } else {
      return null;
    }
  }

  /**
   * Performs bounding box search.
   *
   * @param searchRegion
   *          Rectangle2D representing the rectangular search region
   * @return a list of QuadTreeData that are within the given radius from the
   *         point
   */
  private List<QuadTreeData> queryByBoundingBox(final Rectangle2D searchRegion) {
    return queryByBoundingBox(this.root, new Rectangle2D.Double(EARTH_MIN_X,
        EARTH_MIN_Y, EARTH_MAX_X, EARTH_MAX_Y), searchRegion);
  }

  /**
   * Performs bounding box search.
   *
   * @param node
   *          quad tree root node
   * @param nodeRegion
   *          Rectangle2D representing the rectangular node region
   * @param searchRegion
   *          Rectangle2D representing the rectangular search region
   * @return a list of QuadTreeData that are within the given radius from the
   *         point
   */
  private List<QuadTreeData> queryByBoundingBox(final QuadTreeNode node,
      final Rectangle2D nodeRegion, final Rectangle2D searchRegion) {

    List<QuadTreeData> matches = new ArrayList<QuadTreeData>();
    if (node == null) {
      return matches;
    } else if (node.getNodeType() != NodeType.GRAY) {
      if (node.getNodeType() == NodeType.WHITE)
        return matches;
      else {
        QuadTreeData[] data = node.getData();
        for (int i = 0; i < node.getCount(); i++) {
          if (searchRegion.contains(data[i].getX(), data[i].getY())) {
            matches.add(data[i]);
          }
        }
        return matches;
      }

    } else {
      Rectangle2D swRectangle = new Rectangle2D.Double(nodeRegion.getX(),
          nodeRegion.getY(), nodeRegion.getWidth() / 2,
          nodeRegion.getHeight() / 2);
      Rectangle2D seRectangle = new Rectangle2D.Double(nodeRegion.getX()
          + nodeRegion.getWidth() / 2, nodeRegion.getY(),
          nodeRegion.getWidth() / 2, nodeRegion.getHeight() / 2);
      Rectangle2D nwRectangle = new Rectangle2D.Double(nodeRegion.getX(),
          nodeRegion.getY() + nodeRegion.getHeight() / 2,
          nodeRegion.getWidth() / 2, nodeRegion.getHeight() / 2);
      Rectangle2D neRectangle = new Rectangle2D.Double(nodeRegion.getX()
          + nodeRegion.getWidth() / 2, nodeRegion.getY()
          + nodeRegion.getHeight() / 2, nodeRegion.getWidth() / 2, nodeRegion
          .getHeight() / 2);

      if (swRectangle.intersects(searchRegion)) {
        List<QuadTreeData> swMatches = queryByBoundingBox(node
            .getChild(Quadrant.SW), swRectangle, searchRegion);
        for (QuadTreeData q : swMatches) {
          matches.add(q);
        }
      }
      if (seRectangle.intersects(searchRegion)) {
        List<QuadTreeData> seMatches = queryByBoundingBox(node
            .getChild(Quadrant.SE), seRectangle, searchRegion);
        for (QuadTreeData q : seMatches) {
          matches.add(q);
        }
      }
      if (nwRectangle.intersects(searchRegion)) {
        List<QuadTreeData> nwMatches = queryByBoundingBox(node
            .getChild(Quadrant.NW), nwRectangle, searchRegion);
        for (QuadTreeData q : nwMatches) {
          matches.add(q);
        }
      }
      if (neRectangle.intersects(searchRegion)) {
        List<QuadTreeData> neMatches = queryByBoundingBox(node
            .getChild(Quadrant.NE), neRectangle, searchRegion);
        for (QuadTreeData q : neMatches) {
          matches.add(q);
        }
      }
    }
    return matches;
  }

  /**
   * Returns the size of the quad tree.
   *
   * @return size of the quad tree.
   */
  public int size() {
    return this.size;
  }

  /**
   * Returns the root node of the quad tree.
   *
   * @return root node of the quad tree.
   */
  final QuadTreeNode getRoot() {
    return this.root;
  }

  /**
   * Sets the size of the quad tree.
   *
   * @param size The new quad tree size.
   */
  public void setSize(int size) {
    this.size = size;
  }

  /**
   * Returns the size of the quad tree.
   *
   * @return size of quad tree
   */
  public int getSize() {
    return this.size;
  }

  /**
   * Sets the node size of the quad tree.
   *
   * @param nodeSize The new node size.
   */
  public void setNodeSize(int nodeSize) {
    this.nodeSize = nodeSize;
  }

  /**
   * Returns the node size of the quad tree.
   *
   * @return node size of the quad tree.
   */
  public int getNodeSize() {
    return this.nodeSize;
  }

  /**
   * Returns the capacity of node in the quad tree.
   *
   * @return capacity of node in the quad tree.
   */
  public int getCapacity() {
    return this.capacity;
  }

  /**
   * Returns the maximum depth of the quad tree.
   *
   * @return maximum depth of the quad tree.
   */
  public int getDepth() {
    return this.maxDepth;
  }

  /**
   * Sets the capacity of node in the quad tree.
   *
   * @param capacity the capacity of node in the quad tree.
   */
  public void setCapacity(int capacity) {
    this.capacity = capacity;
  }

  /**
   * Sets the maximum depth of the quad tree.
   *
   * @param depth the maximum depth of the quad tree.
   */
  public void setDepth(int depth) {
    this.maxDepth = depth;
  }
}
