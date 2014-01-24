package gsn.http;


import gsn.smartd.questionFileReader;


import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import weka.core.Attribute;
import weka.core.EuclideanDistance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.neighboursearch.KDTree;

public class SmartDSimilarityHandler implements RequestHandler {
	
	private static transient Logger logger = Logger.getLogger(SmartDSimilarityHandler.class);

    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	
		logger.warn("START_SIMILARITY");
		
		String questions = request.getParameter("questions");
		
		System.out.println(questions);

		int n = 9; // optimal number of nearest neighbors

		String irelandSurveyAnswerTable = "ireland_survey_answer";
		String electricDataTable = "electric_data";

		StringBuilder sb = null;
		String userCondition = "";

		questionFileReader qfr = new questionFileReader();
		HashMap<Integer, Integer> numericalQuestions = qfr.numercalQuestion();		

		String[] parseQuestions = questions.split(";");
		int questionNum = parseQuestions.length;
		int[] question = new int[questionNum];
		int[] answer = new int[questionNum];

		for (int i = 0; i < questionNum; i++) {

			String[] parseQuestionAnswer = parseQuestions[i].split(",");

			String qid = parseQuestionAnswer[0];
			String ans = parseQuestionAnswer[1];
			question[i] = Integer.valueOf(qid);
			answer[i] = Integer.valueOf(ans);
		}

		Connection con = null;
		Statement stm = null;
		ResultSet rs = null;

		String url = "jdbc:mysql://localhost/gsn";
		String user = "gsn";
		String password = "gsnpassword";

