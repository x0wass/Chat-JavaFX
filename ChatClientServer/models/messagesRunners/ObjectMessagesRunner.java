package models.messagesRunners;

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.util.logging.Logger;

import chat.Failure;
import models.Message;
import models.MessagesHandler;

/**
 * Helper class to handle Object messages ({@link Message}) between server and GUI clients.
 * to be used in any GUI Client
 * @author x0wass
 */
public class ObjectMessagesRunner extends AbstractMessagesRunner implements Runnable
{
	/**
	 * Object input stream. Used to read {@link Message}s on the
	 * {@link AbstractClientFrame#inPipe} and display these messages in the
	 * {@link AbstractClientFrame#document}
	 */
	private ObjectInputStream inOIS;

	/**
	 * Constructor
	 * @param messagesHandler The class responsible for didplaying messages anec evt users
	 * @param commonRun The commonRun flag to use with multiple threads
	 * @param parentLogger The caller's logger
	 */
	public ObjectMessagesRunner(MessagesHandler messagesHandler,
	                            Boolean commonRun,
	                            Logger parentLogger)
	{
		super(messagesHandler, commonRun, parentLogger);
	}

	/**
	 * Run loop :
	 * 	- Creates an {@link ObjectInputStream} to read Message objects from
	 * 	{@link AbstractMessagesRunner#inPipe} then enters loop to
	 * 	- reads {@link Message}s object from the {@link ObjectInputStream}
	 * 	- adds message to {@link AbstractMessagesRunner#messagesHandler}
	 * 	- evt adds new userName to {@link AbstractMessagesRunner#messagesHandler}
	 * 	- tells {@link AbstractMessagesRunner#messagesHandler} to {@link MessageHandler#updateMessages}
	 */
	@Override
	public void run()
	{
		//  create an ObjectInputStream on the #inPipe to be able to read
		// Message objects
		try
		{
			inOIS = new ObjectInputStream(inPipe);
		}
		catch (StreamCorruptedException sce)
		{
			logger.severe("ObjectMessagesRunner: "
			        + Failure.USER_INPUT_STREAM.toString()
			        + " Output Object stream: " + "stream header is incorrect, "
			        + sce.getLocalizedMessage());
			System.exit(Failure.USER_INPUT_STREAM.toInteger());
		}
		catch (IOException ioe)
		{
			logger.severe("ObjectMessagesRunner: "
			        + Failure.USER_INPUT_STREAM.toString()
			        + " IOException, " + ioe.getLocalizedMessage());
			System.exit(Failure.USER_INPUT_STREAM.toInteger());
		}

		while(commonRun.booleanValue())
		{
			Message message = null;
			//  Read message from inOIS
			try
			{
				message = (Message)inOIS.readObject();
			}
			catch (ClassNotFoundException | InvalidClassException |
			       StreamCorruptedException | OptionalDataException e)
			{
				logger.severe("ObjectMessagesRunner : error reading object"
				    + e.getLocalizedMessage());
				break;
			}
			catch (IOException e)
			{
				logger.severe("ObjectMessagesRunner : error reading object "
				    + "IO Exception : " + e.getLocalizedMessage());
				break;
			}

			//  Add the current message to the #messagesHandler list
			messagesHandler.addMessage(message);

			//  Update #messagesHandler with evt new author
			String author = message.getAuthor();
			if ((author != null) && (author.length() > 0))
			{
				messagesHandler.addUserName(author);
			}

			//  update all messages on #messagesHandler
			messagesHandler.updateMessages();
		}

		if (commonRun.booleanValue())
		{
			logger.info("ObjectMessagesRunner::run's end: changing run state at the end ... ");
			synchronized (commonRun)
			{
				commonRun = Boolean.FALSE;
			}
		}

		cleanup();
	}

	/**
	 * Cleanup: close streams
	 */
	@Override
	public void cleanup()
	{
		logger.info("closing object input stream ... ");
		try
		{
			inOIS.close();
		}
		catch (IOException e)
		{
			logger.warning("failed to close object input stream "
			    + e.getLocalizedMessage());
		}

		super.cleanup();
	}
}
