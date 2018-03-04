/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002-2004 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: PullNode.java,v 1.8 2004/05/06 18:18:44 aslom Exp $
 */

package org.gjt.xpp.impl.pullnode;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.io.IOException;

import org.gjt.xpp.XmlNode;
import org.gjt.xpp.XmlPullNode;
import org.gjt.xpp.XmlPullParser;
import org.gjt.xpp.XmlPullParserException;

import org.gjt.xpp.impl.tag.PullParserRuntimeException;
import org.gjt.xpp.impl.node.Node;

/**
 * Allows node tree to be constructed on demand.
 * When PullNode is constructed and method setPullPasrser() is
 * executed (or constructor with PullParser arg is called)
 * node is assumend to be incomplete and children will be
 * retrieved on demand (pulled) including automatic creation of
 * sub pull nodes. If no pull parser is associated (it is null)
 * this class must work like regular XmlNode...
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class PullNode extends Node implements XmlPullNode {
    protected int depth;
    protected XmlPullParser pp;

    public PullNode(XmlPullParser pp)
        throws XmlPullParserException
    {
        setPullParser(pp);
    }

    public XmlNode newNode()
        throws XmlPullParserException
    {
        return new PullNode(null);
    }

    public XmlPullNode newPullNode(XmlPullParser pp)
        throws XmlPullParserException
    {
        return new PullNode(pp);
    }

    /** PullNode stays in finished state. */
    public void resetPullNode()
    {
        if(pp != null) {
            readChildrenFullySafe(Integer.MAX_VALUE);
        }
        super.resetNode();
        depth = -1;
    }

    public boolean isFinished() { return pp == null; }

    // returned pull parser will be before possible start tag of the child
    // next() will must return START_TAG or END_TAG if end of children
    public XmlPullParser getPullParser()
        throws IOException, XmlPullParserException
    {
        if(pp == null) {
            throw new XmlPullParserException(
                "Pull node is complete and no pull parser can be returned");
        }

        int currentDepth = pp.getDepth();
        if(currentDepth >= depth + 1) { // parsing deep in another element
            Object lastChild = getChildAt(childrenCount - 1);
            if(lastChild instanceof XmlPullNode) {
                ((XmlPullNode) lastChild).readChildren();
                currentDepth = pp.getDepth();
            } else {
                throw new XmlPullParserException(
                    "underlying pull parser is in incosistent state "
                        +"depth is "+currentDepth+" instead of "+depth
                        +pp.getPosDesc());
            }
        }
        if(currentDepth == depth + 1) {
            if(pp.getEventType() != XmlPullParser.END_TAG) {
                throw new XmlPullParserException(
                    "underlying pull parser is in incosistent state "
                        +" it should be on end tag of last node "
                        +" but it is at "+pp.getPosDesc());
            }
        } else if(currentDepth == depth) {
            // must be on initial start tag
            if(pp.getEventType() != XmlPullParser.START_TAG) {
                throw new XmlPullParserException(
                    "underlying pull parser is in incosistent state "
                        +" it should be on start tag of this node "+getRawName()
                        +" but it is at "+pp.getPosDesc());
            }
        } else {
            throw new XmlPullParserException(
                "underlying pull parser is in incosistent state "
                    +" it is above current node "+getRawName()
                    +" currently at "+pp.getPosDesc());
        }

        XmlPullParser hold = pp;
        pp = null;
        return hold;
    }


    /** Reset pull node to use pull parser. Pull Parser must be on START_TAG */
    public void setPullParser(XmlPullParser pp)
        throws XmlPullParserException
    {
        super.resetNode(); //super.removeChildren();
        this.pp = pp;
        this.depth = -1;
        if(pp == null) {
            return;
        }
        if(pp.getEventType() != XmlPullParser.START_TAG) {
            throw new XmlPullParserException(
                "parser must be at START_TAG to create pull node"
                    +pp.getPosDesc()
            );
        }
        this.depth = pp.getDepth();
        pp.readNodeWithoutChildren(this);
    }

    public Enumeration children()
    {
        if(pp != null) {
            //handle dynamic reading of children
            return new PullNodeEnumerator(this);
        } else {
            return super.children();
        }
    }

    public Object readNextChild()
        throws XmlPullParserException, IOException
    {
        if(pp == null) {
            //throw new XmlPullParserException("no pull parser available");
            return null;
        }

        // make sure that last child is fully constructed
        if(childrenCount > 0) {
            Object lastChild = getChildAt(childrenCount - 1);
            if(lastChild instanceof XmlPullNode) {
                ((XmlPullNode) lastChild).readChildren();
            }
        }
        //while(true) {
        byte state = pp.next();
        if(state == XmlPullParser.START_TAG)
        {
            // double check depth!!!!
            if(pp.getDepth() != depth + 1) {
                throw new XmlPullParserException(
                    "expected start tag at depth "+(depth + 1)+" not "+pp.getDepth()
                        +pp.getPosDesc());
            }
            XmlPullNode newChild = newPullNode(pp);
            //pp.readNodeWithoutChildren(newChild);
            //if(DEBUG) System.err.println("adding node="+newChild+" count="+childrenCount);
            super.appendChild(newChild);
            return newChild;

        } else if(state == XmlPullParser.CONTENT) {
            // double check depth!!!!
            if(pp.getDepth() != depth) {
                throw new XmlPullParserException(
                    "expected content at depth "+depth+" not "+pp.getDepth()
                        +pp.getPosDesc());
            }
            // only add element content if not empty string ""
            //if(pp.getEventEnd() > pp.getEventStart()) {
            String content = pp.readContent();
            //if(DEBUG) System.err.println("adding content="+content+" count="+childrenCount);
            super.appendChild(content);
            return content;
            //}
            // continue looking for other than empty string children
            //continue;
        } else if(state == XmlPullParser.END_TAG) {
            // double check depth!!!!
            if(pp.getDepth() != depth) {
                throw new XmlPullParserException(
                    "expected end tag at depth "+depth+" not "+pp.getDepth()
                        +pp.getPosDesc());
            }
            pp = null;
            return null;
        } else if(state == XmlPullParser.END_DOCUMENT) {
            throw new XmlPullParserException(
                "pull node could not be built -  embedded pull parser"
                    +" was accessed and already finished parsing"
                    +pp.getPosDesc());
        } else {
            throw new XmlPullParserException(
                "unexpected pull parser event "+state+pp.getPosDesc());
        }
        //}
    }

    /**
     * Read all reminaing children up to end tag.
     */
    public void readChildren() throws XmlPullParserException, IOException
    {
        readChildren(Integer.MAX_VALUE);
    }
    /**
     * Read all reminaing children up to pos -1 and start tag at pos.
     */
    private void readChildren(int pos) throws XmlPullParserException, IOException
    {
        if(pp == null) return;
        while(childrenCount - 1 < pos) {
            if(readNextChild() == null) {
                pp = null;
                break;
            }
        }
        // assert (incomplete && childrenCount -1 == pos) || !incomplete
    }

    private void readChildrenFullySafe(int pos)
    {
        if(pp == null) return;
        try {
            readChildren(pos);
            // make sure that current child is fully constructed
            if(pp != null && childrenCount > 0) {
                Object lastChild = getChildAt(childrenCount - 1);
                if(lastChild instanceof XmlPullNode) {
                    ((XmlPullNode) lastChild).readChildren();
                }
            }
        } catch(XmlPullParserException ex) {
            throw new PullParserRuntimeException(ex);
        } catch(IOException ex) {
            throw new PullParserRuntimeException(ex);
        }
    }

    private void readChildrenPartialSafe(int pos)
    {
        try {
            readChildren(pos);
        } catch(XmlPullParserException ex) {
            throw new PullParserRuntimeException(ex);
        } catch(IOException ex) {
            throw new PullParserRuntimeException(ex);
        }
    }

    public void skipChildren() throws XmlPullParserException, IOException
    {
        if(pp == null) return;
        // for last node call skipChildren()
        if(childrenCount > 0) {
            Object lastChild = getChildAt(childrenCount - 1);
            if(lastChild instanceof XmlPullNode) {
                ((XmlPullNode) lastChild).skipChildren();
            }
        }
        while(true) {
            // double check depth!!!!
            byte event = pp.next();
            if(event == XmlPullParser.START_TAG)
            {
                if(pp.getDepth() != depth + 1) {
                    throw new XmlPullParserException(
                        "expected start tag at depth "+(depth + 1)+" not "+pp.getDepth()
                            +pp.getPosDesc());
                }
                pp.skipNode();
            } else if(event == XmlPullParser.CONTENT) {
                ; // ignore it
            } else if(event == XmlPullParser.END_TAG) {
                if(pp.getDepth() != depth) {
                    throw new XmlPullParserException(
                        "expected end tag at depth "+depth+" not "+pp.getDepth()
                            +pp.getPosDesc());
                }
                pp = null;
                break;
            } else {
                throw new XmlPullParserException(
                    "unexpected parser event "+event+pp.getPosDesc());
            }
        }

        // use pp.skipNode for all remaining children
        pp = null;
    }


    /** if unfinished it returns actual number of children... */
    public int getChildrenCountSoFar()
    {
        return childrenCount;
    }

    /** it will reconsruct whole subtree to get count ... */
    public int getChildrenCount()
    {
        if(pp != null) {
            readChildrenFullySafe(Integer.MAX_VALUE);
        }
        return childrenCount;
    }

    public Object getChildAt(int pos)
    {
        if(pp != null && pos >= childrenCount ) {
            readChildrenPartialSafe(pos);
        }
        return super.getChildAt(pos);
    }

    // ---------------- modifiable


    public void appendChild(Object child)
        throws XmlPullParserException
    {
        if(pp != null) {
            readChildrenFullySafe(Integer.MAX_VALUE);
        }
        super.appendChild(child);
    }

    public void insertChildAt(int pos, Object child)
        throws XmlPullParserException
    {
        if(pp != null && pos >= childrenCount ) {
            //TODO: make sure that it is safe to do partial read!!!!
            readChildrenPartialSafe(pos);
        }
        super.insertChildAt(pos, child);
    }

    public void removeChildAt(int pos)
        throws XmlPullParserException
    {
        if(pp != null && pos >= childrenCount ) {
            readChildrenFullySafe(pos);
        }
        super.removeChildAt(pos);
    }

    public void replaceChildAt(int pos, Object child)
        throws XmlPullParserException
    {
        if(pp != null && pos >= childrenCount ) {
            readChildrenFullySafe(pos);
        }
        super.replaceChildAt(pos, child);
    }

    public void removeChildren()
        throws XmlPullParserException
    {
        if(pp != null) {
            try{
                skipChildren();
            } catch(IOException ex) {
                throw new PullParserRuntimeException(
                    "removeChildren(): could not skip children", ex);
            }
        }
        super.removeChildren();
    }

    /**
     * Print this class state into StringBuffer element name
     */
    protected void printFields(StringBuffer buf) {
        //buf.append(" incomplete="+(pp != null));
        buf.append(" pp="+pp);
        buf.append(" depth="+depth);
        super.printFields(buf);
    }

    /**
     * Return string representation of start tag including name
     * and list of attributes.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer("PullNode={");
        printFields(buf);
        buf.append(" }");
        return buf.toString();
    }

}


class PullNodeEnumerator implements Enumeration
{
    XmlPullNode node;
    int pos;

    PullNodeEnumerator(XmlPullNode node)
    {
        this.node = node;
        pos = 0;
    }

    public boolean hasMoreElements() {
        if(node.isFinished()) {
            return pos < node.getChildrenCount();
        } else {
            int countSoFar = node.getChildrenCountSoFar();
            if(pos < countSoFar)
                return true;
            try {
                // read children until pos is achieved or end
                while(true) {
                    Object value = node.readNextChild();
                    if(value != null) {
                        ++countSoFar;
                        if(pos == countSoFar - 1)
                            return true;
                    } else {
                        return false;
                    }
                }
            } catch(XmlPullParserException ex) {
                throw new PullParserRuntimeException(
                    "next value could not be read ", ex);
            } catch(IOException ex) {
                throw new PullParserRuntimeException(
                    "next value could not be read ", ex);
            }
        }
    }

    public Object nextElement() {
        return node.getChildAt(pos++);
    }

}



/*
 * Indiana University Extreme! Lab Software License, Version 1.2
 *
 * Copyright (c) 2002-2004 The Trustees of Indiana University.
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

