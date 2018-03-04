/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002-2004 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: Node.java,v 1.11 2004/05/06 18:18:44 aslom Exp $
 */

package org.gjt.xpp.impl.node;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.gjt.xpp.XmlNode;
import org.gjt.xpp.XmlPullParserException;

import org.gjt.xpp.impl.tag.PullParserRuntimeException;
import org.gjt.xpp.impl.tag.StartTag;

//TODO: check for clearing reference when removing children

/**
 * Encapsulate XML Node with list of associated children and namespaces :-).
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */

public class Node extends StartTag implements XmlNode {
    protected static final Enumeration EMPTY_ENUMERATION = new EmptyEnumerator();
    protected Vector children;
    protected int childrenCount;
    protected Object oneChild;
    protected XmlNode parent;
    //protected int parentMyPos;
    protected String defaultNamespaceUri;
    protected Hashtable prefix2Ns;
    protected int declaredNsEnd;
    protected String[] declaredNs;
    protected String[] declaredPrefixes;

    public boolean equals(Object o) {
        if(o == null) return false;
        if(! (o instanceof Node) ) return false;
        Node node = (Node) o;
        if(childrenCount != node.childrenCount) return false;
        if(childrenCount == 1) {
            if(! oneChild.equals(node.oneChild)) {
                return false;
            }
        } else {
            for (int i = 0; i < childrenCount; i++) {
                if(! children.elementAt(i).equals(node.children.elementAt(i))) {
                    return false;
                }
            }
        }
        String u = defaultNamespaceUri;
        if(u == null) u = "";
        String nodeU = node.defaultNamespaceUri;
        if(nodeU == null) nodeU = "";

        if( ! u.equals(nodeU)) {
            return false;
        }

        //        if(declaredNsEnd != node.declaredNsEnd) return false;
        //        for (int i = 0; i < declaredNsEnd; i++) {
        //            if((!declaredNs[i].equals(node.declaredNs[i]) )
        //               && (!declaredPrefixes[i].equals(node.declaredPrefixes[i]) ) )   {
        //                return false;
        //            }
        //        }

        return super.equals(o);
    }

    public Node() {
    }

    public void resetNode() {
        super.resetStartTag();
        parent = null;
        childrenCount = 0;
        oneChild = null;
        if(children != null) {
            children.removeAllElements();
        }
        removeDeclaredNamespaces();
        //parentMyPos = -1;
    }

    /** context sensitive factory method to create the same type of node */
    public XmlNode newNode()
        throws XmlPullParserException {
        return new Node();
    }

    public XmlNode newNode(String namespaceUri, String localName) throws XmlPullParserException {
        XmlNode node = new Node();
        String prefix = namespace2Prefix(namespaceUri);
        if(prefix == null) {
            throw new XmlPullParserException(
                "namespace '"+namespaceUri+"' has no prefix declared in node tree");
        }
        node.modifyTag(namespaceUri, localName,
                           ("".equals(prefix) ? localName : prefix+':'+localName) );
        return node;
    }

    public XmlNode getParentNode() {
        return parent;
    }

    public Enumeration children() {
        if(childrenCount == 0) {
            return EMPTY_ENUMERATION;
        } else if(childrenCount == 1) {
            return new OneChildEnumerator(oneChild);
        } else {
            return children.elements();
        }
    }

    /** it may need to reconsruct whole subtree to get count ... */
    public int getChildrenCount() {
        return childrenCount;
    }

    public Object getChildAt(int pos) {
        if(childrenCount > 0) {
            if(pos == 0 && childrenCount ==  1) {
                return oneChild;
            } else if(pos < childrenCount) {
                return children.elementAt(pos);
            } else {
                throw new IllegalArgumentException(
                    "no child at position "+pos);
            }
        } else {
            throw new IllegalArgumentException(
                "this node has no children");
        }
    }

    // ---------------- modifiable

    public void setParentNode(XmlNode parent) {
        this.parent = parent;
    }

