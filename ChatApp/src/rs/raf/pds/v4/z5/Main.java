package rs.raf.pds.v4.z5;

import java.util.Arrays;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.converter.CharacterStringConverter;
import rs.raf.pds.v4.z5.ChatClient.ChatClientMessageObserver;
import rs.raf.pds.v4.z5.ChatClient.ChatClientObserver;
import rs.raf.pds.v4.z5.ChatClient.ChatRoomMessageObserver;
import rs.raf.pds.v4.z5.messages.ChatRoomMessage;
import rs.raf.pds.v4.z5.messages.PrivateMessage;

public class Main extends Application implements ChatClientObserver, ChatClientMessageObserver,ChatRoomMessageObserver {

    private ChatClient chatClient;

    private TextField messageField;
    ListView<String> listView;
    ListView<String> messagesListView;
    
    //May God help me
    ObservableList<String> usersAndRooms = FXCollections.observableArrayList();
    
    ObservableList<String> currentMessages = FXCollections.observableArrayList();


    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        String hostName = "localhost";
        int portNumber = 4555;  
        String userName = "Danilo";

        chatClient = new ChatClient(hostName, portNumber, userName);
        chatClient.addObserver(this);
        chatClient.addObserverForMessage(this);
        chatClient.addObserverForRoom(this);

        // UI components
        BorderPane root = new BorderPane();

        // Create a ListView and set the items
        listView = new ListView<>(usersAndRooms);
        listView.setPrefHeight(200); // Set your preferred height

        ScrollPane scrollPane = new ScrollPane(listView);
        scrollPane.setFitToWidth(true); // Adjust as needed
        scrollPane.setFitToHeight(true); // Adjust as needed
        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                // Single-click action (you can change this to handle double-click or other events)
                String selectedItem = listView.getSelectionModel().getSelectedItem();
                
                if (selectedItem != null) {
                    // Call the function in ChatClient based on the selected item
                    chatClient.contextSwitchChat(selectedItem);
                }
            }
        });
        
        // Create a text field for entering the new chat room name
        TextField newChatRoomField = new TextField();
        newChatRoomField.setPromptText("Enter Chat Room Name");
        
        Label warningLabel = new Label();
        warningLabel.setStyle("-fx-text-fill: red;");

        // Use a TextFormatter to limit input to uppercase letters
        newChatRoomField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("[A-Z]*")) {
                warningLabel.setText("Only uppercase letters are allowed");
                newChatRoomField.setText(oldValue);
            } else {
                warningLabel.setText("");
            }
        });
        
        Button createChatRoomButton = new Button("Create Chat Room");
        createChatRoomButton.setOnAction(event -> {
            createChatRoom(newChatRoomField.getText());
            newChatRoomField.clear();
        });
        	
        VBox leftBox = new VBox(scrollPane, createChatRoomButton, newChatRoomField,warningLabel);
        leftBox.setPadding(new Insets(10));
        
        messagesListView = new ListView<>(currentMessages);
        messagesListView.setCellFactory(createCellFactory());

        ScrollPane scrollPane1 = new ScrollPane(messagesListView);
        scrollPane1.setFitToWidth(true); // Adjust as needed
        scrollPane1.setFitToHeight(true);
        messagesListView.setPrefHeight(200);
        //messagesListView.setMouseTransparent(true);
        messagesListView.setFocusTraversable(false);
        
        messageField = new TextField();

        Button sendButton = new Button("Send");
        sendButton.setOnAction(event -> sendMessage());

        VBox rightBox = new VBox(scrollPane1, createMessageInput(sendButton));
        rightBox.setPadding(new Insets(10));

        root.setLeft(leftBox);
        root.setCenter(rightBox);

        Scene scene = new Scene(root, 800, 500);
        scene.getStylesheets().add(getClass().getResource("mystyle.css").toExternalForm());
        // Set up the stage
        primaryStage.setTitle("Chat Client");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> {
        	Platform.runLater(() -> {
                chatClient.stop();
            });
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
    
    private Callback<ListView<String>, ListCell<String>> createCellFactory() {
        return listView -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);

                    // Adjust alignment based on the starting string
                    if (item.startsWith("Danilo")) {
                        setStyle("-fx-alignment: center-right;");
                    } else {
                        setStyle("-fx-alignment: center-left;");
                    }
                }
            }
        };
	}
    
    private void sendMessage() {
        String messageText = messageField.getText().trim();
        if (!messageText.isEmpty()) {
        	
        	if (listView.getSelectionModel().getSelectedItem() != null && listView.getSelectionModel().getSelectedItem().matches("[A-Z]+")) {
        		chatClient.sendRoomMessage(listView.getSelectionModel().getSelectedItem(),messageText);
        	} else {
        		chatClient.sendPrivateMessage(listView.getSelectionModel().getSelectedItem(),messageText);
        	}

            // Clear the messageField after sending the message
            messageField.clear();
        }
    }
    
    @Override
    public void onRecivedListOfEntity(String[] result) {
        Platform.runLater(() -> {
            // Check and add only elements that are not already present in usersAndRooms
            Arrays.stream(result)
                    .filter(element -> !usersAndRooms.contains(element))
                    .forEach(usersAndRooms::add);
        });
    }
    
    @Override
    public void onRecivedListOfMessages(PrivateMessage[] result) {
        Platform.runLater(() -> {
        	
        	String[] messages = new String[result.length];
        	int i = 0;
        	for (PrivateMessage pm: result) {
        		messages[i] = pm.getUser()+": "+pm.getTxt()+"    -"+pm.getTimestamp();
        		i++;
        	}
        	System.out.println(messages);
        	if (result.length!=0) {
        		
        		currentMessages.clear();
        		Arrays.stream(messages)
                .filter(element -> !currentMessages.contains(element))
                .forEach(currentMessages::add);
        		String first = result[0].getRecipient();
        		String second = result[0].getUser();
        		if (first == chatClient.userName) {
        			int index = usersAndRooms.indexOf(first);

                    if (index != -1) {
                        // If the item is found, select it
                        listView.getSelectionModel().select(index);
                    }
        		}else {
        			int index = usersAndRooms.indexOf(second);

                    if (index != -1) {
                        // If the item is found, select it
                        listView.getSelectionModel().select(index);
                    }
        		}
        		messagesListView.scrollTo(currentMessages.size() - 1);
        	}else {
        		currentMessages.clear();
        	}
        });
    }
    
    @Override
    public void onRecivedListOfRoomMessages(ChatRoomMessage[] result) {
        Platform.runLater(() -> {
        	String[] messages = new String[result.length];
        	int i = 0;
        	for (ChatRoomMessage crm: result) {
        		messages[i] = crm.getUser()+": "+crm.getMessage();
        		i++;
        	}
        	System.out.println(messages);
        	if (result.length!=0) {
        		
        		currentMessages.clear();
        		Arrays.stream(messages)
                .filter(element -> !currentMessages.contains(element))
                .forEach(currentMessages::add);
        		
        		int index = usersAndRooms.indexOf(result[0].getRoomName());

                if (index != -1) {
                    // If the item is found, select it
                    listView.getSelectionModel().select(index);
                }
                messagesListView.scrollTo(currentMessages.size() - 1);
        	}else {
        		currentMessages.clear();
        	}
        });
    }
    
    public void createChatRoom(String roomName) {
    	if(!roomName.isBlank()) {
    		chatClient.createChatRoom(roomName);
    	}
    }


    private ToolBar createMessageInput(Button sendButton) {
        ToolBar toolBar = new ToolBar();
        toolBar.getItems().addAll(messageField, sendButton);
        return toolBar;
    }
}




