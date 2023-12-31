package rs.raf.pds.v4.z5.messages;

public class ListAllFromRoom {
	ChatRoomMessage[] chatRoomMessages;
	
	protected ListAllFromRoom() {
		
	}
	public ListAllFromRoom(ChatRoomMessage[] chatRoomMessages) {
		this.chatRoomMessages = chatRoomMessages;
	}

	public ChatRoomMessage[] getListOfAll() {
		return this.chatRoomMessages;
	}
}
