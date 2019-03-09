//
// This file is part of Frenetic Utilities, created by Frenetic LLC.
// This code is Copyright (C) Frenetic LLC under the terms of the MIT license.
// See README.md or LICENSE.txt in the FreneticUtilities source root for the contents of the license.
//

package com.freneticllc.freneticutilities.freneticdatasyntax;

/**
 * Represents a FreneticDataSyntax section or file.
 */
public class FDSSection {

    /**
     * Constructs the FDS Section from textual data.
     */
     * @param contents The contents of the data file.
    public FDSSection(String contents) {
        StartingLine = 1;
        contents = FDSUtility.CleanFileData(contents);
        HashMap<int, FDSSection> spacedsections = new HashMap<int, FDSSection>() { { 0, this } };
        ArrayList<String> ccomments = new ArrayList<String>();
        ArrayList<String> seccomments = new ArrayList<String>();
        FDSSection csection = this;
        String[] data = contents.SplitFast('\n');
        int pspaces = 0;
        String secwaiting = null;
        ArrayList<FDSData> clist = null;
        for (int i = 0; i < data.Length; i++) {
            String line = data[i];
            int spaces;
            for (spaces = 0; spaces < line.Length; spaces++) {
                if (line[spaces] != ' ') {
                    break;
                }
            }
            if (spaces == line.Length) {
                continue;
            }
            String datum = line.SubString(spaces).TrimEnd(' ');
            if (datum.StartsWith("#")) {
                ccomments.Add(datum.SubString(1));
                continue;
            }
            if (spaces < pspaces) {
                if (spacedsections.TryGetValue(spaces, out FDSSection temp)) {
                    csection = temp;
                    foreach (int test in new ArrayList<int>(spacedsections.Keys)) {
                        if (test > spaces) {
                            spacedsections.Remove(test);
                        }
                    }
                }
                else {
                    Exception(i, line, "Spaced incorrectly. Spacing length is less than previous spacing length,"
                        + "but does not match the spacing value of any known section, valid: "
                        + String.Join(" / ", spacedsections.Keys) + ", found: " + spaces + ", was: " + pspaces);
                }
            }
            if (datum[0] == '-' || datum[0] == '=') {
                String clistline = datum.SubString(1).TrimStart(' ');
                if (clist == null) {
                    if (spaces >= pspaces && secwaiting != null) {
                        clist = new ArrayList<FDSData>();
                        csection.SetRootData(FDSUtility.UnEscapeKey(secwaiting), new FDSData() { PrecedingComments = new ArrayList<String>(seccomments), Internal = clist });
                        seccomments.Clear();
                        secwaiting = null;
                    }
                    else {
                        Exception(i, line, "Line purpose unknown, attempted list entry when not building a list");
                    }
                }
                String unescaped = FDSUtility.UnEscape(clistline);
                clist.Add(new FDSData() {
                    PrecedingComments = new ArrayList<String>(ccomments),
                    Internal = datum[0] == '=' ? FDSUtility.FromBase64(unescaped) : FDSUtility.InterpretType(unescaped)
                });
                ccomments.Clear();
                continue;
            }
            clist = null;
            String startofline = "";
            String endofline = "";
            char type = '\0';
            for (int spot = 0; spot < datum.Length; spot++) {
                if (datum[spot] == ':' || datum[spot] == '=') {
                    type = datum[spot];
                    startofline = datum.SubString(0, spot);
                    endofline = spot == datum.Length - 1 ? "": datum.SubString(spot + 1);
                    break;
                }
            }
            endofline = endofline.TrimStart(' ');
            if (type == '\0') {
                Exception(i, line, "Line purpose unknown");
            }
            if (startofline.Length == 0) {
                Exception(i, line, "Empty key label!");
            }
            if (spaces > pspaces && secwaiting != null) {
                FDSSection sect = new FDSSection();
                csection.SetRootData(FDSUtility.UnEscapeKey(secwaiting), new FDSData() {
                    PrecedingComments = new ArrayList<String>(seccomments),
                    Internal = sect
                });
                seccomments.Clear();
                csection = sect;
                spacedsections[spaces] = sect;
                secwaiting = null;
            }
            if (type == '=') {
                csection.SetRootData(FDSUtility.UnEscapeKey(startofline), new FDSData() {
                    PrecedingComments = new ArrayList<String>(ccomments),
                    Internal = FDSUtility.FromBase64(FDSUtility.UnEscape(endofline))
                });
                ccomments.Clear();
            }
            else if (type == ':') {
                if (endofline.Length == 0) {
                    secwaiting = startofline;
                    seccomments = new ArrayList<String>(ccomments);
                    ccomments.Clear();
                }
                else {
                    csection.SetRootData(FDSUtility.UnEscapeKey(startofline), new FDSData() {
                        PrecedingComments = new ArrayList<String>(ccomments),
                        Internal = FDSUtility.InterpretType(FDSUtility.UnEscape(endofline))
                    });
                    ccomments.Clear();
                }
            }
            else {
                Exception(i, line, "Internal issue: unrecognize 'type' value: " + type);
            }
            pspaces = spaces;
        }
        PostComments.AddRange(ccomments);
    }

