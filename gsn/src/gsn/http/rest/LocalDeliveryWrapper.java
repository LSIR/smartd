package gsn.http.rest;

import gsn.DataDistributer;
import gsn.Mappings;
import gsn.VirtualSensorInitializationFailedException;
import gsn.beans.AddressBean;
import gsn.beans.DataField;
import gsn.beans.StreamElement;
import gsn.beans.VSensorConfig;
import gsn.storage.SQLUtils;
import gsn.storage.SQLValidator;
import gsn.utils.Helpers;
import gsn.vsensor.AbstractVirtualSensor;
import gsn.wrappers.AbstractWrapper;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Date;

import javax.naming.OperationNotSupportedException;

import org.apache.log4j.Logger;
import org.joda.time.format.ISODateTimeFormat;

public class LocalDeliveryWrapper extends AbstractWrapper implements DeliverySystem{

	private  final String CURRENT_TIME = ISODateTimeFormat.dateTime().print(System.currentTimeMillis());
	
	private static transient Logger                  logger           = Logger.getLogger( LocalDeliveryWrapper.class );
	
	private VSensorConfig vSensorConfig;
	
	public VSensorConfig getVSensorConfig() {
		return vSensorConfig;
	}
	
	private DataField[] structure;
	
	private DefaultDistributionRequest distributionRequest;

	public String getWrapperName() {
		return "Local-wrapper";
	}

	public boolean initialize() {
		AddressBean params = getActiveAddressBean( );
		String query = params.getPredicateValue("query");
		
		String vsName = params.getPredicateValue( "name" );
		String startTime = params.getPredicateValueWithDefault("start-time",CURRENT_TIME );

		if (query==null && vsName == null) {
			logger.error("For using local-wrapper, either >query< or >name< parameters should be specified"); 
			return false;
		}

		if (query == null) 
			query = "select * from "+vsName;

		long lastVisited;
		try {
			lastVisited = Helpers.convertTimeFromIsoToLong(startTime);
		}catch (Exception e) {
			logger.error("Problem in parsing the start-time parameter, the provided value is:"+startTime+" while a valid input is:"+CURRENT_TIME);
			logger.error(e.getMessage(),e);
			return false;
		}
		try {
			vsName = SQLValidator.getInstance().validateQuery(query);
			if(vsName==null) //while the other instance is not loaded.
				return false;
			query = SQLUtils.newRewrite(query, vsName, vsName.toLowerCase()).toString();
			
			logger.debug("Local wrapper request received for: "+vsName);
			
			vSensorConfig = Mappings.getConfig(vsName);
			distributionRequest = DefaultDistributionRequest.create(this, vSensorConfig, query, lastVisited);
			// This call MUST be executed before adding this listener to the data-distributer because distributer checks the isClose method before flushing.
		}catch (Exception e) {
			logger.error("Problem in the query parameter of the local-wrapper.");
			logger.error(e.getMessage(),e);
			return false;
		}
		return true;
	}

	public boolean sendToWrapper ( String action,String[] paramNames, Serializable[] paramValues ) throws OperationNotSupportedException {
		AbstractVirtualSensor vs;
		try {
			vs = Mappings.getVSensorInstanceByVSName( vSensorConfig.getName( ) ).borrowVS( );
		} catch ( VirtualSensorInitializationFailedException e ) {
			logger.warn("Sending data back to the source virtual sensor failed !: "+e.getMessage( ),e);
			return false;
		}
		boolean toReturn = vs.dataFromWeb( action , paramNames , paramValues );
		Mappings.getVSensorInstanceByVSName( vSensorConfig.getName( ) ).returnVS( vs );
		return toReturn;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("LocalDistributionReq => [" ).append(distributionRequest.getQuery()).append(", Start-Time: ").append(new Date(distributionRequest.getStartTime())).append("]");
		return sb.toString();
	}
	
	public void run() {
		DataDistributer localDistributer = DataDistributer.getInstance(LocalDeliveryWrapper.class);
		localDistributer.addListener(this.distributionRequest);
	}

	public void writeStructure(DataField[] fields) throws IOException {
		this.structure=fields;
		
	}
	
	public DataField[] getOutputFormat() {
		return structure;
	}

	public void close() {
		logger.warn("Closing a local delivery.");
		try {
			releaseResources();
		} catch (SQLException e) {
			logger.error(e.getMessage(),e);
		}
		
	}

	public boolean isClosed() {
		return !isActive();
	}

	public boolean writeStreamElement(StreamElement se) {
		boolean isSucced = postStreamElement(se);
		logger.debug("wants to deliver stream element:"+ se.toString()+ "["+isSucced+"]");
		return true;
	}

    public boolean writeKeepAliveStreamElement() {
        return true;
    }

    public void dispose() {
		
	}


}
