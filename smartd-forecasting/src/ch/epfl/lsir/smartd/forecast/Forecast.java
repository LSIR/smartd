package ch.epfl.lsir.smartd.forecast;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.TimeZone;

public class Forecast {

	static String fileOutput = "electric_data_forecast.sql";
	static PrintWriter frcOut;
	
	static int frcHorizon    = 7;
	static String dbHost     = "localhost";
	static String dbDatabase = "gsn";
	static String dbUsername = "gsn";
	static String dbPassword = "gsnpassword";
	static String dbUrl      = "jdbc:mysql://" + dbHost + "/" + dbDatabase;
	
	static String timezone = "GMT+0";
	
	static String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
	static String DATE_FORMAT = "yyyy-MM-dd";
	static String HOUR_FORMAT = "HH";
	
	static DatabaseAccess db;
	
	
	
	/**
	 * @param args
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws SQLException 
	 * @throws ParseException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws FileNotFoundException, IOException, SQLException, ParseException, InterruptedException {
		
		// store execution start time
		long execStart = (new Date()).getTime();
		
		// get some configuration properties
		Properties prop = new Properties();
		prop.load(new FileInputStream("system.config"));
		
		fileOutput = prop.getProperty("file-output");
		frcHorizon = Integer.parseInt( prop.getProperty("forecast-horizon") );
		timezone   = prop.getProperty("timezone");
		String energyFile  = prop.getProperty("cb-energy-file");
		String excludeFile = prop.getProperty("cb-exclude-file");
		
		dbHost     = prop.getProperty("dbHost");
		dbDatabase = prop.getProperty("dbDatabase");
		dbUsername = prop.getProperty("dbUsername");
		dbPassword = prop.getProperty("dbPassword");		
		dbUrl      = "jdbc:mysql://" + dbHost + "/" + dbDatabase;

		System.err.println("Connecting to " + dbUrl + " with username=" + dbUsername + " and password=" + dbPassword );
		db = new DatabaseAccess(dbUrl, dbUsername, dbPassword);
		System.err.println("Connected.");
		
		//..frcOut = new PrintWriter(fileOutput);
		
		
		
		/** create a table if not exists **/
		String query = "CREATE TABLE IF NOT EXISTS electric_data_forecast (" +
				"METER_ID int(5) NOT NULL, " +
				"TIMESTAMP int(10) unsigned NOT NULL," +
				"VALUE double NOT NULL" +
				") ENGINE=MyISAM;";
		//..frcOut.println(query);
		db.executeUpdate(query);
		
		
		
		/** check the latest date available and set the starting date to forecast**/
		query = "SELECT max(TIMESTAMP) FROM electric_data;";
		ResultSet rs = db.executeQuery(query);
		rs.next();
		long maxDate = Long.parseLong( rs.getString(1) );
		Calendar startCal = getCalFromTimeSQL( maxDate );
		startCal.add(Calendar.DAY_OF_MONTH, 1);
		SimpleDateFormat formatter = new SimpleDateFormat( DATE_FORMAT ); 
		formatter.setTimeZone( TimeZone.getTimeZone(timezone) );
		String startDateStr = formatter.format( startCal.getTime() );
		System.err.println( "starting date to forecast: " + startDateStr );
		System.err.println( "forecasting horizon: " + frcHorizon);
		
		/** get all customers **/ 
		ArrayList<String> custList = getAllCustomers();
		System.err.println("customer list: " + custList);

		
		
		
		/** collect customers data and forecast **/				
		// set the seconds when the measurement is taken every hour
		int[] arrSeconds = {0, 1800};
		// HashMap to map seconds when we have several measurement in an hour
		// <the seconds, index>
		HashMap<Integer, Integer> secondsMap = new HashMap<Integer, Integer>();
		for (int i=0; i<arrSeconds.length; i++) {
			secondsMap.put(arrSeconds[i], i);
		}
		
		// prepare some formatter
		SimpleDateFormat fDate = new SimpleDateFormat( DATE_FORMAT );
		fDate.setTimeZone( TimeZone.getTimeZone( timezone ) );
		SimpleDateFormat fHour = new SimpleDateFormat( HOUR_FORMAT );
		fHour.setTimeZone( TimeZone.getTimeZone( timezone ) );
		
