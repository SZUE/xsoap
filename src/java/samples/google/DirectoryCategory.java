/*
 * This class was automatically generated with 
 * <a href="http://castor.exolab.org">Castor 0.9.3.9+</a>, using an
 * XML Schema.
 * $Id: DirectoryCategory.java,v 1.1 2002/07/07 22:46:40 aslom Exp $
 */

package google;

  //---------------------------------/
 //- Imported classes and packages -/
//---------------------------------/

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;

/**
 * 
 * 
 * @version $Revision: 1.1 $ $Date: 2002/07/07 22:46:40 $
**/
public class DirectoryCategory implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    private java.lang.String _fullViewableName;

    private java.lang.String _specialEncoding;


      //----------------/
     //- Constructors -/
    //----------------/

    public DirectoryCategory() {
        super();
    } //-- google.DirectoryCategory()


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Returns the value of field 'fullViewableName'.
     * 
     * @return the value of field 'fullViewableName'.
    **/
    public java.lang.String getFullViewableName()
    {
        return this._fullViewableName;
    } //-- java.lang.String getFullViewableName() 

    /**
     * Returns the value of field 'specialEncoding'.
     * 
     * @return the value of field 'specialEncoding'.
    **/
    public java.lang.String getSpecialEncoding()
    {
        return this._specialEncoding;
    } //-- java.lang.String getSpecialEncoding() 


    /**
     * Sets the value of field 'fullViewableName'.
     * 
     * @param fullViewableName the value of field 'fullViewableName'
    **/
    public void setFullViewableName(java.lang.String fullViewableName)
    {
        this._fullViewableName = fullViewableName;
    } //-- void setFullViewableName(java.lang.String) 

    /**
     * Sets the value of field 'specialEncoding'.
     * 
     * @param specialEncoding the value of field 'specialEncoding'.
    **/
    public void setSpecialEncoding(java.lang.String specialEncoding)
    {
        this._specialEncoding = specialEncoding;
    } //-- void setSpecialEncoding(java.lang.String) 

    /**
    **/

}
