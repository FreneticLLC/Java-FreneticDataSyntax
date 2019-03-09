//
// This file is part of Frenetic Utilities, created by Frenetic LLC.
// This code is Copyright (C) Frenetic LLC under the terms of the MIT license.
// See README.md or LICENSE.txt in the FreneticUtilities source root for the contents of the license.
//

package com.freneticllc.freneticutilities.freneticdatasyntax;

/**
 * Represents a FreneticDataSyntax section or file.
 */
public class FDSSection
{

    /**
     * Constructs the FDS Section from textual data.
     */
     * @param contents The contents of the data file.
    public FDSSection(string contents)
    {
        StartingLine = 1;
        contents = FDSUtility.CleanFileData(contents);
        Dictionary<int, FDSSection> spacedsections = new Dictionary<int, FDSSection>() { { 0, this } };
        List<string> ccomments = new List<string>();
        List<string> seccomments = new List<string>();
        FDSSection csection = this;
        string[] data = contents.SplitFast('\n');
        int pspaces = 0;
        string secwaiting = null;
        List<FDSData> clist = null;
        for (int i = 0; i < data.Length; i++)
        {
            string line = data[i];
            int spaces;
            for (spaces = 0; spaces < line.Length; spaces++)
            {
                if (line[spaces] != ' ')
                {
                    break;
                }
            }
            if (spaces == line.Length)
            {
                continue;
            }
            string datum = line.Substring(spaces).TrimEnd(' ');
            if (datum.StartsWith("#"))
            {
                ccomments.Add(datum.Substring(1));
                continue;
            }
            if (spaces < pspaces)
            {
                if (spacedsections.TryGetValue(spaces, out FDSSection temp))
                {
                    csection = temp;
                    foreach (int test in new List<int>(spacedsections.Keys))
                    {
                        if (test > spaces)
                        {
                            spacedsections.Remove(test);
                        }
                    }
                }
                else
                {
                    Exception(i, line, "Spaced incorrectly. Spacing length is less than previous spacing length,"
                        + "but does not match the spacing value of any known section, valid: "
                        + string.Join(" / ", spacedsections.Keys) + ", found: " + spaces + ", was: " + pspaces);
                }
            }
            if (datum[0] == '-' || datum[0] == '=')
            {
                string clistline = datum.Substring(1).TrimStart(' ');
                if (clist == null)
                {
                    if (spaces >= pspaces && secwaiting != null)
                    {
                        clist = new List<FDSData>();
                        csection.SetRootData(FDSUtility.UnEscapeKey(secwaiting), new FDSData() { PrecedingComments = new List<string>(seccomments), Internal = clist });
                        seccomments.Clear();
                        secwaiting = null;
                    }
                    else
                    {
                        Exception(i, line, "Line purpose unknown, attempted list entry when not building a list");
                    }
                }
                string unescaped = FDSUtility.UnEscape(clistline);
                clist.Add(new FDSData()
                {
                    PrecedingComments = new List<string>(ccomments),
                    Internal = datum[0] == '=' ? FDSUtility.FromBase64(unescaped) : FDSUtility.InterpretType(unescaped)
                });
                ccomments.Clear();
                continue;
            }
            clist = null;
            string startofline = "";
            string endofline = "";
            char type = '\0';
            for (int spot = 0; spot < datum.Length; spot++)
            {
                if (datum[spot] == ':' || datum[spot] == '=')
                {
                    type = datum[spot];
                    startofline = datum.Substring(0, spot);
                    endofline = spot == datum.Length - 1 ? "": datum.Substring(spot + 1);
                    break;
                }
            }
            endofline = endofline.TrimStart(' ');
            if (type == '\0')
            {
                Exception(i, line, "Line purpose unknown");
            }
            if (startofline.Length == 0)
            {
                Exception(i, line, "Empty key label!");
            }
            if (spaces > pspaces && secwaiting != null)
            {
                FDSSection sect = new FDSSection();
                csection.SetRootData(FDSUtility.UnEscapeKey(secwaiting), new FDSData()
                {
                    PrecedingComments = new List<string>(seccomments),
                    Internal = sect
                });
                seccomments.Clear();
                csection = sect;
                spacedsections[spaces] = sect;
                secwaiting = null;
            }
            if (type == '=')
            {
                csection.SetRootData(FDSUtility.UnEscapeKey(startofline), new FDSData()
                {
                    PrecedingComments = new List<string>(ccomments),
                    Internal = FDSUtility.FromBase64(FDSUtility.UnEscape(endofline))
                });
                ccomments.Clear();
            }
            else if (type == ':')
            {
                if (endofline.Length == 0)
                {
                    secwaiting = startofline;
                    seccomments = new List<string>(ccomments);
                    ccomments.Clear();
                }
                else
                {
                    csection.SetRootData(FDSUtility.UnEscapeKey(startofline), new FDSData()
                    {
                        PrecedingComments = new List<string>(ccomments),
                        Internal = FDSUtility.InterpretType(FDSUtility.UnEscape(endofline))
                    });
                    ccomments.Clear();
                }
            }
            else
            {
                Exception(i, line, "Internal issue: unrecognize 'type' value: " + type);
            }
            pspaces = spaces;
        }
        PostComments.AddRange(ccomments);
    }

