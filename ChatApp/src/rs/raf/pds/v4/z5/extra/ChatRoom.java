package rs.raf.pds.v4.z5.extra;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.esotericsoftware.kryonet.Connection;

public class ChatRoom {
	String name;
	ConcurrentMap<String, Connection> userConnectionMap = new ConcurrentHashMap<String, Connection>();
	ConcurrentMap<Connection, String> connectionUserMap = new ConcurrentHashMap<Connection, String>();
	
	public ChatRoom() {
    }
	
	public ChatRoom(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public ConcurrentMap<String, Connection> getUserConnectionMap() {
        return userConnectionMap;
    }

    public ConcurrentMap<Connection, String> getConnectionUserMap() {
        return connectionUserMap;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public void addUserConnection(String userName, Connection connection) {
        userConnectionMap.put(userName, connection);
        connectionUserMap.put(connection, userName);
    }
    
    public void removeUserConnection(String userName) {
        Connection connection = userConnectionMap.remove(userName);
        if (connection != null) {
            connectionUserMap.remove(connection);
        }
    }
}
