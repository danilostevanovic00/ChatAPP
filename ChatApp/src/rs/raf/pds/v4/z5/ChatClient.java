package rs.raf.pds.v4.z5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import rs.raf.pds.v4.z5.messages.AllChatMessage;
import rs.raf.pds.v4.z5.messages.AllPrivateMessage;
import rs.raf.pds.v4.z5.messages.ChatMessage;
import rs.raf.pds.v4.z5.messages.ChatRoomMessage;
import rs.raf.pds.v4.z5.messages.CreateRoomMessage;
import rs.raf.pds.v4.z5.messages.EditChatRoomMessage;
import rs.raf.pds.v4.z5.messages.GetMoreMessagesMesage;
import rs.raf.pds.v4.z5.messages.InfoMessage;
import rs.raf.pds.v4.z5.messages.InviteToRoomMessage;
import rs.raf.pds.v4.z5.messages.JoinRoomMessage;
import rs.raf.pds.v4.z5.messages.KryoUtil;
import rs.raf.pds.v4.z5.messages.ListAllFromRoom;
import rs.raf.pds.v4.z5.messages.ListFiveAtJoin;
import rs.raf.pds.v4.z5.messages.ListRooms;
import rs.raf.pds.v4.z5.messages.ListUsers;
import rs.raf.pds.v4.z5.messages.Login;
import rs.raf.pds.v4.z5.messages.PrivateMessage;
import rs.raf.pds.v4.z5.messages.RequestPrivateMessage;
import rs.raf.pds.v4.z5.messages.WhoRequest;
import rs.raf.pds.v4.z5.messages.WhoRoomRequest;

public class ChatClient implements Runnable{

	public static int DEFAULT_CLIENT_READ_BUFFER_SIZE = 1000000;
	public static int DEFAULT_CLIENT_WRITE_BUFFER_SIZE = 1000000;
	
	private volatile Thread thread = null;
	
	volatile boolean running = false;
	
	final Client client;
	final String hostName;
	final int portNumber;
	final String userName;
	
	private List<ChatClientObserver> observers = new ArrayList<>();
	private List<ChatClientMessageObserver> observersMessage = new ArrayList<>();
	private List<ChatRoomMessageObserver> observersRoomMessage = new ArrayList<>();
	
	public ChatClient(String hostName, int portNumber, String userName) {
		this.client = new Client(DEFAULT_CLIENT_WRITE_BUFFER_SIZE, DEFAULT_CLIENT_READ_BUFFER_SIZE);
		
		this.hostName = hostName;
		this.portNumber = portNumber;
		this.userName = userName;
		KryoUtil.registerKryoClasses(client.getKryo());
		registerListener();
	}
	
	//
	
	public interface ChatClientObserver {
	    void onRecivedListOfEntity(String[] result);
	}
	
