package chat.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.logging.Logger;

import chat.Failure;
import chat.Vocabulary;
import logger.LoggerFactory;

/**
 * User Handler handles what the user types and send it to the chat server
 * @author x0wass
 */
class UserHandler implements Runnable
{
	/**
	 * User Input Buffered Reader reads user input
	 */
	private BufferedReader userInBR;

	/**
	 * Server Output Print Writer writes user input to server
	 */
	private PrintWriter serverOutPW;

	/**
	 * Common Run execution status between {@link UserHandler} and
	 * {@link ServerHandler}
	 */
	private Boolean commonRun;

	/**
	 * Logger used to display debug or info messages
	 */
	private Logger logger;

	/**
	 * UserHandler constructor
	 * @param in User input stream to read user inputs
	 * @param out Server Output stream to write users inputs to server
	 * @param commonRun Common Run execution status between {@link UserHandler} and
	 * {@link ServerHandler}
	 * @param parentLogger the parent logger
	 */
	public UserHandler(InputStream in,
	                   OutputStream out,
	                   Boolean commonRun,
	                   Logger parentLogger)
	{
		logger = LoggerFactory.getParentLogger(getClass(), parentLogger,
				parentLogger.getLevel());

		/*
		 * User input stream reader instantiation: userInBR created on the
		 * InputStream if it is non null otherwise exit with
		 * Failure.USER_INPUT_STREAM exit status
		 */
		if (in != null)
		{
			logger.info("UserHandler: creating user input buffered reader ... ");

			/*
			 * DONE BufferedReader creation on InputStreamReader of the user
			 * input stream
			 */
			userInBR = new BufferedReader(new InputStreamReader(in));
		}
		else
		{
			logger.severe("UserHandler: null input stream"
					+ Failure.USER_INPUT_STREAM);
			System.exit(Failure.USER_INPUT_STREAM.toInteger());
		}

		/*
		 * Server output print writer instantiation on the output stream if it
		 * is non null otherwise exit with Failure.CLIENT_OUTPUT_STREAM
		 * exit status
		 */
		if (out != null)
		{
			logger.info("UserHandler: creating server output print writer ... ");

			/*
			 * DONE PrintWriter creation on server output stream with autoFlush
			 */
			serverOutPW = new PrintWriter(out, true);
		}
		else
		{
			logger.severe("UserHandler: null output stream"
					+ Failure.CLIENT_OUTPUT_STREAM);
			System.exit(Failure.CLIENT_OUTPUT_STREAM.toInteger());
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
	 * UserHandler main run loop : Listen to user inputs and sends it to server
	 * output
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run()
	{
		String userInput = null;

		/*
		 * Main processing loop:
		 * 	- read line from userInBR
		 * 	- if the line is non null then
		 * 		- send it to serverOutPW
		 * 		- also check for special commannds such as byeCmd from
		 * 		the Vocabulary
		 */
		while (commonRun.booleanValue())
		{
			try
			{
				/*
				 * DONE read line from user. If an exception occur log severe
				 * warning and break processing loop
				 */
				userInput = userInBR.readLine();
			}
			catch (IOException e)
			{
				logger.severe("UserHandler: I/O error reading user" +
				               e.getLocalizedMessage());
				break;
			}

			if (userInput != null)
			{
				/*
				 * DONE Sends user input to server using the server print writer
				 * and check for errors (in such case log severe and break loop)
				 */
				serverOutPW.println(userInput);
				if (serverOutPW.checkError())
				{
					logger.severe("UserHandler: serverOutPW has errors");
					break;
				}

				/*
				 * DONE check if user has typed the Vocabulary.byeCmd from the Vocabulary
				 * and if so break the loop
				 */
				if (userInput.toLowerCase().equals(Vocabulary.byeCmd))
				{
					break;
				}
			}
			else
			{
				logger.warning("UserHandler: null user input");
				break;
			}
		}

		if (commonRun.booleanValue())
		{
			logger.info("UserHandler: changing run state at the end ... ");

			synchronized (commonRun)
			{
				commonRun = Boolean.FALSE;
			}
		}
	}

	/**
	 * Streams cleanup and close
	 */
	public void cleanup()
	{
		logger.info("UserHandler: closing user input stream reader ... ");
		// Input Stream reader close. If an exception occur log severe
		try
		{
			userInBR.close();
		}
		catch (IOException e)
		{
			logger.severe("UserHandler: closing server input stream reader failed");
			logger.severe(e.getLocalizedMessage());
		}

		logger.info("UserHandler: closing server output print writer ... ");
		// Output print writer close
		serverOutPW.close();
	}
}
