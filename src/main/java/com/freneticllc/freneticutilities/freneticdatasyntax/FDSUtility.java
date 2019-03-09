//
// This file is part of Frenetic Utilities, created by Frenetic LLC.
// This code is Copyright (C) Frenetic LLC under the terms of the MIT license.
// See README.md or LICENSE.txt in the FreneticUtilities source root for the contents of the license.
//

package com.freneticllc.freneticutilities.freneticdatasyntax;

/**
 * Utilities for the FreneticDataSyntax engine.
 */
public class FDSUtility
{
    /**
     * The default splitter character for section paths.
     * To change to or a custom default, use "DefaultSectionPathSplit".
     * To change for a specific section, use "FDSSection.SectionPathSplit".
     * This is a dot value.
     */
    public const char DEFAULT_SECTION_PATH_SPLIT = '.';

    /**
     * The default splitter character for section paths.
     * For the internal unmodified default, use "DEFAULT_SECTION_PATH_SPLIT".
     * To change for a specific section, use "FDSSection.SectionPathSplit".
     * This is initially a dot value. Altering this may cause issues (in particular with escaping) depending on the chosen value.
     */
    public static char DefaultSectionPathSplit = DEFAULT_SECTION_PATH_SPLIT;

    /**
     * Reads a file into an "FDSSection". Throws normal exceptions on any issue.
     * NOTE: May be removed or switched for journalling logic in the future.
     */
     * @param fname The name of the file to read.
     * @return An "FDSSection" containing the same data as the file (if successfully read).
    public static FDSSection ReadFile(string fname)
    {
        return new FDSSection(StringConversionHelper.UTF8Encoding.GetString(File.ReadAllBytes(fname)));
    }

    /**
     * Saves an "FDSSection" into a file. Throws normal exceptions on any issue.
     * NOTE: May be removed or switched for journalling logic in the future.
     */
     * @param section The data to save.
     * @param fname The name of the file to read.
    public static void SaveToFile(this FDSSection section, string fname)
    {
        File.WriteAllBytes(fname, StringConversionHelper.UTF8Encoding.GetBytes(section.SaveToString()));
    }

    private static readonly byte[] EMPTY_BYTES = new byte[0];

    /**
     * Converts a Base64 string to a byte array.
     */
     * @param inputString The input string to convert.
     * @return The byte array output.
    public static byte[] FromBase64(string inputString)
    {
        if (inputString.Length == 0)
        {
            return EMPTY_BYTES;
        }
        return Convert.FromBase64String(inputString);
    }

    /**
     * Cleans file line endings, tabs, and any other data that may cause issues.
     */
     * @param contents The original file data.
     * @return The cleaned file data.
    public static string CleanFileData(string contents)
    {
        if (contents.Contains("\r\n"))
        {
            // Windows to Unix
            contents = contents.Replace("\r", "");
        }
        else
        {
            // Old Mac to Unix (leaves Unix form unaltered)
            contents = contents.Replace('\r', '\n');
        }
        return contents.Replace("\t", "    "); // 4 spaces
    }

    /**
     * Escapes a string for output.
     * <para>Only good for values. For keys, use "EscapeKey(string)".</para>
     */
     * @param str The string to escape.
     * @return The escaped string.
    public static string Escape(string str)
    {
        str = str.Replace("\\", "\\\\").Replace("\t", "\\t").Replace("\n", "\\n").Replace("\r", "\\r");
        if (str.EndWithFast(' '))
        {
            str = str + "\\x";
        }
        if (str.StartsWithFast(' '))
        {
            str = "\\x" + str;
        }
        return str;
    }

    /**
     * Escapes a string for usage as a section key.
     */
     * @param str The string to escape.
     * @return The escaped string.
    public static string EscapeKey(string str)
    {
        return Escape(str).Replace(".", "\\d").Replace(":", "\\c").Replace("=", "\\e");
    }

    /**
     * UnEscapes a string for output.
     * <para>Only good for values. For keys, use "UnEscapeKey(string)".</para>
     */
     * @param str The string to unescape.
     * @return The unescaped string.
    public static string UnEscape(string str)
    {
        str = str.Replace("\\t", "\t").Replace("\\n", "\n").Replace("\\r", "\r").Replace("\\x", "").Replace("\\\\", "\\");
        return str;
    }

    /**
     * UnEscapes a string for usage as a section key.
     */
     * @param str The string to unescape.
     * @return The unescaped string.
    public static string UnEscapeKey(string str)
    {
        return UnEscape(str.Replace("\\d", ".").Replace("\\c", ":").Replace("\\e", "="));
    }

    /**
     * Interprets the type of the input text.
     */
     * @param input The input text.
     * @return The correctly typed result.
    public static object InterpretType(string input)
    {
        if (long.TryParse(input, out long aslong) && aslong.ToString() == input)
        {
            return aslong;
        }
        if (double.TryParse(input, out double asdouble) && asdouble.ToString() == input)
        {
            return asdouble;
        }
        if (input == "true")
        {
            return true;
        }
        if (input == "false")
        {
            return false;
        }
        return input;
    }
}
