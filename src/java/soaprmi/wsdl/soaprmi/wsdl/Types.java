
/*
 * The Apache Software License, Version 1.1
 *
 *
 * Copyright (c) 2001-2002 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Axis" and "Apache Software Foundation" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 */

package soaprmi.wsdl;

// Sriram's [srikrish@cs.indiana.edu] addition
import org.apache.axis.wsdl.fromJava.Namespaces;

import org.apache.axis.AxisFault;
import org.apache.axis.Constants;
import org.apache.axis.InternalException;
import org.apache.axis.enum.Style;
import org.apache.axis.description.ServiceDesc;
import org.apache.axis.components.logger.LogFactory;
import org.apache.axis.encoding.Serializer;
import org.apache.axis.encoding.SerializerFactory;
import org.apache.axis.encoding.SimpleType;
import org.apache.axis.encoding.TypeMapping;
import org.apache.axis.encoding.ser.BeanSerializerFactory;
import org.apache.axis.encoding.ser.EnumSerializerFactory;
import org.apache.axis.utils.JavaUtils;
import org.apache.axis.utils.Messages;
import org.apache.axis.utils.XMLUtils;
import org.apache.axis.wsdl.symbolTable.BaseTypeMapping;
import org.apache.axis.wsdl.symbolTable.SymbolTable;
import org.apache.commons.logging.Log;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.holders.Holder;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 *
 * <p>Description: </p> This class is used to recursively serializes a Java Class into
 * an XML Schema representation.
 *
 * It has utility methods to create a schema node, assosiate namespaces to the various types
 *
 *
 * @author unascribed
 */
// Sriram's [srikrish@cs.indiana.edu] extension
public class Types extends org.apache.axis.wsdl.fromJava.Types {

  protected static Log log = LogFactory.getLog(Types.class.getName());

  Definition def;
  Namespaces namespaces = null;
  TypeMapping tm;
  TypeMapping defaultTM;
  String targetNamespace;
  Element wsdlTypesElem = null;
  HashMap schemaTypes = null;
  HashMap schemaElementNames = null;
  HashMap schemaUniqueElementNames = null;
  HashMap wrapperMap = new HashMap();
  List stopClasses = null;
  List beanCompatErrs = new ArrayList();
  ServiceDesc serviceDesc = null;

  /**
   * This class serailizes a <code>Class</code> to XML Schema. The constructor
   * provides the context for the streamed node within the WSDL document
   * @param def WSDL Definition Element to declare namespaces
   * @param tm TypeMappingRegistry to handle known types
   * @param defaultTM default TM
   * @param namespaces user defined or autogenerated namespace and prefix maps
   * @param targetNamespace targetNamespace of the document
   */
  public Types(Definition def,
	       TypeMapping tm,
	       TypeMapping defaultTM,
	       Namespaces namespaces,
	       String targetNamespace,
	       List stopClasses,
	       ServiceDesc serviceDesc) {
    // Sriram's [srikrish@cs.indiana.edu] addition
    // This is because empty constructor doesn't exist for superclass
    super(def, tm, defaultTM, namespaces, targetNamespace, stopClasses, serviceDesc);

    this.def = def;
    this.serviceDesc = serviceDesc;
    createDocumentFragment();
    this.tm = tm;
    this.defaultTM = defaultTM;
    this.namespaces = namespaces;
    this.targetNamespace = targetNamespace;
    this.stopClasses = stopClasses;
    schemaElementNames = new HashMap();
    schemaUniqueElementNames = new HashMap();
    schemaTypes = new HashMap();
  }
   
  /**
   * Sriram's [srikrish@cs.indiana.edu] addition
   * Return the Types element
   */
  public Element getTypeElement() {
    return wsdlTypesElem;
  }

  /**
   * Return the namespaces object for the current context
   */
  public Namespaces getNamespaces() {
    return namespaces;
  }

  /**
   * Loads the types from the input schema file.
   * @param inputSchema file or URL
   */
  public void loadInputSchema(String inputSchema)
    throws IOException, WSDLException, SAXException,
	   ParserConfigurationException
  {
    // Read the input wsdl file into a Document
    Document doc = XMLUtils.newDocument(inputSchema);

    // Ensure that the root element is xsd:schema
    Element root = doc.getDocumentElement();
    if (root.getLocalName().equals("schema") &&
	Constants.isSchemaXSD(root.getNamespaceURI())) {
      Node schema = docHolder.importNode(root, true);
      if (null == wsdlTypesElem) {
	writeWsdlTypesElement();
      }
      wsdlTypesElem.appendChild(schema);

      // Create a symbol table and populate it with the input types
      BaseTypeMapping btm =
	new BaseTypeMapping() {
	    public String getBaseName(QName qNameIn) {
	      QName qName = new QName(
				      qNameIn.getNamespaceURI(),
				      qNameIn.getLocalPart());
	      Class cls = defaultTM.getClassForQName(qName);
	      if (cls == null)
		return null;
	      else
		return JavaUtils.getTextClassName(cls.getName());
	    }
	  };
      SymbolTable symbolTable = new SymbolTable(btm,
						true, false, false);
      symbolTable.populateTypes(new URL(inputSchema), doc);

      processSymTabEntries(symbolTable);
    } else {
      // If not, we'll just bail out... perhaps we should log a warning
      // or throw an exception?
      ;
    }
  }
    
