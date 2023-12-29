package rs.raf.pds.v4.z5.messages;

public class InviteToRoomMessage {
	String roomName;
	String invited;
	
	public InviteToRoomMessage() {
    }
	
	public InviteToRoomMessage(String roomName, String invited) {
        this.roomName = roomName;
        this.invited = invited;
    }

    public String getRoomName() {
        return roomName;
    }
    
    public String getInvited() {
        return invited;
    }
}
