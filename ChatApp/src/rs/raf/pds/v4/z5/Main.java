package rs.raf.pds.v4.z5;

import java.util.Arrays;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    private ChatClient chatClient;

    private ListView<String> clientList;
    private TextArea chatArea;
    private TextField messageField;

    private ObservableList<String> chatEntities = FXCollections.observableArrayList();
    private ListView<String> chatEntityList;


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Initialize your ChatClient here
        String hostName = "localhost";
        int portNumber = 4555;  // Update with your actual port number
        String userName = "Danilo";

        chatClient = new ChatClient(hostName, portNumber, userName);

        // UI components
        BorderPane root = new BorderPane();
        
        Button loadEntitiesButton = new Button("Load Chat Entities");
        loadEntitiesButton.setOnAction(event -> loadChatEntities());
        chatEntityList = new ListView<>(chatEntities);
        chatEntityList.setOnMouseClicked(event -> showChatForSelectedEntity());
        

        // Create a text field for entering the new chat room name
        TextField newChatRoomField = new TextField();
        newChatRoomField.setPromptText("Enter Chat Room Name");
        
       
        Button createChatRoomButton = new Button("Create Chat Room");
        createChatRoomButton.setOnAction(event -> createChatRoom(newChatRoomField));
        	
        VBox leftBox = new VBox(loadEntitiesButton, chatEntityList, createChatRoomButton, newChatRoomField);
        leftBox.setPadding(new Insets(10));
        
        // Right side - Chat Area and Message Input
        chatArea = new TextArea();
        chatArea.setEditable(false);

        messageField = new TextField();
        messageField.setOnAction(event -> sendMessage());

        Button sendButton = new Button("Send");
        sendButton.setOnAction(event -> sendMessage());

        VBox rightBox = new VBox(chatArea, createMessageInput(sendButton));
        rightBox.setPadding(new Insets(10));

        root.setLeft(leftBox);
        root.setCenter(rightBox);

        // Set up the stage
        primaryStage.setTitle("Chat Client");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.setOnCloseRequest(event -> {
            chatClient.stop();
            Platform.exit();
        });

        // Start the client and show the stage
        try {
            chatClient.start();
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void createChatRoom(TextField newChatRoomField) {
        String newChatRoomName = newChatRoomField.getText().trim();
        if (!newChatRoomName.isEmpty()) {
            // Implement the logic to create a new chat room
            // You may need to call a method on your ChatClient to create a new chat room
            // Update this part based on your ChatClient implementation
            chatClient.createChatRoom(newChatRoomName);
            newChatRoomField.clear();
        }
    }

    private void showChatForSelectedEntity() {
        String selectedEntity = chatEntityList.getSelectionModel().getSelectedItem();
        if (selectedEntity != null) {
            String[] allUsers = chatClient.getAllUsers();
            String[] allChatRooms = chatClient.getAllRooms();

            if (allUsers != null && Arrays.asList(allUsers).contains(selectedEntity)) {
                // Fetch and display previous messages for the selected chat client
                // Update this part based on your ChatClient implementation
                chatArea.setText("Previous messages with " + selectedEntity);
            } else if (allChatRooms != null && Arrays.asList(allChatRooms).contains(selectedEntity)) {
                // Fetch and display previous messages for the selected chat room
                // Update this part based on your ChatClient implementation
                chatArea.setText("Previous messages in " + selectedEntity);
            }
        } else {
            chatArea.clear();
        }
    }

    
    private void loadChatEntities() {
        // Implement the logic to load the list of chat clients and chat rooms
        // You may need to call methods on your ChatClient to fetch online users and chat rooms
        String[] onlineUsers = chatClient.getAllUsers();
        String[] chatRooms = chatClient.getAllRooms();

        chatEntities.clear();
        if (onlineUsers != null) {
            chatEntities.addAll(onlineUsers);
        }
        if (chatRooms != null) {
            chatEntities.addAll(chatRooms);
        }
    }

    private void sendMessage() {
        String message = messageField.getText().trim();
        String selectedClient = clientList.getSelectionModel().getSelectedItem();
        if (!message.isEmpty() && selectedClient != null) {
            // Send private message to the selected client
            chatClient.sendPrivateMessage(selectedClient, message);
            messageField.clear();
        }
    }

    private ToolBar createMessageInput(Button sendButton) {
        ToolBar toolBar = new ToolBar();
        toolBar.getItems().addAll(messageField, sendButton);
        return toolBar;
    }
}




