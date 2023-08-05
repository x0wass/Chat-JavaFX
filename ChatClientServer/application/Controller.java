package application;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import application.listcells.MessageCell;
import application.listcells.UserCell;
import chat.Vocabulary;
import chat.client.ChatClient;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.stage.Stage;
import logger.LoggerFactory;
import models.AuthorListFilter;
import models.Message;
import models.Message.MessageOrder;
import models.MessagesHandler;
import models.ModifiableObservableList;
import models.OSCheck;
import models.messagesRunners.AbstractMessagesRunner;
import models.messagesRunners.ObjectMessagesRunner;

/**
 * Controller associated with ClientFrame.fxml
 * @author x0wass
 * @see Initializable so it can initialize FXML related attributes.
 * @see MessageHandler so it can be used by {@link #messagesRunner} to update
 * users and messages.
 * @see ChangeListener so it can listen to selection changes in
 * {@link #usersListView} in order to customize messages filtering according
 * to users selected in {@link #usersListView}
 */
public class Controller implements Initializable, MessagesHandler, ListChangeListener<String>
{
	// -------------------------------------------------------------------------
	// internal attributes
	// -------------------------------------------------------------------------
	/**
	 * Common run when mutiple threads are used for listening to server's
	 * messages
	 */
	protected Boolean commonRun;

	/**
	 * Logger to show debug message or only log them in a file
	 */
	protected Logger logger;

	/**
	 * List of all received messages.
	 * This list might be sorted and/or fitlered in {@link #updateMessages()}
	 * in order to display sorted and/or filtered messages in {@link #messagesObservableList}
	 * associated with {@link #messagesListView}
	 */
	private List<Message> messagesList;

	/**
	 * List of messages displayed in {@link #messagesListView}
	 * @implSpec Needs to to associated with {@link #messagesListView} with
	 * {@link ListView#setItems(ObservableList)}
	 */
	private ObservableList<Message> messagesObservableList;

	/**
	 * The name to use for this client
	 * Can't be set during construction.
	 * Will be set through {@link #setAuthor(String)}
	 */
	private String author;

	/**
	 * List of users shown in {@link #usersListView}
	 * @implSpec Needs to be associated with {@link #usersListView} with
	 * {@link ListView#setItems(ObservableList)}
	 */
	private ObservableList<String> authorsObservableList;

	/**
	 * Filter used to filter messages based on users names selected in the
	 * {@link #usersListView}
	 */
	private AuthorListFilter authorFilter = null;

	/**
	 * Flag indicating the filtering status of messages (on/off)
	 */
	private boolean filtering;

	/**
	 * Flag indicating the orderding status of messages (on/off)
	 */
	private boolean ordering;

	/**
	 * Flag indicating an update of all displayed messages is requested
	 * @implSpec This flag is necessary when filterting or ordering is turned
	 * off to redraw all messages
	 * @implNote shoud be cleared at the end of {@link #updateMessages()} after
	 * full update
	 */
	private boolean fullUpdateRequested;

	/**
	 * Helper runnable to handle messages
	 * 	- messageRunner runs in a thread where it receicves messages from the
	 * 	server. It can then update {@link #messagesList}, {@link #authorsObservableList}
	 * 	and {@link #messagesObservableList}.
	 * 	- it cand also be used to send messages to server with
	 * {@link AbstractMessagesRunner#sendMessage(String)}
	 * @implNote All operations affecting JavaFX Scenegraph performed in
	 * {@link ObjectMessagesRunner#run()} method should be performed through
	 * {@link Platform#runLater(Runnable)} calls to {@link Runnable} in order
	 * to be performed on JavaFX thread to preserve JavaFX Scenegraph consistency.
	 * Several local Runnables can be used to do just that.
	 * @see AddUserNameRunnable
	 * @see AppendMessageRunnable
	 * @see ClearMessagesRunnable
	 */
	private ObjectMessagesRunner messagesRunner;

