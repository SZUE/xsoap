/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: XmlNodeHandler.java,v 1.10 2004/05/06 18:18:47 aslom Exp $
 */

package soaprmi.soapenc;

import java.io.IOException;
import java.io.Writer;

import java.util.*;
import java.text.*;

import org.gjt.xpp.*;

//import org.globus.util.Base64;

import soaprmi.mapping.XmlMapException;
import soaprmi.mapping.XmlJavaTypeMap;
import soaprmi.util.Check;
import soaprmi.util.Util;
//import soaprmi.util.base64.Base64;

import soaprmi.soap.*;

/**
 * This handler convert incoming XML into XML node tree and serialzies node tree to output.
 *
 * @version $Revision: 1.10 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class XmlNodeHandler implements Deserializer, Serializer {
    static XmlPullParserFactory factory;

    public XmlNodeHandler() throws SoapException {
        factory = Util.getPullParserFactory();
//        try {
//            factory = XmlPullParserFactory.newInstance();
//        } catch(Exception ex) {
//            throw new SoapException("could nto create XPP factory for XmlNode handler", ex);
//        }
    }

    public Object readObject(
        DeserializeContext dctx,
        EncodingStyle enc,
        Class expectedClass,
        XmlJavaTypeMap map,
        XmlPullParser pp,
        XmlStartTag stag
    )
        throws DeserializeException, XmlPullParserException, IOException
    {
        if(pp.getEventType() != XmlPullParser.START_TAG) {
            throw new DeserializeException("expected start tag to get XML node tree"
                                               +pp.getPosDesc());
        }

        // NOTE: make sure thaty ANY XML content can be processed and not possibly current
        //     default no mixed content mode for SOAP encoding
        boolean allowedMixedContext = pp.isAllowedMixedContent();
        pp.setAllowedMixedContent(true);

        XmlNode nodeTree;
        try {
            nodeTree = factory.newNode(pp);


            int nDeclared = nodeTree.getDeclaredNamespaceLength();
            int len = nDeclared < 16 ? 16 : 2 * nDeclared;
            String[] uris = new String[len];
            //            nodeTree.readDeclaredNamespaceUris(uris, 0, nDeclared);
            String[] prefixes = new String[len];
            //            nodeTree.readDeclaredPrefixes(prefixes, 0, nDeclared);

            //int off = nDeclared;
            // keep adding prefix declarations going up the element tree
            // NOTE: it MUST go from bottom to top!!!!
            int depth = pp.getDepth() - 1;
            while(depth > 0) {
                nDeclared = pp.getNamespacesLength(depth);
                if(nDeclared > 0) {
                    if(nDeclared > uris.length) {
                        len += 2 * nDeclared;
                        String[] newUris = new String[len];
                        String[] newPrefixes = new String[len];
                        //System.arraycopy(uris, 0, newUris, 0, off);
                        //System.arraycopy(prefixes, 0, newPrefixes, 0, off);
                    }
                    pp.readNamespacesPrefixes(depth, prefixes, 0, nDeclared);
                    pp.readNamespacesUris(depth, uris, 0, nDeclared);
                    for (int i = 0; i < nDeclared; i++)
                    {
                        String p = prefixes[i];
                        String n = uris[i];
                        if(p != null) { // do not care about default namespaces
                            if(nodeTree.prefix2Namespace(p) == null) {
                                nodeTree.addNamespaceDeclaration(p, n);
                            }
                        }
                    }
                }
                --depth;
            }

        } catch(Exception ex) {  //TO FIX ME ...
            throw new DeserializeException(
                "can' deserialize input into XML node tree"
                    +pp.getPosDesc(), ex);
        }
        //if(pp.next() != XmlPullParser.END_TAG)
        //    throw new DeserializeException("expected end tag"+pp.getPosDesc());

        // return whatever mixed content mode was before
        pp.setAllowedMixedContent(allowedMixedContext);

        return nodeTree;
    }

    public void writeObject(
        SerializeContext sctx,
        EncodingStyle enc,
        Object o,
        String name,
        Class baseClass,
        String id)
        throws SerializeException, XmlMapException, IOException
    {
        Writer out = sctx.getWriter();
        //out.write('<');
        //out.write(name);
        //SoapStyle style = sctx.getSoapStyle();
        //out.write('>');
        XmlNode node = (XmlNode) o;
        try {
            XmlRecorder recorder = factory.newRecorder();
            recorder.setOutput(out);
            recorder.writeNode(node);
            out.flush();
        } catch(Exception ex) {  //TO FIX ME ...
            throw new SerializeException(
                "can't serialize XML node tree "+node+" to output stream", ex);
        }

        //out.write("</");
        //out.write(name);
        //out.write(">\n");
    }

}


/*
 * Indiana University Extreme! Lab Software License, Version 1.2
 *
 * Copyright (C) 2002 The Trustees of Indiana University.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1) All redistributions of source code must retain the above
 *    copyright notice, the list of authors in the original source
 *    code, this list of conditions and the disclaimer listed in this
 *    license;
 *
 * 2) All redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the disclaimer
 *    listed in this license in the documentation and/or other
 *    materials provided with the distribution;
 *
 * 3) Any documentation included with all redistributions must include
 *    the following acknowledgement:
 *
 *      "This product includes software developed by the Indiana
 *      University Extreme! Lab.  For further information please visit
 *      http://www.extreme.indiana.edu/"
 *
 *    Alternatively, this acknowledgment may appear in the software
 *    itself, and wherever such third-party acknowledgments normally
 *    appear.
 *
 * 4) The name "Indiana University" or "Indiana University
 *    Extreme! Lab" shall not be used to endorse or promote
 *    products derived from this software without prior written
 *    permission from Indiana University.  For written permission,
 *    please contact http://www.extreme.indiana.edu/.
 *
 * 5) Products derived from this software may not use "Indiana
 *    University" name nor may "Indiana University" appear in their name,
 *    without prior written permission of the Indiana University.
 *
 * Indiana University provides no reassurances that the source code
 * provided does not infringe the patent or any other intellectual
 * property rights of any other entity.  Indiana University disclaims any
 * liability to any recipient for claims brought by any other entity
 * based on infringement of intellectual property rights or otherwise.
 *
 * LICENSEE UNDERSTANDS THAT SOFTWARE IS PROVIDED "AS IS" FOR WHICH
 * NO WARRANTIES AS TO CAPABILITIES OR ACCURACY ARE MADE. INDIANA
 * UNIVERSITY GIVES NO WARRANTIES AND MAKES NO REPRESENTATION THAT
 * SOFTWARE IS FREE OF INFRINGEMENT OF THIRD PARTY PATENT, COPYRIGHT, OR
 * OTHER PROPRIETARY RIGHTS.  INDIANA UNIVERSITY MAKES NO WARRANTIES THAT
 * SOFTWARE IS FREE FROM "BUGS", "VIRUSES", "TROJAN HORSES", "TRAP
 * DOORS", "WORMS", OR OTHER HARMFUL CODE.  LICENSEE ASSUMES THE ENTIRE
 * RISK AS TO THE PERFORMANCE OF SOFTWARE AND/OR ASSOCIATED MATERIALS,
 * AND TO THE PERFORMANCE AND VALIDITY OF INFORMATION GENERATED USING
 * SOFTWARE.
 */