    private void Exception(int linenumber, string line, string reason)
    {
        throw new Exception("[FDS Parsing error] Line " + (linenumber + 1) + ": " + reason + ", from line as follows: `" + line + "`");
    }

    /**
     * Constructs the FDS section from no data, preparing it for usage as a new section.
     */
    public FDSSection()
    {
        // Do nothing, we're init'd enough!
    }

    /**
     * The line number this section starts on.
     * Note that files start at 1.
     * Only accurate at file-load time.
     */
    public int StartingLine = 0;

    /**
     * All data contained by this section.
     */
    public Dictionary<string, FDSData> Data = new Dictionary<string, FDSData>();

    /**
     * Lowercase-stored data for this section.
     */
    public Dictionary<string, FDSData> DataLowered = new Dictionary<string, FDSData>();

    /**
     * Comments at the end of the section (usually only on the file root section).
     */
    public List<string> PostComments = new List<string>();

    /**
     * The section path splitter for this section.
     * Will initially hold a value obtained from "FDSUtility.DefaultSectionPathSplit" at instance construction time.
     * That field is initially a dot value. Altering that default or this value may cause issues (in particular with escaping) depending on the chosen value.
     */
    public char SectionPathSplit = FDSUtility.DefaultSectionPathSplit;

    /**
     * Returns the set of all keys at the root of this section.
     */
     * @return All keys.
    public IEnumerable<string> GetRootKeys()
    {
        return Data.Keys;
    }

    /**
     * Gets a string from the section. Can stringify non-string values.
     * Returns null if not found.
     */
     * @param key The key to get data from.
     * @return The data found, or the default.
    public List<string> GetStringList(string key)
    {
        List<FDSData> dat = GetDataList(key);
        if (dat == null)
        {
            return null;
        }
        List<string> newlist = new List<string>(dat.Count);
        for (int i = 0; i < dat.Count; i++)
        {
            newlist.Add(dat[i].Internal.ToString());
        }
        return newlist;
    }

    /**
     * Gets a string from the section. Can stringify non-string values.
     * Returns null if not found.
     */
     * @param key The key to get data from.
     * @return The data found, or the default.
    public List<FDSData> GetDataList(string key)
    {
        FDSData got = GetData(key);
        if (got == null)
        {
            return null;
        }
        object o = got.Internal;
        if (o is List<FDSData> asList)
        {
            return asList;
        }
        else
        {
            return new List<FDSData>() { got };
        }
    }