    public void appendChild(Object child)
        throws XmlPullParserException {
        if(childrenCount == 0) {
            oneChild = child;
        } else {
            //childrenCount > 0
            if(children == null) {
                children = new Vector(4, 4);
            }
            if(childrenCount == 1) {
                if(children.size() > 0) {
                    children.removeAllElements();
                }
                children.addElement(oneChild);
                oneChild = null;
            }
            children.addElement(child);
        }
        if(child instanceof XmlNode) {
            ((XmlNode)child).setParentNode(this);
        }
        ++childrenCount;
    }

    public void insertChildAt(int pos, Object child)
        throws XmlPullParserException {
        if(childrenCount == 0) {
            if(pos != 0) {
                throw new XmlPullParserException(
                    "to insert first child position must be 0 not "+pos);
            }
            oneChild = child;
        } else {
            if(children == null) {
                children = new Vector(4, 4);
            }
            if(childrenCount == 1) {
                if(children.size() > 0) {
                    children.removeAllElements();
                }
                children.addElement(oneChild);
            }
            children.insertElementAt(child, pos);
        }
        if(child instanceof XmlNode) {
            ((XmlNode)child).setParentNode(this);
        }
        ++childrenCount;
    }

    public void removeChildAt(int pos)
        throws XmlPullParserException {
        Object removedChild = null;
        if(childrenCount == 0) {
            throw new XmlPullParserException(
                "node has no children to remove");
        } else if(childrenCount == 1) {
            if(pos != 0) {
                throw new XmlPullParserException(
                    "to remove last child position must be 0 not "+pos);
            }
            removedChild = oneChild;
            oneChild = null;
        } else if(childrenCount == 2) {
            if(pos == 0) {
                oneChild = children.elementAt(1);
                removedChild = children.elementAt(0);
            } else if(pos == 1) {
                oneChild = children.elementAt(0);
                removedChild = children.elementAt(1);
            } else {
                throw new XmlPullParserException(
                    "only two children position must 0 or 1 but not "+pos);
            }
            children.removeAllElements();
        } else {
            removedChild = children.elementAt(pos);
            children.removeElementAt(pos);
        }
        if(removedChild instanceof XmlNode) {
            ((XmlNode)removedChild).setParentNode(null);
        }
        --childrenCount;
    }

    public void replaceChildAt(int pos, Object child)
        throws XmlPullParserException {
        if(childrenCount == 1) {
            if(pos != 0) {
                throw new XmlPullParserException(
                    "to set first child position must be 0 not "+pos);
            }
            oneChild = child;
        } else if(childrenCount > 1) {
            children.setElementAt(child, pos);
        } else {
            throw new XmlPullParserException(
                "node must have at least one children to set child at "+pos);
        }
        if(child instanceof XmlNode) {
            ((XmlNode)child).setParentNode(this);
        }
    }

    public void ensureChildrenCapacity(int minCapacity)
        throws XmlPullParserException {
        if(minCapacity <= 1) return;
        if(children == null) {
            children = new Vector(minCapacity, 4);
        } else {
            children.ensureCapacity(minCapacity);
        }
    }

    public void removeChildren()
        throws XmlPullParserException {
        // un-parent all children
        if(childrenCount == 1 && oneChild instanceof XmlNode) {
            ((XmlNode)oneChild).setParentNode(null);
        }
        oneChild = null;

        if(children != null && children.size() > 0) {
            Enumeration enum = children.elements();
            while (enum.hasMoreElements()) {
                Object child = enum.nextElement();
                if(child instanceof XmlNode) {
                    ((XmlNode)child).setParentNode(null);
                }
            }
            children.removeAllElements();
        }

        childrenCount = 0;
    }

    // ---------------- namespace related methods -- pain...

    //NOTE: this is initialized based on "namespace" xmlns attributes...

    public String getQNameLocal(String qName) {
        int i = qName.lastIndexOf(':');
        return qName.substring(i + 1);
    }

    public String getQNameUri(String qName) {
        int i = qName.lastIndexOf(':');
        if(i > 0) {
            String prefix = qName.substring(0, i);
            try {
                return prefix2Namespace(prefix);
            } catch(XmlPullParserException ex) {
                throw new PullParserRuntimeException(ex);
            }
        } else {
            return defaultNamespaceUri;
        }
    }

