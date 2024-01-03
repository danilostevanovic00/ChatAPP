package rs.raf.pds.v4.z5;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

import rs.raf.pds.v4.z5.extra.ChatRoom;
import rs.raf.pds.v4.z5.messages.ChatMessage;
import rs.raf.pds.v4.z5.messages.ChatRoomMessage;
import rs.raf.pds.v4.z5.messages.CreateRoomMessage;
import rs.raf.pds.v4.z5.messages.GetMoreMessagesMesage;
import rs.raf.pds.v4.z5.messages.PrivateMessage;
import rs.raf.pds.v4.z5.messages.InfoMessage;
import rs.raf.pds.v4.z5.messages.InviteToRoomMessage;
import rs.raf.pds.v4.z5.messages.JoinRoomMessage;
import rs.raf.pds.v4.z5.messages.KryoUtil;
import rs.raf.pds.v4.z5.messages.ListAllFromRoom;
import rs.raf.pds.v4.z5.messages.ListFiveAtJoin;
import rs.raf.pds.v4.z5.messages.ListRooms;
import rs.raf.pds.v4.z5.messages.ListUsers;
import rs.raf.pds.v4.z5.messages.Login;
import rs.raf.pds.v4.z5.messages.WhoRequest;
import rs.raf.pds.v4.z5.messages.WhoRoomRequest;


public class ChatServer implements Runnable{

	private volatile Thread thread = null;
	
	volatile boolean running = false;
	final Server server;
	final int portNumber;
	ConcurrentMap<String, Connection> userConnectionMap = new ConcurrentHashMap<String, Connection>();
	ConcurrentMap<Connection, String> connectionUserMap = new ConcurrentHashMap<Connection, String>();
	
	ConcurrentMap<String, Queue<PrivateMessage>> privateMessages = new ConcurrentHashMap<>();
	
	ConcurrentMap<String, ChatRoom> chatRooms = new ConcurrentHashMap<>();
	ConcurrentMap<String, ArrayList<ChatRoomMessage>> chatRoomsMessages = new ConcurrentHashMap<>();
	
	public ChatServer(int portNumber) {
		this.server = new Server();
		
		this.portNumber = portNumber;
		KryoUtil.registerKryoClasses(server.getKryo());
		registerListener();
	}
	private void registerListener() {
		server.addListener(new Listener() {
			public void received (Connection connection, Object object) {
				if (object instanceof Login) {
					Login login = (Login)object;
					newUserLogged(login, connection);
					connection.sendTCP(new InfoMessage("Hello "+login.getUserName()));
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return;
				}
				
				if (object instanceof ChatMessage) {
					ChatMessage chatMessage = (ChatMessage)object;
					System.out.println(chatMessage.getUser()+":"+chatMessage.getTxt());
					broadcastChatMessage(chatMessage, connection); 
					return;
				}
				
				if (object instanceof PrivateMessage) {
					PrivateMessage privateMessage = (PrivateMessage) object;
				    String recipient = privateMessage.getRecipient();
				    Queue<PrivateMessage> recipientQueue = privateMessages.get(recipient);
				    if (recipientQueue != null) {
				        recipientQueue.offer(privateMessage);
				    }
				    sendPrivateChatMessage(privateMessage,connection);
				    return;
				}
				
				if (object instanceof ChatRoomMessage) {
					ChatRoomMessage chatRoomMessage = (ChatRoomMessage) object;
				    ChatRoom chatRoom = chatRooms.get(chatRoomMessage.getRoomName());
				    String user = chatRoom.getUserByConn(connection);
				    if (user != null) {
				    	sendChatRoomMessage(chatRoomMessage,connection);
				    	chatRoomsMessages.get(chatRoomMessage.getRoomName()).add(chatRoomMessage);
				    }
				    return;
				}
				
				if (object instanceof InviteToRoomMessage) {
					InviteToRoomMessage inviteToRoomMessage = (InviteToRoomMessage) object;
				    newInviteToRoom(inviteToRoomMessage,connection);
				    return;
				}
				
				if (object instanceof CreateRoomMessage) {
					CreateRoomMessage createRoomMessage = (CreateRoomMessage) object;
				    String roomName = createRoomMessage.getRoomName();
				    newRoomCreated(createRoomMessage,connection);
				    showTextToOne("Room "+roomName+" created!",connection);
				    newJoinToRoom(new JoinRoomMessage(roomName),connection);
				    showTextToOne("User "+connectionUserMap.get(connection)+" joined room "+ roomName,connection);
				    chatRoomsMessages.put(roomName, new ArrayList<>());
				    showTextToOne("Created list for messages for room "+ roomName,connection);
				    return;
				}
				
				if (object instanceof JoinRoomMessage) {
					JoinRoomMessage joinRoomMessage = (JoinRoomMessage) object;
				    String roomName = joinRoomMessage.getRoomName();
				    newJoinToRoom(joinRoomMessage,connection);
				    showTextToOne("User "+connectionUserMap.get(connection)+" joined room "+ roomName,connection);
				    connection.sendTCP(new ListFiveAtJoin(getFiveRoomMessages(roomName)));
				    return;
				}
				
				if (object instanceof GetMoreMessagesMesage) {
					GetMoreMessagesMesage getAllRoomMessage = (GetMoreMessagesMesage) object;
				    String roomName = getAllRoomMessage.getRoomName();
				    connection.sendTCP(new ListAllFromRoom(getAllRoomMessages(roomName)));
				    return;
				}

				if (object instanceof WhoRequest) {
					ListUsers listUsers = new ListUsers(getAllUsers());
					connection.sendTCP(listUsers);
					return;
				}
				
				if (object instanceof WhoRoomRequest) {
					ListRooms listRooms = new ListRooms(getAllRooms());
					connection.sendTCP(listRooms);
					return;
				}
			}
			
			public void disconnected(Connection connection) {
				String user = connectionUserMap.get(connection);
				connectionUserMap.remove(connection);
				userConnectionMap.remove(user);
				showTextToAll(user+" has disconnected!", connection);
			}
		});
	}
	