	/**
	 * List of buttons to change with either
	 * 	- {@link #onDisplayButtonsWithGraphicsOnly(ActionEvent)},
	 * 	- {@link #onDisplayButtonsWithTextAndGraphics(ActionEvent)} or
	 * 	- {@link #onDisplayButtonsWithTextOnly(ActionEvent)}
	 * and containing
	 * 	- {@link #sendButton}
	 * 	- {@link #quitButton}
	 * 	- {@link #clearSelectionButton}
	 * 	- {@link #kickUsersButton}
	 * 	- {@link #clearMessagesButton}
	 * 	- {@link #catchupMessagesButton}
	 * 	- {@link #filterMessagesButton}
	 */
	private List<Labeled> displayLabeled;

	/**
	 * Reference to parent stage so it can be quickly closed on quit
	 */
	private Stage parentStage;

	// -------------------------------------------------------------------------
	// FXML related attributes
	// -------------------------------------------------------------------------
	/**
	 * Application menu bar
	 */
	@FXML
	private MenuBar menuBar;

	/**
	 * Text field containing the message to send
	 */
	@FXML
	private TextField messageText;

	/**
	 * Send button to send message in {@link #messageText} to the server
	 */
	@FXML
	private Button sendButton;

	/**
	 * Quit button
	 */
	@FXML
	private Button quitButton;

	/**
	 * Clear selected users button
	 */
	@FXML
	private Button clearSelectionButton;

	/**
	 * Kick selected users button
	 */
	@FXML
	private Button kickUsersButton;

	/**
	 * Clear messages buttons
	 */
	@FXML
	private Button clearMessagesButton;

	/**
	 * Catchup messages button
	 */
	@FXML
	private Button catchupMessagesButton;

	/**
	 * Filter messages toggle button
	 * @implSpec when selected ensure {@link #filterMessagesMenuItem} and
	 * {@link #contextFilterMessagesMenuItem} are also selected.
	 */
	@FXML
	private ToggleButton filterMessagesButton;

	/**
	 * Filter messages menu item
	 * @implSpec when selected, ensure {@link #filterMessagesButton} and
	 * {@link #contextFilterMessagesMenuItem} are also selected.
	 */
	@FXML
	private CheckMenuItem filterMessagesMenuItem;

	/**
	 * Filter messages menu item from context menu in {@link #messagesListView}
	 * @implSpec when selected, ensure {@link #filterMessagesButton} and
	 * {@link #filterMessagesMenuItem} are also selected.
	 */
	@FXML
	private CheckMenuItem contextFilterMessagesMenuItem;

	/**
	 * Menu Item indicating buttons are shown with graphics only
	 * @implNote Needs to be set to selected during initialization
	 */
	@FXML
	private RadioMenuItem graphicsOnlyMenuItem;

	/**
	 * Menu Item indicating if date should be shown on messages display or not
	 * @implNote Needs to be set to selected during initialization
	 */
	@FXML
	private CheckMenuItem showDateOnMessagesMenuItem;

	/**
	 * The view containing the list of messages
	 * @see #messagesObservableList
	 */
	@FXML
	protected ListView<Message> messagesListView;

	/**
	 * The view containing the list of users that have send messages
	 * @see #authorsObservableList
	 */
	@FXML
	protected ListView<String> usersListView;

	/**
	 * The name of the server on the right of the toolbar
	 */
	@FXML
	private Label serverLabel;

	/**
	 * Default constructor.
	 * Initialize all non FXML attributes
	 * @see ModifiableObservableList
	 */
	public Controller()
	{
		// --------------------------------------------------------------------
		// Initialize own attributes
		// --------------------------------------------------------------------
		// can't get parent logger now, so standalone logger
		logger = LoggerFactory.getParentLogger(getClass(),
		                                       null,
		                                       Level.INFO);
		/*
		 * DONE Initialize all non FXML related attributes
		 * 	- #commonRun to null as it will be set later with #setCommonRun
		 * 	- #messagesList as a Vector or ArrayList of Message
		 * 	- #messagesObservableList to ModifiableObservableList of Message
		 * 	- #author to null as it will be set later with #setAuthor
		 * 	- #authorsObservableList to ModifiableObservableList of String
		 * 	- #authorFilter
		 * 	- #filtering to false: no filtering of messages yet
		 * 	- #ordering to false: no ordering of messages yet
		 * 	- #fullUpdateRequested to false: no full update required yet
		 * 	- #messageRunner to null as it will be set later with #getRunner
		 * 	- #displayLabeled to ArrayList of Labeled
		 */
		commonRun = null;
		messagesList = new ArrayList<Message>();
		messagesObservableList = new ModifiableObservableList<Message>();
		author = null;
		authorsObservableList = new ModifiableObservableList<String>();
		authorFilter = new AuthorListFilter();
		filtering = false;
		ordering = false;
		fullUpdateRequested = false;
		messagesRunner = null;
		displayLabeled = new ArrayList<Labeled>();
	}

