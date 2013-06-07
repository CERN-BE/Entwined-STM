/*
 * Entwined STM
 * 
 * © Copyright 2013 CERN. This software is distributed under the terms of the Apache License Version 2.0, copied
 * verbatim in the file "COPYING". In applying this licence, CERN does not waive the privileges and immunities granted
 * to it by virtue of its status as an Intergovernmental Organization or submit itself to any jurisdiction.
 */
package cern.entwined;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Represents a node of a tree. Supports stack of zero to n child nodes. Every node can be associated with a value of
 * type T.
 * 
 * @author Ivan Koblik
 */
class Node<T> {

    /**
     * Data kept by the node.
     */
    private final T value;

    /**
     * Node's parent node.
     */
    private Node<T> parent;

    /**
     * List of child nodes.
     */
    private final List<Node<T>> children;

    /**
     * Creates a root Node<T> with an instance of T.
     * 
     * @param value an instance of T.
     */
    public Node(T value) {
        this.parent = null;
        this.value = value;
        this.children = new ArrayList<Node<T>>();
    }

    /**
     * Return the children of Node<T>.
     * 
     * @return the children of Node<T>
     */
    public List<Node<T>> getChildren() {
        return Collections.unmodifiableList(this.children);
    }

    /**
     * Returns the number of immediate children of this Node<T>.
     * 
     * @return the number of immediate children.
     */
    public int getNumberOfChildren() {
        return children.size();
    }

    /**
     * Adds a child to the list of children. Single {@link Node} cannot be a child of more than one {@link Node}.
     * 
     * @param child a Node<T> object to set.
     */
    public void addChild(Node<T> child) {
        Utils.checkNull("Child node", child);
        if (null != child.parent) {
            throw new IllegalArgumentException("Added element is a child of another node");
        }
        child.parent = this;
        children.add(child);
    }

    /**
     * Remove the last added child.
     * 
     * @return The removed child node or null.
     */
    public Node<T> removeChild() {
        int childrenLen = children.size();
        if (childrenLen != 0) {
            Node<T> child = children.remove(childrenLen - 1);
            child.parent = null;
            return child;
        }
        return null;
    }

    /**
     * Returns value kept by the node.
     * 
     * @return The node's value.
     */
    public T getValue() {
        return this.value;
    }

    /**
     * Returns the parent node or <code>null</code> if this node is the root.
     * 
     * @return The parent node or <code>null</code>.
     */
    public Node<T> getParent() {
        return this.parent;
    }

    @Override
    public String toString() {
        return super.toString() + ", value=" + String.valueOf(this.value);
    }
}
