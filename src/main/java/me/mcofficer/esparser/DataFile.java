/*
DataFile.java
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DataFile {

    private DataNode root;

    public DataFile(String file) throws IOException {
        this(Files.readAllLines(Paths.get(file)), new DataNodeLogger());
    }

    public DataFile(List<String> data) throws IOException {
        this(data, new DataNodeLogger());
    }

    public DataFile(Stream<String> data) throws IOException {
        this(data, new DataNodeLogger());
    }

    public DataFile(String file, DataNodeLogger logger) throws IOException {
        this(Files.readAllLines(Paths.get(file)), logger);
    }

    public DataFile(List<String> data, DataNodeLogger logger) throws IOException {
        root = new DataNode(null, null, null, logger);
        parse(data, logger);
    }

    public DataFile(Stream<String> data, DataNodeLogger logger) throws IOException {
        root = new DataNode(null, null, null, logger);
        parse(data, logger);
    }

    public ArrayList<DataNode> getNodes() {
        return root.getChildren();
    }

    public ArrayList<DataNode> getNodesReversed() {
        return root.getChildrenReversed();
    }

    private void parse(List<String> data, @Nullable DataNodeLogger logger) {
        parse(data.stream(), logger);
    }

    private void parse(Stream<String> data, @Nullable DataNodeLogger logger) {
        List<String> eoln = data.map(s -> s += "\n").collect(Collectors.toList());
        Stack<DataNode> stack = new Stack<>();
        stack.add(root);
        Stack<Integer> whiteStack = new Stack<>();
        whiteStack.add(-1);

        for (String line : eoln) {
            char[] chars = line.toCharArray();
            int i = 0;
            int white = 0;
            while (i < chars.length && Character.isWhitespace(chars[i]) && chars[i] != '\n') {
                white += 1;
                i += 1;
            }

            if (chars[i] == '#' | chars[i] == '\n')
                continue;

            while (whiteStack.peek() >= white) {
                whiteStack.pop();
                stack.pop();
            }

            DataNode node = new DataNode(null, null, null, logger);
            stack.peek().append(node);

            stack.add(node);
            whiteStack.add(white);

            while (i < chars.length && chars[i] != '\n') {
                char endQuote = chars[i];
                boolean isQuoted = false;
                if (endQuote == '"' || endQuote == '`') {
                    isQuoted = true;
                    i += 1;
                    if(i == chars.length)
                        break;
                }

                String token = "";
                if (isQuoted) {
                    while (i < chars.length && chars[i] != '\n' && chars[i] != endQuote) {
                        token += chars[i];
                        i += 1;
                    }
                    if (i == chars.length || chars[i] != endQuote)
                        node.printTrace("Closing Quote is missing");
                    if(i == chars.length)
                        break;
                    i += 1;
                }
                else {
                    while (i != chars.length && !Character.isWhitespace(chars[i])) {
                        token += chars[i];
                        i += 1;
                    }
                }
                node.getTokens().add(token);

                if (i >= chars.length)
                    break;
                if (chars[i] != '\n')
                    while (i != chars.length && Character.isWhitespace(chars[i]) && chars[i] != '\n')
                        i += 1;
                if (chars[i] == '#')
                    break;
            }
        }
    }

    public void append(DataNode node) {
        root.append(node);
    }

    public void remove(DataNode node) {
        root.remove(node);
    }
}
