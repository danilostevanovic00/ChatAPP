package rs.raf.pds.v4.z5.messages;

public class EditChatRoomMessage {
	String newMessage;
	String originalMessage;
	String recipient;
	String sender;
	
	protected EditChatRoomMessage() {
	}
	
	public EditChatRoomMessage (String newMessage,String originalMessage,String recipient,String sender){
		this.newMessage = newMessage;
		this.originalMessage = originalMessage;
		this.recipient = recipient;
		this.sender = sender;
	}
	public String getNewMessage() {
		return this.newMessage;
	}
	
	public String getoriginalMessage() {
		return this.originalMessage;
	}
	public String getRecipient() {
		return this.recipient;
	}
	
	public String getSender() {
		return this.sender;
	}
}