	/**
	 * Controller initialization to initialize FXML related attributes.
	 * @param location The location used to resolve relative paths for the root
	 * object, or null if the location is not known.
	 * @param resources The resources used to localize the root object, or null
	 * if the root object was not localized.
	 */
	@Override
	public void initialize(URL location, ResourceBundle resources)
	{
		// --------------------------------------------------------------------
		// Initialize FXML related attributes
		// --------------------------------------------------------------------
		final OSCheck.OSType osType = OSCheck.getOperatingSystemType();
		if ((osType == OSCheck.OSType.MacOS) && !menuBar.isUseSystemMenuBar())
		{
			menuBar.setUseSystemMenuBar(true);
		}

		/*
		 * DONE Setup #usersListView with
		 * 	- items from #authorList
		 * 	- custom list cell UserCell
		 * 	- set selectionMode to SelectionMode.MULTIPLE so we can select multiple users
		 * 	- add "this" as a selectedItems listener so that #onChanged method
		 * 	can be called each time selection changes in this list
		 */
		usersListView.setItems(authorsObservableList);
		usersListView.setCellFactory(userCell -> new UserCell());
		usersListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		usersListView.getSelectionModel().getSelectedItems().addListener(this);
		
		/*
		 * DONE Setup #messagesListView with
		 * 	- items from #messagesObservableList
		 * 	- custom list cell MessageCell
		 */
		messagesListView.setItems(messagesObservableList);
		messagesListView.setCellFactory(messageCell -> new MessageCell());
		/*
		 * DONE Set #graphicsOnlyMenuItem to true since all buttons only shows
		 * graphics for now.
		 * will be changed in #onDisplayButtons...() methods
		 */
		graphicsOnlyMenuItem.setSelected(true);
		
		/*
		 * DONE Set #showDateOnMessagesMenuItem to true since all messages show
		 * Date for now
		 * will be changed in #oncActionSortMessagesBy...() methods
		 */
		showDateOnMessagesMenuItem.setSelected(true);
		/*
		 * DONE Adds all buttons that can be changed in #onDisplayButtons... methods
		 * to #displayLabeled list so they can be all changed at once
		 */
		displayLabeled.add(sendButton);
		displayLabeled.add(quitButton);
		displayLabeled.add(clearSelectionButton);
		displayLabeled.add(kickUsersButton);
		displayLabeled.add(clearMessagesButton);
		displayLabeled.add(catchupMessagesButton);
		displayLabeled.add(filterMessagesButton);
	}

	/**
	 * Author setter
	 * @param name the name to set for this client
	 */
	public void setAuthor(String name)
	{
		/*
		 * DONE replace with setting #author and evt #authorList
		 */
		
		for ( String aut : authorsObservableList) {
			if (aut.compareTo(author) == 0)
				aut = name;
		}
		author = name;
		logger.info("adding author name" + name);
	}

	/**
	 * Sets parent logger
	 * @param logger the new parent logger
	 */
	public void setParentLogger(Logger logger)
	{
		this.logger.setParent(logger);
	}

	/**
	 * Set common run.
	 * used by Main to set the same common run between {@link Controller}'s
	 * messages runner and {@link ChatClient}
	 * @param commonRun the new common run
	 */
	public void setCommonRun(Boolean commonRun)
	{
		this.commonRun = commonRun;
	}

	/**
	 * Set server label
	 * @param serverName the name of the server to set on {@link #serverLabel}
	 */
	public void setServer(String serverName)
	{
		/*
		 * DONE Replace ...
		 */
		serverLabel.setText(serverName);
		logger.info("setting server name" + serverName);
	}

	/**
	 * Set parent stage (so it can be closed on quit)
	 * @param stage the new parent stage to set
	 */
	public void setParentStage(Stage stage)
	{
		parentStage = stage;
	}

