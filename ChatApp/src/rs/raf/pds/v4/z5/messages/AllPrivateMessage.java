package rs.raf.pds.v4.z5.messages;

public class AllPrivateMessage {
	PrivateMessage[] privateMessages;
	
	protected AllPrivateMessage() {
		
	}
	public AllPrivateMessage(PrivateMessage[] privateMessages) {
		this.privateMessages = privateMessages;
	}

	public PrivateMessage[] getListOfAll() {
		return this.privateMessages;
	}
}
