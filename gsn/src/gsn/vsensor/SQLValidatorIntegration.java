package gsn.vsensor;

import gsn.Main;
import gsn.VSensorStateChangeListener;
import gsn.beans.VSensorConfig;
import gsn.storage.SQLValidator;
import gsn.storage.StorageManager;

import java.sql.SQLException;

import org.apache.log4j.Logger;

public class SQLValidatorIntegration implements VSensorStateChangeListener{
	
	private SQLValidator validator;
	
	public SQLValidatorIntegration(SQLValidator validator) throws SQLException {
		this.validator = validator;
	}
	

	private static final transient Logger logger = Logger.getLogger(SQLValidatorIntegration.class);

	public boolean vsLoading(VSensorConfig config) {
		try {
            String ddl = Main.getValidationStorage().getStatementCreateTable(config.getName(), config.getOutputStructure(), validator.getSampleConnection()).toString();
			validator.executeDDL(ddl);
		}catch (Exception e) {
			logger.error(e.getMessage(),e);
		}
		return true;
	}

	public boolean vsUnLoading(VSensorConfig config) {
		try {
			String ddl = Main.getValidationStorage().getStatementDropTable(config.getName(), validator.getSampleConnection()).toString();
			validator.executeDDL(ddl);
		}catch (Exception e) {
			logger.error(e.getMessage(),e);
			return false;
		}
		return true;
	}

	public void release() throws Exception {
		validator.release();
		
	}
}
