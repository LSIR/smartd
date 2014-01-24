
SmartD version 1.0 29.12.2013

Installation and running from Eclipse:
-------------------------------------------------
1. Create a new database and call it "gsn"
2. Set the username equal to "gsn" and the password equal to "gsnpassword"
3. Import the tables (electric_data.sql,ireland_survey_answer_options.sql,ireland_survey_answer.sql 
and ireland_survey_question.sql) located in the tables folder to gsn database.

NOTE: this step 4 assumes that you have already gsn in your eclipse (and you understand how to run gsn).
If not, please check README-gsn-related.txt (then you will understand step 4-6 easily).

4. Choose Java 1.6 in your eclipse
5. Right click on the build.xml, choose Run As and choose [2 Ant Build...].
There are a lot of check boxes. First, to build GSN, choose build (default) and run it.
6. To start GSN, choose check box gsn, and run it. If you still have build checked, then uncheck it.

SmartD can be reached at:
web site : http://localhost:22001/SmartDindex.html