    /**
     * Gets a bool from the section.
     * Returns def if not found.
     */
     * @param key The key to get data from.
     * @param def The default object.
     * @return The data found, or the default.
    public bool? GetBool(string key, bool? def = null)
    {
        FDSData got = GetData(key);
        if (got == null)
        {
            return def;
        }
        object o = got.Internal;
        if (o is bool asBool)
        {
            return asBool;
        }
        else
        {
            return o.ToString().ToLowerFast() == "true";
        }
    }

    /**
     * Gets a string from the section. Can stringify non-string values.
     * Returns def if not found.
     */
     * @param key The key to get data from.
     * @param def The default object.
     * @return The data found, or the default.
    public string GetString(string key, string def = null)
    {
        FDSData got = GetData(key);
        if (got == null)
        {
            return def;
        }
        object o = got.Internal;
        if (o is string str)
        {
            return str;
        }
        else
        {
            return o.ToString();
        }
    }

    /**
     * Gets an optional float from the section.
     * Returns def if not found.
     */
     * @param key The key to get data from.
     * @param def The default object.
     * @return The data found, or the default.
    public float? GetFloat(string key, float? def = null)
    {
        double? asDouble = GetDouble(key, def);
        if (asDouble != null)
        {
            return (float)asDouble;
        }
        return null;
    }

    /**
     * Gets an optional double from the section.
     * Returns def if not found.
     */
     * @param key The key to get data from.
     * @param def The default object.
     * @return The data found, or the default.
    public double? GetDouble(string key, double? def = null)
    {
        FDSData got = GetData(key);
        if (got == null)
        {
            return def;
        }
        object o = got.Internal;
        if (o is double asDouble)
        {
            return asDouble;
        }
        else if (o is float asFloat)
        {
            return asFloat;
        }
        if (o is long asLong)
        {
            return (double)asLong;
        }
        else if (o is int asInt)
        {
            return (double)asInt;
        }
        else
        {
            if (double.TryParse(o.ToString(), out double d))
            {
                return d;
            }
            return def;
        }
    }

    /**
     * Gets an optional int from the section.
     * Returns def if not found.
     */
     * @param key The key to get data from.
     * @param def The default object.
     * @return The data found, or the default.
    public int? GetInt(string key, int? def = null)
    {
        long? asLong = GetLong(key, def);
        if (asLong != null)
        {
            return (int)asLong;
        }
        return null;
    }

    /**
     * Gets an optional long from the section.
     * Returns def if not found.
     */
     * @param key The key to get data from.
     * @param def The default object.
     * @return The data found, or the default.
    public long? GetLong(string key, long? def = null)
    {
        FDSData got = GetData(key);
        if (got == null)
        {
            return def;
        }
        object o = got.Internal;
        if (o is long asLong)
        {
            return asLong;
        }
        else if (o is int asInt)
        {
            return asInt;
        }
        else
        {
            if (long.TryParse(o.ToString(), out long l))
            {
                return l;
            }
            return def;
        }
    }

    /**
     * Gets an optional ulong from the section.
     * Returns def if not found.
     */
     * @param key The key to get data from.
     * @param def The default object.
     * @return The data found, or the default.
    public ulong? GetUlong(string key, ulong? def = null)
    {
        FDSData got = GetData(key);
        if (got == null)
        {
            return def;
        }
        object o = got.Internal;
        if (o is ulong oul)
        {
            return oul;
        }
        else if (o is uint oui)
        {
            return oui;
        }
        else
        {
            if (ulong.TryParse(o.ToString(), out ulong ul))
            {
                return ul;
            }
            return def;
        }
    }

    /**
     * Gets an object from the section.
     * Returns def if not found.
     */
     * @param key The key to get data from.
     * @param def The default object.
     * @return The data found, or the default.
    public object GetObject(string key, object def = null)
    {
        FDSData got = GetData(key);
        if (got == null)
        {
            return def;
        }
        return got.Internal;
    }

    /**
     * Sets data to the section.
     * May throw an FDSInputException if Set failed!
     */
     * @param key The key to set data from.
     * @param input The key to set data to.
    public void Set(string key, object input)
    {
        SetData(key, new FDSData() { Internal = input, PrecedingComments = new List<string>() });
    }

