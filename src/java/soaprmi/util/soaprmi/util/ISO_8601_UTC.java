/* -*- mode: Java; c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
/*
 * Copyright (c) 2002 Extreme! Lab, Indiana University. All rights reserved.
 *
 * This software is open source. See the bottom of this file for the licence.
 *
 * $Id: ISO_8601_UTC.java,v 1.2 2002/11/07 23:32:34 aslom Exp $
 */

package soaprmi.util;

/**
 *  ISO 8601 : Date and time parsing and formatting
 *    Utility methods to format date to one of the standard ISO 8601
 *    dat/time formats and to parse given string in ISO 8601 dat/time
 *    format and return a date object
 *
 *  @author Yogesh L. Simmhan [mailto: ysimmhan@cs.indiana.edu]
 *  @version $Revision: 1.2 $ $Date: 2002/11/07 23:32:34 $ (GMT)
 *
 */

import java.util.Date;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;


public class ISO_8601_UTC {

  public static final int BASIC_FORMAT_MSEC = 0;
  public static final int BASIC_FORMAT_COMPACT_MSEC = 1;
  public static final int BASIC_FORMAT_SEC = 2;
  public static final int BASIC_FORMAT_COMPACT_SEC = 3;

  private static final int DEFAULT_FORMAT = BASIC_FORMAT_MSEC;
  private static final int NUM_FORMATS = 4;

  private static final DateFormat[] iso8601UTCFormatter;
  
  static
  {
    iso8601UTCFormatter = new SimpleDateFormat[NUM_FORMATS];
    
    iso8601UTCFormatter[BASIC_FORMAT_MSEC] =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    iso8601UTCFormatter[BASIC_FORMAT_COMPACT_MSEC] =
      new SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS'Z'");
    iso8601UTCFormatter[BASIC_FORMAT_SEC] =
      new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    iso8601UTCFormatter[BASIC_FORMAT_COMPACT_SEC] =
      new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");

    for(int formatId=0; formatId<NUM_FORMATS; formatId++){
      iso8601UTCFormatter[formatId].
	setTimeZone(TimeZone.getTimeZone("UTC"));
    }
  }
  
  public ISO_8601_UTC()
  {
  }
  
  public static String format(Date dateObj, int formatId)
  {
    return (iso8601UTCFormatter[formatId]).format(dateObj);        
  }

  public static String format(Date dateObj)
  {
    return format(dateObj, DEFAULT_FORMAT);
  }
  
    
  public static Date parse(String dateString, int formatId)
    throws ParseException
  {
    Date parsedDate = null;
    try{
      parsedDate = (iso8601UTCFormatter[formatId]).parse(dateString);
    }
    catch(ParseException pe){
      	throw new ParseException("String `"+ dateString +"'did not match " +
				 "the ISO8601 UTC Basic date formats",
				 pe.getErrorOffset());
    }
    return parsedDate;	
  }
  
  public static Date parse(String dateString)
    throws ParseException
  {
    Date parsedDate = null;
    ParseException parseErr = null;
    
    for(int formatId=0; formatId<NUM_FORMATS; formatId++){
      try
      {
	parsedDate = parse(dateString, formatId);
	break;	
      }
      catch(ParseException pe){
	parseErr = pe;	
      }
    }
    if(parsedDate != null)
      return parsedDate;
    else
      throw new ParseException("String `"+ dateString +"'did not match " +
			       "the ISO8601 UTC Basic date formats",
			       parseErr.getErrorOffset());
  }
  
  
  public static void main(String args[]){
    int doWhat = 0;
    String dateArgStr = null;
    long dateArgLong = -1;
    int dateFormatId = -1;
    String errMsg = null;
    

    if(args.length == 0){
      doWhat = 0;      
    }

    if(args.length == 1){
      dateArgStr = args[0];
      doWhat=3;      
    }

    if(args.length == 2){
      try{
	dateArgStr = args[0];
	dateFormatId = new Integer(args[1]).intValue();
	if((dateFormatId < NUM_FORMATS) && (dateFormatId >= 0))
	  doWhat=3;
	else{	  
	  doWhat = -1;
	  errMsg = "Error in DateFormat";
	}
	
      }
      catch(NumberFormatException nfe){
	doWhat = -1;
	errMsg = "Error in DateLong";
      }
    }

    if(args.length > 2){
      doWhat = -1;
      errMsg = "Extra arguments found";
    }
    
    
    if(doWhat>0){
      try{
	dateArgLong = new Long(dateArgStr).longValue();
	doWhat = 1;
      }
      catch(NumberFormatException nfe){
	doWhat = 2;
      }
    }
    
    switch(doWhat){
      case 0:
      {
	Date currentTime_1 = new Date();
	
	System.out.println("LONG   " +
			   currentTime_1.getTime());
	
	System.out.println("TEXT   " +
			   currentTime_1.toString());


	for(int formatId=0; formatId<NUM_FORMATS; formatId++){
	  System.out.println("UTC" + formatId + "   " +
			     ISO_8601_UTC.format(currentTime_1,
						 formatId));
	}
      }
      break;
	
      case 1:
      {
	Date currentTime_1 = new Date(dateArgLong);
	
	if(dateFormatId < 0){
	  System.out.println("LONG   " +
			     currentTime_1.getTime());
	  
	  System.out.println("TEXT   " +
			   currentTime_1.toString());

	  for(int formatId=0; formatId<NUM_FORMATS; formatId++){
	    System.out.println("UTC" + formatId + "   " +
			       ISO_8601_UTC.format(currentTime_1,
						   formatId));
	  }
	}
	else
	  System.out.println("UTC" + dateFormatId + "   " +
			     ISO_8601_UTC.format(currentTime_1,
						 dateFormatId));
      }
      break;
	
      case 2:
      {
	Date currentTime_1;
	try{	  
	  currentTime_1 = ISO_8601_UTC.parse(dateArgStr);
	  
	  if(dateFormatId < 0){
	    System.out.println("LONG   " +
			       currentTime_1.getTime());
	    
	    System.out.println("TEXT   " +
			       currentTime_1.toString());
	    
	    for(int formatId=0; formatId<NUM_FORMATS; formatId++){
	      System.out.println("UTC" + formatId + "   " +
				 ISO_8601_UTC.format(currentTime_1,
						     formatId));
	    }
	  }
	  else
	    System.out.println("UTC" + dateFormatId + "   " +
			       ISO_8601_UTC.format(currentTime_1,
						   dateFormatId));
	}
	catch(ParseException pe){
	  System.err.println("ParseException while parsing "+
			     dateArgStr+"\n"+
			     pe.getMessage());
	  pe.printStackTrace();	  
	}
      }
      break;
	
      default:
      {
	if(errMsg != null){
	  System.err.println("Error: " + errMsg);
	}
	
	System.err.println("Usage: ISO_8601_UTC " +
			   "[ DateString | DateLong [DateFormat]] \n"+
			   " DateString :" +
			   " eg. 2001-10-28T12:34:56.789Z \n" +
			   " DateLong   :" +
			   " eg. 1004272496789 \n" +
			   " DateFormat :" +
			   " [0.." + (NUM_FORMATS-1) + "]");
      }
    }
  }
}

