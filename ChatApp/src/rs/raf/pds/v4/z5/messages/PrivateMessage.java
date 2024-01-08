package rs.raf.pds.v4.z5.messages;

public class PrivateMessage {
	String recipient;
	String user;
	String txt;
	long timestamp= System.currentTimeMillis();
	
	protected PrivateMessage() {
		
	}

	public PrivateMessage (String recipient, String user, String txt){
		this.user = user;
		this.txt = txt;
		this.recipient= recipient;
	}
	
	public String getUser() {
		return user;
	}

	public String getTxt() {
		return txt;
	}
	
	public String getRecipient() {
		return this.recipient;
	}
	
	public long getTimestamp() {
		return this.timestamp;
	}
	
	

}
