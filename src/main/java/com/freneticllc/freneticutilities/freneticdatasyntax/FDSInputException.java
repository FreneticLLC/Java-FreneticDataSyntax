//
// This file is part of Frenetic Utilities, created by Frenetic LLC.
// This code is Copyright (C) Frenetic LLC under the terms of the MIT license.
// See README.md or LICENSE.txt in the FreneticUtilities source root for the contents of the license.
//

package com.freneticllc.freneticutilities.freneticdatasyntax;

/**
 * Represents an exception throw while inputting data to an FDS section.
 */
public class FDSInputException : Exception {
    /**
     * Construct the FDS exception.
     */
     * @param message The message explaining the error.
    public FDSInputException(String message)
        : base(message) {
        // No init needed.
    }
}
