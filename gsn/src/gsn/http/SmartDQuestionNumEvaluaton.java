package gsn.http;


import gsn.smartd.questionFileReader;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.core.Attribute;
import weka.core.EuclideanDistance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.neighboursearch.KDTree;



public class SmartDQuestionNumEvaluaton implements RequestHandler{
	
	private static transient Logger logger = Logger.getLogger(SmartDQuestionNumEvaluaton.class);
	
	public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		logger.warn("START_QUESTION");

		int numOfQuestions = Integer.valueOf(request.getParameter("questionNum"));
		int numOfNeighbors = Integer.valueOf(request.getParameter("neighborNum")); // optimal nearest neighbors' number

		int resultNum = numOfQuestions / 2;
		int allQuestionNum = 143;

		int[] questionRank = new int[allQuestionNum];

		String electricDataTable = "electric_data";
		String irelandSurveyAnswerTable = "ireland_survey_answer";

		StringBuilder sb = null;

		String season = request.getParameter("season");
		String seasonCondition = "";

		if (season != null && season.trim().length() != 0) {

			if (season.equals("winter")) {

				seasonCondition = " where ((MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='November') "
						+ "or (MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='December') or "
						+ "(MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='January')) ";

			} else if (season.equals("spring")) {

				seasonCondition = " where ((MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='February') "
						+ "or (MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='March') or "
						+ "(MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='April')) ";

			} else if (season.equals("summer")) {

				seasonCondition = " where ((MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='May') "
						+ "or (MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='June') or "
						+ "(MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='July')) ";

			} else if (season.equals("autumn")) {

				seasonCondition = " where ((MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='August') "
						+ "or (MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='September') or "
						+ "(MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='October')) ";
			}

		}

		questionFileReader qfr = new questionFileReader();
		HashMap<Integer, Integer> numericalQuestions = qfr.numercalQuestion();
		HashMap<Integer, Integer> omittedQuestions = qfr.omittedQuestion();

		int userNum = 0;
		ArrayList<String> users = new ArrayList<String>();
		HashMap<Integer, Integer> userIndex = new HashMap<Integer, Integer>();

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

			// retrieve all of the meter ids and save them in users array list
			rs = stm.executeQuery("select distinct METER_ID from "
					+ electricDataTable);

			while (rs.next()) {

				users.add(rs.getString("METER_ID"));
				userIndex.put(rs.getInt("METER_ID"), userNum);
				userNum++;
			}

			int[][] userAnswer = new int[userNum][allQuestionNum];