		try {
			con = DriverManager.getConnection(url, user, password);
			stm = con.createStatement();

			int userNum = 0;
			ArrayList<String> users = new ArrayList<String>();
			HashMap<Integer, Integer> userIndex = new HashMap<Integer, Integer>();

			// retrieve all of the meter ids and save them in users array list
			rs = stm.executeQuery("select distinct METER_ID from "
					+ electricDataTable);

			while (rs.next()) {

				users.add(rs.getString("METER_ID"));
				userIndex.put(rs.getInt("METER_ID"), userNum);
				userNum++;
			}

			int[][] userAnswer = new int[userNum][143];

			for (int usr = 0; usr < userNum; usr++)
				for (int qid = 0; qid < 143; qid++)
					userAnswer[usr][qid] = 99999;

			int questionID = 0;

			// retrieve all of the answers of the users and save them in the
			// matrix userAnswer[userNumber][questionNum]
			for (int id = 0; id < userNum; id++) {

				String meter_id = users.get(id);
				rs = stm.executeQuery("select qid,answer from "
						+ irelandSurveyAnswerTable + " where meter_id="
						+ meter_id);
				while (rs.next()) {

					questionID = rs.getInt("qid");

					if (rs.getString("answer") != null)
						userAnswer[id][questionID - 1] = Integer.valueOf(rs
								.getString("answer"));
					else
						userAnswer[id][questionID - 1] = 99999;
				}
			}

			FastVector nominalAttrID = new FastVector(userNum);
			for (int usr = 0; usr < userNum; usr++) {

				String st = users.get(usr);
				nominalAttrID.addElement(st);
			}

			Attribute meterId = new Attribute("ID", nominalAttrID);
			Attribute[] Q = new Attribute[questionNum];

			for (int q = 0; q < questionNum; q++) {

				int qid = question[q];
				if (numericalQuestions.containsKey(qid)) // if the question is
															// numeric
					Q[q] = new Attribute("Q" + String.valueOf(q));
				else // if the question is nominal
				{
					ArrayList<String> elements = new ArrayList<String>();
					rs = stm.executeQuery("select distinct answer from "
							+ irelandSurveyAnswerTable + " where qid=" + qid);
					int elemNum = 0;

					while (rs.next()) {

						if (rs.getString("answer") != null) {
							elements.add(rs.getString("answer"));
							elemNum++;
						}
					}

					elements.add("99999");
					elemNum++;

					Iterator<String> itrAttr = elements.iterator();

					FastVector nominalAttr = new FastVector(elemNum);
					for (int elem = 0; elem < elemNum; elem++) {

						String st = itrAttr.next().toString();
						// logger.warn(st);
						nominalAttr.addElement(st);
					}
					Q[q] = new Attribute("Q" + String.valueOf(q), nominalAttr);
				}
			}

			// Declare the feature vector

			FastVector fvAttributes = new FastVector(questionNum + 1);
			fvAttributes.addElement(meterId);
			for (int q = 0; q < questionNum; q++) {
				fvAttributes.addElement(Q[q]);
			}

			Instances dataset = new Instances("my_dataset", fvAttributes, 0);

			for (int id = 0; id < userNum; id++) {

				Instance ins = new Instance(questionNum + 1);
				ins.setValue(meterId, users.get(id));

				for (int q = 0; q < questionNum; q++) {

					int qid = question[q];
					if (numericalQuestions.containsKey(qid))
						ins.setValue(Q[q], userAnswer[id][qid - 1]);
					else
						ins.setValue(Q[q],
								String.valueOf(userAnswer[id][qid - 1]));
				}
				ins.setDataset(dataset);
				dataset.add(ins);
				// logger.warn(ins);
			}

			KDTree tree = new KDTree();
			try {
				tree.setInstances(dataset);

				EuclideanDistance df = new EuclideanDistance(dataset);
				df.setDontNormalize(true);

				tree.setDistanceFunction(df);
			} catch (Exception e) {
				e.printStackTrace();
			}

			int[] nearestNeighbors = new int[n + 1]; // first nearest neighbor
														// is the customer
														// itself
			Instance insNewUser = new Instance(questionNum + 1);
			for (int q = 0; q < questionNum; q++) {

				insNewUser.setValue(meterId, 0); // new customer has no id

				int qid = question[q];
				if (numericalQuestions.containsKey(qid)) // if the question is
															// numeric
					insNewUser.setValue(Q[q], answer[q]);
				else // if the question is nominal
					insNewUser.setValue(Q[q], String.valueOf(answer[q]));
			}

			insNewUser.setDataset(dataset);

			try {

				Instances neighbors = tree
						.kNearestNeighbours(insNewUser, n + 1);
				for (int nn = 0; nn < n + 1; nn++) {
					nearestNeighbors[nn] = Integer.valueOf(neighbors.instance(nn).toString(0));
				}
			} catch (Exception e) {

				logger.warn("EXCEPTION!" + e);
			}

			userCondition = " (";
			for (int id = 1; id < n + 1; id++) {

				if (id != n)
					userCondition = userCondition + " METER_ID="
							+ nearestNeighbors[id] + " or ";
				else
					userCondition = userCondition + " METER_ID="
							+ nearestNeighbors[id];
			}

			userCondition = userCondition + ") ";
			// logger.warn(userCondition);

			rs = stm.executeQuery("select avg(Value) as avg, HOUR(FROM_UNIXTIME(TIMESTAMP)) as hour from "
					+ electricDataTable
					+ " where "
					+ userCondition
					+ " group by HOUR(FROM_UNIXTIME(TIMESTAMP)) ");

			sb = new StringBuilder("<result>\n");
			while (rs.next()) {

				sb.append("<stream-element>\n");
				sb.append("<field name=\"").append("hour\">")
						.append(rs.getString("hour")).append("</field>\n");
				sb.append("<field name=\"").append("avg\">")
						.append(rs.getString("avg")).append("</field>\n");
				sb.append("</stream-element>\n");
			}
			sb.append("</result>");

			response.setHeader("Cache-Control", "no-store");
			response.setDateHeader("Expires", 0);
			response.setHeader("Pragma", "no-cache");
			response.getWriter().write(sb.toString());

			logger.warn("END_SIMILARITY");

		} catch (SQLException ex) {
			logger.warn("SQLException:" + ex);

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
				logger.warn("SQLException:" + ex);
			}
		}
			    
    }
    
    public boolean isValid(HttpServletRequest request, HttpServletResponse response) throws IOException {    	
        return true;
    }

}
