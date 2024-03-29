package rs.raf.pds.v4.z5;

import java.util.Arrays;
import java.util.Optional;

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
import javafx.util.converter.DefaultStringConverter;
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

        BorderPane root = new BorderPane();

        listView = new ListView<>(usersAndRooms);
        listView.setPrefHeight(200); 

        ScrollPane scrollPane = new ScrollPane(listView);
        scrollPane.setFitToWidth(true); 
        scrollPane.setFitToHeight(true); 
        listView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                String selectedItem = listView.getSelectionModel().getSelectedItem();
                
                if (selectedItem != null) {
                    chatClient.contextSwitchChat(selectedItem);
                }
            }
        });
        
        TextField newChatRoomField = new TextField();
        newChatRoomField.setPromptText("Enter Chat Room Name");
        
        Label warningLabel = new Label();
        warningLabel.setStyle("-fx-text-fill: red;");

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
        scrollPane1.setFitToWidth(true); 
        scrollPane1.setFitToHeight(true);
        messagesListView.setPrefHeight(200);
        //messagesListView.setMouseTransparent(true);
        messagesListView.setCellFactory(param -> {
            ListCell<String> cell = new ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                    	String displayedText = item.substring(0, item.length() - 17);
                        setText(displayedText);
                        setWrapText(true);

                        if (item.startsWith("Danilo")) {
                            setStyle("-fx-alignment: center-right;");
                        } else {
                            setStyle("-fx-alignment: center-left;");
                        }
                        
                        setOnMouseClicked(event -> {
                            if (item.startsWith("Danilo")) {
                            	ContextMenu contextMenu = new ContextMenu();
                                MenuItem editItem = new MenuItem("Edit");
                                editItem.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");
                                editItem.setOnAction(event1 -> editMessage(item));
                                contextMenu.getItems().add(editItem);
                                setContextMenu(contextMenu);
                            }else {
                            	ContextMenu contextMenu = new ContextMenu();
                                MenuItem editItem = new MenuItem("Reply");
                                editItem.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");
                                editItem.setOnAction(event2 -> replyToMessage(item));
                                contextMenu.getItems().add(editItem);
                                setContextMenu(contextMenu);
                            }
                        });
                    }
                }
            };

            return cell;
        });
        
        messageField = new TextField();
        messageField.setPromptText("max 40 characters");

     // Set a filter to limit input to 40 characters
        messageField.setTextFormatter(new TextFormatter<>(change -> {
            if (change.isContentChange()) {
                if (change.getControlNewText().length() <= 40) {
                    return change;
                }
            }
            return null;
        }));

        Button sendButton = new Button("Send");
        sendButton.setOnAction(event -> sendMessage());
        sendButton.disableProperty().bind(Bindings.isNull(listView.getSelectionModel().selectedItemProperty()));

        VBox rightBox = new VBox(scrollPane1, createMessageInput(sendButton));
        rightBox.setPadding(new Insets(10));

        root.setLeft(leftBox);
        root.setCenter(rightBox);

        Scene scene = new Scene(root, 800, 500);
        scene.getStylesheets().add(getClass().getResource("mystyle.css").toExternalForm());
        
        primaryStage.setTitle("DeniChatApp");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> {
        	Platform.runLater(() -> {
                chatClient.stop();
            });
            Platform.exit();
        });
        
        try {
            chatClient.start();
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void editMessage(String originalMessage) {
        TextInputDialog dialog = new TextInputDialog(originalMessage);
        dialog.setTitle("Edit Message");
        dialog.setHeaderText("");
        dialog.setContentText("Enter the new message:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(newMessage -> {
        	if (newMessage!=originalMessage) {
            	sendEditMessageToServer(newMessage,originalMessage,listView.getSelectionModel().getSelectedItem(),chatClient.userName);
        	}
        });
    }
    
    private void replyToMessage(String message) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Reply to "+message.substring(0, message.length() - 17));
        dialog.setHeaderText("");
        dialog.setContentText("Enter reply message:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(replyMessage -> {
        	if (replyMessage.trim().length()!=0 && listView.getSelectionModel().getSelectedItem().matches("[A-Z]*")) {
        		String newMessage ="Reply to "+message.substring(0, message.length() - 17).split(":")[0]+"- "+message.substring(0, message.length() - 17).split(":")[1]+" -> "+replyMessage;
        		chatClient.sendRoomMessage(listView.getSelectionModel().getSelectedItem(),newMessage);
        	}else {
        		String newMessage ="Reply to "+message.substring(0, message.length() - 17).split(":")[0]+"- "+message.substring(0, message.length() - 17).split(":")[1]+" -> "+replyMessage;
        		chatClient.sendPrivateMessage(listView.getSelectionModel().getSelectedItem(),newMessage);
        	}
        });
    }
    
    private void sendEditMessageToServer(String newMessage, String originalMessage,String recipient,String sender) {
    	chatClient.sendEditedMessage(newMessage, originalMessage,recipient, sender);
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
                	String displayedText = item.substring(0, item.length() - 18);
                    setText(displayedText);

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

            messageField.clear();
        }
    }
    
    @Override
    public void onRecivedListOfEntity(String[] result) {
        Platform.runLater(() -> {
            Arrays.stream(result)
                    .filter(element -> !usersAndRooms.contains(element) && (element!=chatClient.userName))
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

        		if (!first.equals(chatClient.userName)) {
        		    int index = usersAndRooms.indexOf(first);
        		    System.out.println("Prvi");
        		    if (index != -1) {
        		        listView.getSelectionModel().select(index);
        		    }
        		} else if (!second.equals(chatClient.userName)) {
        		    int index = usersAndRooms.indexOf(second);
        		    System.out.println("DRUGI");
        		    if (index != -1) {
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
        		messages[i] = crm.getUser()+": "+crm.getMessage()+"    -"+crm.getTimestamp();
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




