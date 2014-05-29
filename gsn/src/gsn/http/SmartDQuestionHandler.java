package gsn.http;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

public class SmartDQuestionHandler implements RequestHandler{
	
	private static transient Logger logger = Logger.getLogger(SmartDQuestionHandler.class);
	
	public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		
		String question = request.getParameter("question");
				
		String irelandSurveyQuestionTable = "ireland_survey_question"; 
		String irelandSurveyAnswerOptionsTable = "ireland_survey_answer_options";
        
        StringBuilder sb = null;
        
        String fields = "ireland_survey_question.qid,ireland_survey_question.question, "
        		+ "ireland_survey_answer_options.answer,ireland_survey_answer_options.description";
        
        
        String joinCondition = "ireland_survey_answer_options.qid=ireland_survey_question.qid";
        
        String condition = ""; 
        
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
            
			if (question != null && question.trim().length() != 0) {

				if (question.equalsIgnoreCase("all")) { //request for all of the question. qid and the question will be returned. 

					rs = stm.executeQuery("select * from " + irelandSurveyQuestionTable + " order by qid");
					sb = new StringBuilder("<result>\n");
					while (rs.next()) {
						
						String[] parseQuestion = rs.getString("question").split(":");
						String questionTxt = parseQuestion[1];
						sb.append("<stream-element>\n");
						sb.append("<field name=\"").append("qid\">").append(rs.getString("qid"))
								.append("</field>\n");
						sb.append("<field name=\"").append("question\">").append(questionTxt)
								.append("</field>\n");
						sb.append("</stream-element>\n");
					}
					sb.append("</result>");
				}
				
				else //request for the question for the given id. qid, question and answer options will be returned.
				{					
					
					condition = "ireland_survey_question.qid="+question;
					
					rs = stm.executeQuery("select "+ fields + " from " + irelandSurveyQuestionTable + 
        					" join "+ irelandSurveyAnswerOptionsTable + " on "+ joinCondition + 
        					" where " + condition);
					
					sb = new StringBuilder("<result>\n");
					sb.append("<stream-element>\n");
					
					if(rs.next()){
					
						String[] parseQuestion = rs.getString("question").split(":");
						String questionTxt = parseQuestion[1];
					
					sb.append("<field name=\"").append("qid\">").append(rs.getString("qid"))
							.append("</field>\n");
					sb.append("<field name=\"").append("question\">").append(questionTxt)
							.append("</field>\n");
					sb.append("<field name=\"").append(rs.getString("answer")).append("\">").append(rs.getString("description"))
					.append("</field>\n");
					
					}
					
					else{
						
						rs = stm.executeQuery("select * from "+ irelandSurveyQuestionTable + " where qid=" + question);	            

			            rs.next();
			            String[] parseQuestion = rs.getString("question").split(":");
						String questionTxt = parseQuestion[1];
						sb.append("<field name=\"").append("qid\">").append(rs.getString("qid"))
								.append("</field>\n");
						sb.append("<field name=\"").append("question\">").append(questionTxt)
								.append("</field>\n");
						
					}
					while (rs.next()) {
						
						sb.append("<field name=\"").append(rs.getString("answer")).append("\">").append(rs.getString("description"))
								.append("</field>\n");						
					}
					
					sb.append("</stream-element>\n");
					sb.append("</result>");
					
					
				}

				response.setHeader("Cache-Control", "no-store");
				response.setDateHeader("Expires", 0);
				response.setHeader("Pragma", "no-cache");
				response.getWriter().write(sb.toString());
			}

			else {

				logger.warn("Incorrect Request");
			}
            
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
    	
        return true;
    }

}