  /**
   * Walk the type/element entries in the symbol table and
   * add each one to the list of processed types. This prevents
   * the types from being duplicated.
   *      
   * @param symbolTable
   */ 
  private void processSymTabEntries(SymbolTable symbolTable)
  {
    Iterator iterator = symbolTable.getElementIndex().keySet().iterator();
    while(iterator.hasNext()) {
      QName name = (QName) iterator.next();
      addToElementsList(name);
    }
    iterator = symbolTable.getTypeIndex().keySet().iterator();
    while(iterator.hasNext()) {
      QName name = (QName) iterator.next();
      addToTypesList(name);
    }
  }

  /**
   * Load the types from the input wsdl file.
   * @param inputWSDL file or URL
   */
  public void loadInputTypes(String inputWSDL)
    throws IOException, WSDLException,
	   SAXException, ParserConfigurationException
  {

    // Read the input wsdl file into a Document
    Document doc = XMLUtils.newDocument(inputWSDL);

    // Search for the 'types' element
    NodeList elements = doc.getChildNodes();
    if (elements.getLength() > 0 &&
	elements.item(0).getLocalName().equals("definitions")) {
      elements = elements.item(0).getChildNodes();
      for (int i=0;
	   i < elements.getLength() && wsdlTypesElem == null;
	   i++) {
	Node node = elements.item(i);
	if (node.getLocalName() != null &&
	    node.getLocalName().equals("types")) {
	  wsdlTypesElem = (Element) node;
	}
      }
    }

    // If types element not found, there is no need to continue.
    if (wsdlTypesElem == null) {
      return;
    }

    // Import the types element into the Types docHolder document
    wsdlTypesElem =
      (Element) docHolder.importNode(wsdlTypesElem, true);
    docHolder.appendChild(wsdlTypesElem);

    // Create a symbol table and populate it with the input wsdl document
    BaseTypeMapping btm =
      new BaseTypeMapping() {
	  public String getBaseName(QName qNameIn) {
	    QName qName = new QName(
				    qNameIn.getNamespaceURI(),
				    qNameIn.getLocalPart());
	    Class cls = defaultTM.getClassForQName(qName);
	    if (cls == null)
	      return null;
	    else
	      return JavaUtils.getTextClassName(cls.getName());
	  }
	};
    SymbolTable symbolTable = new SymbolTable(btm,
					      true, false, false);
    symbolTable.populate(null, doc);

    processSymTabEntries(symbolTable);
  }

  /**
   * Write out a type referenced by a part type attribute.
   *
   * @param type <code>Class</code> to generate the XML Schema info for
   * @param qname <code>QName</code> of the type.  If null, qname is
   *             defaulted from the class.
   * @return the QName of the generated Schema type, null if void,
   *         if the Class type cannot be converted to a schema type
   *         then xsd:anytype is returned.
   */
  public QName writeTypeForPart(Class type, QName qname) throws AxisFault {
    //patch by costin to fix an NPE; commented out till we find out what the problem is
    //if you get NullPointerExceptions in this class, turn it on and submit some
    //replicable test data to the Axis team via bugzilla
    /*
      if( type==null ) {
      return null;
      }
    */
    if (type.getName().equals("void")) {
      return null;
    }

    if (Holder.class.isAssignableFrom(type)) {
      type = JavaUtils.getHolderValueType(type);
    }

    // Get the qname
    if (qname == null ||
	(Constants.isSOAP_ENC(qname.getNamespaceURI()) &&
	 "Array".equals(qname.getLocalPart()))) {
      qname = getTypeQName(type);
      if (qname == null) {
	throw new AxisFault("Class:" + type.getName());
      }
    }

    if(!makeTypeElement(type, qname, null)) {
      // Sriram's [srikrish@cs.indiana.edu] fix
      // Remove this entry from the types list
      // If this stays, types represented as xsd:any are incorrectly written 
      ArrayList types = (ArrayList) schemaTypes.get(qname.getNamespaceURI());
      if (types != null) {
	int index = types.indexOf(qname.getLocalPart());
	if (index != -1) {
	  types.remove(index);
	}
      }

      qname = Constants.XSD_ANYTYPE;
    }

    return qname;
  }

