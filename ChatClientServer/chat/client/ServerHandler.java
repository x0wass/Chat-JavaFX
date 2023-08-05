package chat.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StreamCorruptedException;
import java.util.logging.Logger;

import chat.Failure;
import chat.UserOutputType;
import logger.LoggerFactory;
import models.Message;

/**
 * Server Handler: Reads messages stream from server and writes messages to
 * user's (client) output stream.
 * A client should accept either:
 * <ul>
 * <li>text messages (on the console client)</li>
 * <li>{@link Message} objects (such as the one sent by the server, on the GUI
 * client
 * which allow to extract message components such as author, dateand
 * content)</li>
 * </ul>
 * @author x0wass
 */
class ServerHandler implements Runnable
{
	/**
	 * Input stream from server (contains {@link Message} objects)
	 */
	private ObjectInputStream serverInOS;

	/**
	 * The kind of messages supported by the client (either text or message
	 * objects)
	 * @see UserOutputType
	 */
	private UserOutputType userOutType;

	/**
	 * Print writer to user output (when using text messages only)
	 */
	private PrintWriter userOutPW;

	/**
	 * Object output stream to user output (when using Message objects)
	 */
	private ObjectOutputStream userOutOS;

	/**
	 * Common run between {@link ServerHandler} and {@link UserHandler}
	 */
	private Boolean commonRun;

	/**
	 * Logger used to display debug or info messages
	 */
	private Logger logger;

	/**
	 * server handler constructor
	 * @param name our user name on server
	 * @param in input stream from server
	 * @param out output stream to user
	 * @param outType output type (text or {@link Message} objects)
	 * @param commonRun common run between this and {@link UserHandler}
	 * @param parentLogger parent logger
	 */
	public ServerHandler(String name,
	                     InputStream in,
	                     OutputStream out,
	                     UserOutputType outType,
	                     Boolean commonRun,
	                     Logger parentLogger)
	{
		logger = LoggerFactory.getParentLogger(getClass(),
		                                       parentLogger,
		                                       parentLogger.getLevel());
		/*
		 * Check for non null input stream and Object input stream instantiation
		 * on the input stream.
		 */
		if (in != null)
		{
			logger.info("ServerHandler: creating server input reader ... ");
			/*
			 * DONE ObjectInputStream instantiation from server input stream. If
			 * an exception occur shut down app with CLIENT_INPUT_STREAM Failure
			 * status
			 */
			try
			{
				serverInOS = new ObjectInputStream(in);
			}
			catch (IOException e)
			{
				logger.severe("ServerHandler: " + Failure.CLIENT_INPUT_STREAM
					+ " unable to open object input stream");
				System.exit(Failure.CLIENT_INPUT_STREAM.toInteger());
			}
		}
		else
		{
			logger.severe("ServerHandler: " + Failure.CLIENT_INPUT_STREAM);
			System.exit(Failure.CLIENT_INPUT_STREAM.toInteger());
		}

		/*
		 * check for non null output stream and Object output stream
		 * instantiation on the output stream
		 */
		if (out != null)
		{
			logger.info("ServerHandler: creating user output ... ");
			/*
			 * DONE According to outType create either
			 * 	- A Print writer on the user output stream
			 * 	- An Object output stream on the user output stream
			 * If an exception occurs exit with
			 * Failure.USER_OUTPUT_STREAM status
			 */
			userOutType = outType;
			switch (userOutType)
			{
				case OBJECT:
					userOutPW = null;
					try
					{
						userOutOS = new ObjectOutputStream(out);
					}
					catch (IOException e)
					{
						logger.severe("ServerHandler: unable to create "
							+ "object output stream "
							+ Failure.USER_OUTPUT_STREAM);
						System.exit(Failure.USER_OUTPUT_STREAM.toInteger());
					}
					break;
				case TEXT:
				default:
					userOutPW = new PrintWriter(out);
					userOutOS = null;
					break;
			}
		}
		else
		{
			logger.severe("ServerHandler: " + Failure.USER_OUTPUT_STREAM);
			System.exit(Failure.USER_OUTPUT_STREAM.toInteger());
		}

		if (commonRun != null)
		{
			this.commonRun = commonRun;
		}
		else
		{
			logger.severe("ServerHandler: null common run " + Failure.OTHER);
			System.exit(Failure.OTHER.toInteger());
		}
	}

