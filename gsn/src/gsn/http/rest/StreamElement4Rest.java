package gsn.http.rest;

import gsn.beans.DataField;
import gsn.beans.StreamElement;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.thoughtworks.xstream.XStream;

public class StreamElement4Rest {
	
	private List<Field4Rest> fields = new ArrayList<Field4Rest>();

	public StreamElement4Rest(StreamElement se) {
		this.timestamp=new Date(se.getTimeStamp());
		for (int i =0;i<se.getFieldNames().length;i++) {
			fields.add( new Field4Rest(se.getFieldNames()[i],se.getFieldTypes()[i],se.getData()[i]));
		}
	}
	
	public String toString() {
        StringBuilder sb = new StringBuilder("StreamElement4Rest: (timestamp:");
        sb.append(timestamp.toString());
		sb.append("Fields =>{ ");
		for (Field4Rest field : fields)
			sb.append(field.toString()).append(", ");
        sb.append("})");
		return sb.toString();
    }

	private Date timestamp;

	public StreamElement toStreamElement() {
		String[] names = new String[fields.size()];
		Serializable[] values = new Serializable[fields.size()];
		Byte[] types = new Byte[fields.size()];
		int idx = 0;
		for (Field4Rest field : fields) {
			names[idx] = field.getName();
			values[idx] = field.getValue();
			types[idx] = field.getType();
			idx++;
		}
		
		return new StreamElement(names,types,values,timestamp.getTime());

	}
	
	public static XStream getXstream() {
		XStream xstream = new XStream();
		xstream.alias("stream-element", StreamElement4Rest.class);
		xstream.alias("field", Field4Rest.class);
		xstream.useAttributeFor(StreamElement4Rest.class,"timestamp");
		xstream.addImplicitCollection(StreamElement4Rest.class, "fields");
		xstream.registerConverter(new Field4RestConverter());
		xstream.alias("strcture", DataField.class);
		
		
		return xstream;
	}
	
	public static XStream getXstream4Structure() {
		XStream xstream = new XStream();
//		xstream.alias("stream-element", StreamElement4Rest.class);
//		xstream.alias("field", Field4Rest.class);
//		xstream.useAttributeFor(StreamElement4Rest.class,"timestamp");
//		xstream.addImplicitCollection(StreamElement4Rest.class, "fields");
//		xstream.registerConverter(new Field4RestConverter());
		return xstream;
	}

	public Date getTimestamp() {
		return timestamp;
	}
	
	
	

}
