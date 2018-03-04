/*
 * This class was automatically generated with 
 * <a href="http://castor.exolab.org">Castor 0.9.3.9+</a>, using an
 * XML Schema.
 * $Id: ResultElement.java,v 1.1 2002/07/07 22:46:40 aslom Exp $
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
public class ResultElement implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    private java.lang.String _summary;

    private java.lang.String _URL;

    private java.lang.String _snippet;

    private java.lang.String _title;

    private java.lang.String _cachedSize;

    private boolean _relatedInformationPresent;

    /**
     * keeps track of state for field: _relatedInformationPresent
    **/
    private boolean _has_relatedInformationPresent;

    private java.lang.String _hostName;

    private DirectoryCategory _directoryCategory;

    private java.lang.String _directoryTitle;


      //----------------/
     //- Constructors -/
    //----------------/

    public ResultElement() {
        super();
    } //-- google.ResultElement()


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Returns the value of field 'cachedSize'.
     * 
     * @return the value of field 'cachedSize'.
    **/
    public java.lang.String getCachedSize()
    {
        return this._cachedSize;
    } //-- java.lang.String getCachedSize() 

    /**
     * Returns the value of field 'directoryCategory'.
     * 
     * @return the value of field 'directoryCategory'.
    **/
    public DirectoryCategory getDirectoryCategory()
    {
        return this._directoryCategory;
    } //-- DirectoryCategory getDirectoryCategory() 

    /**
     * Returns the value of field 'directoryTitle'.
     * 
     * @return the value of field 'directoryTitle'.
    **/
    public java.lang.String getDirectoryTitle()
    {
        return this._directoryTitle;
    } //-- java.lang.String getDirectoryTitle() 

    /**
     * Returns the value of field 'hostName'.
     * 
     * @return the value of field 'hostName'.
    **/
    public java.lang.String getHostName()
    {
        return this._hostName;
    } //-- java.lang.String getHostName() 

    /**
     * Returns the value of field 'relatedInformationPresent'.
     * 
     * @return the value of field 'relatedInformationPresent'.
    **/
    public boolean getRelatedInformationPresent()
    {
        return this._relatedInformationPresent;
    } //-- boolean getRelatedInformationPresent() 

    /**
     * Returns the value of field 'snippet'.
     * 
     * @return the value of field 'snippet'.
    **/
    public java.lang.String getSnippet()
    {
        return this._snippet;
    } //-- java.lang.String getSnippet() 

    /**
     * Returns the value of field 'summary'.
     * 
     * @return the value of field 'summary'.
    **/
    public java.lang.String getSummary()
    {
        return this._summary;
    } //-- java.lang.String getSummary() 

    /**
     * Returns the value of field 'title'.
     * 
     * @return the value of field 'title'.
    **/
    public java.lang.String getTitle()
    {
        return this._title;
    } //-- java.lang.String getTitle() 

    /**
     * Returns the value of field 'URL'.
     * 
     * @return the value of field 'URL'.
    **/
    public java.lang.String getURL()
    {
        return this._URL;
    } //-- java.lang.String getURL() 

    /**
    **/
    public boolean hasRelatedInformationPresent()
    {
        return this._has_relatedInformationPresent;
    } //-- boolean hasRelatedInformationPresent() 

    /**
     * Sets the value of field 'cachedSize'.
     * 
     * @param cachedSize the value of field 'cachedSize'.
    **/
    public void setCachedSize(java.lang.String cachedSize)
    {
        this._cachedSize = cachedSize;
    } //-- void setCachedSize(java.lang.String) 

    /**
     * Sets the value of field 'directoryCategory'.
     * 
     * @param directoryCategory the value of field
     * 'directoryCategory'.
    **/
    public void setDirectoryCategory(DirectoryCategory directoryCategory)
    {
        this._directoryCategory = directoryCategory;
    } //-- void setDirectoryCategory(DirectoryCategory) 

    /**
     * Sets the value of field 'directoryTitle'.
     * 
     * @param directoryTitle the value of field 'directoryTitle'.
    **/
    public void setDirectoryTitle(java.lang.String directoryTitle)
    {
        this._directoryTitle = directoryTitle;
    } //-- void setDirectoryTitle(java.lang.String) 

    /**
     * Sets the value of field 'hostName'.
     * 
     * @param hostName the value of field 'hostName'.
    **/
    public void setHostName(java.lang.String hostName)
    {
        this._hostName = hostName;
    } //-- void setHostName(java.lang.String) 

    /**
     * Sets the value of field 'relatedInformationPresent'.
     * 
     * @param relatedInformationPresent the value of field
     * 'relatedInformationPresent'.
    **/
    public void setRelatedInformationPresent(boolean relatedInformationPresent)
    {
        this._relatedInformationPresent = relatedInformationPresent;
        this._has_relatedInformationPresent = true;
    } //-- void setRelatedInformationPresent(boolean) 

    /**
     * Sets the value of field 'snippet'.
     * 
     * @param snippet the value of field 'snippet'.
    **/
    public void setSnippet(java.lang.String snippet)
    {
        this._snippet = snippet;
    } //-- void setSnippet(java.lang.String) 

    /**
     * Sets the value of field 'summary'.
     * 
     * @param summary the value of field 'summary'.
    **/
    public void setSummary(java.lang.String summary)
    {
        this._summary = summary;
    } //-- void setSummary(java.lang.String) 

    /**
     * Sets the value of field 'title'.
     * 
     * @param title the value of field 'title'.
    **/
    public void setTitle(java.lang.String title)
    {
        this._title = title;
    } //-- void setTitle(java.lang.String) 

    /**
     * Sets the value of field 'URL'.
     * 
     * @param URL the value of field 'URL'.
    **/
    public void setURL(java.lang.String URL)
    {
        this._URL = URL;
    } //-- void setURL(java.lang.String) 


}