    /**
     * Sets data to the section.
     * May throw an FDSInputException if SetData failed!
     */
     * @param key The key to set data from.
     * @param data The key to set data to.
    public void SetData(string key, FDSData data)
    {
        int lind = key.LastIndexOf(SectionPathSplit);
        if (lind < 0)
        {
            SetRootData(key, data);
            return;
        }
        if (lind == key.Length - 1)
        {
            throw new FDSInputException("Invalid SetData key: Ends in a path splitter!");
        }

        FDSSection sec = GetSectionInternal(key.Substring(0, lind), false, false);
        sec.SetRootData(key.Substring(lind + 1), data);
    }

    /**
     * Defaults data to the section (IE, sets it if not present!)
     */
     * @param key The key to set data from.
     * @param input The key to set data to.
    public void Default(string key, object input)
    {
        DefaultData(key, new FDSData() { Internal = input, PrecedingComments = new List<string>() });
    }

    /**
     * Defaults data to the section (IE, sets it if not present!)
     */
     * @param key The key to set data from.
     * @param data The key to set data to.
    public void DefaultData(string key, FDSData data)
    {
        int lind = key.LastIndexOf(SectionPathSplit);
        if (lind < 0)
        {
            if (GetRootData(key) == null)
            {
                SetRootData(key, data);
            }
            return;
        }
        if (lind == key.Length - 1)
        {
            throw new FDSInputException("Invalid SetData key: Ends in a path splitter!");
        }

        FDSSection sec = GetSectionInternal(key.Substring(0, lind), false, false);
        string k = key.Substring(lind + 1);
        if (sec.GetRootData(k) == null)
        {
            sec.SetRootData(k, data);
        }
    }

    /**
     * Checks if a key exists in the FDS section.
     */
     * @param key The key to check for.
     * @return Whether the key is present.
    public bool HasKey(string key)
    {
        return GetData(key) != null;
    }

    /**
     * Gets data from the section.
     * Returns null if not found.
     */
     * @param key The key to get data from.
     * @return The data found, or null.
    public FDSData GetData(string key)
    {
        int lind = key.LastIndexOf(SectionPathSplit);
        if (lind < 0)
        {
            return GetRootData(key);
        }
        if (lind == key.Length - 1)
        {
            return null;
        }
        FDSSection sec = GetSection(key.Substring(0, lind));
        if (sec == null)
        {
            return null;
        }
        return sec.GetRootData(key.Substring(lind + 1));
    }

    /**
     * Gets data from the section.
     * Returns null if not found.
     */
     * @param key The key to get data from.
     * @return The data found, or null.
    public FDSData GetDataLowered(string key)
    {
        key = key.ToLowerFast();
        int lind = key.LastIndexOf(SectionPathSplit);
        if (lind < 0)
        {
            return GetRootDataLowered(key);
        }
        if (lind == key.Length - 1)
        {
            return null;
        }
        FDSSection sec = GetSectionInternal(key.Substring(0, lind), true, true);
        if (sec == null)
        {
            return null;
        }
        return sec.GetRootDataLowered(key.Substring(lind + 1));
    }

    /**
     * Gets a sub-section of this FDS section.
     * Returns null if not found.
     */
     * @param key The key of the section.
     * @return The subsection.
    public FDSSection GetSection(string key)
    {
        return GetSectionInternal(key, true, false);
    }

    /**
     * Gets a sub-section of this FDS section.
     * Returns null if not found.
     */
     * @param key The key of the section.
     * @return The subsection.
    public FDSSection GetSectionLowered(string key)
    {
        return GetSectionInternal(key.ToLowerFast(), true, true);
    }

