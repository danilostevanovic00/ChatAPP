package rs.raf.pds.v4.z5.messages;

public class ListFiveAtJoin {
	ChatRoomMessage[] chatRoomMessages;
	
	protected ListFiveAtJoin() {
		
	}
	public ListFiveAtJoin(ChatRoomMessage[] chatRoomMessages) {
		this.chatRoomMessages = chatRoomMessages;
	}

	public ChatRoomMessage[] getListFiveAtJoin() {
		return this.chatRoomMessages;
	}
	
}
