package rs.raf.pds.v4.z5.messages;

public class JoinRoomMessage {
	String roomName;
	
	public JoinRoomMessage() {
    }
	
	public JoinRoomMessage(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomName() {
        return roomName;
    }
}
