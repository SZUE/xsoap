/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/

package soaprmi;

import soaprmi.util.logging.Logger;

/**
 * Allow to detect and check for required version of XSOAP.
 * See <a href="http://www.extreme.indiana.edu/bugzilla/show_bug.cgi?id=42">bug 42</a>
 * for more background and motivation for this function.
 *
 * @version $Revision: 1.24 $ $Date: 2004/10/29 07:20:40 $ (GMT)
 * @author Aleksander Slominski [http://www.extreme.indiana.edu/~aslom]
 */

public class Version
{
    private static Logger logger = Logger.getLogger();

    private static final String IMPL_VERSION_TEMPLATE =
        // WARNING: DO NOT EDIT THIS LINE --- THIS IS AUTOMATICALLY UPDATED BY build.xml
        "@XSOAP_IMPL_VERSION_START:1.2.31-B1@";
    private static String IMPL_VERSION = null;
    private static final String MAIN_VERSION_TEMPLATE =
        // WARNING: DO NOT EDIT THIS LINE --- THIS IS AUTOMATICALLY UPDATED BY build.xml
        "@XSOAP_MAIN_VERSION_START:1.2.31@";
    private static String VERSION = null;
    private static int VERSION_MAJOR = -1;
    private static int VERSION_MINOR = -1;
    private static int VERSION_INCREMENT = -1;

    private static synchronized void extractCurrentVersion() throws IncompatibleVersionException
    {
        int start = IMPL_VERSION_TEMPLATE.indexOf(':');
        int end = IMPL_VERSION_TEMPLATE.lastIndexOf('@');
        IMPL_VERSION = IMPL_VERSION_TEMPLATE.substring(start+1, end);
        start = MAIN_VERSION_TEMPLATE.indexOf(':');
        end = MAIN_VERSION_TEMPLATE.lastIndexOf('@');
        VERSION = MAIN_VERSION_TEMPLATE.substring(start+1, end);
        int[] parsed;
        try {
            parsed = parseVersion(VERSION);
        } catch(NumberFormatException ex) {
            throw new IncompatibleVersionException(
                "internal problem: could not parse internal XSOAP version string "+VERSION, ex);
        }
        VERSION_MAJOR = parsed[0];
        VERSION_MINOR = parsed[1];
        VERSION_INCREMENT = parsed[2];
    }

    /**
     * This is just string that identies current implementation verion.
     * <p><b>NOTE:</b>
     */
    public static String getSpecificationVersion() throws IncompatibleVersionException {
        try {
            if(VERSION_MAJOR <0) extractCurrentVersion();
        } catch(IncompatibleVersionException ex) {
            throw new IllegalStateException(
                "internal problem: could not parse internal XSOAP version string "
                    +"main="+VERSION+" impl="+IMPL_VERSION+":"+ex);
        }
        return VERSION;
    }

    /**
     * This is just string that is useful to print major verion of XSOAP.
     */
    public static String getImplementationVersion()  {
        try {
            if(VERSION_MAJOR <0) extractCurrentVersion();
        } catch(IncompatibleVersionException ex) {
            throw new IllegalStateException(
                "internal problem: could not parse internal XSOAP version string "
                    +"main="+VERSION+" impl="+IMPL_VERSION+":"+ex);
        }
        return IMPL_VERSION;
    }

    /**
     * Version mut be of form M.N[.K] where M is major version,
     * N is minor version and K is icrement.
     * This method returns true if current major version is the same
     * and minor is bigger or equal to current minor verion.
     * If provided major and minor verisons are equals to current version
     * then increment is also checked and check is passed when increment
     * is bigger or equal to current increment version.
     */
    public static void require(String version)
        throws IncompatibleVersionException
    {
        // NOTE: this is safe as int operations are atomic ...
        if(VERSION_MAJOR <0) extractCurrentVersion();
        int[] parsed;
        try {
            parsed = parseVersion(version);
        } catch(NumberFormatException ex) {
            throw new IncompatibleVersionException(
                "could not parse XSOAP version string "+version, ex);
        }
        int major = parsed[0];
        int minor = parsed[1];
        int increment = parsed[2];

        if(major != VERSION_MAJOR) {
            throw new IncompatibleVersionException("required XSOAP "+version
                                                       +" has different major version"
                                                       +" from current "+VERSION);
        }
        if(minor > VERSION_MINOR) {
            throw new IncompatibleVersionException("required XSOAP "+version
                                                       +" has too big minor version"
                                                       +" when compared to current "+VERSION);
        }
        if(minor == VERSION_MINOR) {
            if(increment > VERSION_INCREMENT) {
                throw new IncompatibleVersionException("required XSOAP "+version
                                                           +" has too big increment version"
                                                           +" when compared to current "+VERSION);
            }
        }
    }

    /**
     * Parse verion string N.M[.K] into thre subcomponents (M=major,N=minor,K=increment)
     * that are returned in array with three elements.
     * M and N must be non negative, and K if present must be positive integer.
     * Increment K is optional and if not present in verion strig it is returned as zero.
     */
    public static int[] parseVersion(String version)
        throws NumberFormatException
    {
        int[] parsed = new int[3];
        int firstDot = version.indexOf('.');
        if(firstDot == -1) {
            throw new NumberFormatException(
                "expected version string N.M but there is no dot in "+version);
        }
        String majorVersion = version.substring(0, firstDot);
        parsed[0] = Integer.parseInt(majorVersion);
        if(parsed[0] < 0) {
            throw new NumberFormatException(
                "major N version number in N.M can not be negative in "+version);
        }
        int secondDot = version.indexOf('.', firstDot+1);
        String minorVersion;
        if(secondDot >= 0) {
            minorVersion = version.substring(firstDot+1, secondDot);
        } else {
            minorVersion = version.substring(firstDot+1);
        }
        parsed[1] = Integer.parseInt(minorVersion);
        if(parsed[1] < 0) {
            throw new NumberFormatException(
                "minor M version number in N.M can not be negative in "+version);
        }
        if(secondDot >= 0) {
            String incrementVersion = version.substring(secondDot+1);
            parsed[2] = Integer.parseInt(incrementVersion);
            if(parsed[2] < 0) {
                throw new NumberFormatException(
                    "increment K version number in N.M.K must be positive number in "+version);
            }
        }
        return parsed;
    }

}





