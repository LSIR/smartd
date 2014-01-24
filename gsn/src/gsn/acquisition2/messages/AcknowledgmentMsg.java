package gsn.acquisition2.messages;
/**
 * Represents Ack (success) and Nack (failure) messages.
 */
public class AcknowledgmentMsg extends AbstractMessage {
  
  private static final long serialVersionUID = 3096327899834535287L;

  public static transient final int SUCCESS = 1;  
  public static transient final int FAILURE = 2;
  
  private long seqNumber = -1;
  
  private int value = -1;
  
  public boolean isAck() {
    return value == SUCCESS;
  }
  
  public boolean isNack() {
    return value == FAILURE;
  }

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }

  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + value;
    return result;
  }

  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final AcknowledgmentMsg other = (AcknowledgmentMsg) obj;
    if (value != other.value)
      return false;
    return true;
  }

  public AcknowledgmentMsg(int messageType,long seqNo) {
    this.value = messageType;
    this.seqNumber=seqNo;
  }
  
  public AcknowledgmentMsg(int messageType) {
    this.value = messageType;
  }

  public long getSeqNumber() {
    return seqNumber;
  }
  
 
}