  /**
   * Write out an element referenced by a part element attribute.
   *
   * @param type <code>Class</code> to generate the XML Schema info for
   * @param qname <code>QName</code> of the element.  If null, qname is
   *             defaulted from the class.
   * @return the QName of the generated Schema type, null if no element
   */
  public QName writeElementForPart(Class type, QName qname) throws AxisFault {
    //patch by costin to fix an NPE; commented out till we find out what the problem is
    //if you get NullPointerExceptions in this class, turn it on and submit some
    //replicable test data to the Axis team via bugzilla
    /*
      if( type==null ) {
      return null;
      }
    */
    if (type.getName().equals("void")) {
      return null;
    }

    if (Holder.class.isAssignableFrom(type)) {
      type = JavaUtils.getHolderValueType(type);
    }

    // Get the qname
    if (qname == null ||
	(Constants.isSOAP_ENC(qname.getNamespaceURI()) &&
	 "Array".equals(qname.getLocalPart()))) {
      qname = getTypeQName(type);
      if (qname == null) {
	throw new AxisFault("Class:" +type.getName());
      }
    }

    // Return null it a simple type (not an element)
    String nsURI = qname.getNamespaceURI();
    if (Constants.isSchemaXSD(nsURI) ||
	(Constants.isSOAP_ENC(nsURI) &&
	 !"Array".equals(qname.getLocalPart()))) {
      return null;
    }

    // Make sure a types section is present
    if (wsdlTypesElem == null) {
      writeWsdlTypesElement();
    }

    // Write Element, if problems occur return null.
    if (writeTypeAsElement(type, qname) == null) {
      qname = null;
    }
    return qname;
  }

  /**
   * Write the element definition for a WRAPPED operation.  This will
   * write out any necessary namespace/schema declarations, an an element
   * definition with an internal (anonymous) complexType.  The name of the
   * element will be *foo*Request or *foo*Response depending on whether the
   * request boolean is true.  If the operation contains parameters, then
   * we also generate a &gt;sequence&lt; node underneath the complexType,
   * and return it for later use by writeWrappedParameter() below.
   *
   * @param qname the desired element QName 
   * @param request true if we're writing the request wrapper, false if
   *        writing the response.
   * @param hasParams true if there are parameters, and thus a sequence
   *        node is needed
   * @return a DOM Element for the sequence, inside which we'll write the
   *         parameters as elements, or null if there are no parameters
   */ 
  public Element writeWrapperElement(QName qname,
				     boolean request,
				     boolean hasParams) throws AxisFault {
    // Make sure a types section is present
    if (wsdlTypesElem == null) {
      writeWsdlTypesElement();
    }

    // Write the namespace definition for the wrapper
    writeTypeNamespace(qname);
        
    // Create an <element> for the wrapper
    Element wrapperElement = docHolder.createElement("element");
    writeSchemaElement(qname, wrapperElement);
    wrapperElement.setAttribute("name", qname.getLocalPart());

    // Create an anonymous <complexType> for the wrapper
    Element complexType = docHolder.createElement("complexType");
    wrapperElement.appendChild(complexType);

    // If we have parameters in the operation, create a <sequence>
    // under the complexType and return it.
    if (hasParams) {
      Element sequence = docHolder.createElement("sequence");
      complexType.appendChild(sequence);
      return sequence;
    }
        
    return null;
  }
    
  /**
   * Write a parameter (a sub-element) into a sequence generated by
   * writeWrapperElement() above.
   *
   * @param sequence the &lt;sequence&gt; in which we're writing
   * @param name is the name of an element to add to the wrapper element.
   * @param type is the QName of the type of the element.
   */
  public void writeWrappedParameter(Element sequence, String name,
				    QName type, Class javaType)
    throws AxisFault {

    if (javaType == void.class) {
      return;
    }
        
    if (type == null) {
      type = writeTypeForPart(javaType, type);
    }
        
    if (type == null) {
      // throw an Exception!!
    }
        
    Element childElem;
    if (isAnonymousType(type)) {
      childElem = createElementWithAnonymousType(name, javaType,
						 false, docHolder);
    } else {
      // Create the child <element> and add it to the wrapper <sequence>
      childElem = docHolder.createElement("element");
      childElem.setAttribute("name", name);

      String prefix = namespaces.getCreatePrefix(type.getNamespaceURI());
      String prefixedName = prefix+":"+type.getLocalPart();
      childElem.setAttribute("type", prefixedName);
    }
    sequence.appendChild(childElem);
  }

  private boolean isAnonymousType(QName type) {
    return type.getLocalPart().indexOf(SymbolTable.ANON_TOKEN) != -1;
  }

  /**
   * Create a schema element for the given type
   * @param type the class type
   * @return the QName of the generated Element or problems occur
   */
  private QName writeTypeAsElement(Class type, QName qName) throws AxisFault {
    if (qName == null ||
	Constants.equals(Constants.SOAP_ARRAY, qName)) {
      qName = getTypeQName(type);
    }
    QName typeQName = writeTypeNamespace(type, qName);

    String elementType = writeType(type, qName);
    if (elementType != null) {
      Element element = createElementDecl(qName.getLocalPart(), type, qName, isNullable(type), false);
      if (element != null)
	writeSchemaElement(typeQName,element);
      return qName;
    }
    return null;
  }

  /**
   * write out the namespace declaration and return the type QName for the
   * given <code>Class</code>
   *
   * @param type input Class
   * @param qName qname of the Class
   * @return QName for the schema type representing the class
   */
  private QName writeTypeNamespace(Class type, QName qName) {
    if (qName == null) {
      qName = getTypeQName(type);
    }
    writeTypeNamespace(qName);
    return qName;
  }

