package rs.raf.pds.v4.z5.messages;

public class AllChatMessage {
	ChatRoomMessage[] chatRoomMessages;
	
	protected AllChatMessage() {
		
	}
	public AllChatMessage(ChatRoomMessage[] chatRoomMessages) {
		this.chatRoomMessages = chatRoomMessages;
	}

	public ChatRoomMessage[] getListOfAll() {
		return this.chatRoomMessages;
	}
}