	/**
	 * Adds new message to {@link #messagesList}
	 * @param m the message to add
	 * @see MessageHandler
	 */
	@Override
	public void addMessage(Message m)
	{
		/*
		 * DONE Replace ...
		 */
		messagesList.add(m);
		logger.info("adding message: " + m);
	}

	/**
	 * Adds new message (from string)
	 * @param s the text of the message to add
	 * @see MessageHandler
	 */
	@Override
	public void addMessage(String s)
	{
		// Nothing, we don't expect text messages in this client
	}

	/**
	 * Adds a new user to {@link #authorsObservableList}
	 * @param user the new user to add
	 * @implNote Since this operation might be triggered by another thread
	 * (where {@link #messagesRunner} is running) the actual modification of
	 * {@link #authorsObservableList} shall be performed with
	 * {@link Platform#runLater(Runnable)} on JavaFX thread for JavaFX scene
	 * graph consistency:
	 * @see MessageHandler
	 */
	@Override
	public void addUserName(String user)
	{
		/*
		 * Platform#runLater ensures this operation is performed on
		 * JavaFX thread to preserve JavaFX Scenegraph consistency
		 */
		Platform.runLater(new AddUserNameRunnable(user));
	}

	/**
	 * Update all messages according to internal filtering and sorting policies
	 * @implNote All the operations influencing JavaFX scene graph must be
	 * launched from the JavaFX thread with a {@link Platform#runLater(Runnable)}
	 * @see MessageHandler
	 */
	@Override
	public void updateMessages()
	{
		logger.info("Update all messages");

		/*
		 * Full update of all messages (evt filtered and/or sorted)
		 */
		if (filtering || ordering || fullUpdateRequested)
		{
			/*
			 * DONE Clears all displayed messages
			 * Platform#runLater ensures this operation is performed on
			 * JavaFX thread to preserve JavaFX Scenegraph consistency
			 */
			Platform.runLater(new ClearMessagesRunnable());

			/*
			 * DONE Then creates a stream from #messagesList
			 */
			Stream<Message> stream = messagesList.stream();

			/*
			 * DONE If Message has any orders then sort the stream
			 */
			if(ordering) 
				stream = stream.sorted(); 
			

			/*
			 * DONE If filtering is on then filter the stream with authorFilter
			 */
			if(filtering) 
				stream = stream.filter(authorFilter);
			

			/*
			 * DONE Finally append all remaining messages on the stream with
			 * appenMessage(...)
			 */
			stream.forEach((Message m) -> appendMessage(m));
			/*
			 * Reset #fullUpdateRequested
			 */
			fullUpdateRequested = false;
		}
		else
		{
			/*
			 * DONE Only appends last received message
			 */
			appendMessage(messagesList.get(messagesList.size() - 1));
		}
	}

	/**
	 * Appends new message at the end of {@link #messagesListView}
	 * @param message The message to display
	 * @implNote Since this operation might be triggered by another thread
	 * (where {@link #messagesRunner} is running) the actual modification of
	 * {@link #messagesObservableList} shall be performed with
	 * {@link Platform#runLater(Runnable)} on JavaFX thread for JavaFX scene
	 * graph consistency:
	 * @see AppendMessageRunnable
	 */
	protected void appendMessage(Message message)
	{
		/*
		 * Platform#runLater ensures this operation is performed on
		 * JavaFX thread to preserve JavaFX Scenegraph consistency
		 */
		Platform.runLater(new AppendMessageRunnable(message));
	}

	/**
	 * Get controller's runner.
	 * If current {@link #messagesRunner} is null then it is created and returned,
	 * otherwise it is simply returned.
	 * @return the controller's runner.
	 */
	public AbstractMessagesRunner getRunner()
	{
		if (messagesRunner ==  null)
		{
			/*
			 * DONE Create runner with "this" as the MessageHandler,
			 * commonRun (which needs to be already set) and current logger
			 */
			messagesRunner = new ObjectMessagesRunner(this, commonRun, logger);
		}

		return messagesRunner;
	}

	/**
	 * Local Runnable to add a user name to {@link #authorsObservableList}.
	 * Shall be called with {@link Platform#runLater(Runnable)} within
	 * {@link Controller#addUserName(String)} in order to ensure that all
	 * operations on JavaFX scene graph components are performed on JavaFX thread,
	 * thus avoiding JavaFX scene graph inconsistency.
	 */
	class AddUserNameRunnable implements Runnable
	{
		/**
		 * the user to add to #authorsObservableList
		 */
		private String user;