		for (int c=0; c<custList.size(); c++ ) {
		//for (int c=0; c<5; c++ ) { // this line is used for testing purposes only
			String cust = custList.get(c);
			
			System.err.println("processing METER_ID: " + cust + " ("+(c+1)+"/"+custList.size()+")");
			// initialize data container
			Object[] data = new Object[ secondsMap.size() ];
			for (int i=0; i<secondsMap.size(); i++ ) {
				data[i] = new ArrayList<String>();			
			}		

			query = "select TIMESTAMP, VALUE from electric_data where METER_ID=" + cust + " order by TIMESTAMP;" ;
			rs = db.executeQuery(query);
			while ( rs.next() ) {
				
				long timestamp = rs.getLong(1);
				double value = rs.getDouble(2);

				Date date = new Date(timestamp * 1000);
				int seconds = (int) timestamp % 3600;
				
				String dateStr = fDate.format(date);
				String hourStr = fHour.format(date);		
				String lines = dateStr + "," + hourStr + "," + value;
				
				((ArrayList<String>) data[ secondsMap.get(seconds) ]).add(lines);								
				
			}			
			
			//String fileFrcOutput = "frc-output.tmp";
			for (int i=0; i<data.length; i++) {
				writeToFile((ArrayList<String>) data[i], energyFile);
				//String command = "java -jar ComputeBaseline.jar -z " + frcHorizon + " -e " + excludeFile +
				//		" Supervised cb-supervised.config " + startDateStr;
				String command = "java -jar ComputeBaseline.jar -z " + frcHorizon + " -e " + excludeFile +
						" ISONE " + energyFile + " " + startDateStr;
				Process proc = Runtime.getRuntime().exec(command);
				proc.waitFor();
				BufferedReader buf = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				/* testing system
				String line;
				while ( (line=buf.readLine() ) != null ) {
					System.out.println(line);
				}
				System.exit(0);
				*/
				writeInsertFrcRows( cust, arrSeconds[i], buf );
			}
			db.executeUpdate("COMMIT;");
			//..frcOut.flush();
		}
		//..frcOut.close();
		
		
		System.err.println("done (" + ( (new Date()).getTime() - execStart ) / 1000.0 + " sec)."  );	
	}

	
	/**
	 * 
	 * @param cust the METER_ID
	 * @param seconds 0 means on the minute 0 of every hour, 1800 means on the minutes 30 of every hour
	 * @param in input stream to be read
	 * @throws IOException
	 * @throws ParseException
	 */
	private static void writeInsertFrcRows(String cust, int seconds, BufferedReader in) throws IOException, ParseException {
		
		String line;		 
		String query;
		
		SimpleDateFormat formatter = new SimpleDateFormat(DATETIME_FORMAT);
		formatter.setTimeZone( TimeZone.getTimeZone(timezone) );
		
		//BufferedReader in = new BufferedReader(new FileReader(fileFrcOutput) );
		while ( ( line = in.readLine() ) != null ) {
			
			String[] lineArr = line.split(",");
			String dateTimeStr = lineArr[0] + " " + lineArr[1] + ":00:00";
			Date date = formatter.parse(dateTimeStr);
			long timestamp = date.getTime() / 1000 + seconds;
			//long timestamp = Long.parseLong( lineArr[3] ) / 1000 + seconds;
			
			// remove the old forecast if exists
			query = "DELETE FROM electric_data_forecast WHERE METER_ID=" + cust + " AND TIMESTAMP=" + timestamp + ";";
			//frcOut.println(query);
			db.executeUpdate(query);
		
			// insert the new forecast
			query = "INSERT INTO electric_data_forecast VALUES (" + cust + ", " + timestamp + ", " + lineArr[2] + ");";
			//frcOut.println(query);
			db.executeUpdate(query);
		
			//System.err.println(line + " " + formatter.format(date));
		}
		in.close();
	}

	private static Calendar getCalFromTimeSQL(long time) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone( TimeZone.getTimeZone( timezone ) );		
		cal.setTimeInMillis(time * 1000);
		return cal;
	}
	
	private static ArrayList<String> getAllCustomers() throws SQLException {
		ArrayList<String> result = new ArrayList<String>();
		String query = "select distinct METER_ID from electric_data;";
		ResultSet rs = db.executeQuery(query);
		while ( rs.next() ) {
			result.add( rs.getString(1) );
		}
		return result;
	}
	
	private static void writeToFile( ArrayList<String> lines, String fileName) throws FileNotFoundException {
		PrintWriter out = new PrintWriter(fileName);
		for (String line : lines) {
			out.println( line );
		}
		out.close();
	}
}



// read input: database url, username password
// connect to the database
// create a new table "electric_data_forecast" 
	// check what if we create if not exist
//+-----------+------------------+------+-----+---------+----------------+
//| Field     | Type             | Null | Key | Default | Extra          |
//+-----------+------------------+------+-----+---------+----------------+
//| METER_ID  | int(5)           | NO   | MUL | NULL    |                |
//| TIMESTAMP | int(10) unsigned | NO   | MUL | NULL    |                |
//| VALUE     | double           | NO   |     | NULL    |                |
//+-----------+------------------+------+-----+---------+----------------+
// check the latest date of the database + length of the forecasting (default is 7) - set into properties file
// because the data is every 30 minutes, while our forecasting is every hour, then 
// for each consumer
	// for minute xx = {0, 30}
		// read energy data for each consumer on the minute xx 
		// output it into a file of <date>,<the hour>,<kWh>
		// run the forecasting (ComputeBaseline.jar) store the result into a file
		// read the file output, put it into the "electric_data_forecast" table
