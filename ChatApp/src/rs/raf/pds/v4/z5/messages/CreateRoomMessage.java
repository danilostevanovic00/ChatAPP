package rs.raf.pds.v4.z5.messages;

public class CreateRoomMessage {
	String roomName;
	
	public CreateRoomMessage() {
    }
	
	public CreateRoomMessage(String roomName) {
        this.roomName = roomName;
    }

    public String getRoomName() {
        return roomName;
    }
}