    public String prefix2Namespace(String prefix)
        throws XmlPullParserException {
        if(prefix == null) {
            return null;
        }
        if("".equals(prefix)) {
            return defaultNamespaceUri;
        }
        String namespaceUri = null;
        if(declaredNsEnd > 0) {
            if(prefix2Ns == null) {
                //populatePrefix2Ns();
                prefix2Ns = new Hashtable();
                for (int i = 0; i < declaredNsEnd; i++) {
                    prefix2Ns.put(declaredPrefixes[i], declaredNs[i]);
                }
            }
            namespaceUri = (String) prefix2Ns.get(prefix);
        }
        if(parent != null && namespaceUri == null) {
            return parent.prefix2Namespace(prefix);
        }
        return namespaceUri;
    }

    public String namespace2Prefix(String namespaceUri) throws XmlPullParserException {
        if(namespaceUri == null) {
            throw new XmlPullParserException("null is not allowed for namespace name");
        }
        if(namespaceUri.equals(getDefaultNamespaceUri())) {
            return "";
        }
        for(int i = 0 ; i < declaredNsEnd; ++i) {
            if(namespaceUri.equals(declaredNs[i])) {
                return declaredPrefixes[i];
            }
        }

        if(parent != null) {
            return parent.namespace2Prefix(namespaceUri);
        } else {
            return null;
        }
    }

    public String getDefaultNamespaceUri() {
        return defaultNamespaceUri;
    }

    public void setDefaultNamespaceUri(String defaultNamespaceUri) {
        this.defaultNamespaceUri = defaultNamespaceUri;
    }

    public int getDeclaredNamespaceLength() {
        return declaredNsEnd;
    }

    public void readDeclaredNamespaceUris(String[] uris, int off, int len) {
        if(len > declaredNsEnd) throw new IllegalArgumentException(
                "this node has only "+declaredNsEnd+" namespace URIs and not "+len);

        if(declaredNs != null) {
            System.arraycopy(declaredNs, 0, uris, off, len);
        }
        //    Enumeration enum = prefix2Ns.keys();
        //    int i = off;
        //    while (enum.hasMoreElements() && i < off + len)
        //    {
        //        uris[i++] = (String) prefix2Ns.get( enum.nextElement() );
        //    }
    }

    public void readDeclaredPrefixes(String[] prefixes, int off, int len) {
        if(len > declaredNsEnd) throw new IllegalArgumentException(
                "this node has only "+declaredNsEnd+" prefixes and nor "+len);
        if(declaredPrefixes != null) {
            System.arraycopy(declaredPrefixes, 0, prefixes, off, len);
        }
    }

    public void ensureDeclaredNamespacesCapacity(int minCapacity) {
        if(declaredNs == null || declaredNs.length < minCapacity) {
            String[] newDeclaredNs = new String[minCapacity];
            String[] newDeclaredPrefixes = new String[minCapacity];
            if(declaredNsEnd > 0) {
                System.arraycopy(
                    declaredNs, 0, newDeclaredNs, 0, declaredNsEnd);
                System.arraycopy(
                    declaredPrefixes, 0, newDeclaredPrefixes, 0, declaredNsEnd);
            }
            declaredNs = newDeclaredNs;
            declaredPrefixes = newDeclaredPrefixes;
        }
    }

    public void addNamespaceDeclaration(String prefix, String namespaceUri)
        throws XmlPullParserException
    {
        if(prefix2Ns != null) {
            Object exisitingNs = prefix2Ns.get(prefix);
            if(exisitingNs != null) {
                throw new IllegalArgumentException(
                    "prefix '"+prefix+"' already bound to '"+exisitingNs
                        +"' (and can not be rebound to '"+namespaceUri+"'");
            }
        }
        ensureDeclaredNamespacesCapacity(declaredNsEnd + 1);
        declaredNs[declaredNsEnd] = namespaceUri;
        declaredPrefixes[declaredNsEnd] = prefix;
        declaredNsEnd++;
        if(prefix2Ns != null) {
            prefix2Ns.put(prefix, namespaceUri);
        }
    }