		/**
		 * Valued constructor to set the user to add to {@link #authorsObservableList}
		 * @param user the user to set
		 */
		public AddUserNameRunnable(String user)
		{
			this.user = user;
		}

		/**
		 * Adds the specified user to {@link #authorsObservableList}
		 */
		@Override
		public void run()
		{
			/*
			 * Done replace with: add user to #authorList
			 */
			if(!authorsObservableList.contains(user)) {
				authorsObservableList.add(user);
				logger.info("adding user " + user);
			}
			
		}
	}

	/**
	 * Local Runnable to add a message to {@link Controller#messagesObservableList}.
	 * Shall be called with {@link Platform#runLater(Runnable)} within
	 * {@link Controller#appendMessage(Message)} in order to ensure that all
	 * operations on JavaFX scene graph components are performed on JavaFX thread,
	 * thus avoiding JavaFX scene graph inconsistency.
	 * @implNote It would be nice if #messagesListView could automatically scroll
	 * down to the newest message
	 */
	class AppendMessageRunnable implements Runnable
	{
		/**
		 * The message to append to {@link #displayMessages}
		 */
		private Message message;

		/**
		 * Valued constructor to set the message to append
		 * @param message the message to append
		 */
		public AppendMessageRunnable(Message message)
		{
			this.message = message;
		}

		/**
		 * Add the new message to {@link Controller#messagesObservableList}
		 */
		@Override
		public void run()
		{
			/*
			 * DONE replace with: add message to #messagesObservableList and scroll
			 */
			messagesObservableList.add(message);
			messagesListView.scrollTo(message);
			logger.info("adding message " + message);
		}
	}

	/**
	 * Local Runnable to clear all messages in {@link #messagesObservableList}.
	 * Shall be called with {@link Platform#runLater(Runnable)} within
	 * {@link #appendMessage(Message)} in order to ensure that all
	 * operations on JavaFX scene graph components are performed on JavaFX thread,
	 * thus avoiding JavaFX scene graph inconsistency.
	 */
	class ClearMessagesRunnable implements Runnable
	{
		/**
		 * Clears {@link Controller#messagesObservableList}
		 */
		@Override
		public void run()
		{
			/*
			 * DONE replace with: clear #messagesObservableList
			 */
			messagesObservableList.clear();
			logger.info("clearing messages");
		}
	}

	// -------------------------------------------------------------------------
	// UI related callbacks
	// -------------------------------------------------------------------------
	/**
	 * Action to send message contained in {@link #messageText}.
	 * Uses #messageRunner to send the message.
	 * @param event event associated with this action [not used]
	 */
	@FXML
	public void onSendAction(ActionEvent event)
	{
		/*
		 * DONE replace with send message contained in #messageText to server and clears #messageText
		 */
		messagesRunner.sendMessage(messageText.getText());
		messageText.clear();
		logger.info("Send Action triggered");
	}

	/**
	 * Action to send "bye" to server and quit the application by getting
	 * the parent {@link Stage} and close it.
	 * @param event event associated with this action [not used]
	 */
	@FXML
	public void onQuitAction(ActionEvent event)
	{
		/*
		 * DONE replace with Quit action:
		 * 	- sends Vocabulary.byeCmd
		 * 	- closes parentStage (iff non null)
		 */
		messagesRunner.sendMessage(Vocabulary.byeCmd);
		if (parentStage != null )  parentStage.close();
		logger.info("Quit action triggered");
	}

	/**
	 * Action to clear all messages
	 * @param event event associated with this action [not used]
	 */
	@FXML
	public void onClearMessagesAction(ActionEvent event)
	{
		/*
		 * DONE replace with clear messages action
		 */
		Platform.runLater(new ClearMessagesRunnable());
		logger.info("Clear action triggered");
	}

	/**
	 * Action to catchup all messages from server
	 * @param event event associated with this action [not used]
	 */
	@FXML
	public void onCatchupMessagesAction(ActionEvent event)
	{
		/*
		 * DONE replace catchup messages action:
		 * 	- uses Vocabulary#catchUpCmd
		 */
		messagesRunner.sendMessage(Vocabulary.catchUpCmd);
		logger.info("Catchup action triggered");
	}