  /**
   * write out the namespace declaration.
   *
   * @param qName qname of the type
   */
  private void writeTypeNamespace(QName qName) {
    if (qName != null && !qName.getNamespaceURI().equals("")) {
      String pref = def.getPrefix(qName.getNamespaceURI());
      if (pref == null)
	def.addNamespace(namespaces.getCreatePrefix(qName.getNamespaceURI()),
			 qName.getNamespaceURI());

    }
  }

  /**
   * Return the QName of the specified javaType
   * @param javaType input javaType Class
   * @return QName
   */
  public QName getTypeQName(Class javaType) {
    QName qName = null;

    // Use the typeMapping information to lookup the qName.
    QName dQName = null;
    if (defaultTM != null) {
      dQName = defaultTM.getTypeQName(javaType);
    }
    if (tm != null) {
      qName = tm.getTypeQName(javaType);
    }
    if (qName == null) {
      qName = dQName;
    } else if (qName != null && qName != dQName) {
      // If the TM and default TM resulted in different
      // names, choose qName unless it is a schema namespace.
      // (i.e. prefer soapenc primitives over schema primitives)
      if (Constants.isSchemaXSD(qName.getNamespaceURI())) {
	qName = dQName;
      }
    }

    // If the javaType is an array and the qName is
    // SOAP_ARRAY, construct the QName using the
    // QName of the component type
    if (javaType.isArray() &&
	qName != null &&
	Constants.equals(Constants.SOAP_ARRAY, qName)) {
      Class componentType = javaType.getComponentType();
      // If component namespace uri == targetNamespace
      // Construct ArrayOf<componentLocalPart>
      // Else
      // Construct ArrayOf_<componentPrefix>_<componentLocalPart>
      QName cqName = getTypeQName(componentType);
      if (targetNamespace.equals(cqName.getNamespaceURI())) {
	qName = new QName(
			  targetNamespace,
			  "ArrayOf" + cqName.getLocalPart());
      } else {
	String pre = namespaces.getCreatePrefix(cqName.getNamespaceURI());
	qName = new QName(
			  targetNamespace,
			  "ArrayOf_" + pre + "_" + cqName.getLocalPart());
      }
      return qName;
    }

    // If a qName was not found construct one using the
    // class name information.
    if (qName == null) {
      String pkg = getPackageNameFromFullName(javaType.getName());
      String lcl = getLocalNameFromFullName(javaType.getName());

      String ns = namespaces.getCreate(pkg);
      namespaces.getCreatePrefix(ns);
      String localPart = lcl.replace('$', '_');
      qName = new QName(ns, localPart);
    }

    return qName;
  }

  /**
   * Return a string suitable for representing a given QName in the context
   * of this WSDL document.  If the namespace of the QName is not yet
   * registered, we will register it up in the Definitions.
   *
   * @param qname a QName (typically a type)
   * @return a String containing a standard "ns:localPart" rep of the QName
   */
  public String getQNameString(QName qname) {
    String prefix = namespaces.getCreatePrefix(qname.getNamespaceURI());
    return prefix + ":" + qname.getLocalPart();
  }

  /**
   * Utility method to get the package name from a fully qualified java class name
   * @param full input class name
   * @return package name
   */
  public static String getPackageNameFromFullName(String full) {
    if (full.lastIndexOf('.') < 0)
      return "";
    else
      return full.substring(0, full.lastIndexOf('.'));
  }

  /**
   * Utility method to get the local class name from a fully qualified java class name
   * @param full input class name
   * @return package name
   */
  public static String getLocalNameFromFullName(String full) {
    if (full.lastIndexOf('.') < 0)
      return full;
    else
      return full.substring(full.lastIndexOf('.')+1);
  }

