//
// This file is part of Frenetic Utilities, created by Frenetic LLC.
// This code is Copyright (C) Frenetic LLC under the terms of the MIT license.
// See README.md or LICENSE.txt in the FreneticUtilities source root for the contents of the license.
//

package com.freneticllc.freneticutilities.freneticdatasyntax;

import java.util.ArrayList;
import java.util.Base64;

/**
 * Represents a piece of data within an FDS Section.
 */
public class FDSData {

    /**
     * Construct an empty FDS Data.
     */
    public FDSData() {
    }

    /**
     * Construct an FDS Data.
     * @param internal the internal object.
     */
    public FDSData(Object internal) {
        this.internal = internal;
    }

    /**
     * Construct an FDS Data.
     * @param internal the internal object.
     * @param precedingComments the preceeding comments.
     */
    public FDSData(Object internal, ArrayList<String> precedingComments) {
        this.internal = internal;
        this.precedingComments = precedingComments;
    }

    /**
     * The list of comments preceding this data piece.
     */
    public ArrayList<String> precedingComments;

    /**
     * Adds a preceding comment to this data piece.
     * @param comment The comment to add.
     */
    public void addComment(String comment) {
        comment = comment.replaceAll("\\r", "");
        for (String str : comment.split("\\n")) {
            precedingComments.add(FDSUtility.trimEnd(str));
        }
    }

    /**
     * The internal represented data.
     */
    public Object internal;

    /**
     * Returns the output-able string representation of this data.
     * @return The resultant data.
     */
    public String outputable() {
        if (internal instanceof ArrayList) {
            StringBuilder outputBuilder = new StringBuilder();
            for (FDSData dat : (ArrayList<FDSData>) internal) {
                outputBuilder.append(dat.outputable()).append('|');
            }
            return outputBuilder.toString();
        }
        if (internal instanceof byte[]) {
            return Base64.getEncoder().encodeToString((byte[]) internal);
        }
        if (internal instanceof Boolean) {
            return ((Boolean) internal) ? "true" : "false";
        }
        return FDSUtility.escape(internal.toString());
    }
}