	/**
	 * Return the "selected" state of the source object of this event
	 * (considering only {@link ToggleButton} and {@link CheckMenuItem})
	 * @param event the event to investigate
	 * @return true if the source object of this event is "selected", and false
	 * if it is not selected and/or not selectable.
	 */
	private boolean isSelected(ActionEvent event)
	{
		boolean selected;
		Object source = event.getSource();
		if (source instanceof Toggle)
		{
			Toggle toggle = (Toggle) source;
			selected = toggle.isSelected();
		}
		else if (source instanceof CheckMenuItem)
		{
			CheckMenuItem checkMenuItem = (CheckMenuItem) source;
			selected = checkMenuItem.isSelected();
		}
		else
		{
			logger.warning("Unknown event source: " + source.getClass().getSimpleName());
			selected = false;
		}
		return selected;
	}

	/**
	 * Action to toggle messages filtering according to selected users
	 * in the #usersListView
	 * @param event event associated with this action
	 */
	@FXML
	public void onFilterMessagesAction(ActionEvent event)
	{
		boolean selected = isSelected(event);
		@SuppressWarnings("unused")
		Object source = event.getSource();
		logger.info("Filter action triggered: " + (selected ? "On" : "Off"));
		/*
		 * DONE Ensure all filtering items have the same selected state
		 * 	- #filterMessagesMenuItem
		 * 	- #contextFilterMessagesMenuItem
		 * 	- #filterMessagesButton
		 */
		
        if (filterMessagesButton.isSelected() != selected)
            filterMessagesButton.setSelected(selected);
        
        if (contextFilterMessagesMenuItem.isSelected() != selected)
            contextFilterMessagesMenuItem.setSelected(selected);
             
        if (filterMessagesMenuItem.isSelected() != selected)
            filterMessagesMenuItem.setSelected(selected);
        
        authorFilter.setFiltering(selected);
        
        filtering = selected;
        fullUpdateRequested = true;
        updateMessages();
	}

	/**
	 * Utility method to change the order of {@link Message}s
	 * @param event the event to investigate
	 * @param order the message order criterium to change according to event
	 * @return true if the ordering of messages have been changed, false otherwise
	 * @see Message#addOrder(MessageOrder)
	 * @see Message#removeOrder(MessageOrder)
	 * @see onActionSortMessagesByDate
	 * @see onActionSortMessagesByAuthor
	 * @see onActionSortMessagesByContent
	 */
	private boolean changeMessageOrder(ActionEvent event, MessageOrder order)
	{
		/*
		 * DONE replace with adding or removing MessageOrder to Message
		 */
		boolean selected = isSelected(event);
		logger.info("change messages order on " + event + " with "+ order);
		if(selected) {
			Message.addOrder(order);	
		}
		else Message.removeOrder(order);
		
		ordering = Message.orderSize() != 0;
        if (ordering)
            updateMessages();
        
        return true;
	
	}

	/**
	 * Action to toggle messages sorting by message date
	 * @param event event associated with this action
	 * @see changeMessageOrder
	 */
	@FXML
	public void onActionSortMessagesByDate(ActionEvent event)
	{
		boolean selected = isSelected(event);
		/*
		 * DONE replace with adding or removing MessageOrder#DATE
		 */
		changeMessageOrder(event, MessageOrder.DATE);
		logger.info("Sort by date action triggered: " + (selected ? "On" : "Off"));
	}

	/**
	 * Action to toggle messages sorting by message author
	 * @param event event associated with this action
	 * @see changeMessageOrder
	 */
	@FXML
	public void onActionSortMessagesByAuthor(ActionEvent event)
	{
		boolean selected = isSelected(event);
		/*
		 * DONE replace with adding or removing MessageOrder#AUTHOR
		 */
		changeMessageOrder(event, MessageOrder.AUTHOR);
		logger.info("Sort by author action triggered: " + (selected ? "On" : "Off"));
	}