  /**
   * Write out the given Element into the appropriate schema node.
   * If need be create the schema node as well
   *
   * @param qName qName to get the namespace of the schema node
   * @param element the Element to append to the Schema node
   */
  public void writeSchemaElement(QName qName, Element element) 
    throws AxisFault {
    if (wsdlTypesElem == null) {
      try {
	writeWsdlTypesElement();
      } catch (Exception e) {
	log.error(e);
	return;
      }
    }
    String namespaceURI = qName.getNamespaceURI();
    if (namespaceURI == null || namespaceURI.equals("")) {
      throw new AxisFault(Constants.FAULT_SERVER_GENERAL,
			  Messages.getMessage("noNamespace00",
					      qName.toString()),
			  null, null);
    }

    Element schemaElem = null;
    NodeList nl = wsdlTypesElem.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++ ) {
      NamedNodeMap attrs = nl.item(i).getAttributes();
      if (attrs != null) {
	for (int n = 0; n < attrs.getLength(); n++) {
	  Attr a = (Attr)attrs.item(n);
	  if (a.getName().equals("targetNamespace") &&
	      a.getValue().equals(namespaceURI))
	    schemaElem = (Element)nl.item(i);
	}
      }
    }
    if (schemaElem == null) {
      schemaElem = docHolder.createElement("schema");
      wsdlTypesElem.appendChild(schemaElem);
      schemaElem.setAttribute("xmlns", Constants.URI_DEFAULT_SCHEMA_XSD);
      schemaElem.setAttribute("targetNamespace", namespaceURI);

      // Add SOAP-ENC namespace import if necessary
      if (serviceDesc.getStyle() == Style.RPC) {
	Element importElem = docHolder.createElement("import");
	schemaElem.appendChild(importElem);
	importElem.setAttribute("namespace", Constants.URI_DEFAULT_SOAP_ENC);
      }
            
      writeTypeNamespace(qName);
    }
    schemaElem.appendChild(element);
  }

  /**
   * Get the Types element for the WSDL document. If not present, create one
   */
  private void writeWsdlTypesElement() {
    if (wsdlTypesElem == null) {
      // Create a <wsdl:types> element corresponding to the wsdl namespaces.
      wsdlTypesElem =
	docHolder.createElementNS(Constants.NS_URI_WSDL11, "types");
      wsdlTypesElem.setPrefix(Constants.NS_PREFIX_WSDL);
    }
  }

  /**
   * Write a schema representation for the given <code>Class</code>. Recurse
   * through all the public fields as well as fields represented by java
   * bean compliant accessor methods.
   *
   * Then return the qualified string representation of the generated type
   *
   * @param type Class for which to generate schema
   * @return a prefixed string for the schema type
   */
  public String writeType(Class type) throws AxisFault {
    return writeType(type, null);
  }

  /**
   * Write a schema representation for the given <code>Class</code>. Recurse
   * through all the public fields as well as fields represented by java
   * bean compliant accessor methods.
   *
   * Then return the qualified string representation of the generated type
   *
   * @param type Class for which to generate schema
   * @param qName of the type to write
   * @return a prefixed string for the schema type or null if problems occur
   */
  public String writeType(Class type, QName qName) throws AxisFault {
    // Get a corresponding QName if one is not provided
    if (qName == null ||
	Constants.equals(Constants.SOAP_ARRAY, qName)) {
      qName = getTypeQName(type);
    }

    if(!makeTypeElement(type, qName, null)) {
      return null;
    }
    return getQNameString(qName);
  }

  public Element createArrayElement(String componentTypeName) {
    // ComplexType representation of array
    Element complexType = docHolder.createElement("complexType");

    Element complexContent = docHolder.createElement("complexContent");
    complexType.appendChild(complexContent);

    Element restriction = docHolder.createElement("restriction");
    complexContent.appendChild(restriction);
    restriction.setAttribute("base",
			     Constants.NS_PREFIX_SOAP_ENC + ":Array");

    Element attribute = docHolder.createElement("attribute");
    restriction.appendChild(attribute);
    attribute.setAttribute("ref",
			   Constants.NS_PREFIX_SOAP_ENC +":arrayType");
    attribute.setAttribute(Constants.NS_PREFIX_WSDL +":arrayType",
			   componentTypeName);

    return complexType;
  }

  /**
   * Returns true if indicated type matches the JAX-RPC enumeration class.
   * Note: supports JSR 101 version 0.6 Public Draft
   */
  public static boolean isEnumClass(Class cls) {
    try {
      java.lang.reflect.Method m  = cls.getMethod("getValue", null);
      java.lang.reflect.Method m2 = cls.getMethod("toString", null);
      if (m != null && m2 != null) {
	java.lang.reflect.Method m3 =
	  cls.getDeclaredMethod("fromString",
				new Class[] {java.lang.String.class});
	java.lang.reflect.Method m4 =
	  cls.getDeclaredMethod("fromValue",
				new Class[] {m.getReturnType()});

	if (m3 != null &&
	    Modifier.isStatic(m3.getModifiers()) &&
	    Modifier.isPublic(m3.getModifiers()) &&
	    m4 != null &&
	    Modifier.isStatic(m4.getModifiers()) &&
	    Modifier.isPublic(m4.getModifiers())) {
	  // Return false if there is a setValue member method
	  try {
	    if (cls.getMethod("setValue",
			      new Class[] {m.getReturnType()}) == null)
	      return true;
	    return false;
	  } catch (java.lang.NoSuchMethodException e) {
	    return true;
	  }
	}
      }
    } catch (java.lang.NoSuchMethodException e) {}
    return false;
  }

  /**
   * Write Enumeration Complex Type
   * (Only supports enumeration classes of string types)
   * @param qName QName of type.
   * @param cls class of type
   */
  public Element writeEnumType(QName qName, Class cls)
    throws NoSuchMethodException, IllegalAccessException, AxisFault  {

    if (!isEnumClass(cls))
      return null;

    // Get the base type of the enum class
    java.lang.reflect.Method m  = cls.getMethod("getValue", null);
    Class base = m.getReturnType();

    // Create simpleType, restriction elements
    Element simpleType = docHolder.createElement("simpleType");
    simpleType.setAttribute("name", qName.getLocalPart());
    Element restriction = docHolder.createElement("restriction");
    simpleType.appendChild(restriction);
    String baseType = writeType(base, null);
    restriction.setAttribute("base", baseType);

    // Create an enumeration using the field values
    Field[] fields= cls.getDeclaredFields();
    for (int i=0; i < fields.length; i++) {
      Field field = fields[i];
      int mod = field.getModifiers();

      // Inspect each public static final field of the same type
      // as the base
      if (Modifier.isPublic(mod) &&
	  Modifier.isStatic(mod) &&
	  Modifier.isFinal(mod) &&
	  field.getType() == base) {
	// Create an enumeration using the value specified
	Element enumeration = docHolder.createElement("enumeration");
	enumeration.setAttribute("value", field.get(null).toString());
	restriction.appendChild(enumeration);
      }
    }

    return simpleType;
  }

  /**
   * Create Element
   * @param nillable nillable attribute of the element
   * @return the created Element
   */
  public Element createElementDecl(String name,
				   Class javaType,
				   QName typeQName,
				   boolean nillable,
				   boolean omittable) throws AxisFault {
    Element element = docHolder.createElement("element");

    // Generate an element name that matches the type.

    element.setAttribute("name", name);

    if (nillable)
      element.setAttribute("nillable", "true");
    if (omittable) {
      element.setAttribute("minOccurs", "0");
      element.setAttribute("maxOccurs", "1");
    }

    // Write the type for this element, handling anonymous or named
    // types appropriately.
    makeTypeElement(javaType, typeQName, element);
    return element;
  }

  /**
   * Create Element with a given name and type
   * @param elementName the name of the created element
   * @param elementType schema type representation of the element
   * @param nullable nullable attribute of the element
   * @return the created Element
   */
  public Element createElement(String elementName,
			       String elementType,
			       boolean nullable,
			       boolean omittable,
			       Document docHolder) {
    Element element = docHolder.createElement("element");
    element.setAttribute("name", elementName);
    if (nullable)
      element.setAttribute("nillable", "true");
    if (omittable) {
      element.setAttribute("minOccurs", "0");
      element.setAttribute("maxOccurs", "1");
    }
    if (elementType != null)
      element.setAttribute("type", elementType);
    return element;
  }


  /**
   * Create Attribute Element with a given name and type
   * @param elementName the name of the created element
   * @param nullable nullable attribute of the element
   * @return the created Element
   */
  public Element createAttributeElement(String elementName,
					Class javaType,
					QName xmlType,
					boolean nullable,
					Document docHolder) throws AxisFault {
    Element element = docHolder.createElement("attribute");
    element.setAttribute("name", elementName);
    if (nullable)
      element.setAttribute("nillable", "true");

    makeTypeElement(javaType, xmlType, element);
    return element;
  }

  /**
   * Is the given class one of the simple types?  In other words,
   * do we have a mapping for this type which is in the xsd or
   * soap-enc namespaces?
   *
   * @param type input Class
   * @return true if the type is a simple type
   */
  boolean isSimpleType(Class type) {
    QName qname = tm.getTypeQName(type);
    if (qname == null)
      return false;  // No mapping

    String nsURI = qname.getNamespaceURI();
    return (Constants.isSchemaXSD(nsURI) ||
	    Constants.isSOAP_ENC(nsURI));
  }

  /**
   * Is the given class acceptable as an attribute
   * @param type input Class
   * @return true if the type is a simple, enum type or extends SimpleType
   */
  public boolean isAcceptableAsAttribute(Class type) {
    return isSimpleType(type) ||
      isEnumClass(type) ||
      implementsSimpleType(type);
  }

  /**
   * Does the class implement SimpleType
   * @param type input Class
   * @return true if the type implements SimpleType
   */
  boolean implementsSimpleType(Class type) {
    Class[] impls = type.getInterfaces();
    for(int i=0; i<impls.length; i++) {
      if (impls[i] == SimpleType.class) {
	return true;
      }
    }
    return false;
  }
  /**
   * Generates a unique element name for a given namespace of the form
   * el0, el1 ....
   *
   * @param qName the namespace for the generated element
   * @return elementname
   */
  // *** NOT USED? ***
  //
  //    private String generateUniqueElementName(QName qName) {
  //      Integer count = (Integer)schemaUniqueElementNames.get(qName.getNamespaceURI());
  //      if (count == null)
  //        count = new Integer(0);
  //      else
  //        count = new Integer(count.intValue() + 1);
  //      schemaUniqueElementNames.put(qName.getNamespaceURI(), count);
  //      return "el" + count.intValue();
  //    }

  /**
   * Add the type to an ArrayList and return true if the Schema node
   * needs to be generated
   * If the type already exists, just return false to indicate that the type is already
   * generated in a previous iteration
   *
   * @param qName of the type.
   * @return if the type is added returns true,
   * else if the type is already present returns false
   */
  private boolean addToTypesList (QName qName) {
    boolean added = false;
    ArrayList types = (ArrayList)schemaTypes.get(qName.getNamespaceURI());

    // Quick return if schema type (will never add these ourselves)
    if (Constants.isSchemaXSD(qName.getNamespaceURI()) ||
	(Constants.isSOAP_ENC(qName.getNamespaceURI()) &&
	 !"Array".equals(qName.getLocalPart()))) {
      // Make sure we do have the namespace declared, though...
      writeTypeNamespace(qName);
      return false;
    }

    if (types == null) {
      types = new ArrayList();
      types.add(qName.getLocalPart());
      schemaTypes.put(qName.getNamespaceURI(), types);
      added = true;
    }
    else {
      if (!types.contains(qName.getLocalPart())) {
	types.add(qName.getLocalPart());
	added = true;
      }
    }

    // If addded, look at the namespace uri to see if the schema element should be
    // generated.
    if (added) {
      String prefix = namespaces.getCreatePrefix(qName.getNamespaceURI());
      if (prefix.equals(Constants.NS_PREFIX_SOAP_ENV) ||
	  prefix.equals(Constants.NS_PREFIX_SOAP_ENC) ||
	  prefix.equals(Constants.NS_PREFIX_SCHEMA_XSD) ||
	  prefix.equals(Constants.NS_PREFIX_WSDL) ||
	  prefix.equals(Constants.NS_PREFIX_WSDL_SOAP))
	return false;
      else
	return true;
    }
    return false;
  }

  /**
   * Add the element to an ArrayList and return true if the Schema element
   * needs to be generated
   * If the element already exists, just return false to indicate that the type is already
   * generated in a previous iteration
   *
   * @param qName the name space of the element
   * @return if the type is added returns true, else if the type is already present returns false
   */
  private boolean addToElementsList (QName qName) {
    if (qName == null) {
      return false;
    }

    boolean added = false;
    ArrayList elements = (ArrayList)schemaElementNames.get(qName.getNamespaceURI());
    if (elements == null) {
      elements = new ArrayList();
      elements.add(qName.getLocalPart());
      schemaElementNames.put(qName.getNamespaceURI(), elements);
      added = true;
    }
    else {
      if (!elements.contains(qName.getLocalPart())) {
	elements.add(qName.getLocalPart());
	added = true;
      }
    }
    return added;
  }


  /**
   * Determines if the field is nullable. All non-primitives except
   * for byte[] are nillable.
   *
   * @param type input Class
   * @return true if nullable
   */
  public boolean isNullable(Class type) {
    if (type.isPrimitive() ||
	(type.isArray() && type.getComponentType() == byte.class))
      return false;
    else
      return true;
  }


  /** todo ravi: Get rid of Doccument fragment and import node stuuf,
   *  once I have a handle on the wsdl4j mechanism to get at types.
   *
   *  Switch over notes: remove docHolder, docFragment in favor of wsdl4j Types
   */
  //DocumentFragment docFragment;
  Document docHolder;

  private void createDocumentFragment () {
    try {
      this.docHolder = XMLUtils.newDocument();
    }  catch (ParserConfigurationException e) {
      // This should not occur
      throw new InternalException(e);
    }
  }

  public void updateNamespaces() {
    Namespaces namespaces = getNamespaces();
    Iterator nspIterator = namespaces.getNamespaces();
    while(nspIterator.hasNext()) {
      String nsp = (String) nspIterator.next();
      String pref = def.getPrefix(nsp);
      if (pref == null) {
	def.addNamespace(namespaces.getCreatePrefix(nsp), nsp);
      }                
    }
  }
    
  /**
   * Inserts the type fragment into the given wsdl document
   * @param doc
   */
  public void insertTypesFragment(Document doc) {
    updateNamespaces();
    if (wsdlTypesElem != null) {
      // Import the wsdlTypesElement into the doc.
      org.w3c.dom.Node node = doc.importNode(wsdlTypesElem, true);
      // Insert the imported element at the beginning of the document
      doc.getDocumentElement().
	insertBefore(node,
		     doc.getDocumentElement().getFirstChild());
    }
  }

  /**
   * Return the list of classes that we should not emit WSDL for.
   */
  public List getStopClasses() {
    return stopClasses;
  }

  /**
   * Create a DOM Element in this context
   */
  public Element createElement(String elementName)
  {
    return docHolder.createElement(elementName);
  }

  /**
   * isBeanCompatible
   * @param javaType Class
   * @param issueErrors if true, issue messages if not compatible
   * Returns true if it appears that this class is a bean and
   * can be mapped to a complexType
   */
  protected boolean isBeanCompatible(Class javaType,
				     boolean issueErrors) {

    // Must be a non-primitive and non array
    if (javaType.isArray() ||
	javaType.isPrimitive()) {
      if (issueErrors &&
	  !beanCompatErrs.contains(javaType)) {
	log.warn(Messages.getMessage("beanCompatType00",
				     javaType.getName()));
	beanCompatErrs.add(javaType);
      }
      return false;
    }

    // Anything in the java or javax package that
    // does not have a defined mapping is excluded.
    if (javaType.getName().startsWith("java.") ||
	javaType.getName().startsWith("javax.")) {
      if (issueErrors &&
	  !beanCompatErrs.contains(javaType)) {
	log.warn(Messages.getMessage("beanCompatPkg00",
				     javaType.getName()));
	beanCompatErrs.add(javaType);
      }
      return false;
    }

    // Return true if appears to be an enum class
    if (JavaUtils.isEnumClass(javaType)) {
      return true;
    }

    // Must have a default public constructor if not
    // Throwable
    if (!java.lang.Throwable.class.isAssignableFrom(javaType)) {
      try {
	javaType.getConstructor(new Class[] {});
      } catch (java.lang.NoSuchMethodException e) {
	if (issueErrors &&
	    !beanCompatErrs.contains(javaType)) {
	  log.warn(Messages.getMessage("beanCompatConstructor00",
				       javaType.getName()));
	  beanCompatErrs.add(javaType);
	}
	return false;
      }
    }

    // Make sure superclass is compatible
    Class superClass = javaType.getSuperclass();
    if (superClass != null &&
	superClass != java.lang.Object.class &&
	superClass != java.lang.Exception.class &&
	superClass != java.lang.Throwable.class &&
	superClass != java.rmi.RemoteException.class &&
	superClass != org.apache.axis.AxisFault.class &&
	(stopClasses == null ||
	 !(stopClasses.contains(superClass.getName()))) ) {

      if (!isBeanCompatible(superClass, false)) {

	if (issueErrors &&
	    !beanCompatErrs.contains(javaType)) {
	  log.warn(Messages.getMessage("beanCompatExtends00",
				       javaType.getName(),
				       superClass.getName(),
				       javaType.getName()));
	  beanCompatErrs.add(javaType);
	}
	return false;
      }
    }
    return true;
  }

  /**
   * Write an &lt;element&gt; with an anonymous internal ComplexType
   *
   * @param elementName
   * @param fieldType
   * @param omittable
   * @param ownerDocument
   */
  public Element createElementWithAnonymousType(String elementName,
						Class fieldType,
						boolean omittable,
						Document ownerDocument) throws AxisFault {
    Element element = docHolder.createElement("element");
    element.setAttribute("name", elementName);
    if (isNullable(fieldType))
      element.setAttribute("nillable", "true");
    if (omittable) {
      element.setAttribute("minOccurs", "0");
      element.setAttribute("maxOccurs", "1");
    }

    makeTypeElement(fieldType, null, element);

    return element;
  }

  /**
   * Create a schema type element (either simpleType or complexType) for
   * the particular type/qName combination.  If the type is named, we
   * handle inserting the new type into the appropriate &lt;schema&gt;
   * in the WSDL types section.  If the type is anonymous, we append the
   * definition underneath the Element which was passed as the container
   * (typically a field of a higher-level type or a parameter in a wrapped
   * operation).
   *
   * @param type Java type to write
   * @param qName the desired type QName
   * @param containingElement a schema element ("element" or "attribute")
   *             which should either receive a type="" attribute decoration
   *             (for named types) or a child element defining an anonymous
   *             type
   * @return true if the type was already present or was added, false if there was a problem 
   * @throws AxisFault
   */
  private boolean makeTypeElement(Class type,
				  QName qName,
				  Element containingElement) throws AxisFault {
    // Get a corresponding QName if one is not provided
    if (qName == null ||
	Constants.equals(Constants.SOAP_ARRAY, qName)) {
      qName = getTypeQName(type);
    }

    boolean anonymous = isAnonymousType(qName);

    // Can't have an anonymous type outside of a containing element
    if (anonymous && containingElement == null) {
      throw new AxisFault(
			  Messages.getMessage("noContainerForAnonymousType",
					      qName.toString()));
    }

    // If we've already got this type (because it's a native type or
    // because we've already written it), just add the type="" attribute
    // (if appropriate) and return.

    if (!addToTypesList(qName)) {
      if (containingElement != null)
	containingElement.setAttribute("type", getQNameString(qName));
      return true;
    }

    // look up the serializer in the TypeMappingRegistry
    Serializer ser = null;
    SerializerFactory factory = null;
    if (tm != null) {
      factory = (SerializerFactory)tm.getSerializer(type);
    } else {
      factory = (SerializerFactory)defaultTM.getSerializer(type);
    }

    // If no factory is found, use the BeanSerializerFactory
    // if applicable, otherwise issue errors and treat as an anyType
    if (factory == null) {
      if (isEnumClass(type)) {
	factory = new EnumSerializerFactory(type, qName);
      } else if (isBeanCompatible(type, true)) {
	factory = new BeanSerializerFactory(type, qName);
      } else {
	return false;
      }
    }

    if (factory != null) {
      ser = (Serializer)factory.getSerializerAs(Constants.AXIS_SAX);
    }

    // if we can't get a serializer, that is bad.
    if (ser == null) {
      throw new AxisFault(
			  Messages.getMessage("NoSerializer00", type.getName()));
    }

    Element typeEl;
    try {
      typeEl = ser.writeSchema(type, this);
    } catch (Exception e) {
      throw AxisFault.makeFault(e);
    }

    // If this is an anonymous type, just make the type element a child
    // of containingElement.  If not, set the "type" attribute of
    // containingElement to the right QName, and make sure the type is
    // correctly written into the appropriate <schema> element.
    if (anonymous) {
      containingElement.appendChild(typeEl);
    } else {
      if (typeEl != null) {
	typeEl.setAttribute("name", qName.getLocalPart());

	// Write the type in the appropriate <schema>
	writeSchemaElement(qName, typeEl);
      }

      if (containingElement != null)
	containingElement.setAttribute("type", getQNameString(qName));
    }
    return true;
  }
}