	/**
	 * Server handler run loop: Listen to server's input and send it to user's
	 * output
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run()
	{
		/*
		 * Main processing loop:
		 * - Reads message from server input object stream.
		 * If an exception occurs, logs a warning and break the loop
		 * - Then writes the message to user output in either text or
		 * object format
		 * Any error or exception breaks the loop then the common run is set to
		 * false (in a synchronized block to ensure atomicity) which causes the
		 * UserHandler to terminate as well
		 */
		while (commonRun.booleanValue())
		{
			Message message = null;
			try
			{
				/*
				 * DONE read message from server on the serverInOS.
				 * If an exception occurs log a warning and break the loop
				 */
				message = (Message) serverInOS.readObject();
			}
			catch (ClassNotFoundException cnfe)
			{
				logger.warning("ServerHandler: Class of a serialized object "
					+ "cannot be found after readObject" +
					cnfe.getLocalizedMessage());
				break;
			}
			catch (InvalidClassException ice)
			{
				logger.warning("ServerHandler: Something is wrong with a class "
					+ "used by serialization after readObject" +
					ice.getLocalizedMessage());
				break;
			}
			catch (StreamCorruptedException sce)
			{
				logger.warning("ServerHandler: Control information in the "
					+ "stream is inconsistent after readObject" +
					sce.getLocalizedMessage());
				break;
			}
			catch (OptionalDataException ode)
			{
				logger.warning("ServerHandler: Primitive data was found in the "
					+ "stream instead of objects after readObject" +
					ode.getLocalizedMessage());
				break;
			}
			catch (IOException e)
			{
				logger.warning("ServerHandler: I/O error reading server : " +
			                   e.getLocalizedMessage());
				break;
			}

			if ((message != null))
			{
				/*
				 * DONE Display message to user with either
				 * - userOutPW.println when using text messages (check userOutPW for
				 * errors and log warning if any) or
				 * - userOutOS.writeObject when using Message objects
				 * if an error occurs set error = true;
				 */
				boolean error = false;
				switch (userOutType)
				{
					case OBJECT:
						try
						{
							userOutOS.writeObject(message);
						}
						catch (IOException e)
						{
							logger.warning("Serverhandler: userOutOS has "
								+ "IOException" + e.getLocalizedMessage());
							error = true;
						}
						break;
					case TEXT:
					default:
						userOutPW.println(message);

						if (userOutPW.checkError())
						{
							logger.warning("Serverhandler: userOutPw has errors");
							error = true;
						}
						break;
				}
				if (error)
				{
					break; // break this loop
				}
			}
			else
			{
				logger.warning("ServerHandler: null input read");
				break;
			}
		}

		if (commonRun.booleanValue())
		{
			logger.info("ServerHandler: changing run state at the end ... ");

			synchronized (commonRun)
			{
				commonRun = Boolean.FALSE;
			}
		}
	}

	/**
	 * Cleanup and close streams
	 */
	public void cleanup()
	{
		logger.info("ServerHandler: closing server input stream reader ... ");
		/*
		 * Close Server input stream.
		 * If an exception occurs log severe
		 */
		try
		{
			serverInOS.close();
		}
		catch (IOException e)
		{
			logger.severe("ServerHandler: closing server input stream reader failed: " +
			              e.getLocalizedMessage());
		}

		logger.info("ServerHandler: closing user output print writer ... ");

		/*
		 * Close user output (if non null).
		 * If an exception occurs log severe
		 */
		if (userOutPW != null)
		{
			userOutPW.close();

			if (userOutPW.checkError())
			{
				logger.severe("ServerHandler: closed user text output has errors: ");
			}
		}

		if (userOutOS != null)
		{
			try
			{
				userOutOS.close();
			}
			catch (IOException e)
			{
				logger.severe("ServerHandler: closing user object output stream failed: "
						+ e.getLocalizedMessage());
			}
		}
	}
}
