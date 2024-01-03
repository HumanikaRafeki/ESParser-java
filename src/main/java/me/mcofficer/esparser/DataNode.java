/*
DataNode.java
Copyright (c) 2014 Michael Zahniser
Copyright (C) 2017 Frederick W. Goy IV
Copyright (C) 2018 MCOfficer

This program is a derivative of the source code from the Endless Sky
project, which is licensed under the GNU GPLv3.

Endless Sky source: https://github.com/endless-sky/endless-sky


This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/


package me.mcofficer.esparser;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.OptionalDouble;
import java.lang.ref.WeakReference;
import java.util.Optional;
import java.io.File;
import java.io.BufferedReader;
import java.util.List;
import java.io.IOException;

public class DataNode {

    private DataNode parent;
    private ArrayList<DataNode> children;
    private ArrayList<String> tokens;
    private DataNodeLogger logger;
    private WeakReference<DataFile> file;
    private int firstLine = -1;
    private int lastLine = -1;

    /** Creates a DataNode. Generally, this should only be called by a DataFile
     * @param parent The parent, or null if this is the root of the tree.
     * @children All children of this node, or null if it is a leaf
     * @tokens All tokens on the first line of the node, or null for the root of the tree
     * @logger An object to use when logging messages related to this node */
    public DataNode(@Nullable DataNode parent, @Nullable ArrayList<DataNode> children, @Nullable ArrayList<String> tokens, @Nullable DataNodeLogger logger) {
        this.parent = parent;
        this.children = children == null ? new ArrayList<>() : children;
        this.tokens = tokens == null ? new ArrayList<>() : tokens;
        this.logger = logger;
    }

    /** The number of tokens on the first line of the DataNode
     *
     * @returns the length of the array returned by getTokens() */
    public int size() {
        return tokens.size();
    }

    /** Gets an array of string tokens on the first line of the DataNode.
     *
     * @returns an array of tokens */
    public ArrayList<String> getTokens() {
        return tokens;
    }

    /** Sets the one-based line number of the first line in the file that this DataNode was read from. Use -1 if the information is unknown.
     *
     * @param line the line number, or -1 if the information is unknown */
    void setFirstLine(int line) {
        firstLine = line;
    }

    /** Sets the one-based line number of the last line in the file that this DataNode was read from. Use -1 if the information is unknown.
     *
     * @param line the line number, or -1 if the information is unknown */
    void setLastLine(int line) {
        lastLine = line;
    }

    /** Sets the DataFile that this DataNode was read from. This DataNode only keeps a weak reference to the DataFile. If the caller loses all references to the DataFile, the DataNode won't see it anymore
    *
    * @param file The DataFile that read this DataNode */
    void setFile(DataFile file) {
        this.file = file != null ? new WeakReference<>(file) : null;
    }

    /** Gets the one-based line number of the first line in the file that this DataNode was read from
     *
     * @returns -1 if the information is unknown, or a number greater than 0 as the line number */
    public int getFirstLine() {
        return firstLine;
    }

    /** Gets the one-based line number of the last line in the file that this DataNode was read from
     *
     * @returns -1 if the information is unknown, or a number greater than 0 as the line number */
    public int getLastLine() {
        return lastLine;
    }

    /** Returns the DataFile that created this DataNode, if available. The DataNode only keeps a weak reference to its DataFile. If the original caller loses reference to the DataFile, the DataNodes won't see it anymore.
     *
     * @returns an Optional containing the DataFile, or an empty optional if the information is unavailable */
    public Optional<DataFile> getFile() {
        WeakReference<DataFile> file = this.file;
        if (file != null)
            return Optional.ofNullable(file.get());
        else
            return Optional.empty();
    }

    /** Reads all lines of the current node from its file. All lines will end with an end-of-line character ('\n')
     *
     * @returns all relevant lines in a List, or an empty list if that information is unavailable
     */
    public List<String> getLines() {
        WeakReference<DataFile> fileRef = this.file;
        if(fileRef == null)
            return Collections.emptyList();
        final DataFile file = fileRef.get();
        final int firstLine = this.firstLine;
        final int lastLine = this.lastLine;
        if(file == null || firstLine <= 0 || lastLine <= 0)
            return Collections.emptyList();
        DataFile dataFile = this.file.get();
        return dataFile.getLines(firstLine, lastLine + 1);
    }

    /** Gets the string token at the given zero-based index from the first line of the DataNode.
     *
     * @param index The index to search; must be 0 or more. This is the index within getTokens()
     */
    public String token(int index) {
        if (index > tokens.size())
            return "";
        else
            return tokens.get(index);
    }

    /** Converts the string token at the given index to a double. Returns zero if the token is out-of-bounds or not a number (according to isNumberAt()). This uses the token from token() of that index
     * 
     * @param index The index to search; must be 0 or more. This is the index within getTokens()
     * @returns the double at that index, or 0 if the token is not a number, or is beyond the last available token. */
    public double valueAt(int index) {
        if (index > tokens.size() || !isNumberAt(index)) {
            printTrace("Cannot convert token at index " + index + " to a number.");
            return .0;
        }

        return Double.valueOf(tokens.get(index));
    }

    /** Converts the string token at the given index to a double. Returns empty if the token is out-of-bounds or not a number (according to isNumberAt()). This uses the token from token() of that index
     * 
     * @param index The index to search; must be 0 or more. This is the index within getTokens()
     * @returns the double at that index, or an empty OptionalDouble if the token is not a number, or is beyond the last available token. */
    public OptionalDouble optionalValue(int index) {
        if (index > tokens.size() || !isNumberAt(index))
            return OptionalDouble.empty();
        return OptionalDouble.of(Double.valueOf(tokens.get(index)));
    }

    /** Uses the same algorithm as in Endless Sky to decide if the token at a given index is a number. This is the token on the first line of the node, as from the token() function.
     *
     * @param index The index to search; must be 0 or more. This is the index within getTokens()
     * @returns true if the token is a number, or false otherwise */
    public boolean isNumberAt(int index) {
        if (index >= size())
            return  false;

        String token = tokens.get(index);

        boolean hasDecimalPoint = false;
        boolean hasExponent = false;
        boolean isLeading = true;

        for (char c : token.toCharArray()) {
            if (isLeading) {
                isLeading = false;
                if (c == '-' || c == '+')
                    continue;
            }

            if (c == '.') {
                if (hasDecimalPoint || hasExponent)
                    return false;
                hasDecimalPoint = true;
            }
            else if (c == 'e' || c == 'E') {
                if (hasExponent)
                    return false;
                hasExponent = true;
                isLeading = true;
            }
            else if (!Character.isDigit(c))
                return false;
        }
        return true;
    }

    /** Does the node have at least one DataNode child?
     *
     * @returns true if there is at least one DataNode child, or false otherwise */
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    /** Gets the list of children
     *
     * @returns a list of children. If there are no children, the list is empty. */
    public ArrayList<DataNode> getChildren() {
        return children;
    }

    /** Walks the entire tree under this node producing a list of all descendents of this node.
     *
     * @returns a list of descendents in depth-first order. If there are no children, the list is empty. */
    public ArrayList<DataNode> getChildrenFlat() {
        ArrayList<DataNode> yield = new ArrayList<>();
        for (DataNode child : children) {
            yield.add(child);
            yield.addAll(child.getChildrenFlat());
        }
        return yield;
    }

    /** Creates a new ArrayList containing the children in reversed order
     * 
     * @returns a new ArrayList with the children, or an empty list if there are no children */
    public ArrayList<DataNode> getChildrenReversed() {
        ArrayList<DataNode> reversed = new ArrayList<>(children);
        Collections.reverse(reversed);
        return reversed;
    }

    /** Adds a child node to this DataNode. Generally, this should only be called by DataFile
     *
     * @param node The DataNode to add */
    public void append(DataNode node) {
        node.parent = this;
        children.add(node);
    }

    /** Removes a child node from this DataNode. Generally, this should only be called by DataFile
     *
     * @param node the DataNode to remove */
    public void remove(DataNode node) {
        node.parent = null;
        children.remove(node);
    }

    /** Returns a deep copy of this DataNode. All chidren and tokens are duplicated, recursively. Other attributes are copied as references.
     *
     * @returns the deep copy */
    public DataNode copy() {
        DataNode copy = new DataNode(null, null, new ArrayList<>(tokens), logger);
        if(hasChildren())
            for (DataNode child : children)
                copy.append(child.copy());
        return copy;
    }

    /** Returns a deep copy of this DataNode.
     *
     * @returns the deep copy */
    protected Object clone() throws CloneNotSupportedException {
        return copy();
    }

    /** Removes this node from its tree */
    public void delete() {
        if (parent != null)
            parent.remove(this);

        tokens = null;

        for (DataNode child : children)
            child.delete();

        children = new ArrayList<DataNode>();
    }

    /** Prints a "stack trace" within the node tree of this node with an optional message. If there is a logger, it is logged there.
     *
     * @param message the optional message */
    public void printTrace(@Nullable String message) {
        ArrayList<String> trace = new ArrayList<String>();
        makeTrace(trace);
        logger.log(message, trace);
    }

    /** Internal implementation of printTrace. Recurses through the node tree upward creating a stack trace.
     * 
     * @param trace The container for the trace, one tree level per String.
     * @returns the number of spaces that was used by this makeTrace */
    private int makeTrace(ArrayList<String> trace) {
        int indent = 0;
        if (parent != null)
            indent = parent.makeTrace(trace) + 2;
        if (tokens.isEmpty())
            return indent;

        StringBuilder builder = new StringBuilder();

        if(parent != null && firstLine > 0)
            builder.append('L').append(firstLine).append(": ");
        for(int i = 0; i < indent; i++)
            builder.append(' ');

        boolean first = true;
        for (String token : tokens) {
            if (!first)
                builder.append(' ');
            first = false;

            boolean hasSpace = false;
            boolean hasQuote = false;

            for (char c : token.toCharArray()) {
                if (Character.isWhitespace(c))
                    hasSpace = true;
                if (c == '"')
                    hasQuote = true;
            }

            String quotationMark = null;
            if (hasSpace)
                quotationMark = hasQuote ? "`" : "\"";

            if (quotationMark != null)
                builder.append(quotationMark);
            builder.append(token);
            if (quotationMark != null)
                builder.append(quotationMark);
        }
        trace.add(builder.toString());
        return indent;
    }
}
