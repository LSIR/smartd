package gsn.smartd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import weka.attributeSelection.AttributeSelection;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class questionSelectionRanking {
	
	public questionSelectionRanking(){
		
	}

	public int[] questionRank(String dayContext, String seasonContext) {

		String weekDay = dayContext;
		String season = seasonContext;

		int questionNum = 143;

		int[] questionRank = new int[questionNum];

		String electricDataTable = "electric_data";
		String irelandSurveyAnswerTable = "ireland_survey_answer";
		String condition = "";

		int weekdayIndex = 0;

		if (weekDay != null && weekDay.trim().length() != 0) {

			if (!weekDay.equals("weekdays") && !weekDay.equals("weekend")) {

				if (weekDay.equals("monday"))
					weekdayIndex = 0;
				else if (weekDay.equals("tuesday"))
					weekdayIndex = 1;
				else if (weekDay.equals("wednesday"))
					weekdayIndex = 2;
				else if (weekDay.equals("thursday"))
					weekdayIndex = 3;
				else if (weekDay.equals("friday"))
					weekdayIndex = 4;
				else if (weekDay.equals("saturday"))
					weekdayIndex = 5;
				else if (weekDay.equals("sunday"))
					weekdayIndex = 6;

				condition = "WEEKDAY(FROM_UNIXTIME(TIMESTAMP))=" + weekdayIndex;
			}

			else {

				if (weekDay.equals("weekdays"))
					condition = "(WEEKDAY(FROM_UNIXTIME(TIMESTAMP))!=" + 5
							+ " and WEEKDAY(FROM_UNIXTIME(TIMESTAMP))!=" + 6
							+ ") ";
				else if (weekDay.equals("weekend"))
					condition = "(WEEKDAY(FROM_UNIXTIME(TIMESTAMP))=" + 5
							+ " or WEEKDAY(FROM_UNIXTIME(TIMESTAMP))=" + 6
							+ ") ";
			}
		}

		if (season != null && season.trim().length() != 0) {

			if (season.equals("winter")) {

				condition = condition
						+ " and "
						+ "((MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='November') "
						+ "or (MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='December') or "
						+ "(MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='January')) ";

			} else if (season.equals("spring")) {

				condition = condition
						+ " and "
						+ "((MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='February') "
						+ "or (MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='March') or "
						+ "(MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='April')) ";

			} else if (season.equals("summer")) {

				condition = condition + " and "
						+ "((MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='May') "
						+ "or (MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='June') or "
						+ "(MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='July')) ";

			} else if (season.equals("autumn")) {

				condition = condition
						+ " and "
						+ "((MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='August') "
						+ "or (MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='September') or "
						+ "(MONTHNAME(FROM_UNIXTIME(TIMESTAMP))='October')) ";
			}

		}

		questionFileReader qfr = new questionFileReader();
		HashMap<Integer, Integer> numericalQuestions = qfr.numercalQuestion();
		HashMap<Integer, Integer> omittedQuestions = qfr.omittedQuestion();

		Connection con = null;
		Statement stm = null;
		ResultSet rs = null;

		String url = "jdbc:mysql://localhost/gsn";
		String user = "gsn";
		String password = "gsnpassword";

		try {
			con = DriverManager.getConnection(url, user, password);
			stm = con.createStatement();

			// retrieve all of the meter ids and save them in users array list
			rs = stm.executeQuery("select distinct METER_ID from "
					+ electricDataTable);
			int userNum = 0;
			ArrayList<String> users = new ArrayList<String>();
			HashMap<Integer, Integer> userIndex = new HashMap<Integer, Integer>();

			while (rs.next()) {

				users.add(rs.getString("METER_ID"));
				userIndex.put(rs.getInt("METER_ID"), userNum);
				userNum++;
			}

			int[][] userAnswer = new int[userNum][questionNum];

			for (int usr = 0; usr < userNum; usr++)
				for (int qid = 0; qid < questionNum; qid++)
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

			// retrieve the average of energy consumption for all of the user
			// per hour based on the season (all,winter,spring,summer and autumn)
			// Save the values in the matrix
			// energyConsumtion[userNumber][24]---24 hours

			double[][] energyConsumption = new double[userNum][24];

			rs = stm.executeQuery("select METER_ID, HOUR(FROM_UNIXTIME(TIMESTAMP)) as hour, avg(VALUE) as avg from "
					+ electricDataTable
					+ " where "
					+ condition
					+ " group by HOUR(FROM_UNIXTIME(TIMESTAMP)), METER_ID ");

			while (rs.next()) {

				int idIndex = userIndex.get(rs.getInt("METER_ID"));
				energyConsumption[idIndex][rs.getInt("hour")] = rs
						.getFloat("avg");
			}

			// Declare numeric attributes

			Attribute[] Q = new Attribute[questionNum];

			for (int qid = 0; qid < questionNum; qid++) {

				if (!omittedQuestions.containsKey(qid + 1)) {

					if (numericalQuestions.containsKey(qid + 1))// if the question is numeric
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

						elements.add("99999");
						elemNum++;

						Iterator<String> itrAttr = elements.iterator();

						FastVector nominalAttr = new FastVector(elemNum);
						for (int elem = 0; elem < elemNum; elem++) {

							String st = itrAttr.next().toString();
							// System.out.println(st);
							nominalAttr.addElement(st);
						}

						Q[qid] = new Attribute("Q" + String.valueOf(qid),
								nominalAttr);
					}
				}
			}

			// Declare the class attribute
			Attribute consumption = new Attribute("consumption");

			// Declare the feature vector

			FastVector fvAttributes = new FastVector(141);
			for (int j = 0; j < questionNum; j++) {

				if (!omittedQuestions.containsKey(j + 1))
					fvAttributes.addElement(Q[j]);
			}

			fvAttributes.addElement(consumption);

			for (int h = 0; h <= 23; h++) {

				Instances dataset = new Instances("my_dataset", fvAttributes, 0);

				for (int id = 0; id < userNum; id++) {

					Instance ins = new Instance(141);
					for (int q = 0; q < questionNum; q++) {

						if (!omittedQuestions.containsKey(q + 1)) {

							if (numericalQuestions.containsKey(q + 1)) // if the question is numeric
								ins.setValue(Q[q], userAnswer[id][q]);
							else // if the question is nominal
							{
								ins.setValue(Q[q],
										String.valueOf(userAnswer[id][q]));
							}
						}
					}

					ins.setValue(consumption, energyConsumption[id][h]);
					ins.setDataset(dataset);
					dataset.add(ins);
					// System.out.println(ins);

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
			 * System.out.println(questionRank[l]+"     "+l); }
			 */

		} catch (SQLException ex) {
			System.out.println("SQLException:" + ex);

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
				System.out.println("SQLException:" + ex);
			}
		}

		return questionRank;
	}

}
