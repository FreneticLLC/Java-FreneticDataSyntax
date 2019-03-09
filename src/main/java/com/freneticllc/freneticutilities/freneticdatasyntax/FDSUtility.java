//
// This file is part of Frenetic Utilities, created by Frenetic LLC.
// This code is Copyright (C) Frenetic LLC under the terms of the MIT license.
// See README.md or LICENSE.txt in the FreneticUtilities source root for the contents of the license.
//

package com.freneticllc.freneticutilities.freneticdatasyntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utilities for the FreneticDataSyntax engine.
 */
public class FDSUtility {

    /**
     * The default splitter character for section paths.
     * To change to or a custom default, use "DefaultSectionPathSplit".
     * To change for a specific section, use "FDSSection.SectionPathSplit".
     * This is a dot value.
     */
    public static final char DEFAULT_SECTION_PATH_SPLIT = '.';

    /**
     * The default splitter character for section paths.
     * For the internal unmodified default, use "DEFAULT_SECTION_PATH_SPLIT".
     * To change for a specific section, use "FDSSection.SectionPathSplit".
     * This is initially a dot value. Altering this may cause issues (in particular with escaping) depending on the chosen value.
     */
    public static char defaultSectionPathSplit = DEFAULT_SECTION_PATH_SPLIT;

    /**
     * Cleans file line endings, tabs, and any other data that may cause issues.
     * @param contents The original file data.
     * @return The cleaned file data.
     */
    public static String cleanFileData(String contents) {
        if (contents.contains("\r\n")) {
            // Windows to Unix
            contents = contents.replace("\r", "");
        }
        else {
            // Old Mac to Unix (leaves Unix form unaltered)
            contents = contents.replace('\r', '\n');
        }
        return contents.replace("\t", "    "); // 4 spaces
    }

    /**
     * Escapes a String for output.
     * <para>Only good for values. For keys, use EscapeKey
     * @param str The String to escape.
     * @return The escaped String.
     */
    public static String escape(String str) {
        str = str.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
        if (str.endsWith(" ")) {
            str = str + "\\x";
        }
        if (str.startsWith(" ")) {
            str = "\\x" + str;
        }
        return str;
    }

    /**
     * Escapes a String for usage as a section key.
     * @param str The String to escape.
     * @return The escaped String.
     */
    public static String escapeKey(String str) {
        return escape(str).replace(".", "\\d").replace(":", "\\c").replace("=", "\\e");
    }

    /**
     * <summary>
     * UnEscapes a String for output.
     * Only good for values. For keys, use unEscapeKey.
     * @param str The String to unescape.
     * @return The unescaped String.
     */
    public static String unEscape(String str) {
        str = str.replace("\\t", "\t").replace("\\n", "\n").replace("\\r", "\r").replace("\\x", "").replace("\\\\", "\\");
        return str;
    }

    /**
     * UnEscapes a String for usage as a section key.
     * @param str The String to unescape.
     * @return The unescaped String.
     */
    public static String unEscapeKey(String str) {
        return unEscape(str.replace("\\d", ".").replace("\\c", ":").replace("\\e", "="));
    }

    public static final Pattern LONG_PATTERN = Pattern.compile("\\d+");
    public static final Pattern DOUBLE_PATTERN = Pattern.compile("\\d+(\\.\\d+)?");

    /**
     * Interprets the type of the input text.
     * @param input The input text.
     * @return The correctly typed result.
     */
    public static Object interpretType(String input) {
        if (DOUBLE_PATTERN.matcher(input).matches()) {
            try {
                if (LONG_PATTERN.matcher(input).matches()) {
                        Long asLong = Long.parseLong(input);
                        if (asLong.toString().equals(input)) {
                            return asLong;
                        }
                }
                else {
                    Double asDouble = Double.parseDouble(input);
                    if (asDouble.toString().equals(input)) {
                        return asDouble;
                    }
                }
            }
            catch (NumberFormatException ex) {
                // Ignore
            }
        }
        else {
            if (input.equals("true")) {
                return true;
            }
            if (input.equals("false")) {
                return false;
            }
        }
        return input;
    }

    /**
     * Non-regex by-character split method.
     * @param str the string to split
     * @param c the character to split around
     * @return the split list.
     */
    public static String[] split(String str, char c) {
        List<String> strings = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) {
                strings.add(str.substring(start, i));
                start = i + 1;
            }
        }
        strings.add(str.substring(start));
        return strings.toArray(new String[0]);
    }

    /**
     * Non-regex by-character split method.
     * @param str the string to split
     * @param c the character to split around
     * @param max the maximum splits allowed
     * @return the split list.
     */
    public static String[] split(String str, char c, int max) {
        List<String> strings = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) {
                strings.add(str.substring(start, i));
                start = i + 1;
                if (strings.size() + 1 == max) {
                    break;
                }
            }
        }
        strings.add(str.substring(start));
        return strings.toArray(new String[0]);
    }

    /**
     * Quick ASCII toLowerCase method.
     */
    public static String toLowerCase(String input) {
        char[] data = input.toCharArray();
        for (int i = 0; i < data.length; i++) {
            if (data[i] >= 'A' && data[i] <= 'Z') {
                data[i] -= 'A' - 'a';
            }
        }
        return new String(data);
    }

    /**
     * Trims the start of a string only.
     * @param str the original string.
     * @return the string with no preceding spaces.
     */
    public static String trimStart(String str) {
        for (int start = 0; start < str.length(); start++) {
            char c = str.charAt(start);
            if (c != ' ') {
                return str.substring(start);
            }
        }
        return "";
    }

    /**
     * Trims the end of a string only.
     * @param str the original string.
     * @return the string with no trailing spaces.
     */
    public static String trimEnd(String str) {
        for (int end = str.length() - 1; end >= 0; end--) {
            char c = str.charAt(end);
            if (c != ' ') {
                return str.substring(0, end + 1);
            }
        }
        return "";
    }

    /**
     * Joins a set of objects with a separator in-between each.
     * @param separator the separator string.
     * @param objects the object set.
     * @return the joined string.
     */
    public static String join(String separator, Set objects) {
        StringBuilder builder = new StringBuilder((separator.length() + 4) * objects.size());
        int count = 0;
        for (Object val : objects) {
            builder.append(val);
            count++;
            if (count != objects.size()) {
                builder.append(separator);
            }
        }
        return builder.toString();
    }
}
