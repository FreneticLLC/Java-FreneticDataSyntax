//
// This file is part of Frenetic Utilities, created by Frenetic LLC.
// This code is Copyright (C) Frenetic LLC under the terms of the MIT license.
// See README.md or LICENSE.txt in the FreneticUtilities source root for the contents of the license.
//

package com.freneticllc.freneticutilities.freneticdatasyntax;

/**
 * Represents a piece of data within an FDS Section.
 */
public class FDSData {
    /**
     * The list of comments preceding this data piece.
     */
    public ArrayList<String> PrecedingComments;

    /**
     * Adds a preceding comment to this data piece.
     */
     * @param comment The comment to add.
    public void AddComment(String comment) {
        comment = comment.Replace("\r", "");
        PrecedingComments.AddRange(comment.Split('\n').Select(str => str.TrimEnd()));
    }

    /**
     * The internal represented data.
     */
    public object Internal;

    /**
     * Returns the output-able String representation of this data.
     */
     * @return The resultant data.
    public String Outputable() {
        if (Internal is ArrayList<FDSData> list) {
            StringBuilder outputBuilder = new StringBuilder();
            foreach (FDSData dat in list) {
                outputBuilder.Append(dat.Outputable()).Append('|');
            }
            return outputBuilder.ToString();
        }
        if (Internal is byte[]) {
            return Convert.ToBase64String((byte[])Internal, Base64FormattingOptions.None);
        }
        if (Internal is bool) {
            return ((bool)Internal) ? "true" : "false";
        }
        return FDSUtility.Escape(Internal.ToString());
    }
}
