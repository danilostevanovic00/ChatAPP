package rs.raf.pds.v4.z5.messages;

public class GetMoreMessagesMesage {
	String chatRoom;
	
	protected GetMoreMessagesMesage() {
		
	}
	public GetMoreMessagesMesage(String chatRoom) {
		this.chatRoom = chatRoom;
	}

	public String getRoomName() {
		return this.chatRoom;
	}
}
