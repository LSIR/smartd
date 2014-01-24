package gsn.smartd;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class questionFileReader {
	
	public questionFileReader(){
		
	}
	
	public HashMap<Integer, Integer> numercalQuestion(){
		
		//read from the file that contains the id of the numeric questions and save the question ids in numericalQuestions
    	HashMap<Integer, Integer> numericalQuestions = new HashMap<Integer, Integer>();
		BufferedReader br = null;
		 
			try {
	 
				String attr;
	 
				br = new BufferedReader(new FileReader("./SmartD/numerical-question-full.txt"));
	 
				while ((attr = br.readLine()) != null) {
					
					int numAttr = Integer.valueOf(attr);
					numericalQuestions.put(numAttr,numAttr);
				}
	 
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (br != null)br.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
			
			return numericalQuestions;
		
	}
	
	public HashMap<Integer, Integer> omittedQuestion(){
		
		//read from the file that contains the id of the omitted questions and save the question ids in omittedQuestions
		HashMap<Integer, Integer> omittedQuestions = new HashMap<Integer, Integer>();
		BufferedReader br = null;
		 
			try {
	 
				String attr;
	 
				br = new BufferedReader(new FileReader("./SmartD/omitted_question.txt"));
	 
				while ((attr = br.readLine()) != null) {						
					int numAttr = Integer.valueOf(attr);
					omittedQuestions.put(numAttr,numAttr);
					//logger.warn(omittedQuestions.get(numAttr));
				}
	 
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (br != null)br.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
	
			
			return omittedQuestions;
		
	}

}
