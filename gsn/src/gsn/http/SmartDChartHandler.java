package gsn.http;

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
import java.util.Date;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

public class SmartDChartHandler implements RequestHandler {

    private static transient Logger logger = Logger.getLogger(SmartDChartHandler.class);

    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {

    	logger.warn("START_1");
    	
        String electricDataTable = request.getParameter("name");
        
        String vsIDs = request.getParameter("ids");
        String[] parseIDs = null;        
        
        StringBuilder sb = null;
        
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
        Date start = null, end=null;
        
        String vsStartTime = request.getParameter("startTime");                 
        String vsEndTime= request.getParameter("endTime");          
        
        String vsInterval = request.getParameter("interval");
        String vsNormalization = request.getParameter("normalization");
        String vsIntervalAggregation = request.getParameter("intervalAggregation");
        String vsAggregation = request.getParameter("aggregation");
        
        String startTime = "";
        String endTime = "";
        
        String timeCondition=null;
        
        
        //------------interval-----------------------------------------       
        if (vsInterval == null || vsInterval.trim().length() == 0){        	
        	vsInterval = "30";
        }          
        
        
        //-------------interval aggregation function-------------------------       
        if (vsIntervalAggregation == null || vsIntervalAggregation.trim().length() == 0)
        	vsIntervalAggregation =  "avg";  
        
        
        //-------------aggregation function-------------------------       
        if (vsAggregation == null || vsAggregation.trim().length() == 0)
        	vsAggregation =  "avg";  
        
        
        //--------------start time--------------------------        
        if (vsStartTime != null && vsStartTime.trim().length() != 0)
        {
        	try {
    			start = formatter.parse(vsStartTime);
    		} catch (ParseException e1) {
    			e1.printStackTrace();
    		}   
        	startTime = String.valueOf(start.getTime()/1000);
        	timeCondition =  startTime+"<=TIMESTAMP";         		
        }
        
        
        //--------------end time-------------------------------------------        
        if (vsEndTime != null && vsEndTime.trim().length() != 0)
        {
        	try {
    			end = formatter.parse(vsEndTime);
    		} catch (ParseException e1) {
    			e1.printStackTrace();
    		}        	
        	endTime = String.valueOf(end.getTime()/1000);
        	if(timeCondition.equals(""))
        		timeCondition =  endTime+">=TIMESTAMP";
        	else
        		timeCondition =  timeCondition+" and "+endTime+">=TIMESTAMP";       	
        }
        
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
        
        int userNum = 0;		
		ArrayList<String> users = new ArrayList<String>();

        try {
        	
            con = DriverManager.getConnection(url, user, password);
            stm = con.createStatement();
            
        
            if (vsIDs != null && vsIDs.trim().length() != 0){
        	
				if (vsIDs.equalsIgnoreCase("all")) {

					// retrieve all of the meter ids and save them in users
					// array list
					rs = stm.executeQuery("select distinct METER_ID from " + electricDataTable);

					while (rs.next()) {

						users.add(rs.getString("METER_ID"));
						userNum++;
					}

					parseIDs = new String[userNum];
					for (int id = 0; id < userNum; id++) {
						parseIDs[id] = users.get(id);
					}

					/*
					 * for(int j = 0 ; j<parseIDs.length;j++)
					 * logger.warn("ID="+parseIDs[j]);
					 */
				}

				else {
					parseIDs = vsIDs.split(",");
					//.. logger.warn("parseIDs: "+vsIDs);
				}
				
            } else {       	
            	logger.warn("Please enter the use IDs!");        	
            } 	
        
            
        //-------------normalization-----------------------       
        if (vsNormalization == null || vsNormalization.trim().length() == 0)
        	vsNormalization =  "false";
       
        
        /** get the data required for plotting **/
        ArrayList<Object> resultData = getData(parseIDs, vsInterval, vsAggregation, vsIntervalAggregation, vsNormalization, timeCondition, electricDataTable, stm, rs);   
        String[] resultTiming = (String[]) resultData.get(0);
    	double[] resultValue = (double[]) resultData.get(1);
        
    	
    	/** get the forecasted data **/
    	String query = "select max(TIMESTAMP) from " + electricDataTable + ";" ;
    	rs = stm.executeQuery(query);
    	rs.next();
    	startTime = rs.getString(1); // renew startTime, endTime does not change 
    	boolean frcFlag = false;
    	
    	if ( Long.parseLong(startTime) < Long.parseLong(endTime) ) {
    		frcFlag = true;
    	}
    	
		String[] resultTimingFrc = null;
		double[] resultValueFrc = null;
    	
    	if ( frcFlag==true ) {
    		String timeConditionFrc = "TIMESTAMP >= " + startTime + " and TIMESTAMP <= " + endTime;
    		//..logger.warn(timeConditionFrc);
    		String electricDataTableFrc = electricDataTable + "_forecast";
    		ArrayList<Object> resultDataFrc = getData(parseIDs, vsInterval, vsAggregation, vsIntervalAggregation, vsNormalization, timeConditionFrc, electricDataTableFrc, stm, rs);   
    		resultTimingFrc = (String[]) resultDataFrc.get(0);
    		resultValueFrc = (double[]) resultDataFrc.get(1);
    	}
    	
    	
        /* 
        String st = "";
        for(int resultIndex=0; resultIndex<resultNum;resultIndex++){       	
        	st = st + resultValue[resultIndex] + ","; 
        }       
        System.out.println(st);
        */
                
        
    	
        //----------------------------------result as XML------------------------------------------
        
        sb = new StringBuilder("<result>\n");
        int lastIdx = 0;
        for(int i=0; i<resultTiming.length; i++){
        	
        	if(resultTiming[i]!=null){        	
        		sb.append("<stream-element>\n");
        		sb.append("<field name=\"").append("TIMESTAMP\">").
        		append(StringEscapeUtils.escapeXml(resultTiming[i])).
        		append("</field>\n");
        		sb.append("<field name=\"").append("VALUE\">").
        		append(StringEscapeUtils.escapeXml(String.valueOf(resultValue[i]))).
        		append("</field>\n");
        		sb.append("</stream-element>\n");
        		lastIdx = i;
        	}
        	
        }
        
       	
        // add forecasted value	       
        if ( frcFlag == true ) {

    		sb.append("<forecast-element>\n");
    		sb.append("<field name=\"").append("TIMESTAMP\">").
    		append(StringEscapeUtils.escapeXml(resultTiming[lastIdx])).
    		append("</field>\n");
    		sb.append("<field name=\"").append("VALUE\">").
    		append(StringEscapeUtils.escapeXml(String.valueOf(resultValue[lastIdx]))).
    		append("</field>\n");
    		sb.append("</forecast-element>\n");       

            for(int i=0; i<resultTimingFrc.length;i++){
            	
            	if(resultTimingFrc[i]!=null){        	
            		sb.append("<forecast-element>\n");
            		sb.append("<field name=\"").append("TIMESTAMP\">").
            		append(StringEscapeUtils.escapeXml(resultTimingFrc[i])).
            		append("</field>\n");
            		sb.append("<field name=\"").append("VALUE\">").
            		append(StringEscapeUtils.escapeXml(String.valueOf(resultValueFrc[i]))).
            		append("</field>\n");
            		sb.append("</forecast-element>\n");       
            	}
            	
            }
	
        }
                
       
        sb.append("</result>");                 
        /*..
        for (int i=0; i<resultTimingFrc.length; i++) {
        	logger.warn(resultTimingFrc[i]);
        }
        */
        response.setHeader("Cache-Control", "no-store");
        response.setDateHeader("Expires", 0);
        response.setHeader("Pragma", "no-cache");
        response.getWriter().write(sb.toString());
        
        //..logger.warn(sb.toString());
        logger.warn("END_1");
        
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
        
        
    }

    
    public boolean isValid(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	
        /*String vsName = request.getParameter("name");
        
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        if (vsName == null || vsName.trim().length() == 0) {
            response.sendError(WebConstants.MISSING_VSNAME_ERROR, "The virtual sensor name is missing");
            return false;
        }
        VSensorConfig sensorConfig = Mappings.getVSensorConfig(vsName);
        if (sensorConfig == null) {
            response.sendError(WebConstants.ERROR_INVALID_VSNAME, "The specified virtual sensor doesn't exist.");
            return false;
        }
        
        if (Main.getContainerConfig().isAcEnabled() == true) {
            if (user != null) // meaning, that a login session is active, otherwise we couldn't get there
                if (user.hasReadAccessRight(vsName) == false && user.isAdmin() == false)  // ACCESS_DENIED
                {
                    response.sendError(WebConstants.ACCESS_DENIED, "Access denied to the specified virtual sensor .");
                    return false;
                }
        }*/

        return true;
    }


    private ArrayList<Object> getData(String[] parseIDs, String vsInterval, String vsAggregation, String vsIntervalAggregation, String vsNormalization, String timeCondition, String electricDataTable, Statement stm, ResultSet rs) throws SQLException {

    	int resultTimingIndex = 0;
        int resultValueIndex = 0;

        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        double sum = 0.0;
        double aggrValue = 0.0;
        double readingValue = 0.0;

        
        String vsCondition = "";
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");

        int resultNum = 0;     
        String[] resultTiming = null;
    	double[] resultValue = null;

    	//TODO: this code could be shortened using the aggregation facility from the database  
    	for(int id=0; id<parseIDs.length ;id++){
        	
        	resultValueIndex = 0;
        	min = Double.MAX_VALUE;
        	max = Double.MIN_VALUE;
        	sum = 0.0;
        	
        	int idNum = id+1;
        	
        	String meterID = parseIDs[id];        	
        	vsCondition = "";
        	
        	if(timeCondition==null)
        		vsCondition =  meterID+"=METER_ID"; 
    		else{	
    			vsCondition =  timeCondition+" and "+meterID+"=METER_ID";
    			
    		}
        	
        	if(id==0){
        		
        		 rs = stm.executeQuery("select * from " + electricDataTable + " where " + vsCondition );                

                 while (rs.next()) {              	
                 	
                 	resultNum++;                
                 }        	
                 
                 if (vsInterval.equals("30")) {
						
					}

					else if (vsInterval.equals("hour")) {
						resultNum = resultNum / 2 + 1;
					}

					else if (vsInterval.equals("day")) {
						
						resultNum = resultNum / (2*24) + 1;
					}

					else if (vsInterval.equals("week")) {
						resultNum = resultNum / (2*24*7) + 1;

					}

					else if (vsInterval.equals("month")) {
						resultNum = resultNum / (2*24*7*4) + 1;

					}
                 
                 if(resultNum>0){
            	
                	 resultTiming = new String[resultNum];
                	 resultValue = new double[resultNum];
                 }
        	
        	}
        	
        	rs = stm.executeQuery("select * from " + electricDataTable + " where " + vsCondition + " order by timestamp ASC" );                
            int counterDateTime = 1;
            int counterValue = 1;

			while (rs.next()) {

					if (id == 0) {

						Date dt = new Date(rs.getLong("TIMESTAMP") * 1000);
						if (vsInterval.equals("30")) {
							resultTiming[resultTimingIndex] = formatter.format(dt); // this has to be moved outside?
							resultTimingIndex++; // this has to be moved outside?
							counterDateTime = 0; // this has to be moved outside?
						}

						else if (vsInterval.equals("hour")
								&& (counterDateTime % 2 == 0)) {
							resultTiming[resultTimingIndex] = formatter.format(dt);
							resultTimingIndex++;
							counterDateTime = 0;

						}

						else if (vsInterval.equals("day")
								&& (counterDateTime % (2 * 24) == 0)) {
							resultTiming[resultTimingIndex] = formatter.format(dt);
							resultTimingIndex++;
							counterDateTime = 0;

						}

						else if (vsInterval.equals("week")
								&& (counterDateTime % (2 * 24 * 7) == 0)) {
							resultTiming[resultTimingIndex] = formatter.format(dt);
							resultTimingIndex++;
							counterDateTime = 0;

						}

						else if (vsInterval.equals("month")
								&& (counterDateTime % (2 * 24 * 7 * 4) == 0)) {
							resultTiming[resultTimingIndex] = formatter.format(dt);
							resultTimingIndex++;
							counterDateTime = 0;

						}

						counterDateTime++;
					}

					double val = Double.valueOf(rs.getDouble("VALUE"));

					if (vsIntervalAggregation.equals("sum"))
						sum = sum + val;

					else if (vsIntervalAggregation.equals("avg"))
						sum = sum + val;

					else if (vsIntervalAggregation.equals("min")) {
						if (val < min)
							min = val;
					}

					else if (vsIntervalAggregation.equals("max")) {
						if (val > max)
							max = val;
					}

					if (vsInterval.equals("30")) {

						readingValue = rs.getDouble("VALUE");

						if (vsAggregation.equals("min")) {

							if (id == 0) {
								resultValue[resultValueIndex] = readingValue;
							}

							else if (readingValue < resultValue[resultValueIndex])
								resultValue[resultValueIndex] = readingValue;

							resultValueIndex++;

						}

						else if (vsAggregation.equals("max")) {

							if (id == 0) {
								resultValue[resultValueIndex] = readingValue;
							}

							else if (readingValue > resultValue[resultValueIndex])
								resultValue[resultValueIndex] = readingValue;

							resultValueIndex++;
						}

						else if (vsAggregation.equals("sum")) {

							if (id == 0) {
								resultValue[resultValueIndex] = readingValue;
							}

							else
								resultValue[resultValueIndex] = resultValue[resultValueIndex]
										+ readingValue;

							resultValueIndex++;
						}

						else if (vsAggregation.equals("avg")) {

							if (id == 0) {
								resultValue[resultValueIndex] = readingValue;
							}

							else
								resultValue[resultValueIndex] = resultValue[resultValueIndex]
										+ readingValue;

							if (id == (parseIDs.length) - 1) {
								resultValue[resultValueIndex] = resultValue[resultValueIndex]
										/ (double) idNum;
							}
							resultValueIndex++;

						}

					}

					else if (vsInterval.equals("hour")
							&& (counterValue % 2 == 0)) {

						if (vsIntervalAggregation.equals("sum")) {
							aggrValue = sum;
							sum = 0.0;
						}

						else if (vsIntervalAggregation.equals("avg")) {
							if (counterValue == 0)
								aggrValue = sum;
							else
								aggrValue = sum / (double) counterValue;

							sum = 0.0;
						}

						else if (vsIntervalAggregation.equals("min")) {
							aggrValue = min;
							min = Double.MAX_VALUE;
						}

						else if (vsIntervalAggregation.equals("max")) {
							aggrValue = max;
							max = Double.MIN_VALUE;
						}

						readingValue = aggrValue;

						if (vsAggregation.equals("min")) {

							if (id == 0) {
								resultValue[resultValueIndex] = readingValue;
							}

							else if (readingValue < resultValue[resultValueIndex])
								resultValue[resultValueIndex] = readingValue;

							resultValueIndex++;

						}

						else if (vsAggregation.equals("max")) {

							if (id == 0) {
								resultValue[resultValueIndex] = readingValue;
							}

							else if (readingValue > resultValue[resultValueIndex])
								resultValue[resultValueIndex] = readingValue;

							resultValueIndex++;
						}

						else if (vsAggregation.equals("sum")) {

							if (id == 0) {
								resultValue[resultValueIndex] = readingValue;
							}

							else
								resultValue[resultValueIndex] = resultValue[resultValueIndex]
										+ readingValue;

							resultValueIndex++;
						}

						else if (vsAggregation.equals("avg")) {

							if (id == 0) {
								resultValue[resultValueIndex] = readingValue;
							}

							else
								resultValue[resultValueIndex] = resultValue[resultValueIndex]
										+ readingValue;

							if (id == (parseIDs.length) - 1) {
								resultValue[resultValueIndex] = resultValue[resultValueIndex]
										/ (double) idNum;
							}
							resultValueIndex++;

						}

						counterValue = 0;

					}

					else if (vsInterval.equals("day")
							&& (counterValue % (2 * 24) == 0)) {

						if (vsIntervalAggregation.equals("sum")) {
							aggrValue = sum;
							sum = 0.0;
						}

						else if (vsIntervalAggregation.equals("avg")) {
							if (counterValue == 0)
								aggrValue = sum;
							else
								aggrValue = sum / (double) counterValue;
							sum = 0.0;
						}

						else if (vsIntervalAggregation.equals("min")) {
							aggrValue = min;
							min = Double.MAX_VALUE;
						}

						else if (vsIntervalAggregation.equals("max")) {
							aggrValue = max;
							max = Double.MIN_VALUE;
						}

						readingValue = aggrValue;

						if (vsAggregation.equals("min")) {

							if (id == 0) {
								resultValue[resultValueIndex] = readingValue;
							}

							else if (readingValue < resultValue[resultValueIndex])
								resultValue[resultValueIndex] = readingValue;

							resultValueIndex++;

						}

						else if (vsAggregation.equals("max")) {

							if (id == 0) {
								resultValue[resultValueIndex] = readingValue;
							}

							else if (readingValue > resultValue[resultValueIndex])
								resultValue[resultValueIndex] = readingValue;

							resultValueIndex++;
						}

						else if (vsAggregation.equals("sum")) {

							if (id == 0) {
								resultValue[resultValueIndex] = readingValue;
							}

							else
								resultValue[resultValueIndex] = resultValue[resultValueIndex]
										+ readingValue;

							resultValueIndex++;
						}

						else if (vsAggregation.equals("avg")) {

							if (id == 0) {
								resultValue[resultValueIndex] = readingValue;
							}

							else
								resultValue[resultValueIndex] = resultValue[resultValueIndex]
										+ readingValue;

							if (id == (parseIDs.length) - 1) {
								resultValue[resultValueIndex] = resultValue[resultValueIndex]
										/ (double) idNum;
							}
							resultValueIndex++;

						}

						counterValue = 0;

					}

					else if (vsInterval.equals("week")
							&& (counterValue % (2 * 24 * 7) == 0)) {
						if (vsIntervalAggregation.equals("sum")) {
							aggrValue = sum;
							sum = 0.0;
						}

						else if (vsIntervalAggregation.equals("avg")) {
							if (counterValue == 0)
								aggrValue = sum;
							else
								aggrValue = sum / (double) counterValue;
							sum = 0.0;
						}

						else if (vsIntervalAggregation.equals("min")) {
							aggrValue = min;
							min = Double.MAX_VALUE;
						}

						else if (vsIntervalAggregation.equals("max")) {
							aggrValue = max;
							max = Double.MIN_VALUE;
						}

						readingValue = aggrValue;

						if (vsAggregation.equals("min")) {

							if (id == 0) {
								resultValue[resultValueIndex] = readingValue;
							}

							else if (readingValue < resultValue[resultValueIndex])
								resultValue[resultValueIndex] = readingValue;

							resultValueIndex++;

						}

						else if (vsAggregation.equals("max")) {

							if (id == 0) {
								resultValue[resultValueIndex] = readingValue;
							}

							else if (readingValue > resultValue[resultValueIndex])
								resultValue[resultValueIndex] = readingValue;

							resultValueIndex++;
						}

						else if (vsAggregation.equals("sum")) {

							if (id == 0) {
								resultValue[resultValueIndex] = readingValue;
							}

							else
								resultValue[resultValueIndex] = resultValue[resultValueIndex]
										+ readingValue;

							resultValueIndex++;
						}

						else if (vsAggregation.equals("avg")) {

							if (id == 0) {
								resultValue[resultValueIndex] = readingValue;
							}

							else
								resultValue[resultValueIndex] = resultValue[resultValueIndex]
										+ readingValue;

							if (id == (parseIDs.length) - 1) {
								resultValue[resultValueIndex] = resultValue[resultValueIndex]
										/ (double) idNum;
							}
							resultValueIndex++;

						}

						counterValue = 0;

					}

					else if (vsInterval.equals("month")
							&& (counterValue % (2 * 24 * 7 * 4) == 0)) {

						if (vsIntervalAggregation.equals("sum")) {
							aggrValue = sum;
							sum = 0.0;
						}

						else if (vsIntervalAggregation.equals("avg")) {
							if (counterValue == 0)
								aggrValue = sum;
							else
								aggrValue = sum / (double) counterValue;
							sum = 0.0;
						}

						else if (vsIntervalAggregation.equals("min")) {
							aggrValue = min;
							min = Double.MAX_VALUE;
						}

						else if (vsIntervalAggregation.equals("max")) {
							aggrValue = max;
							max = Double.MIN_VALUE;
						}

						readingValue = aggrValue;

						if (vsAggregation.equals("min")) {

							if (id == 0) {
								resultValue[resultValueIndex] = readingValue;
							}

							else if (readingValue < resultValue[resultValueIndex])
								resultValue[resultValueIndex] = readingValue;

							resultValueIndex++;

						}

						else if (vsAggregation.equals("max")) {

							if (id == 0) {
								resultValue[resultValueIndex] = readingValue;
							}

							else if (readingValue > resultValue[resultValueIndex])
								resultValue[resultValueIndex] = readingValue;

							resultValueIndex++;
						}

						else if (vsAggregation.equals("sum")) {

							if (id == 0) {
								resultValue[resultValueIndex] = readingValue;
							}

							else
								resultValue[resultValueIndex] = resultValue[resultValueIndex]
										+ readingValue;

							resultValueIndex++;
						}

						else if (vsAggregation.equals("avg")) {

							if (id == 0) {
								resultValue[resultValueIndex] = readingValue;
							}

							else
								resultValue[resultValueIndex] = resultValue[resultValueIndex]
										+ readingValue;

							if (id == (parseIDs.length) - 1) {
								resultValue[resultValueIndex] = resultValue[resultValueIndex]
										/ (double) idNum;
							}
							resultValueIndex++;

						}

						counterValue = 0;

					}

					counterValue++;

			} // end while (rs.next())

		} // end for 
        
        
        //-----------------------------------normalization TRUE-----------------------------------
        
        if(vsNormalization.equals("true")){
        	
            double mean = 0.0;
            double stddev = 1.0;

        	double sumNorm = 0.0;
        	
        	for(int resultIndex=0; resultIndex<resultNum;resultIndex++){
            	
            	sumNorm = sumNorm + resultValue[resultIndex]; 
            }
        	
        	if(resultNum!=0)
        		mean = sumNorm / (double)resultNum ;
        	
        	sumNorm = 0.0;
        	
        	for(int resultIndex=0; resultIndex<resultNum;resultIndex++){
            	
            	sumNorm = sumNorm + Math.pow((resultValue[resultIndex]-mean),2);            	
            }
        	
        	if(resultNum!=0)
        		stddev = Math.sqrt(sumNorm/(double)(resultNum-1));
        	
        	//System.out.println("STDEV="+stddev);
        	
        	if(stddev!=0.0)
        		for(int resultIndex=0; resultIndex<resultNum;resultIndex++){
            	
        			resultValue[resultIndex] = (resultValue[resultIndex]-mean)/stddev;            	
        		}
        	
        }

    	ArrayList<Object> result = new ArrayList<Object>();
    	result.add(resultTiming);
    	result.add(resultValue);
    	
        return result;
        
    }

}
