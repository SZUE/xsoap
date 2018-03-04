/*
 * This class was automatically generated with 
 * <a href="http://castor.exolab.org">Castor 0.9.3.9+</a>, using an
 * XML Schema.
 * $Id: GoogleSearchResult.java,v 1.1 2002/07/07 22:46:40 aslom Exp $
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
public class GoogleSearchResult implements java.io.Serializable {


      //--------------------------/
     //- Class/Member Variables -/
    //--------------------------/

    private boolean _documentFiltering;

    /**
     * keeps track of state for field: _documentFiltering
    **/
    private boolean _has_documentFiltering;

    private java.lang.String _searchComments;

    private int _estimatedTotalResultsCount;

    /**
     * keeps track of state for field: _estimatedTotalResultsCount
    **/
    private boolean _has_estimatedTotalResultsCount;

    private boolean _estimateIsExact;

    /**
     * keeps track of state for field: _estimateIsExact
    **/
    private boolean _has_estimateIsExact;

    private ResultElement[] _resultElements;

    private java.lang.String _searchQuery;

    private int _startIndex;

    /**
     * keeps track of state for field: _startIndex
    **/
    private boolean _has_startIndex;

    private int _endIndex;

    /**
     * keeps track of state for field: _endIndex
    **/
    private boolean _has_endIndex;

    private java.lang.String _searchTips;

    private DirectoryCategory[] _directoryCategories;

    private double _searchTime;

    /**
     * keeps track of state for field: _searchTime
    **/
    private boolean _has_searchTime;


      //----------------/
     //- Constructors -/
    //----------------/

    public GoogleSearchResult() {
        super();
    } //-- google.GoogleSearchResult()


      //-----------/
     //- Methods -/
    //-----------/

    /**
     * Returns the value of field 'directoryCategories'.
     * 
     * @return the value of field 'directoryCategories'.
    **/
    public DirectoryCategory[] getDirectoryCategories()
    {
        return this._directoryCategories;
    } //-- DirectoryCategories getDirectoryCategories() 

    /**
     * Returns the value of field 'documentFiltering'.
     * 
     * @return the value of field 'documentFiltering'.
    **/
    public boolean getDocumentFiltering()
    {
        return this._documentFiltering;
    } //-- boolean getDocumentFiltering() 

    /**
     * Returns the value of field 'endIndex'.
     * 
     * @return the value of field 'endIndex'.
    **/
    public int getEndIndex()
    {
        return this._endIndex;
    } //-- int getEndIndex() 

    /**
     * Returns the value of field 'estimateIsExact'.
     * 
     * @return the value of field 'estimateIsExact'.
    **/
    public boolean getEstimateIsExact()
    {
        return this._estimateIsExact;
    } //-- boolean getEstimateIsExact() 

    /**
     * Returns the value of field 'estimatedTotalResultsCount'.
     * 
     * @return the value of field 'estimatedTotalResultsCount'.
    **/
    public int getEstimatedTotalResultsCount()
    {
        return this._estimatedTotalResultsCount;
    } //-- int getEstimatedTotalResultsCount() 

    /**
     * Returns the value of field 'resultElements'.
     * 
     * @return the value of field 'resultElements'.
    **/
    public ResultElement[] getResultElements()
    {
        return this._resultElements;
    } //-- ResultElements getResultElements() 

    /**
     * Returns the value of field 'searchComments'.
     * 
     * @return the value of field 'searchComments'.
    **/
    public java.lang.String getSearchComments()
    {
        return this._searchComments;
    } //-- java.lang.String getSearchComments() 

    /**
     * Returns the value of field 'searchQuery'.
     * 
     * @return the value of field 'searchQuery'.
    **/
    public java.lang.String getSearchQuery()
    {
        return this._searchQuery;
    } //-- java.lang.String getSearchQuery() 

    /**
     * Returns the value of field 'searchTime'.
     * 
     * @return the value of field 'searchTime'.
    **/
    public double getSearchTime()
    {
        return this._searchTime;
    } //-- double getSearchTime() 

    /**
     * Returns the value of field 'searchTips'.
     * 
     * @return the value of field 'searchTips'.
    **/
    public java.lang.String getSearchTips()
    {
        return this._searchTips;
    } //-- java.lang.String getSearchTips() 

    /**
     * Returns the value of field 'startIndex'.
     * 
     * @return the value of field 'startIndex'.
    **/
    public int getStartIndex()
    {
        return this._startIndex;
    } //-- int getStartIndex() 

    /**
    **/
    public boolean hasDocumentFiltering()
    {
        return this._has_documentFiltering;
    } //-- boolean hasDocumentFiltering() 

    /**
    **/
    public boolean hasEndIndex()
    {
        return this._has_endIndex;
    } //-- boolean hasEndIndex() 

    /**
    **/
    public boolean hasEstimateIsExact()
    {
        return this._has_estimateIsExact;
    } //-- boolean hasEstimateIsExact() 

    /**
    **/
    public boolean hasEstimatedTotalResultsCount()
    {
        return this._has_estimatedTotalResultsCount;
    } //-- boolean hasEstimatedTotalResultsCount() 

    /**
    **/
    public boolean hasSearchTime()
    {
        return this._has_searchTime;
    } //-- boolean hasSearchTime() 

    /**
    **/
    public boolean hasStartIndex()
    {
        return this._has_startIndex;
    } //-- boolean hasStartIndex() 

    /**
    **/

    /**
     * Sets the value of field 'directoryCategories'.
     * 
     * @param directoryCategories the value of field
     * 'directoryCategories'.
    **/
    public void setDirectoryCategories(DirectoryCategory[] directoryCategories)
    {
        this._directoryCategories = directoryCategories;
    } //-- void setDirectoryCategories(DirectoryCategories) 

    /**
     * Sets the value of field 'documentFiltering'.
     * 
     * @param documentFiltering the value of field
     * 'documentFiltering'.
    **/
    public void setDocumentFiltering(boolean documentFiltering)
    {
        this._documentFiltering = documentFiltering;
        this._has_documentFiltering = true;
    } //-- void setDocumentFiltering(boolean) 

    /**
     * Sets the value of field 'endIndex'.
     * 
     * @param endIndex the value of field 'endIndex'.
    **/
    public void setEndIndex(int endIndex)
    {
        this._endIndex = endIndex;
        this._has_endIndex = true;
    } //-- void setEndIndex(int) 

    /**
     * Sets the value of field 'estimateIsExact'.
     * 
     * @param estimateIsExact the value of field 'estimateIsExact'.
    **/
    public void setEstimateIsExact(boolean estimateIsExact)
    {
        this._estimateIsExact = estimateIsExact;
        this._has_estimateIsExact = true;
    } //-- void setEstimateIsExact(boolean) 

    /**
     * Sets the value of field 'estimatedTotalResultsCount'.
     * 
     * @param estimatedTotalResultsCount the value of field
     * 'estimatedTotalResultsCount'.
    **/
    public void setEstimatedTotalResultsCount(int estimatedTotalResultsCount)
    {
        this._estimatedTotalResultsCount = estimatedTotalResultsCount;
        this._has_estimatedTotalResultsCount = true;
    } //-- void setEstimatedTotalResultsCount(int) 

    /**
     * Sets the value of field 'resultElements'.
     * 
     * @param resultElements the value of field 'resultElements'.
    **/
    public void setResultElements(ResultElement[] resultElements)
    {
        this._resultElements = resultElements;
    } //-- void setResultElements(ResultElements) 

    /**
     * Sets the value of field 'searchComments'.
     * 
     * @param searchComments the value of field 'searchComments'.
    **/
    public void setSearchComments(java.lang.String searchComments)
    {
        this._searchComments = searchComments;
    } //-- void setSearchComments(java.lang.String) 

    /**
     * Sets the value of field 'searchQuery'.
     * 
     * @param searchQuery the value of field 'searchQuery'.
    **/
    public void setSearchQuery(java.lang.String searchQuery)
    {
        this._searchQuery = searchQuery;
    } //-- void setSearchQuery(java.lang.String) 

    /**
     * Sets the value of field 'searchTime'.
     * 
     * @param searchTime the value of field 'searchTime'.
    **/
    public void setSearchTime(double searchTime)
    {
        this._searchTime = searchTime;
        this._has_searchTime = true;
    } //-- void setSearchTime(double) 

    /**
     * Sets the value of field 'searchTips'.
     * 
     * @param searchTips the value of field 'searchTips'.
    **/
    public void setSearchTips(java.lang.String searchTips)
    {
        this._searchTips = searchTips;
    } //-- void setSearchTips(java.lang.String) 

    /**
     * Sets the value of field 'startIndex'.
     * 
     * @param startIndex the value of field 'startIndex'.
    **/
    public void setStartIndex(int startIndex)
    {
        this._startIndex = startIndex;
        this._has_startIndex = true;
    } //-- void setStartIndex(int) 

}
