/*
DataNodeLogger.java
Copyright (c) 2023 by an anonymous author

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

public class DataNodeLogger {
    public DataNodeLogger() {}

    public void log(@Nullable String message, @Nullable ArrayList<String> trace) {
        if (message != null)
            System.out.println(message);
        if (trace != null) {
            int depth = 0;
            StringBuilder indent = new StringBuilder();
            indent.ensureCapacity(trace.size() * 2);
            for(String line : trace) {
                System.out.println(indent.toString() + line);
                indent.append("  ");
            }
        }
    }
}
