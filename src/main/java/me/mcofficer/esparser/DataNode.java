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

public class DataNode {

    private DataNode parent;
    private ArrayList<DataNode> children;
    private ArrayList<String> tokens;
    private DataNodeLogger logger;

    public DataNode(@Nullable DataNode parent, @Nullable ArrayList<DataNode> children, @Nullable ArrayList<String> tokens, @Nullable DataNodeLogger logger) {
        this.parent = parent;
        this.children = children == null ? new ArrayList<>() : children;
        this.tokens = tokens == null ? new ArrayList<>() : tokens;
        this.logger = logger;
    }

    public int size() {
        return tokens.size();
    }

    public ArrayList<String> getTokens() {
        return tokens;
    }

    public double valueAt(int index) {
        if (!isNumberAt(index)) {
            printTrace("Cannot convert token at index " + index + " to a number.");
            return .0;
        }

        return Double.valueOf(tokens.get(index));
    }

    private boolean isNumberAt(int index) {
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

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public ArrayList<DataNode> getChildren() {
        return children;
    }

    public ArrayList<DataNode> getChildrenFlat() {
        ArrayList<DataNode> yield = new ArrayList<>();
        for (DataNode child : children) {
            yield.add(child);
            yield.addAll(child.getChildrenFlat());
        }
        return yield;
    }

    public ArrayList<DataNode> getChildrenReversed() {
        ArrayList<DataNode> reversed = new ArrayList<>(children);
        Collections.reverse(reversed);
        return reversed;
    }

    public void append(DataNode node) {
        node.parent = this;
        children.add(node);
    }

    public void remove(DataNode node) {
        node.parent = null;
        children.remove(node);
    }

    public DataNode copy() {
        DataNode copy = new DataNode(null, null, new ArrayList<>(tokens), logger);
        if(hasChildren())
            for (DataNode child : children)
                copy.append(child.copy());
        return copy;
    }

    public void delete() {
        if (parent != null)
            parent.remove(this);

        tokens = null;

        for (DataNode child : children)
            child.delete();

        children = new ArrayList<DataNode>();
    }

    public void printTrace(@Nullable String message) {
        ArrayList<String> trace = new ArrayList<String>();
        makeTrace(trace);
        logger.log(message, trace);
    }

    private int makeTrace(ArrayList<String> trace) {
        int indent = 0;
        if (parent != null)
            indent = parent.makeTrace(trace) + 2;
        if (tokens.isEmpty())
            return indent;

        StringBuilder builder = new StringBuilder();
        for (String token : tokens) {
            if (builder.length() > 0)
                builder.append(' ');

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
