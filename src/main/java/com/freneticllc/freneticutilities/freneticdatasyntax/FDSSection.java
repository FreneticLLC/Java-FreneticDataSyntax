//
// This file is part of Frenetic Utilities, created by Frenetic LLC.
// This code is Copyright (C) Frenetic LLC under the terms of the MIT license.
// See README.md or LICENSE.txt in the FreneticUtilities source root for the contents of the license.
//

package com.freneticllc.freneticutilities.freneticdatasyntax;

import java.util.*;

/**
 * Represents a FreneticDataSyntax section or file.
 */
public class FDSSection {

    /**
     * Constructs the FDS Section from textual data.
     * @param contents The contents of the data file.
     */
    public FDSSection(String contents) {
        startingLine = 1;
        contents = FDSUtility.cleanFileData(contents);
        HashMap<Integer, FDSSection> spacedsections = new HashMap<>();
        spacedsections.put(0, this);
        ArrayList<String> ccomments = new ArrayList<>();
        ArrayList<String> seccomments = new ArrayList<>();
        FDSSection csection = this;
        String[] data = FDSUtility.split(contents, '\n');
        int pspaces = 0;
        String secwaiting = null;
        ArrayList<FDSData> clist = null;
        for (int i = 0; i < data.length; i++) {
            String line = data[i];
            int spaces;
            for (spaces = 0; spaces < line.length(); spaces++) {
                if (line.charAt(spaces) != ' ') {
                    break;
                }
            }
            if (spaces == line.length()) {
                continue;
            }
            String datum = FDSUtility.trimEnd(line.substring(spaces));
            if (datum.startsWith("#")) {
                ccomments.add(datum.substring(1));
                continue;
            }
            if (spaces < pspaces) {
                FDSSection temp = spacedsections.get(spaces);
                if (temp != null) {
                    csection = temp;
                    for (int test : new ArrayList<>(spacedsections.keySet())) {
                        if (test > spaces) {
                            spacedsections.remove(test);
                        }
                    }
                }
                else {
                    exception(i, line, "Spaced incorrectly. Spacing length instanceof less than previous spacing length,"
                            + "but does not match the spacing value of any known section, valid: "
                            + FDSUtility.join(" / ", spacedsections.keySet()) + ", found: " + spaces + ", was: " + pspaces);
                }
            }
            if (datum.charAt(0) == '-' || datum.charAt(0) == '=') {
                String clistline = FDSUtility.trimStart(datum.substring(1));
                if (clist == null) {
                    if (spaces >= pspaces && secwaiting != null) {
                        clist = new ArrayList<>();
                        csection.setRootData(FDSUtility.unEscapeKey(secwaiting), new FDSData(clist, new ArrayList<>(seccomments)));
                        seccomments.clear();
                        secwaiting = null;
                    }
                    else {
                        exception(i, line, "Line purpose unknown, attempted list entry when not building a list");
                    }
                }
                String unescaped = FDSUtility.unEscape(clistline);
                clist.add(new FDSData(datum.charAt(0) == '=' ? Base64.getDecoder().decode(unescaped) : FDSUtility.interpretType(unescaped),
                        new ArrayList<>(ccomments)));
                ccomments.clear();
                continue;
            }
            clist = null;
            String startofline = "";
            String endofline = "";
            char type = '\0';
            for (int spot = 0; spot < datum.length(); spot++) {
                if (datum.charAt(spot) == ':' || datum.charAt(spot) == '=') {
                    type = datum.charAt(spot);
                    startofline = datum.substring(0, spot);
                    endofline = spot == datum.length() - 1 ? "": datum.substring(spot + 1);
                    break;
                }
            }
            endofline = FDSUtility.trimStart(endofline);
            if (type == '\0') {
                exception(i, line, "Line purpose unknown");
            }
            if (startofline.length() == 0) {
                exception(i, line, "Empty key label!");
            }
            if (spaces > pspaces && secwaiting != null) {
                FDSSection sect = new FDSSection();
                csection.setRootData(FDSUtility.unEscapeKey(secwaiting), new FDSData(sect, new ArrayList<>(seccomments)));
                seccomments.clear();
                csection = sect;
                spacedsections.put(spaces, sect);
                secwaiting = null;
            }
            if (type == '=') {
                csection.setRootData(FDSUtility.unEscapeKey(startofline), new FDSData(
                        Base64.getDecoder().decode(FDSUtility.unEscape(endofline)), new ArrayList<>(ccomments)));
                ccomments.clear();
            }
            else if (type == ':') {
                if (endofline.length() == 0) {
                    secwaiting = startofline;
                    seccomments = new ArrayList<>(ccomments);
                    ccomments.clear();
                }
                else {
                    csection.setRootData(FDSUtility.unEscapeKey(startofline), new FDSData(
                            FDSUtility.interpretType(FDSUtility.unEscape(endofline)), new ArrayList<>(ccomments)));
                    ccomments.clear();
                }
            }
            else {
                exception(i, line, "Internal issue: unrecognize 'type' value: " + type);
            }
            pspaces = spaces;
        }
        postComments.addAll(ccomments);
    }