			for (int usr = 0; usr < userNum; usr++)
				for (int qid = 0; qid < allQuestionNum; qid++)
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
						// if the answer has been null
						userAnswer[id][questionID - 1] = 99999;
				}
			}

			logger.warn("AVG----START");

			// retrieve the average of energy consumption for all of the user
			// per hour based on the season (all,winter summer)
			// Save the values in the matrix
			// energyConsumtion[userNumber][24]---24 hours

			double[][] energyConsumption = new double[userNum][24];

			rs = stm.executeQuery("select METER_ID, HOUR(FROM_UNIXTIME(TIMESTAMP)) as hour, avg(VALUE) as avg from "
					+ electricDataTable
					+ seasonCondition
					+ " group by HOUR(FROM_UNIXTIME(TIMESTAMP)), METER_ID ");

			while (rs.next()) {

				int idIndex = userIndex.get(rs.getInt("METER_ID"));
				energyConsumption[idIndex][rs.getInt("hour")] = rs
						.getFloat("avg");
			}

			logger.warn("AVG----END");

			// Declare numeric attributes

			Attribute[] Q = new Attribute[allQuestionNum];

			for (int qid = 0; qid < allQuestionNum; qid++) {

				// logger.warn("QID="+qid);

				if (!omittedQuestions.containsKey(qid + 1)) {

					if (numericalQuestions.containsKey(qid + 1))// if the
																// question is
																// numeric
						Q[qid] = new Attribute("Q" + String.valueOf(qid));

					else // if the question is nominal
					{
						ArrayList<String> elements = new ArrayList<String>();
						rs = stm.executeQuery("select distinct answer from "
								+ irelandSurveyAnswerTable + " where qid="
								+ (qid + 1));
						int elemNum = 0;

						while (rs.next()) {

							if (rs.getString("answer") != null) {
								elements.add(rs.getString("answer"));
								elemNum++;
							}

						}

						elements.add("99999"); // if the answer has been null
						elemNum++;

						Iterator<String> itrAttr = elements.iterator();

						FastVector nominalAttr = new FastVector(elemNum);
						for (int elem = 0; elem < elemNum; elem++) {

							String st = itrAttr.next().toString();
							nominalAttr.addElement(st);
						}

						Q[qid] = new Attribute("Q" + String.valueOf(qid),
								nominalAttr);

					}
				}
			}

			Attribute consumption = new Attribute("consumption");

			// Declare the feature vector

			// 3 questions were omitted, so the total number of questions is
			// 140. 1 attribute is declared for energy consumption
			FastVector fvAttributes = new FastVector(141);
			for (int j = 0; j < allQuestionNum; j++) {

				if (!omittedQuestions.containsKey(j + 1))
					fvAttributes.addElement(Q[j]);
			}

			fvAttributes.addElement(consumption);

			for (int h = 0; h <= 23; h++) {

				Instances dataset = new Instances("my_dataset", fvAttributes, 0);

				for (int id = 0; id < userNum; id++) {

					Instance ins = new Instance(141);
					for (int q = 0; q < allQuestionNum; q++) {

						if (!omittedQuestions.containsKey(q + 1)) {

							if (numericalQuestions.containsKey(q + 1))// for
																		// numeric
																		// attributes
								ins.setValue(Q[q], userAnswer[id][q]);
							else // for nominal attributes
							{
								ins.setValue(Q[q],
										String.valueOf(userAnswer[id][q]));
							}
						}
					}

					ins.setValue(consumption, energyConsumption[id][h]);
					ins.setDataset(dataset);
					dataset.add(ins);
					// logger.warn(ins);

				}

				AttributeSelection attsel = new AttributeSelection();
				CfsSubsetEval eval = new CfsSubsetEval();
				BestFirst search = new BestFirst();
				attsel.setEvaluator(eval);
				attsel.setSearch(search);
				try {
					attsel.SelectAttributes(dataset);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// obtain the attribute indices that were selected
				try {
					int[] indices = attsel.selectedAttributes();
					// String st = "";
					for (int l = 0; l < indices.length - 1; l++) {
						// st = st + indices[k]+",";
						questionRank[indices[l]]++;
					}
					// logger.warn(st);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			/*
			 * for(int l=0;l<questionRank.length;l++){
			 * 
			 * logger.warn(questionRank[l]+"     "+l); }
			 */

			// find the first 20 question ids that were ranked the highest and
			// save them in the array questionRankSort

			int[] questionRankSort = new int[numOfQuestions - 1];

			int max = Integer.MIN_VALUE;
			int qid = -1;

			for (int rank = 0; rank < numOfQuestions - 1; rank++) {

				for (int q = 0; q < allQuestionNum; q++) {

					if (questionRank[q] > max) {
						max = questionRank[q];
						qid = q;
					}
				}

				questionRankSort[rank] = qid + 1;
				questionRank[qid] = Integer.MIN_VALUE;
				max = Integer.MIN_VALUE;
				qid = -1;
			}

			// create attributes
			Attribute[] H = new Attribute[24];
			for (int h = 0; h < 24; h++) {
				H[h] = new Attribute("H" + String.valueOf(h));
			}

			// declare feature vector
			FastVector fvAttributesNeighbor = new FastVector(24);
			for (int h = 0; h < 24; h++) {
				fvAttributesNeighbor.addElement(H[h]);
			}

			Instances datasetNeighbor = new Instances("my_dataset",	fvAttributesNeighbor, 0);
			double[] distance = new double[resultNum];

			// -------------------------------------------------------------------------------------------------

			int questionNum = 1;

			while (questionNum < numOfQuestions) {

				FastVector nominalAttrID = new FastVector(userNum);
				for (int usr = 0; usr < userNum; usr++) {
					String st = users.get(usr);
					nominalAttrID.addElement(st);
				}

				Attribute meterId = new Attribute("ID", nominalAttrID);

				int Qnum = 0;

				for (int q = 0; q < questionNum; q++) {
					if (!omittedQuestions.containsKey(questionRankSort[q]))
						Qnum++;
				}

				// System.out.println("NUM="+Qnum);

				Attribute[] QN = new Attribute[questionNum];

				for (int q = 0; q < questionNum; q++) {

					if (!omittedQuestions.containsKey(questionRankSort[q])) {

						if (numericalQuestions.containsKey(questionRankSort[q]))
							QN[q] = new Attribute("Q"+ String.valueOf(questionRankSort[q]));
						else {

							ArrayList<String> elements = new ArrayList<String>();
							rs = stm.executeQuery("select distinct answer from "
									+ irelandSurveyAnswerTable
									+ " where qid="
									+ questionRankSort[q]);
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
								nominalAttr.addElement(st);
							}

							QN[q] = new Attribute("Q"+ String.valueOf(questionRankSort[q]),	nominalAttr);

						}
					}
				}

				// Declare the feature vector

				FastVector fvAttributesQ = new FastVector(Qnum + 1);
				
				fvAttributesQ.addElement(meterId);
				
				for (int q = 0; q < questionNum; q++) {
					if (!omittedQuestions.containsKey(questionRankSort[q]))
						fvAttributesQ.addElement(QN[q]);
				}

				Instances dataset = new Instances("my_dataset", fvAttributesQ,0);

				for (int id = 0; id < userNum; id++) {

					Instance ins = new Instance(Qnum + 1);
					ins.setValue(meterId, users.get(id));

					for (int q = 0; q < questionNum; q++) {

						if (!omittedQuestions.containsKey(questionRankSort[q])) {

							if (numericalQuestions.containsKey(questionRankSort[q]))
								ins.setValue(QN[q], userAnswer[id][questionRankSort[q]-1]);
							else 
								ins.setValue(QN[q],String.valueOf(userAnswer[id][questionRankSort[q] - 1]));
							}
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

				int[][] nearestNeighbors = new int[userNum][numOfNeighbors + 1];

				for (int id = 0; id < userNum; id++) {

					Instance insUserQuestionNum = new Instance(Qnum + 1);
					for (int q = 0; q < questionNum; q++) {

						insUserQuestionNum.setValue(meterId, users.get(id));
						if (!omittedQuestions.containsKey(questionRankSort[q])) {
							if (numericalQuestions.containsKey(questionRankSort[q]))
								insUserQuestionNum.setValue(QN[q], userAnswer[id][questionRankSort[q] - 1]);
							else 
								insUserQuestionNum.setValue(QN[q], String.valueOf(userAnswer[id][questionRankSort[q]-1]));
						}
					}

					insUserQuestionNum.setDataset(dataset);

					try {
						Instances neighbors = tree.kNearestNeighbours(insUserQuestionNum, numOfNeighbors + 1);
						for (int nn = 0; nn < numOfNeighbors + 1; nn++) {
							nearestNeighbors[id][nn] = Integer
									.valueOf(neighbors.instance(nn).toString(0));
						}
						
					} catch (Exception e) {

						logger.warn("EXCEPTION!" + e);
					}

					Instance insUser = new Instance(24);

					for (int h = 0; h < 24; h++) {
						insUser.setValue(H[h], energyConsumption[id][h]);
					}

					insUser.setDataset(datasetNeighbor);
					datasetNeighbor.add(insUser);

					double[] neighborEnergyConsumption = new double[24];

					Instance insNeighbor = new Instance(24);

					for (int nn = 1; nn < numOfNeighbors + 1; nn++) {
						for (int h = 0; h < 24; h++) {
							int meter_id = nearestNeighbors[id][nn];
							int idIndex = userIndex.get(meter_id);
							neighborEnergyConsumption[h] = neighborEnergyConsumption[h]	+ energyConsumption[idIndex][h];
						}

					}

					for (int h = 0; h < 24; h++) {
						insNeighbor.setValue(H[h], neighborEnergyConsumption[h]	/ numOfNeighbors);
					}

					insNeighbor.setDataset(datasetNeighbor);
					datasetNeighbor.add(insNeighbor);

					EuclideanDistance df = new EuclideanDistance(datasetNeighbor);

					Double dist = df.distance(insNeighbor, insUser);
					int index = questionNum / 2;
					distance[index] = distance[index] + dist;
				}

				questionNum = questionNum + 2;
			}

			sb = new StringBuilder("<result>\n");

			for (int index = 0; index < resultNum; index++) {
				sb.append("<stream-element>\n");
				sb.append("<field name=\"")
						.append("Number")
						.append("\">")
						.append(StringEscapeUtils.escapeXml(String
								.valueOf(index * 2 + 1))).append("</field>\n");
				sb.append("<field name=\"")
						.append("Distance")
						.append("\">")
						.append(StringEscapeUtils.escapeXml(String
								.valueOf(distance[index] / (double) userNum)))
						.append("</field>\n");
				sb.append("</stream-element>\n");
			}
			sb.append("</result>");

			response.setHeader("Cache-Control", "no-store");
			response.setDateHeader("Expires", 0);
			response.setHeader("Pragma", "no-cache");
			response.getWriter().write(sb.toString());

			logger.warn("END_QUESTION");

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
