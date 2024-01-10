package rs.raf.pds.v4.z5.messages;

public class ChatRoomMessage {
	String user;
	String roomName;
	String message;
	long timestamp= System.currentTimeMillis();
	
	protected ChatRoomMessage() {
	}
	
	public ChatRoomMessage(String user, String roomName, String message) {
		this.user = user;
		this.message = message;
		this.roomName = roomName;
	}
	
	public String getMessage() {
		return this.message;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getRoomName() {
		return this.roomName;
	}
	
	public String getUser() {
		return this.user;
	}
	
	public long getTimestamp() {
		return this.timestamp;
	}
}