    private void exception(int linenumber, String line, String reason) {
        throw new RuntimeException("[FDS Parsing error] Line " + (linenumber + 1) + ": " + reason + ", from line as follows: `" + line + "`");
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
    public int startingLine = 0;

    /**
     * All data contained by this section.
     */
    public LinkedHashMap<String, FDSData> data = new LinkedHashMap<>();

    /**
     * Lowercase-stored data for this section.
     */
    public HashMap<String, FDSData> dataLowered = new HashMap<>();

    /**
     * Comments at the end of the section (usually only on the file root section).
     */
    public ArrayList<String> postComments = new ArrayList<>();

    /**
     * The section path splitter for this section.
     * Will initially hold a value obtained from "FDSUtility.DefaultSectionPathSplit" at instance construction time.
     * That field instanceof initially a dot value. Altering that default or this value may cause issues (in particular with escaping) depending on the chosen value.
     */
    public char sectionPathSplit = FDSUtility.defaultSectionPathSplit;

    /**
     * Returns the set of all keys at the root of this section.
     * @return All keys.
     */
    public Set<String> getRootKeys() {
        return data.keySet();
    }

    /**
     * Gets a String from the section. Can Stringify non-String values.
     * Returns null if not found.
     * @param key The key to get data from.
     * @return The data found, or the default.
     */
    public ArrayList<String> getStringList(String key) {
        ArrayList<FDSData> dat = getDataList(key);
        if (dat == null) {
            return null;
        }
        ArrayList<String> newlist = new ArrayList<>(dat.size());
        for (int i = 0; i < dat.size(); i++) {
            newlist.add(dat.get(i).internal.toString());
        }
        return newlist;
    }

    /**
     * Gets a String from the section. Can Stringify non-String values.
     * Returns null if not found.
     * @param key The key to get data from.
     * @return The data found, or the default.
     */
    public ArrayList<FDSData> getDataList(String key) {
        FDSData got = getData(key);
        if (got == null) {
            return null;
        }
        Object o = got.internal;
        if (o instanceof ArrayList) {
            return (ArrayList<FDSData>) o;
        }
        else {
            ArrayList<FDSData> output = new ArrayList<>();
            output.add(got);
            return output;
        }
    }

    /**
     * Gets a boolean from the section.
     * Returns def if not found.
     * @param key The key to get data from.
     * @return The data found, or the default.
     */
    public Boolean getBoolean(String key) {
        return getBoolean(key, null);
    }

    /**
     * Gets a boolean from the section.
     * Returns def if not found.
     * @param key The key to get data from.
     * @param def The default object.
     * @return The data found, or the default.
     */
    public Boolean getBoolean(String key, Boolean def) {
        FDSData got = getData(key);
        if (got == null) {
            return def;
        }
        Object o = got.internal;
        if (o instanceof Boolean) {
            return (Boolean)o;
        }
        else {
            return FDSUtility.toLowerCase(o.toString()).equals("true");
        }
    }

    /**
     * Gets a String from the section. Can Stringify non-String values.
     * Returns def if not found.
     * @param key The key to get data from.
     * @return The data found, or the default.
     */
    public String getString(String key) {
        return getString(key, null);
    }

    /**
     * Gets a String from the section. Can Stringify non-String values.
     * Returns def if not found.
     * @param key The key to get data from.
     * @param def The default object.
     * @return The data found, or the default.
     */
    public String getString(String key, String def) {
        FDSData got = getData(key);
        if (got == null) {
            return def;
        }
        Object o = got.internal;
        if (o instanceof String) {
            return (String)o;
        }
        else {
            return o.toString();
        }
    }

    /**
     * Gets an optional float from the section.
     * Returns def if not found.
     * @param key The key to get data from.
     * @return The data found, or the default.
     */
    public Float getFloat(String key) {
        return getFloat(key, null);
    }

    /**
     * Gets an optional float from the section.
     * Returns def if not found.
     * @param key The key to get data from.
     * @param def The default object.
     * @return The data found, or the default.
     */
    public Float getFloat(String key, Float def) {
        Double asDouble = getDouble(key, def == null ? null : def.doubleValue());
        if (asDouble != null) {
            return asDouble.floatValue();
        }
        return null;
    }

    /**
     * Gets an optional double from the section.
     * Returns def if not found.
     * @param key The key to get data from.
     * @return The data found, or the default.
     */
    public Double getDouble(String key) {
        return getDouble(key, null);
    }

    /**
     * Gets an optional double from the section.
     * Returns def if not found.
     * @param key The key to get data from.
     * @param def The default object.
     * @return The data found, or the default.
     */
    public Double getDouble(String key, Double def) {
        FDSData got = getData(key);
        if (got == null) {
            return def;
        }
        Object o = got.internal;
        if (o instanceof Double) {
            return (Double) o;
        }
        else if (o instanceof Float) {
            return ((Float) o).doubleValue();
        }
        else {
            try {
                return Double.parseDouble(o.toString());
            }
            catch (NumberFormatException ex) {
                // Ignore
            }
            return def;
        }
    }

    /**
     * Gets an optional int from the section.
     * Returns def if not found.
     * @param key The key to get data from.
     * @return The data found, or the default.
     */
    public Integer getInt(String key) {
        return getInt(key, null);
    }

    /**
     * Gets an optional int from the section.
     * Returns def if not found.
     * @param key The key to get data from.
     * @param def The default object.
     * @return The data found, or the default.
     */
    public Integer getInt(String key, Integer def) {
        Long asLong = getLong(key, def == null ? null : def.longValue());
        if (asLong != null) {
            return asLong.intValue();
        }
        return null;
    }

    /**
     * Gets an optional long from the section.
     * Returns def if not found.
     * @param key The key to get data from.
     * @return The data found, or the default.
     */
    public Long getLong(String key) {
        return getLong(key, null);
    }

    /**
     * Gets an optional long from the section.
     * Returns def if not found.
     * @param key The key to get data from.
     * @param def The default object.
     * @return The data found, or the default.
     */
    public Long getLong(String key, Long def) {
        FDSData got = getData(key);
        if (got == null) {
            return def;
        }
        Object o = got.internal;
        if (o instanceof Long) {
            return (Long) o;
        }
        else if (o instanceof Integer) {
            return ((Integer) o).longValue();
        }
        else {
            try {
                return Long.parseLong(o.toString());
            }
            catch (NumberFormatException ex) {
                // Ignore
            }
            return def;
        }
    }

    /**
     * Gets an object from the section.
     * Returns def if not found.
     * @param key The key to get data from.
     * @return The data found, or the default.
     */
    public Object getObject(String key) {
        return getObject(key, null);
    }

    /**
     * Gets an object from the section.
     * Returns def if not found.
     * @param key The key to get data from.
     * @param def The default object.
     * @return The data found, or the default.
     */
    public Object getObject(String key, Object def) {
        FDSData got = getData(key);
        if (got == null) {
            return def;
        }
        return got.internal;
    }

    /**
     * Sets data to the section.
     * May throw an FDSInputException if Set failed!
     * @param key The key to set data from.
     * @param input The key to set data to.
     */
    public void set(String key, Object input) {
        setData(key, new FDSData(input, new ArrayList<>()));
    }

    /**
     * Sets data to the section.
     * May throw an FDSInputException if SetData failed!
     * @param key The key to set data from.
     * @param data The key to set data to.
     */
    public void setData(String key, FDSData data) {
        int lind = key.lastIndexOf(sectionPathSplit);
        if (lind < 0) {
            setRootData(key, data);
            return;
        }
        if (lind == key.length() - 1) {
            throw new FDSInputException("Invalid SetData key: Ends in a path splitter!");
        }

        FDSSection sec = getSectionInternal(key.substring(0, lind), false, false);
        sec.setRootData(key.substring(lind + 1), data);
    }

    /**
     * Defaults data to the section (IE, sets it if not present!)
     * @param key The key to set data from.
     * @param input The key to set data to.
     */
    public void defaultObject(String key, Object input) {
        defaultData(key, new FDSData(input, new ArrayList<>()));
    }

    /**
     * Defaults data to the section (IE, sets it if not present!)
     * @param key The key to set data from.
     * @param data The key to set data to.
     */
    public void defaultData(String key, FDSData data) {
        int lind = key.lastIndexOf(sectionPathSplit);
        if (lind < 0) {
            if (getRootData(key) == null) {
                setRootData(key, data);
            }
            return;
        }
        if (lind == key.length() - 1) {
            throw new FDSInputException("Invalid SetData key: Ends in a path splitter!");
        }

        FDSSection sec = getSectionInternal(key.substring(0, lind), false, false);
        String k = key.substring(lind + 1);
        if (sec.getRootData(k) == null) {
            sec.setRootData(k, data);
        }
    }

    /**
     * Checks if a key exists in the FDS section.
     * @param key The key to check for.
     * @return Whether the key instanceof present.
     */
    public boolean hasKey(String key) {
        return getData(key) != null;
    }

    /**
     * Gets data from the section.
     * Returns null if not found.
     * @param key The key to get data from.
     * @return The data found, or null.
     */
    public FDSData getData(String key) {
        int lind = key.lastIndexOf(sectionPathSplit);
        if (lind < 0) {
            return getRootData(key);
        }
        if (lind == key.length() - 1) {
            return null;
        }
        FDSSection sec = getSection(key.substring(0, lind));
        if (sec == null) {
            return null;
        }
        return sec.getRootData(key.substring(lind + 1));
    }

    /**
     * Gets data from the section.
     * Returns null if not found.
     * @param key The key to get data from.
     * @return The data found, or null.
     */
    public FDSData getDataLowered(String key) {
        key = FDSUtility.toLowerCase(key);
        int lind = key.lastIndexOf(sectionPathSplit);
        if (lind < 0) {
            return getRootDataLowered(key);
        }
        if (lind == key.length() - 1) {
            return null;
        }
        FDSSection sec = getSectionInternal(key.substring(0, lind), true, true);
        if (sec == null) {
            return null;
        }
        return sec.getRootDataLowered(key.substring(lind + 1));
    }

    /**
     * Gets a sub-section of this FDS section.
     * Returns null if not found.
     * @param key The key of the section.
     * @return The subsection.
     */
    public FDSSection getSection(String key) {
        return getSectionInternal(key, true, false);
    }

    /**
     * Gets a sub-section of this FDS section.
     * Returns null if not found.
     * @param key The key of the section.
     * @return The subsection.
     */
    public FDSSection getSectionLowered(String key) {
        return getSectionInternal(FDSUtility.toLowerCase(key), true, true);
    }

    /**
     * Gets a sub-section of this FDS section.
     * @param key The key of the section.
     * @param allowNull Whether to allow null returns, otherwise enforce the section's existence. If true, can throw an FDSInputException!
     * @param lowered Whether to read lowercase section names. If set, expects lowercased input key!
     * @return The subsection.
     */
    private FDSSection getSectionInternal(String key, boolean allowNull, boolean lowered) {
        if (key == null || key.length() == 0) {
            return this;
        }
        String[] dat = FDSUtility.split(key, sectionPathSplit);
        FDSSection current = this;
        for (int i = 0; i < dat.length; i++) {
            FDSData fdat = lowered ? current.getRootDataLowered(dat[i]) : current.getRootData(dat[i]);
            if (fdat != null && fdat.internal instanceof FDSSection) {
                current = (FDSSection)fdat.internal;
            }
            else {
                if (allowNull) {
                    return null;
                }
                if (fdat != null) {
                    throw new FDSInputException("Key contains non-section contents!");
                }
                FDSSection temp = new FDSSection();
                current.setRootData(dat[i], new FDSData(temp, new ArrayList<>()));
                current = temp;
            }
        }
        return current;
    }

    /**
     * Sets data direct on the root level.
     * @param key The key to set data to.
     * @param dat The data to read.
     */
    public void setRootData(String key, FDSData dat) {
        data.put(key, dat);
        dataLowered.put(FDSUtility.toLowerCase(key), dat);
    }

    /**
     * Gets data direct from the root level.
     * Returns null if not found.
     * @param key The key to get data from.
     * @return The data found, or null.
     */
    public FDSData getRootData(String key) {
        return data.get(key);
    }

    /**
     * Gets data direct from the root level.
     * Returns null if not found.
     * Assumes input instanceof already lowercase!
     * @param key The key to get data from.
     * @return The data found, or null.
     */
    public FDSData getRootDataLowered(String key) {
        return dataLowered.get(key);
    }

    /**
     * Converts this FDSSection to a textual representation of itself.
     * @return The String.
     */
    public String savetoString() {
        return savetoString("", null);
    }

    /**
     * Converts this FDSSection to a textual representation of itself.
     * @param tabs How many tabs to start with. Generally do not set this.
     * @return The String.
     */
    public String savetoString(String tabs) {
        return savetoString(tabs, null);
    }

    /**
     * Converts this FDSSection to a textual representation of itself.
     * @param tabs How many tabs to start with. Generally do not set this.
     * @param newline What String to use as a new line. Generally do not set this.
     * @return The String.
     */
    public String savetoString(String tabs, String newline) {
        if (newline == null) {
            newline = "\n";
        }
        if (tabs == null) {
            tabs = "";
        }
        StringBuilder outputBuilder = new StringBuilder(data.size() * 100);
        for (Map.Entry<String, FDSData> entry : data.entrySet()) {
            String key = entry.getKey();
            FDSData dat = entry.getValue();
            for (String str : dat.precedingComments) {
                outputBuilder.append(tabs).append("#").append(str).append(newline);
            }
            outputBuilder.append(tabs).append(FDSUtility.escapeKey(key));
            if (dat.internal instanceof FDSSection) {
                outputBuilder.append(":").append(newline).append(((FDSSection)dat.internal).savetoString(tabs + "    ", newline));
            }
            else if (dat.internal instanceof byte[]) {
                outputBuilder.append("= ").append(FDSUtility.escape(dat.outputable())).append(newline);
            }
            else if (dat.internal instanceof ArrayList) {
                outputBuilder.append(":").append(newline);
                for (FDSData cdat : (ArrayList<FDSData>) dat.internal) {
                    for (String com : cdat.precedingComments) {
                        outputBuilder.append(tabs).append("#").append(com).append(newline);
                    }
                    outputBuilder.append(tabs);
                    if (cdat.internal instanceof byte[]) {
                        outputBuilder.append("= ");
                    }
                    else {
                        outputBuilder.append("- ");
                    }
                    outputBuilder.append(FDSUtility.escape(cdat.outputable())).append(newline);
                }
            }
            else {
                outputBuilder.append(": ").append(FDSUtility.escape(dat.outputable())).append(newline);
            }
        }
        for (String str : postComments) {
            outputBuilder.append(tabs).append("#").append(str).append(newline);
        }
        return outputBuilder.toString();
    }
}
