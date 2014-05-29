package gsn.http;

import gsn.smartd.questionSelectionRanking;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;

public class SmartDQuestionSelection implements RequestHandler{
	
private static transient Logger logger = Logger.getLogger(SmartDQuestionSelection.class);
	
	public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
		
		logger.warn("START_QSELECT");

		int questionNum = 5; // optimal questions number
		StringBuilder sb = null;

		String weekDay = request.getParameter("day");
		String season = request.getParameter("season");

		questionSelectionRanking qsr = new questionSelectionRanking();
		int[] questionRank = qsr.questionRank(weekDay, season);

		// find the first questionNum(here is 5) question ids that were ranked
		// the highest and save them in the array questionRankSort

		int[] questionRankSort = new int[questionNum];

		int max = Integer.MIN_VALUE;
		int qid = -1;

		for (int rank = 0; rank < questionNum; rank++) {

			for (int q = 0; q < 143; q++) {

				if (questionRank[q] > max) {
					max = questionRank[q];
					qid = q;
				}

			}
			//System.err.println(qid + ":" + questionRank[qid]);
			questionRankSort[rank] = qid + 1;
			questionRank[qid] = Integer.MIN_VALUE;
			max = Integer.MIN_VALUE;
			qid = -1;
		}

		// returns questionNum(here is 5) highest ranked question ids

		sb = new StringBuilder("<result>\n");
		sb.append("<stream-element>\n");

		for (int rank = 0; rank < questionNum; rank++) {

			sb.append("<field name=\"")
					.append("Question" + (rank + 1) + "\">")
					.append(StringEscapeUtils.escapeXml(String
							.valueOf(questionRankSort[rank])))
					.append("</field>\n");

		}

		sb.append("</stream-element>\n");
		sb.append("</result>");

		response.setHeader("Cache-Control", "no-store");
		response.setDateHeader("Expires", 0);
		response.setHeader("Pragma", "no-cache");
		response.getWriter().write(sb.toString());

		logger.warn("END_QSELECTION");

	}

	public boolean isValid(HttpServletRequest request, HttpServletResponse response) throws IOException {    	
        
        return true;
    }

}