    /**
     * Gets a sub-section of this FDS section.
     */
     * @param key The key of the section.
     * @param allowNull Whether to allow null returns, otherwise enforce the section's existence. If true, can throw an FDSInputException!
     * @param lowered Whether to read lowercase section names. If set, expects lowercased input key!
     * @return The subsection.
    private FDSSection GetSectionInternal(string key, bool allowNull, bool lowered)
    {
        if (key == null || key.Length == 0)
        {
            return this;
        }
        string[] dat = key.SplitFast(SectionPathSplit);
        FDSSection current = this;
        for (int i = 0; i < dat.Length; i++)
        {
            FDSData fdat = lowered ? current.GetRootDataLowered(dat[i]) : current.GetRootData(dat[i]);
            if (fdat != null && fdat.Internal is FDSSection asSection)
            {
                current = asSection;
            }
            else
            {
                if (allowNull)
                {
                    return null;
                }
                if (fdat != null)
                {
                    throw new FDSInputException("Key contains non-section contents!");
                }
                FDSSection temp = new FDSSection();
                current.SetRootData(dat[i], new FDSData() { Internal = temp, PrecedingComments = new List<string>() });
                current = temp;
            }
        }
        return current;
    }

    /**
     * Sets data direct on the root level.
     */
     * @param key The key to set data to.
     * @param data The data to read.
    public void SetRootData(string key, FDSData data)
    {
        Data[key] = data;
        DataLowered[key.ToLowerFast()] = data;
    }

    /**
     * Gets data direct from the root level.
     * Returns null if not found.
     */
     * @param key The key to get data from.
     * @return The data found, or null.
    public FDSData GetRootData(string key)
    {
        if (Data.TryGetValue(key, out FDSData temp))
        {
            return temp;
        }
        return null;
    }

    /**
     * Gets data direct from the root level.
     * Returns null if not found.
     * Assumes input is already lowercase!
     */
     * @param key The key to get data from.
     * @return The data found, or null.
    public FDSData GetRootDataLowered(string key)
    {
        if (DataLowered.TryGetValue(key, out FDSData temp))
        {
            return temp;
        }
        return null;
    }

    /**
     * Converts this FDSSection to a textual representation of itself.
     */
     * @param tabulation How many tabs to start with. Generally do not set this.
     * @param newline What string to use as a new line. Generally do not set this.
     * @return The string.
    public string SaveToString(int tabulation = 0, string newline = null)
    {
        if (newline == null)
        {
            newline = "\n";
        }
        string tabs = new string('\t', tabulation);
        StringBuilder outputBuilder = new StringBuilder(Data.Count * 100);
        foreach (KeyValuePair<string, FDSData> entry in Data)
        {
            FDSData dat = entry.Value;
            foreach (string str in dat.PrecedingComments)
            {
                outputBuilder.Append(tabs).Append("#").Append(str).Append(newline);
            }
            outputBuilder.Append(tabs).Append(FDSUtility.EscapeKey(entry.Key));
            if (dat.Internal is FDSSection asSection)
            {
                outputBuilder.Append(":").Append(newline).Append(asSection.SaveToString(tabulation + 1, newline));
            }
            else if (dat.Internal is byte[])
            {
                outputBuilder.Append("= ").Append(FDSUtility.Escape(dat.Outputable())).Append(newline);
            }
            else if (dat.Internal is List<FDSData> datums)
            {
                outputBuilder.Append(":").Append(newline);
                foreach (FDSData cdat in datums)
                {
                    foreach (string com in cdat.PrecedingComments)
                    {
                        outputBuilder.Append(tabs).Append("#").Append(com).Append(newline);
                    }
                    outputBuilder.Append(tabs);
                    if (cdat.Internal is byte[])
                    {
                        outputBuilder.Append("= ");
                    }
                    else
                    {
                        outputBuilder.Append("- ");
                    }
                    outputBuilder.Append(FDSUtility.Escape(cdat.Outputable())).Append(newline);
                }
            }
            else
            {
                outputBuilder.Append(": ").Append(FDSUtility.Escape(dat.Outputable())).Append(newline);
            }
        }
        foreach (string str in PostComments)
        {
            outputBuilder.Append(tabs).Append("#").Append(str).Append(newline);
        }
        return outputBuilder.ToString();
    }
}