	public void addObserver(ChatClientObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(ChatClientObserver observer) {
        observers.remove(observer);
    }

    public void forwardToMain(String[] entity) {
        
        notifyObservers(entity);
    }

    private void notifyObservers(String[] result) {
        for (ChatClientObserver observer : observers) {
            observer.onRecivedListOfEntity(result);
        }
    }
    
    public interface ChatClientMessageObserver {
	    void onRecivedListOfMessages(PrivateMessage[] result);
	}
    public void addObserverForMessage(ChatClientMessageObserver observer) {
    	observersMessage.add(observer);
    }

    public void removeObserverMessages(ChatClientMessageObserver observer) {
    	observersMessage.remove(observer);
    }

    public void forwardToMainForMessage(PrivateMessage[] entity) {
        notifyObserversForMessages(entity);
    }

    private void notifyObserversForMessages(PrivateMessage[] result) {
        for (ChatClientMessageObserver observer : observersMessage) {
            observer.onRecivedListOfMessages(result);
        }
    }
    
    public interface ChatRoomMessageObserver {
	    void onRecivedListOfRoomMessages(ChatRoomMessage[] result);
	}
    public void addObserverForRoom(ChatRoomMessageObserver observer) {
    	observersRoomMessage.add(observer);
    }

    public void removeObserverRoom(ChatRoomMessageObserver observer) {
    	observersRoomMessage.remove(observer);
    }

    public void forwardToMainForRoom(ChatRoomMessage[] entity) {
        notifyObserversForRoomMessages(entity);
    }

    private void notifyObserversForRoomMessages(ChatRoomMessage[] result) {
        for (ChatRoomMessageObserver observer : observersRoomMessage) {
            observer.onRecivedListOfRoomMessages(result);
        }
    }
    
    public void contextSwitchChat(String selectedUser) {
    	RequestPrivateMessage requestPrivateMessage = new RequestPrivateMessage(userName,selectedUser);
	    client.sendTCP(requestPrivateMessage);
    }
	
	private void registerListener() {
		client.addListener(new Listener() {
			public void connected (Connection connection) {
				Login loginMessage = new Login(userName);
				client.sendTCP(loginMessage);
			}
			
			public void received (Connection connection, Object object) {
				if (object instanceof ChatMessage) {
					ChatMessage chatMessage = (ChatMessage)object;
					showChatMessage(chatMessage);
					return;
				}

				if (object instanceof ListUsers) {
					ListUsers listUsers = (ListUsers)object;
					showOnlineUsers(listUsers.getUsers());
					forwardToMain(listUsers.getUsers());
					return;
				}
				
				if (object instanceof ListRooms) {
					ListRooms listRooms = (ListRooms)object;
					showRooms(listRooms.getRooms());
					forwardToMain(listRooms.getRooms());
					return;
				}
				
				if (object instanceof ListFiveAtJoin) {
					ListFiveAtJoin listFiveMessages = (ListFiveAtJoin)object;
					showLastFiveMessages(listFiveMessages);
					return;
				}
				
				if (object instanceof ListAllFromRoom) {
					ListAllFromRoom listAllMessages = (ListAllFromRoom)object;
					showAllMessagesFromRoom(listAllMessages);
					forwardToMainForRoom(listAllMessages.getListOfAll());
					return;
				}
				
				if (object instanceof InfoMessage) {
					InfoMessage message = (InfoMessage)object;
					showMessage("Server:"+message.getTxt());
					return;
				}
				
				if (object instanceof ChatMessage) {
					ChatMessage message = (ChatMessage)object;
					showMessage(message.getUser()+"r:"+message.getTxt());
					return;
				}
				
				if (object instanceof ChatRoomMessage) {
					ChatRoomMessage message = (ChatRoomMessage)object;
					showMessage(message.getRoomName()+" from "+message.getUser()+" : "+message.getMessage());
					return;
				}
				
				if (object instanceof PrivateMessage) {
					PrivateMessage privateMessage = (PrivateMessage) object;
				    showPrivateMessage(privateMessage);
				    contextSwitchChat(privateMessage.getUser());
				    return;
				}
				
				if (object instanceof AllPrivateMessage) {
					AllPrivateMessage allOfPrivateMessage = (AllPrivateMessage) object;
					showAllPrivateMessage(allOfPrivateMessage);
					forwardToMainForMessage(allOfPrivateMessage.getListOfAll());
				    return;
				}
			}
			
			public void disconnected(Connection connection) {
				
			}
		});
	}
	
	private void showChatMessage(ChatMessage chatMessage) {
		System.out.println(chatMessage.getUser()+":"+chatMessage.getTxt());
	}
	
	private void showPrivateMessage(PrivateMessage privateMessage) {
	    System.out.println("[Private] " + privateMessage.getUser() + ": " + privateMessage.getTxt());
	}
	
	private void showAllPrivateMessage(AllPrivateMessage allPrivateMessage) {
	    PrivateMessage[] privateMessages = allPrivateMessage.getListOfAll();
	    System.out.println("Private message with requested user ");
	    
	    if (privateMessages.length == 0) {
	        System.out.print("No previous messages");
	    } else {
	        for (PrivateMessage pm : privateMessages) {
	            String user = pm.getUser();
	            String message = pm.getTxt();
	            System.out.println("[Private] " + user + " : " + message);
	        }
	    }
	}
	
	public void sendPrivateMessage(String recipient, String text) {
	    PrivateMessage privateMessage = new PrivateMessage(recipient, userName, text);
	    //addMessage(privateMessage);
	    client.sendTCP(privateMessage);
	}
	
	public void sendEditedMessage(String newMessage, String originalMessage,String recipient,String sender) {
    	client.sendTCP(new EditChatRoomMessage(newMessage,originalMessage,recipient, sender));
    }
	public void sendRoomMessage(String roomName, String text) {
		ChatRoomMessage chatRoomMessage = new ChatRoomMessage(userName,roomName, text);
	    //addMessage(privateMessage);
	    client.sendTCP(chatRoomMessage);
	}
	
	private void showMessage(String txt) {
		System.out.println(txt);
	}
	private void showOnlineUsers(String[] users) {
		System.out.print("Server:");
		for (int i=0; i<users.length; i++) {
			String user = users[i];
			System.out.print(user);
			System.out.printf((i==users.length-1?"\n":", "));
		}
	}
	
	private void showRooms(String[] rooms) {
		System.out.print("Server:");
		for (int i=0; i<rooms.length; i++) {
			String room = rooms[i];
			System.out.print(room);
			System.out.printf((i==rooms.length-1?"\n":", "));
		}
	}
	
	private void showLastFiveMessages(ListFiveAtJoin listOfFive) {
		System.out.print("Previous messages from room: \n");
		ChatRoomMessage[] chatRoomMessages = listOfFive.getListFiveAtJoin();
		if (chatRoomMessages[0]==null) {
			System.out.print("No previous messages");
		}else {
			for (int i = 0; i<chatRoomMessages.length; i++) {
				if (chatRoomMessages[i] == null) {
					break;
				}else {
					String user = chatRoomMessages[i].getUser();
					String message = chatRoomMessages[i].getMessage();
					System.out.println(user+" : "+message);
				}
			}
		}
	}
	
	private void showAllMessagesFromRoom(ListAllFromRoom listOfAll) {
		System.out.print("All previous messages from room: \n");
		ChatRoomMessage[] chatRoomMessages = listOfAll.getListOfAll();
		if (chatRoomMessages.length==0) {
			System.out.print("No previous messages");
		}else {
			for (ChatRoomMessage crm :chatRoomMessages) {
				String user = crm.getUser();
				String message = crm.getMessage();
				System.out.println(user+" : "+message);
			}
		}
	}
	
	public void sendWhoRequest() {
		client.sendTCP(new WhoRequest());
	}
	
	public void sendWhoRoom() {
		client.sendTCP(new WhoRoomRequest());
	}
	
	public void createChatRoom(String roomName) {
		client.sendTCP(new CreateRoomMessage(roomName));
	}
	
	public void start() throws IOException {
		client.start();
		connect();
		
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
	
	public void connect() throws IOException {
		client.connect(1000, hostName, portNumber);
	}
	public void run() {
		
		try (
				BufferedReader stdIn = new BufferedReader(
	                    new InputStreamReader(System.in))	// Za čitanje sa standardnog ulaza - tastature!
	        ) {
			
				sendWhoRequest();
				sendWhoRoom();
					            
				String userInput;
				running = true;
				
	            while (running) {
	            	userInput = stdIn.readLine();
	            	if (userInput == null || "BYE".equalsIgnoreCase(userInput)) // userInput - tekst koji je unet sa tastature!
	            	{
	            		running = false;
	            	}
	            	else if ("WHO".equalsIgnoreCase(userInput)){
	            		client.sendTCP(new WhoRequest());
	            	}
	            	else if ("ALL ROOMS".equalsIgnoreCase(userInput)){
	            		client.sendTCP(new WhoRoomRequest());
	            	}
	            	else if (userInput.startsWith("PRIVATE ")) {
	                     // Primer: PRIVATE imePrihvatioca tekst poruke
	                     String[] parts = userInput.split(" ", 3);
	                     if (parts.length == 3) {
	                         String recipient = parts[1];
	                         String messageText = parts[2];
	                         client.sendTCP(new PrivateMessage(recipient, userName, messageText));
	                     } else {
	                         System.out.println("Invalid private message format. Use: PRIVATE recipient message");
	                     }
	                }
	            	else if (userInput.startsWith("ALL PRIVATE ")) {
	                     // Primer: PRIVATE imePrihvatioca tekst poruke
	                     String[] parts = userInput.split(" ", 3);
	                     if (parts.length == 3) {
	                         String reciver = parts[2];
	                         client.sendTCP(new RequestPrivateMessage(userName,reciver));
	                     } else {
	                         System.out.println("Invalid all private message format. Use: All PRIVATE recipient ");
	                     }
	                }
	            	else if (userInput.startsWith("INVITE ")) {
	                     // Primer: INVITE imeSobe
	                     String[] parts = userInput.split(" ", 3);
	                     if (parts.length == 3) {
	                         String room = parts[1];
	                         String invited = parts[2];
	                         client.sendTCP(new InviteToRoomMessage(room,invited));
	                     } else {
	                         System.out.println("Invalid invite to room message format. Use: INVITE roomName invitedPerson");
	                     }
	                }
	            	else if (userInput.startsWith("JOIN ")) {
	                     // Primer: JOIN imeSobe
	                     String[] parts = userInput.split(" ", 2);
	                     if (parts.length == 2) {
	                         String room = parts[1];
	                         client.sendTCP(new JoinRoomMessage(room));
	                     } else {
	                         System.out.println("Invalid join room message format. Use: JOIN roomName");
	                     }
	                }
	            	else if (userInput.startsWith("CREATE ROOM ")) {
	                     // Primer: CREATE ROOM imeSobe
	                     String[] parts = userInput.split(" ", 3);
	                     if (parts.length == 3) {
	                         String roomName = parts[2];
	                         client.sendTCP(new CreateRoomMessage(roomName));
	                     } else {
	                         System.out.println("Invalid create room format. Use: CREATE ROOM roomName");
	                     }
	                }
	            	else if (userInput.startsWith("ROOM ")) {
	                     // Primer: CREATE ROOM imeSobe
	                     String[] parts = userInput.split(" ", 3);
	                     if (parts.length == 3) {
	                         String roomName = parts[1];
	                         String message = parts[2];
	                         client.sendTCP(new ChatRoomMessage(userName,roomName,message));
	                     } else {
	                         System.out.println("Invalid chat room message format. Use: ROOM roomName message");
	                     }
	                }
	            	else if (userInput.startsWith("ALL ROOM ")) {
	                     // Primer: CREATE ROOM imeSobe
	                     String[] parts = userInput.split(" ", 3);
	                     if (parts.length == 3) {
	                         String roomName = parts[2];
	                         client.sendTCP(new GetMoreMessagesMesage(roomName));
	                     } else {
	                         System.out.println("Invalid all msg chat room message format. Use: All ROOM roomName");
	                     }
	                }
	            	else {
	            		ChatMessage message = new ChatMessage(userName, userInput);
	            		client.sendTCP(message);
	            	}
	            	
	            	
	            	if (!client.isConnected() && running)
	            		connect();
	            	
	           }
	            
	    } catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			running = false;
			System.out.println("CLIENT SE DISCONNECTUJE");
			client.close();;
		}
	}
	public static void main(String[] args) {
		if (args.length != 3) {
		
            System.err.println(
                "Usage: java -jar chatClient.jar <host name> <port number> <username>");
            System.out.println("Recommended port number is 54555");
            System.exit(1);
        }
 
        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        String userName = args[2];
        
        try{
        	ChatClient chatClient = new ChatClient(hostName, portNumber, userName);
        	chatClient.start();
        }catch(IOException e) {
        	e.printStackTrace();
        	System.err.println("Error:"+e.getMessage());
        	System.exit(-1);
        }
	}
}