	/**
	 * Action to toggle messages sorting by message content
	 * @param event event associated with this action
	 * @see changeMessageOrder
	 */
	@FXML
	public void onActionSortMessagesByContent(ActionEvent event)
	{
		boolean selected = isSelected(event);
		/*
		 * DONE replace with adding or removing MessageOrder#CONTENT
		 */
		changeMessageOrder(event, MessageOrder.CONTENT);
		logger.info("Sort by content action triggered: " + (selected ? "On" : "Off"));
	}

	/**
	 * Action to clear selection in {@link #usersListView}
	 * @param event event associated with this action
	 */
	@FXML
	public void onClearSelectedUsers(ActionEvent event)
	{
		/*
		 * DONE replace with clear selection on #userListView
		 */
		for( String userCleared :  usersListView.getSelectionModel().getSelectedItems()) {
			usersListView.getItems().remove(userCleared);
			logger.info("Clear user list selection action triggered: " + userCleared);
		}	
	}

	/**
	 * Action to kick all users selected in {@link #usersListView}
	 * @param event event associated with this action
	 */
	@FXML
	public void onKickSelectedUsers(ActionEvent event)
	{
		/*
		 * DONE replace with kicking all selected users in #userListView
		 */
	
		for( String userKick :  usersListView.getSelectionModel().getSelectedItems()) {
			messagesRunner.sendMessage(Vocabulary.kickCmd + " " + userKick);
			logger.info("Kick selected users action triggered: " + userKick);
		}	

	}

	/**
	 * Action to show buttons with Graphics only
	 * @param event event associated with this action
	 */
	@FXML
	public void onDisplayButtonsWithGraphicsOnly(ActionEvent event)
	{
		/*
		 * DONE replace with setting all elts in #displayLabeled to graphics only
		 */
		for(Labeled lab: displayLabeled) {
			lab.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		}
		
		logger.info("Display Buttons with Graphics only action triggered: ");
	}

	/**
	 * Action to show buttons with Text and Graphics
	 * @param event event associated with this action
	 */
	@FXML
	public void onDisplayButtonsWithTextAndGraphics(ActionEvent event)
	{
		/*
		 * DONE replace with setting all elts in #displayLabeled to text and graphics
		 */
		for(Labeled lab: displayLabeled) {
			lab.setContentDisplay(ContentDisplay.LEFT);		
		}
		logger.info("Display Buttons with Text and Graphics action triggered: ");
	}

	/**
	 * Action to show buttons with Text only
	 * @param event event associated with this action
	 */
	@FXML
	public void onDisplayButtonsWithTextOnly(ActionEvent event)
	{
		/*
		 * DONE replace with setting all elts in #displayLabeled to text only
		 */
		for(Labeled lab: displayLabeled) {
			lab.setContentDisplay(ContentDisplay.TEXT_ONLY);
		}
		logger.info("Display Buttons with Text only action triggered: ");
	}

	/**
	 * Action to show or hide date with messages
	 * @param event event associated with this action
	 */
	@FXML
	public void onShowDateOnMessages(ActionEvent event)
	{
		boolean selected = isSelected(event);
		/*
		 * DONE replace with showing or hiding messages date in MessageCell
		 */
		if(selected) {
			MessageCell.setDateVisibility(true);
		} else MessageCell.setDateVisibility(false);
		logger.info("Show Date on messages action triggered: "+ (selected ? "On" : "Off"));
	}

	/**
	 * List Change listener method used to react to selection changes in
	 * {@link #usersListView} which might require an {@link #updateMessages()}
	 * call when filtering is on.
	 * @param c an object representing the change that was done
	 * @see ListChangeListener
	 */
	@Override
	public void onChanged(Change<? extends String> c)
	{
		logger.info("List Change Listener triggered with change=" + c);
		boolean selectionChanged = false;
		while (c.next())
		{
			/*
			 * DONE We only care about elements added to or removed from
			 * selection so we can update our #authorFilter
			 */
			selectionChanged = true;
			if(c.wasAdded()) {
				for(String auth : c.getAddedSubList())
					authorFilter.add(auth);
				selectionChanged = true;
			}
			if(c.wasRemoved()) {
				for(String auth : c.getRemoved())
					authorFilter.remove(auth);
				selectionChanged = true;
			}
		}

		/*
		 * DONE If filtering is on and selection has changed then
		 * trigger updateMessages
		 */
		if(filtering && selectionChanged)
			updateMessages();
	}
}
