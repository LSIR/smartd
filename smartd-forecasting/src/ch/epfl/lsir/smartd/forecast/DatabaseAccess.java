package ch.epfl.lsir.smartd.forecast;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


/**
 * 
 * To establish connection and execute queries to a mysql database 
 * @author Tri Kurniawan Wijaya <tri-kurniawan.wijaya@epfl.ch>
 * @date Fri 03 May 2013 12:13:17 PM CEST 
 *
 */
public class DatabaseAccess {
	
	private Connection connect;
	private Statement statement;
	private ResultSet resultSet;
	
	private String databaseURL;
	private String userName;
	private String password;
	
	public DatabaseAccess(String databaseURL, String userName, String password) {
		this.databaseURL = databaseURL;
		this.userName = userName;
		this.password = password;
	}
	
	public void openConn(){
		try {
			Class.forName("com.mysql.jdbc.Driver");
			connect = DriverManager.getConnection(databaseURL, userName, password);
		} catch (ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
				
	}
	
	public void closeConn() {
		try {
			if (resultSet!=null) resultSet.close();
			if (statement!=null) statement.close();
			if (connect != null) connect.close();		
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public ResultSet executeQuery(String query) {
		closeConn();
		openConn();		
		resultSet = null;
		try {
			statement = connect.createStatement();
			resultSet = statement.executeQuery(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}		
		return resultSet;		
	}

	public int executeUpdate(String query)  {
		
		closeConn();
		openConn();
		
		int result=-1;
		try {
			statement = connect.createStatement();
			result = statement.executeUpdate(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}		
		return result;		
	}

	

}