    private void Exception(int linenumber, String line, String reason) {
        throw new Exception("[FDS Parsing error] Line " + (linenumber + 1) + ": " + reason + ", from line as follows: `" + line + "`");
    }

    /**
     * Constructs the FDS section from no data, preparing it for usage as a new section.
     */
    public FDSSection() {
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
    public HashMap<String, FDSData> Data = new HashMap<String, FDSData>();

    /**
     * Lowercase-stored data for this section.
     */
    public HashMap<String, FDSData> DataLowered = new HashMap<String, FDSData>();

    /**
     * Comments at the end of the section (usually only on the file root section).
     */
    public ArrayList<String> PostComments = new ArrayList<String>();

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
    public Set<String> GetRootKeys() {
        return Data.Keys;
    }

    /**
     * Gets a String from the section. Can Stringify non-String values.
     * Returns null if not found.
     */
     * @param key The key to get data from.
     * @return The data found, or the default.
    public ArrayList<String> GetStringList(String key) {
        ArrayList<FDSData> dat = GetDataList(key);
        if (dat == null) {
            return null;
        }
        ArrayList<String> newlist = new ArrayList<String>(dat.Count);
        for (int i = 0; i < dat.Count; i++) {
            newlist.Add(dat[i].Internal.ToString());
        }
        return newlist;
    }

    /**
     * Gets a String from the section. Can Stringify non-String values.
     * Returns null if not found.
     */
     * @param key The key to get data from.
     * @return The data found, or the default.
    public ArrayList<FDSData> GetDataList(String key) {
        FDSData got = GetData(key);
        if (got == null) {
            return null;
        }
        object o = got.Internal;
        if (o is ArrayList<FDSData> asList) {
            return asList;
        }
        else {
            return new ArrayList<FDSData>() { got };
        }
    }

    /**
     * Gets a bool from the section.
     * Returns def if not found.
     */
     * @param key The key to get data from.
     * @param def The default object.
     * @return The data found, or the default.
    public bool? GetBool(String key, bool? def = null) {
        FDSData got = GetData(key);
        if (got == null) {
            return def;
        }
        object o = got.Internal;
        if (o is bool asBool) {
            return asBool;
        }
        else {
            return o.ToString().ToLowerFast() == "true";
        }
    }

    /**
     * Gets a String from the section. Can Stringify non-String values.
     * Returns def if not found.
     */
     * @param key The key to get data from.
     * @param def The default object.
     * @return The data found, or the default.
    public String GetString(String key, String def = null) {
        FDSData got = GetData(key);
        if (got == null) {
            return def;
        }
        object o = got.Internal;
        if (o is String str) {
            return str;
        }
        else {
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
    public float? GetFloat(String key, float? def = null) {
        double? asDouble = GetDouble(key, def);
        if (asDouble != null) {
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
    public double? GetDouble(String key, double? def = null) {
        FDSData got = GetData(key);
        if (got == null) {
            return def;
        }
        object o = got.Internal;
        if (o is double asDouble) {
            return asDouble;
        }
        else if (o is float asFloat) {
            return asFloat;
        }
        if (o is long asLong) {
            return (double)asLong;
        }
        else if (o is int asInt) {
            return (double)asInt;
        }
        else {
            if (double.TryParse(o.ToString(), out double d)) {
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
    public int? GetInt(String key, int? def = null) {
        long? asLong = GetLong(key, def);
        if (asLong != null) {
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
    public long? GetLong(String key, long? def = null) {
        FDSData got = GetData(key);
        if (got == null) {
            return def;
        }
        object o = got.Internal;
        if (o is long asLong) {
            return asLong;
        }
        else if (o is int asInt) {
            return asInt;
        }
        else {
            if (long.TryParse(o.ToString(), out long l)) {
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
    public ulong? GetUlong(String key, ulong? def = null) {
        FDSData got = GetData(key);
        if (got == null) {
            return def;
        }
        object o = got.Internal;
        if (o is ulong oul) {
            return oul;
        }
        else if (o is uint oui) {
            return oui;
        }
        else {
            if (ulong.TryParse(o.ToString(), out ulong ul)) {
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
    public object GetObject(String key, object def = null) {
        FDSData got = GetData(key);
        if (got == null) {
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
    public void Set(String key, object input) {
        SetData(key, new FDSData() { Internal = input, PrecedingComments = new ArrayList<String>() });
    }

    /**
     * Sets data to the section.
     * May throw an FDSInputException if SetData failed!
     */
     * @param key The key to set data from.
     * @param data The key to set data to.
    public void SetData(String key, FDSData data) {
        int lind = key.LastIndexOf(SectionPathSplit);
        if (lind < 0) {
            SetRootData(key, data);
            return;
        }
        if (lind == key.Length - 1) {
            throw new FDSInputException("Invalid SetData key: Ends in a path splitter!");
        }

        FDSSection sec = GetSectionInternal(key.SubString(0, lind), false, false);
        sec.SetRootData(key.SubString(lind + 1), data);
    }

    /**
     * Defaults data to the section (IE, sets it if not present!)
     */
     * @param key The key to set data from.
     * @param input The key to set data to.
    public void Default(String key, object input) {
        DefaultData(key, new FDSData() { Internal = input, PrecedingComments = new ArrayList<String>() });
    }

    /**
     * Defaults data to the section (IE, sets it if not present!)
     */
     * @param key The key to set data from.
     * @param data The key to set data to.
    public void DefaultData(String key, FDSData data) {
        int lind = key.LastIndexOf(SectionPathSplit);
        if (lind < 0) {
            if (GetRootData(key) == null) {
                SetRootData(key, data);
            }
            return;
        }
        if (lind == key.Length - 1) {
            throw new FDSInputException("Invalid SetData key: Ends in a path splitter!");
        }

        FDSSection sec = GetSectionInternal(key.SubString(0, lind), false, false);
        String k = key.SubString(lind + 1);
        if (sec.GetRootData(k) == null) {
            sec.SetRootData(k, data);
        }
    }

    /**
     * Checks if a key exists in the FDS section.
     */
     * @param key The key to check for.
     * @return Whether the key is present.
    public bool HasKey(String key) {
        return GetData(key) != null;
    }

    /**
     * Gets data from the section.
     * Returns null if not found.
     */
     * @param key The key to get data from.
     * @return The data found, or null.
    public FDSData GetData(String key) {
        int lind = key.LastIndexOf(SectionPathSplit);
        if (lind < 0) {
            return GetRootData(key);
        }
        if (lind == key.Length - 1) {
            return null;
        }
        FDSSection sec = GetSection(key.SubString(0, lind));
        if (sec == null) {
            return null;
        }
        return sec.GetRootData(key.SubString(lind + 1));
    }

    /**
     * Gets data from the section.
     * Returns null if not found.
     */
     * @param key The key to get data from.
     * @return The data found, or null.
    public FDSData GetDataLowered(String key) {
        key = key.ToLowerFast();
        int lind = key.LastIndexOf(SectionPathSplit);
        if (lind < 0) {
            return GetRootDataLowered(key);
        }
        if (lind == key.Length - 1) {
            return null;
        }
        FDSSection sec = GetSectionInternal(key.SubString(0, lind), true, true);
        if (sec == null) {
            return null;
        }
        return sec.GetRootDataLowered(key.SubString(lind + 1));
    }

    /**
     * Gets a sub-section of this FDS section.
     * Returns null if not found.
     */
     * @param key The key of the section.
     * @return The subsection.
    public FDSSection GetSection(String key) {
        return GetSectionInternal(key, true, false);
    }

    /**
     * Gets a sub-section of this FDS section.
     * Returns null if not found.
     */
     * @param key The key of the section.
     * @return The subsection.
    public FDSSection GetSectionLowered(String key) {
        return GetSectionInternal(key.ToLowerFast(), true, true);
    }

    /**
     * Gets a sub-section of this FDS section.
     */
     * @param key The key of the section.
     * @param allowNull Whether to allow null returns, otherwise enforce the section's existence. If true, can throw an FDSInputException!
     * @param lowered Whether to read lowercase section names. If set, expects lowercased input key!
     * @return The subsection.
    private FDSSection GetSectionInternal(String key, bool allowNull, bool lowered) {
        if (key == null || key.Length == 0) {
            return this;
        }
        String[] dat = key.SplitFast(SectionPathSplit);
        FDSSection current = this;
        for (int i = 0; i < dat.Length; i++) {
            FDSData fdat = lowered ? current.GetRootDataLowered(dat[i]) : current.GetRootData(dat[i]);
            if (fdat != null && fdat.Internal is FDSSection asSection) {
                current = asSection;
            }
            else {
                if (allowNull) {
                    return null;
                }
                if (fdat != null) {
                    throw new FDSInputException("Key contains non-section contents!");
                }
                FDSSection temp = new FDSSection();
                current.SetRootData(dat[i], new FDSData() { Internal = temp, PrecedingComments = new ArrayList<String>() });
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
    public void SetRootData(String key, FDSData data) {
        Data[key] = data;
        DataLowered[key.ToLowerFast()] = data;
    }

    /**
     * Gets data direct from the root level.
     * Returns null if not found.
     */
     * @param key The key to get data from.
     * @return The data found, or null.
    public FDSData GetRootData(String key) {
        if (Data.TryGetValue(key, out FDSData temp)) {
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
    public FDSData GetRootDataLowered(String key) {
        if (DataLowered.TryGetValue(key, out FDSData temp)) {
            return temp;
        }
        return null;
    }

    /**
     * Converts this FDSSection to a textual representation of itself.
     */
     * @param tabulation How many tabs to start with. Generally do not set this.
     * @param newline What String to use as a new line. Generally do not set this.
     * @return The String.
    public String SaveToString(int tabulation = 0, String newline = null) {
        if (newline == null) {
            newline = "\n";
        }
        String tabs = new String('\t', tabulation);
        StringBuilder outputBuilder = new StringBuilder(Data.Count * 100);
        foreach (KeyValuePair<String, FDSData> entry in Data) {
            FDSData dat = entry.Value;
            foreach (String str in dat.PrecedingComments) {
                outputBuilder.Append(tabs).Append("#").Append(str).Append(newline);
            }
            outputBuilder.Append(tabs).Append(FDSUtility.EscapeKey(entry.Key));
            if (dat.Internal is FDSSection asSection) {
                outputBuilder.Append(":").Append(newline).Append(asSection.SaveToString(tabulation + 1, newline));
            }
            else if (dat.Internal is byte[]) {
                outputBuilder.Append("= ").Append(FDSUtility.Escape(dat.Outputable())).Append(newline);
            }
            else if (dat.Internal is ArrayList<FDSData> datums) {
                outputBuilder.Append(":").Append(newline);
                foreach (FDSData cdat in datums) {
                    foreach (String com in cdat.PrecedingComments) {
                        outputBuilder.Append(tabs).Append("#").Append(com).Append(newline);
                    }
                    outputBuilder.Append(tabs);
                    if (cdat.Internal is byte[]) {
                        outputBuilder.Append("= ");
                    }
                    else {
                        outputBuilder.Append("- ");
                    }
                    outputBuilder.Append(FDSUtility.Escape(cdat.Outputable())).Append(newline);
                }
            }
            else {
                outputBuilder.Append(": ").Append(FDSUtility.Escape(dat.Outputable())).Append(newline);
            }
        }
        foreach (String str in PostComments) {
            outputBuilder.Append(tabs).Append("#").Append(str).Append(newline);
        }
        return outputBuilder.ToString();
    }
}