	String[] getAllUsers() {
		String[] users = new String[userConnectionMap.size()];
		int i=0;
		for (String user: userConnectionMap.keySet()) {
			users[i] = user;
			i++;
		}
		return users;
	}
	
	ChatRoomMessage[] getFiveRoomMessages(String roomName) {
		ChatRoomMessage[] roomMessages = new ChatRoomMessage[5];
		ArrayList<ChatRoomMessage> roomMessageList = chatRoomsMessages.get(roomName);
		System.out.println(roomMessageList);
		List<ChatRoomMessage> listOfFive;
		if (roomMessageList.size()>5) {
			listOfFive = roomMessageList.subList(roomMessageList.size()-5, roomMessageList.size());
		}else {
			listOfFive = roomMessageList;
		}
		int i = 0;
		for (ChatRoomMessage crm: listOfFive) {
			roomMessages[i]=crm;
			i++;
		}
		return roomMessages;
	}
	
	ChatRoomMessage[] getAllRoomMessages(String roomName) {
		ChatRoomMessage[] roomMessages = new ChatRoomMessage[chatRoomsMessages.get(roomName).size()];
		ArrayList<ChatRoomMessage> roomMessageList = chatRoomsMessages.get(roomName);
		int i = 0;
		for (ChatRoomMessage crm : roomMessageList) {
			roomMessages[i]= crm;
			i++;
		}
		return roomMessages;
	}
	
	String[] getAllRooms() {
		String[] rooms = new String[chatRooms.size()];
		int i=0;
		for (String room: chatRooms.keySet()) {
			rooms[i] = room;
			i++;
		}
		return rooms;
	}
	
	private void newUserLogged(Login loginMessage, Connection conn) {
		userConnectionMap.put(loginMessage.getUserName(), conn);
		connectionUserMap.put(conn, loginMessage.getUserName());
		privateMessages.put(loginMessage.getUserName(), new LinkedList<>());
		showTextToAll("User "+loginMessage.getUserName()+" has connected!", conn);
	}
	
	private void newRoomCreated(CreateRoomMessage createRoomMessage,Connection conn) {
		ChatRoom newChatRoom = new ChatRoom(createRoomMessage.getRoomName());
		chatRooms.put(createRoomMessage.getRoomName(),newChatRoom );
	}
	
	private void newJoinToRoom(JoinRoomMessage joinRoomMessage,Connection conn) {
		String roomName = joinRoomMessage.getRoomName();
		String userName = connectionUserMap.get(conn);
		ChatRoom room = chatRooms.get(roomName);
		room.addUserConnection(userName, conn);
	}
	
	private void newInviteToRoom(InviteToRoomMessage inviteToRoomMessage,Connection conn) {
		String roomName = inviteToRoomMessage.getRoomName();
		String invited = inviteToRoomMessage.getInvited();
		String userThatInvited = connectionUserMap.get(conn);
		showTextToOne("User "+userThatInvited+" inveted you to room "+roomName,userConnectionMap.get(invited));
	}
	
	private void broadcastChatMessage(ChatMessage message, Connection exception) {
		for (Connection conn: userConnectionMap.values()) {
			if (conn.isConnected() && conn != exception)
				conn.sendTCP(message);
		}
	}
	
	private void sendChatRoomMessage(ChatRoomMessage message, Connection exception) {
		for (Connection conn: chatRooms.get(message.getRoomName()).getUserConnectionMap().values()) {
			if (conn.isConnected() && conn != exception)
				conn.sendTCP(message);
		}
	}
	
	private void sendPrivateChatMessage(PrivateMessage message, Connection exception) {
		Connection reciver = userConnectionMap.get(message.getRecipient());
		reciver.sendTCP(message);
	}
	
	private void showTextToAll(String txt, Connection exception) {
		System.out.println(txt);
		for (Connection conn: userConnectionMap.values()) {
			if (conn.isConnected() && conn != exception)
				conn.sendTCP(new InfoMessage(txt));
		}
	}
	
	private void showTextToOne(String txt, Connection connection) {
		System.out.println(txt);
		connection.sendTCP(new InfoMessage(txt));
	}
	
	public void start() throws IOException {
		server.start();
		server.bind(portNumber);
		
		if (thread == null) {
			thread = new Thread(this);
			thread.start();
		}
	}
	public void stop() {
		Thread stopThread = thread;
		thread = null;
		running = false;
		if (stopThread != null)
			stopThread.interrupt();
	}
	@Override
	public void run() {
		running = true;
		
		while(running) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	public static void main(String[] args) {
		
		if (args.length != 1) {
	        System.err.println("Usage: java -jar chatServer.jar <port number>");
	        System.out.println("Recommended port number is 54555");
	        System.exit(1);
	   }
	    
	   int portNumber = Integer.parseInt(args[0]);
	   try { 
		   ChatServer chatServer = new ChatServer(portNumber);
	   	   chatServer.start();
	   
			chatServer.thread.join();
	   } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	   } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	   }
	}
	
   
   
}
