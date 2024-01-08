package rs.raf.pds.v4.z5.messages;

public class RequestPrivateMessage {
	String sender;
	String reciver;
	
	protected RequestPrivateMessage() {
		
	}
	public RequestPrivateMessage(String sender, String reciver) {
		this.sender = sender;
		this.reciver = reciver;
	}

	public String getSender() {
		return this.sender;
	}
	
	public String getReciver() {
		return this.reciver;
	}
}