    public void addDeclaredNamespaces(
        String[] prefix, int off, int len, String[] namespaceUri) {
        if(len == 0) return;
        // assert len > 0
        if(len < 0) throw new IllegalArgumentException(
                "number of added namespaces can not be negative");
        if(prefix2Ns != null) {
            for (int i = 0; i < prefix.length; i++)
            {
                String pref = prefix[i];
                Object exisitingNs = prefix2Ns.get(pref);
                if(exisitingNs != null) {
                    throw new IllegalArgumentException(
                        "prefix '"+pref+"' already bound to '"+exisitingNs
                            +"' (and can not be rebound to '"+namespaceUri[i]+"'");
                }
            }
        }

        ensureDeclaredNamespacesCapacity(declaredNsEnd + len);
        System.arraycopy(
            namespaceUri, off, declaredNs, declaredNsEnd, len);
        System.arraycopy(
            prefix, off, declaredPrefixes, declaredNsEnd, len);
        declaredNsEnd += len;
        //        for(int i = off; i < off + len; i++) {
        //              System.err.println("adding"+
        //                " prefix["+i+"] = "+prefix[i]+
        //                " namespaceUri["+i+"] = "+namespaceUri[i]);
        //        }
        if(prefix2Ns != null) {
            for(int i = off; i < off + len; i++) {
                // TODO: assert both are not null...
                prefix2Ns.put(prefix[i], namespaceUri[i]);
            }
        }
    }

    public void removeDeclaredNamespaces() {
        //      if(prefix2Ns != null) {
        //        prefix2Ns.clear();
        //      }
        prefix2Ns = null;
        declaredNsEnd = 0;
        defaultNamespaceUri = null;
    }

    /**
     * Print into StringBuffer element name
     */
    protected void printFields(StringBuffer buf) {
        super.printFields(buf);
        buf.append(" children=[ ");
        if(childrenCount == 0) {
            buf.append("");
        } else if(childrenCount == 1) {
            buf.append("'");
            buf.append(oneChild);
            buf.append("'");
        } else if(children != null) {
            for(int i = 0; i < children.size(); ++i) {
                buf.append('\'');
                buf.append(children.elementAt(i));
                buf.append("', ");
            }
        }
        buf.append(" ]");

        XmlNode parent = getParentNode();
        String defaultUri = getDefaultNamespaceUri();
        if(defaultUri != null
           && (parent == null
                   || ! defaultUri.equals(parent.getDefaultNamespaceUri())
              )
          ) {
            buf.append(" xmlns='");
            buf.append(defaultUri);
            buf.append('\'');
        }
        if(declaredNsEnd > 0) {
            buf.append(" namespaces = [");
            for(int i = 0; i < declaredNsEnd; ++i) {
                buf.append(" xmlns:");
                buf.append(declaredPrefixes[i]);
                buf.append("='");
                buf.append(declaredNs[i]);
                buf.append("'");
            }
            //        Enumeration enum = prefix2Ns.keys();
            //        while (enum.hasMoreElements())
            //        {
            //              Object key = enum.nextElement();
            //              buf.append("xmlns:");
            //              buf.append(key);
            //              buf.append("='");
            //              buf.append(prefix2Ns.get( key  ));
            //              buf.append("'");
            //        }
            buf.append(" ]");
        }
    }

    /**
     * Return string representation of start tag including name
     * and list of attributes.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer("Node={");
        printFields(buf);
        buf.append(" }");
        return buf.toString();
    }

    /** Special enumeration that will enumerate just one element ... */

}

class OneChildEnumerator implements Enumeration {
    Object oneChild;

    OneChildEnumerator(Object value) {
        oneChild = value;
    }

    public boolean hasMoreElements() {
        return oneChild != null;
    }

    public Object nextElement() {
        if(oneChild != null) {
            Object value = oneChild;
            oneChild = null;
            return value;
        } else {
            throw new PullParserRuntimeException(
                "trying to access elements beyond enb of enumeration");
        }

    }
}

class EmptyEnumerator implements Enumeration {
    public boolean hasMoreElements() {
        return false;
    }

    public Object nextElement() {
        throw new PullParserRuntimeException(
            "trying to access elements beyond enbd of enumeration");
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


