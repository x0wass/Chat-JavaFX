package models.messagesRunners;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import logger.LoggerFactory;
import models.MessagesHandler;

/**
 * Baseclass of all Helper class handling messages between server and clients.
 * to be used in any Client
 * @author x0wass
 */
public abstract class AbstractMessagesRunner implements Runnable
{
	/**
	 * The class responsible for displaying messages and users
	 */
	protected MessagesHandler messagesHandler;

	/**
	 * Logger to show debug message or only log them in a file
	 */
	protected Logger logger;

	/**
	 * Common run when mutiple threads are used for listening to server's
	 * messages
	 */
	protected Boolean commonRun;

	/**
	 * Piped intput stream to read messages from server
	 */
	protected final PipedInputStream inPipe;

	/**
	 * Piped output stream to write messages to server
	 */
	protected final PipedOutputStream outPipe;

	/**
	 * Print writer to write the content of the {@link #txtFieldSend} containing
	 * the message to the {@link #outPipe} to the server
	 */
	protected final PrintWriter outPW;

	/**
	 * Map associating names to colors so that each message from specific users
	 * can be displayed with a specific color
	 * This map is updated with calls to {@link #getColorFromName(String)}
	 * @see #getColorFromName(String)
	 */
	protected Map<String, java.awt.Color> colorMap;

	/**
	 * Constructor
	 * @param messagesHandler The class responsible for didplaying messages anec evt users
	 * @param commonRun The commonRun flag to use with multiple threads
	 * @param parentLogger The caller's logger
	 */
	public AbstractMessagesRunner(MessagesHandler messagesHandler,
	    	                      Boolean commonRun,
	    	                      Logger parentLogger)
	{
		this.messagesHandler = messagesHandler;

		logger = LoggerFactory.getParentLogger(getClass(),
		                                       parentLogger,
		                                       (parentLogger == null ?
		                                    	Level.INFO :
		                                    	parentLogger.getLevel()));

		if (commonRun != null)
		{
			this.commonRun = commonRun;
		}
		else
		{
			this.commonRun = Boolean.TRUE;
		}

		inPipe = new PipedInputStream();
		logger.info("PipedInputStream Created");

		outPipe = new PipedOutputStream();
		logger.info("PipedOutputStream Created");
		outPW = new PrintWriter(outPipe, true);
		if (outPW.checkError())
		{
			logger.warning("Output PrintWriter has errors");
		}
		else
		{
			logger.info("Printwriter to PipedOutputStream Created");
		}

		// ---------------------------------------------------------------------
		// Others
		// ---------------------------------------------------------------------
		colorMap = new TreeMap<String, java.awt.Color>();
	}

	/**
	 * Send message into {@link #outPipe} (iff non null) using the
	 * {@link #outPW}
	 * @param the message to send
	 */
	public void sendMessage(String message)
	{
		logger.info("writing out: "
		    + (message == null ? "NULL" : message));
		/*
		 * send message with #outPW and check for errors. If an error
		 * occurs log a warning
		 */
		if (message != null)
		{
			outPW.println(message);
			if (outPW.checkError())
			{
				logger.warning("error writing");
			}
		}
	}

	/**
	 * Compute Color from name: retrieve color from {@link #colorMap} and if
	 * this name is not already in the map add a new <name, color> to the map
	 * before retrieving
	 * @param name the name to generate color from
	 * @return a {@link Color} associated to the name or null if name is null or
	 * empty
	 * @warning Ensure similar names don't get similar colors
	 */
	public java.awt.Color getColorFromName(String name)
	{
		/*
		 *  return a color (not too bright, using Color#darker()) from the
		 * provided name.
		 */
		if (name != null)
		{
			if (name.length() > 0)
			{
				if (!colorMap.containsKey(name))
				{
					Random rand = new Random(name.hashCode());
					colorMap.put(name, new java.awt.Color(rand.nextInt()).darker());
					// colorMap.put(name, name.hashCode()).darker();
				}

				return colorMap.get(name);
			}
		}

		return null;
	}

	/**
	 * {@link #inPipe} accessor to connect to a {@link PipedOutputStream}
	 * @return The {@link #inPipe}
	 */
	public PipedInputStream getInPipe()
	{
		return inPipe;
	}

	/**
	 * {@link #outPipe} accessor to connecto to a {@link PipedInputStream}
	 * @return The {@link #outPipe}
	 */
	public PipedOutputStream getOutPipe()
	{
		return outPipe;
	}

	/**
	 * Cleanup: close streams
	 */
	public void cleanup()
	{
		logger.info("closing output print writer ... ");
		outPW.close();

		logger.info("closing output stream ... ");
		try
		{
			outPipe.close();
		}
		catch (IOException e)
		{
			logger.warning("failed to close output stream"
				+ e.getLocalizedMessage());
		}

		logger.info("closing input stream ... ");
		try
		{
			inPipe.close();
		}
		catch (IOException e)
		{
			logger.warning("failed to close input stream "
				+ e.getLocalizedMessage());
		}
	}

	/**
	 * Run loop : to be implemented in subclasses
	 */
	@Override
	public abstract void run();
}
