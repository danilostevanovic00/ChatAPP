package rs.raf.pds.v4.z5.messages;

public class ListRooms {
	String[] rooms;
	
	protected ListRooms() {
		
	}
	public ListRooms(String[] rooms) {
		this.rooms = rooms;
	}

	public String[] getRooms() {
		return this.rooms;
	}
}
