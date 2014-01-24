package gsn.http;

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
        
        int resultNum = 0;     
        String vsCondition = "";
        String timeCondition=null;
        int resultTimingIndex = 0;
        int resultValueIndex = 0;
        
        String[] resultTiming = null;
    	double[] resultValue = null;
        
        double mean = 0.0;
        double stddev = 1.0;
        
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        double sum = 0.0;
        double aggrValue = 0.0;
        double readingValue = 0.0;
        
        
        int counterDateTime = 0;
        int counterValue = 0;
        
        Integer startTimeYear, startTimeMonth, startTimeDay,startTimeHour, startTimeMinute;
        Integer endTimeYear, endTimeMonth, endTimeDay, endTimeHour, endTimeMinute;
        
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
    			// TODO Auto-generated catch block
    			e1.printStackTrace();
    		}   
        	String[] parseStartDateTime = vsStartTime.split("T");
        	
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
        
        if (vsEndTime != null && vsEndTime.trim().length() != 0)
        {
        	try {
    			end = formatter.parse(vsEndTime);
    		} catch (ParseException e1) {
    			// TODO Auto-generated catch block
    			e1.printStackTrace();
    		}
        	
        	String[] parseEndDateTime = vsEndTime.split("T");
        	
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
        
        
        Connection con = null;
        Statement stm = null;
        ResultSet rs = null;

        String url = "jdbc:mysql://localhost/gsn";
        String user = "gsn";
        String password = "gsnpassword";
        
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

				}
        }
        
        else{
        	
        	logger.warn("Please enter the use IDs!");        	
        } 	
        
        //-------------normalization-----------------------
        
        if (vsNormalization == null || vsNormalization.trim().length() == 0)
        	vsNormalization =  "false";
       
        
       resultTimingIndex = 0;
        
        for(int id=0; id<parseIDs.length ;id++){
        	
        	resultValueIndex = 0;
        	min = Double.MAX_VALUE;
        	max = Double.MIN_VALUE;
        	sum = 0.0;
        	counterValue = 0;
        	
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

				while (rs.next()) {

					if (id == 0) {

						Date dt = new Date(rs.getLong("TIMESTAMP") * 1000);
						if (vsInterval.equals("30")) {
							resultTiming[resultTimingIndex] = formatter
									.format(dt);
							resultTimingIndex++;
							counterDateTime = 0;
						}

						else if (vsInterval.equals("hour")
								&& (counterDateTime % 2 == 0)) {
							resultTiming[resultTimingIndex] = formatter
									.format(dt);
							resultTimingIndex++;
							counterDateTime = 0;

						}

						else if (vsInterval.equals("day")
								&& (counterDateTime % (2 * 24) == 0)) {
							resultTiming[resultTimingIndex] = formatter
									.format(dt);
							resultTimingIndex++;
							counterDateTime = 0;

						}

						else if (vsInterval.equals("week")
								&& (counterDateTime % (2 * 24 * 7) == 0)) {
							resultTiming[resultTimingIndex] = formatter
									.format(dt);
							resultTimingIndex++;
							counterDateTime = 0;

						}

						else if (vsInterval.equals("month")
								&& (counterDateTime % (2 * 24 * 7 * 4) == 0)) {
							resultTiming[resultTimingIndex] = formatter
									.format(dt);
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

				}

			}
        
        
       /* String st = "";
        for(int resultIndex=0; resultIndex<resultNum;resultIndex++){
        	
        	st = st + resultValue[resultIndex] + ","; 
        }
        
        System.out.println(st);*/
        
        //-----------------------------------normalization TRUE-----------------------------------
        
        if(vsNormalization.equals("true")){
        	
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
                
        
        //----------------------------------result as XML------------------------------------------
        
        sb = new StringBuilder("<result>\n");
        for(int resultIndex=0; resultIndex<resultNum;resultIndex++){
        	
        	if(resultTiming[resultIndex]!=null){
        	
        		sb.append("<stream-element>\n");
        		sb.append("<field name=\"").append("TIMESTAMP\">").
        		append(StringEscapeUtils.escapeXml(resultTiming[resultIndex])).
        		append("</field>\n");
        		sb.append("<field name=\"").append("VALUE\">").
        		append(StringEscapeUtils.escapeXml(String.valueOf(resultValue[resultIndex]))).
        		append("</field>\n");
        		sb.append("</stream-element>\n");       
        	}
        	
        }
       
        sb.append("</result>");                 
        
        response.setHeader("Cache-Control", "no-store");
        response.setDateHeader("Expires", 0);
        response.setHeader("Pragma", "no-cache");
        response.getWriter().write(sb.toString());
        
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


}
