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

/**
 * Implementation of quad tree node.
 *
 */
final class QuadTreeNode {

  private QuadTreeData[] data;
  private QuadTreeNode nw;
  private QuadTreeNode ne;
  private QuadTreeNode se;
  private QuadTreeNode sw;
  private NodeType type;
  private int id;
  private int capacity;
  private int dataCount;
  private static final int MIN_CAPACITY = 10;

  /**
   * Constructs a quad tree node that can store data
   *
   * @param id
   *          node's id
   * @param capacity
   *          node's capcacity
   */
  public QuadTreeNode(int id, int capacity) {
    this.capacity = capacity > 0 ? capacity:MIN_CAPACITY;
    this.dataCount = 0;
    this.data = new QuadTreeData[this.capacity];
    this.type = NodeType.BLACK;
    this.nw = null;
    this.ne = null;
    this.sw = null;
    this.se = null;
    this.id = id;
  }

  /**
   * Constructs a quad tree node that acts as a parent.
   *
   * @param type
   *          node's type
   * @param id
   *          node's id
   */
  public QuadTreeNode(NodeType type, int id) {
    this.type = type;
    this.nw = null;
    this.ne = null;
    this.sw = null;
    this.se = null;
    this.data = null;
    this.id = id;
  }

  /**
   * Add data to the node.
   *
   * @param data
   *          data to be added
   */
  public void addData(QuadTreeData data) {
    if (this.dataCount < this.capacity) {
      this.data[dataCount] = data;
      this.dataCount++;
    }
  }

  /**
   * Gets the number of data stored in the node
   *
   * @return number of data stored in the node
   */
  public int getCount() {
    return this.dataCount;
  }

  /**
   * Gets the node type.
   *
   * @return node type
   */
  public NodeType getNodeType() {
    return this.type;
  }

  /**
   * Sets the node's quadrant to point to the specified child.
   *
   * @param child
   *          child of this node
   * @param q
   *          quadrant where the child resides
   */
  public void setChild(QuadTreeNode child, Quadrant q) {
    switch (q) {
    case NW:
      this.nw = child;
      break;
    case NE:
      this.ne = child;
      break;
    case SW:
      this.sw = child;
      break;
    case SE:
      this.se = child;
      break;
    }
  }

  /**
   * Returns the child of this node that resides in the specified quadrant.
   *
   * @param q
   *          specified quadrant
   * @return child in the specified quadrant
   */
  public QuadTreeNode getChild(Quadrant q) {
    switch (q) {
    case NW:
      return this.nw;
    case NE:
      return this.ne;
    case SW:
      return this.sw;
    case SE:
      return this.se;
    default:
      return null;
    }
  }

  /**
   * Returns the data stored in this node.
   *
   * @return data stored in this node
   */
  public QuadTreeData[] getData() {
    return this.data;
  }

  /**
   * Returns node's id.
   *
   * @return node's id
   */
  public int getId() {
    return this.id;
  }

  /**
   * Returns node's capacity.
   *
   * @return node's capacity
   */
  public int getCapacity() {
    return this.capacity;
  }
}
