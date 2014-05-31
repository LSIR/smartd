package gsn.http;

import gsn.Main;
import gsn.Mappings;
import gsn.beans.DataTypes;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.http.ac.User;
import gsn.storage.DataEnumerator;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.w3c.tidy.ParserImpl.ParseInline;

import weka.classifiers.Classifier;
import weka.classifiers.functions.LinearRegression;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;


public class SmartDValueDistribution implements RequestHandler {

    private static transient Logger logger = Logger.getLogger(SmartDValueDistribution.class);

    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	
    	logger.warn("START_DISTRIBUTION");
    	
    	
    	String electricDataTable = "electric_data";
    	StringBuilder sb = null;
    	
    	String meterID = request.getParameter("id");        
    	String bin = request.getParameter("bin");    	
    	String binInput = request.getParameter("binInput"); 
    	String groupBy = "";
    	
    	double sizeOfBin = 0;    	
        
        String startTime = request.getParameter("startTime");                 
        String endTime= request.getParameter("endTime");
        
        Integer startTimeYear, startTimeMonth, startTimeDay,startTimeHour, startTimeMinute;
        Integer endTimeYear, endTimeMonth, endTimeDay, endTimeHour, endTimeMinute;
        
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        Date start = null, end=null;
        
        String condition = "";
        String timeCondition=null;
        
        //--------------start time--------------------------
        
        if (startTime != null && startTime.trim().length() != 0)
        {
        	try {
    			start = formatter.parse(startTime);
    		} catch (ParseException e1) {
    			e1.printStackTrace();
    		}   
        	String[] parseStartDateTime = startTime.split("T");
        	
        	String[] parseStartDate = parseStartDateTime[0].split("-");
        	startTimeYear = Integer.valueOf(parseStartDate[0]);
    		startTimeMonth = Integer.valueOf(parseStartDate[1]);
    		startTimeDay = Integer.valueOf(parseStartDate[2]);
    		
        	
        	String[] parseTime = parseStartDateTime[1].split(":");
        	startTimeHour = Integer.valueOf(parseTime[0]);
        	startTimeMinute = Integer.valueOf(parseTime[1]);
        	  
        	
        	startTime = String.valueOf(start.getTime()/1000);
        	
        	if(startTimeYear==2010 || startTimeYear==2009 || startTimeYear==2011){            	
        		timeCondition =  startTime+"<=TIMESTAMP";  
        		
        	}
        	else {
        		
        	}
        }
        else {
        	
        }
        
        //--------------end time-------------------------------------------
        
        if (endTime != null && endTime.trim().length() != 0)
        {
        	try {
    			end = formatter.parse(endTime);
    		} catch (ParseException e1) {
    			e1.printStackTrace();
    		}
        	
        	String[] parseEndDateTime = endTime.split("T");
        	
        	String[] parseDate = parseEndDateTime[0].split("-");
        	endTimeYear = Integer.valueOf(parseDate[0]);
    		endTimeMonth = Integer.valueOf(parseDate[1]);
    		endTimeDay = Integer.valueOf(parseDate[2]);
        	
        	String[] parseTime = parseEndDateTime[1].split(":");
        	endTimeHour = Integer.valueOf(parseTime[0]);
        	endTimeMinute = Integer.valueOf(parseTime[1]);
        	
        	endTime = String.valueOf(end.getTime()/1000);
        	
        	if(endTimeYear==2009||endTimeYear==2010 || endTimeYear==2011){ 
        		
        		if(timeCondition.equals(""))
        			timeCondition =  endTime+">=TIMESTAMP";
        		else
        			timeCondition =  timeCondition+" and "+endTime+">=TIMESTAMP";
        	}
        	else {
        		
        	}
        }
         else {
        	 
         }
        
        
        if(timeCondition==null)
    		condition =  meterID+"=METER_ID"; 
		else	
			condition =  timeCondition+" and "+meterID+"=METER_ID";
        
        Connection con = null;
        Statement stm = null;
        ResultSet rs = null;

		Properties prop = new Properties();
		prop.load(new FileInputStream("smartd.config"));
		String dbHost     = prop.getProperty("dbHost");
		String dbDatabase = prop.getProperty("dbDatabase");
		String user 	  = prop.getProperty("dbUsername");
		String password   = prop.getProperty("dbPassword");		
		String url        = "jdbc:mysql://" + dbHost + "/" + dbDatabase;

        try {
        	
            con = DriverManager.getConnection(url, user, password);
            stm = con.createStatement();
            
            if(bin.equals("binSize")){
        		
            	sizeOfBin = Double.valueOf(binInput);
        		//groupBy = " group by VALUE div "+ sizeOfBin;       		
        	}
        	
        	else if(bin.equals("binNum")){
        		
        		double max = 0;
        		String query = "select max(VALUE) as maximum from " + electricDataTable + " where " + condition;
        		logger.warn( query );
        		rs = stm.executeQuery(query);
				while (rs.next()) {
					max = rs.getFloat("maximum");
				}
				double numOfBin = Double.valueOf(binInput);
				sizeOfBin = max/numOfBin;
				sizeOfBin = Math.ceil( sizeOfBin * 10 ) / 10;
				//groupBy = " group by VALUE div "+ sizeOfBin;        		
        	}
            groupBy = " group by floor( VALUE/"+ sizeOfBin + " )";
            String query = "select count(*) from " + electricDataTable + " where " + condition;
            logger.warn( query );
            rs = stm.executeQuery( query );
            int resultNum = 0 ;
			while (rs.next()) {
				
				 resultNum = rs.getInt("count(*)");
			}
			
			String sizeOfBinSt = String.valueOf(sizeOfBin);
			String[] parseBin = sizeOfBinSt.split("\\.");
			
			String floatingPoints = parseBin[1];
			int floatingPointNum = floatingPoints.length();
			
			double binPlot = 0.0;

			query = "select VALUE , count(VALUE) from " + electricDataTable + " where " + condition + groupBy;
            logger.warn(query);
            rs = stm.executeQuery(query);
            sb = new StringBuilder("<result>\n");
            
			while (rs.next()) {
				
				binPlot = Math.round(binPlot * Math.pow(10,floatingPointNum))/Math.pow(10,floatingPointNum);
				
				double prob = rs.getFloat("count(VALUE)")/(double) resultNum;
				
				sb.append("<stream-element>\n");
        		sb.append("<field name=\"").append("bin\">").
        		append(String.valueOf(binPlot)).
        		append("</field>\n");
        		sb.append("<field name=\"").append("count\">").
        		append(String.valueOf(prob)).
        		append("</field>\n");
        		sb.append("</stream-element>\n");  
        		
        		binPlot = binPlot + sizeOfBin;
				
			}
			
			sb.append("</result>"); 
			//..logger.warn( sb.toString() );
            
            
        } catch (SQLException ex) {
            logger.warn("SQLException:"+ex);

        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stm != null) {
                    stm.close();
                }
                if (con != null) {
                    con.close();
                }

            } catch (SQLException ex) {
            	logger.warn("SQLException:"+ex);
            }
        }
                                
        
        response.setHeader("Cache-Control", "no-store");
        response.setDateHeader("Expires", 0);
        response.setHeader("Pragma", "no-cache");
        response.getWriter().write(sb.toString());
        
        
        logger.warn("END_DISTRIBUTION");            
    	
    	
    }    


    public boolean isValid(HttpServletRequest request, HttpServletResponse response) throws IOException {
	
   
    	return true;
    }

}
