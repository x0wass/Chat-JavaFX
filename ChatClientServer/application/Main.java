package application;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import chat.Failure;
import chat.UserOutputType;
import chat.client.ChatClient;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import logger.LoggerFactory;
import models.messagesRunners.AbstractMessagesRunner;


/**
 * JavaFX Chat Client Application
 * @author x0wass
 */
public class Main extends Application
{
	/**
	 * Default connection port number
	 */
	public static final int DEFAULTPORT = 1396;

	/**
	 * Connection port number between server and clients
	 */
	private int port = DEFAULTPORT;

	/**
	 * Verbose status indicating if debug messages should be displayed
	 * on the console or only sent to a log file
	 */
	private boolean verbose = true;

	/**
	 * Logger used to display debug or info messages
	 * @implNote Needs to be initialized {@link #init()}
	 */
	private Logger logger = null;

	/**
	 * Chat server host string
	 */
	private String host = null;

	/**
	 * User name for server connection
	 */
	private String name = null;

	/**
	 * Input stream to read user messages from
	 */
	private InputStream userIn = null;

	/**
	 * Output stream to write messages to the user
	 */
	private OutputStream userOut = null;

	/**
	 * Clients threads pool containg all threads used in the client.
	 */
	private Vector<Thread> threadPool = null;

	/**
	 * Common run to set on {@link ChatClient} and {@link Controller}'s message
	 * runner.
	 */
	private Boolean commonRun = null;

	/**
	 * Application initialization method.
	 * Called after construction and before actual starting
	 */
	@Override
	public void init() throws Exception
	{
		super.init();

		Application.Parameters appParameters = getParameters();
		List<String> rawParameters = appParameters.getRaw();

		if (rawParameters.contains("--verbose"))
		{
			verbose = true;
		}

		/*
		 * logger instantiation
		 */
		logger = null;
		Class<?> runningClass = getClass();
		String logFilename =
		    (verbose ? null : runningClass.getSimpleName() + ".log");
		Logger parent = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
		Level level = (verbose ? Level.ALL : Level.INFO);
		try
		{
			logger = LoggerFactory.getLogger(runningClass,
			                                 verbose,
			                                 logFilename,
			                                 false,
			                                 parent,
			                                 level);
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
			System.exit(Failure.OTHER.toInteger());
		}

		setAttributes(rawParameters);
		threadPool = new Vector<Thread>();
		commonRun = Boolean.TRUE;
	}

	/**
	 * The main entry point for all JavaFX applications.
	 * The start method is called after the init method has returned,
	 * and after the system is ready for the application to begin running.
	 * NOTE: This method is called on the JavaFX Application Thread.
	 */
	@Override
	public void start(Stage primaryStage)
	{
		// --------------------------------------------------------------------
		// Loads Scene from FXML
		// --------------------------------------------------------------------
		logger.info("Loading FXML file ...");
		FXMLLoader loader = new FXMLLoader(getClass().getResource("ClientFrame.fxml"));
		BorderPane root = null;
		try
		{
			/*
			 * Complete ClientFrame.fxml with SceneBuilder until
			 * every FXML annotated attribute defined in Controller is assigned
			 * as an fx:id in the fxml file. Otherwise the load method below
			 * will raise an IOException.
			 */
			root = loader.<BorderPane>load();
		}
		catch (IOException e)
		{
			logger.severe("Can't load FXML file " + e.getLocalizedMessage());
			System.exit(Failure.CLIENT_NOT_READY.toInteger());
		}
		primaryStage.setHeight(550.);
		primaryStage.setWidth(915.);
		primaryStage.setResizable(false); // taille fenêtre fixé
		
		
		// --------------------------------------------------------------------
		// Get controller's instance and get/set some values such as
		//	- set name as author's name into controller
		//	- set this logger as parent logger of controller
		//	- set #commonRun on controller
		//	- set #host server on controller
		//	- set parent stage on controller so it can be closed later
		//	- get controller's runner in order to thread it
		// --------------------------------------------------------------------
		logger.info("Setting up controller");
		Controller controller = (Controller) loader.getController();
		
		controller.setAuthor(name);
		controller.setParentLogger(logger);
		controller.setCommonRun(commonRun);
		controller.setServer(host);
		controller.setParentStage(primaryStage);
		AbstractMessagesRunner messageRunner = controller.getRunner();

		if (messageRunner == null)
		{
			logger.severe(Failure.CLIENT_NOT_READY
			    + " Null Message Runner, Abort ...");
			System.exit(Failure.CLIENT_NOT_READY.toInteger());
		}

		// --------------------------------------------------------------------
		// I/O Connections between controller's message runner and this to
		// prepare for the ChatClient creation
		//	- runner : outPipe <--> userIn
		//	- runner : inPipe <--> userOut
		// --------------------------------------------------------------------
		logger.info("setting up I/O connections");
		@SuppressWarnings("resource") // inPipe is closed at the end of messageRunner Run loop
		PipedInputStream inPipe = messageRunner.getInPipe();
		try
		{
			userOut = new PipedOutputStream(inPipe);
		}
		catch (IOException e1)
		{
			logger.severe(Failure.USER_OUTPUT_STREAM
			    + " unable to get Piped Output Stream");
			logger.severe(e1.getLocalizedMessage());
			System.exit(Failure.USER_OUTPUT_STREAM.toInteger());
		}

		@SuppressWarnings("resource") // outPipe is closed at the end of messageRunner Run loop
		PipedOutputStream outPipe = messageRunner.getOutPipe();
		try
		{
			userIn = new PipedInputStream(outPipe);
		}
		catch (IOException e1)
		{
			logger.severe(Failure.USER_INPUT_STREAM
			    + " unable to get user Piped Input Stream");
			logger.severe(e1.getLocalizedMessage());
			System.exit(Failure.USER_INPUT_STREAM.toInteger());
		}

		// --------------------------------------------------------------------
		// Launch runner
		// Create a new thread with runner
		// Add this thread to threadPool
		// Launch runner
		// --------------------------------------------------------------------
		logger.info("Launching Messages runner");
		Thread guiThread = new Thread(messageRunner);
		guiThread.setName("Messages Runner Thread");
		threadPool.add(guiThread);
		guiThread.start();

		// --------------------------------------------------------------------
		// Creates ChatClient
		// --------------------------------------------------------------------
		logger.info("Creating ChatClient ... ");
		ChatClient client = new ChatClient(host,		// server's name or IP
		                                   port,		// tcp port
		                                   name,		// user's name
		                                   userIn,		// user input
		                                   userOut,		// user output
		                                   UserOutputType.OBJECT,		// user output type (text or object)
		                                   commonRun,	// GUI commonRun
		                                   logger);		// parent logger
		// --------------------------------------------------------------------
		// If Client is ready then
		//	- Create a new thread with ChatClient
		//	- add this thread to threadPool
		//	- launch this thread
		// --------------------------------------------------------------------
		logger.info("Launching ChatClient");
		if (client.isReady())
		{
			Thread clientThread = new Thread(client);
			clientThread.setName("ChatClient Thread");
			threadPool.add(clientThread);
			clientThread.start();
		}
		else
		{
			logger.severe(Failure.CLIENT_NOT_READY + " abort ...");
			System.exit(Failure.CLIENT_NOT_READY.toInteger());
		}

		// --------------------------------------------------------------------
		// Finally launch GUI
		// --------------------------------------------------------------------
		logger.info("Setting up GUI...");
		Scene scene = new Scene(root, 600, 400, true, SceneAntialiasing.BALANCED);
		scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
		primaryStage.setScene(scene);
		if (name != null)
		{
			primaryStage.setTitle(name);
		}
		primaryStage.show();
	}

	/**
	 * This method is called when the application should stop, and provides a
	 * convenient place to prepare for application exit and destroy resources.
	 * NOTE: This method is called on the JavaFX Application Thread.
	 */
	@Override
	public void stop() throws Exception
	{
		commonRun = false;

		// Wait for all threads in the pool to terminate
		for (Thread t : threadPool)
		{
			try
			{
				t.join();
				logger.info(t.getName() + " terminated");
			}
			catch (InterruptedException e)
			{
				logger.severe("join interrupted" + e.getLocalizedMessage());
			}
		}

		// close userIn
		if (userIn != null)
		{
			try
			{
				userIn.close();
			}
			catch (IOException e)
			{
				logger.warning(": error closing user input stream: "
				    + e.getLocalizedMessage());
			}
		}

		// close userOut
		if (userOut != null)
		{
			try
			{
				userOut.close();
			}
			catch (IOException e)
			{
				logger.warning(": error closing user output stream: "
				    + e.getLocalizedMessage());
			}
		}

		super.stop();
	}

	/**
	 * Main program to launch Application
	 * @param args
	 */
	public static void main(String[] args)
	{
		launch(args);
	}

	/**
	 * Sets attributes values based on argument parsing
	 * @param parameters for setting attributes values
	 * for {@link #port}, {@link #host}, {@link #name}
	 */
	protected void setAttributes(List<String> args)
	{
		/*
		 * Attributes are initialized to their default value
		 */
		port = DEFAULTPORT;
		verbose = false;
		host = null;
		name = null;

		/*
		 * Arguments parsing
		 * 	-v | --verbose : for verbose setting
		 * 	-p | --port : for port setting used in the serverSocket
		 *	-h | --host : server name or IP address
		 *	-n | --name : user name on server
		 */
		for (Iterator<String> argIt = args.iterator(); argIt.hasNext();)
		{
			String arg = argIt.next();
			if (arg.startsWith("-")) // option argument
			{
				if (arg.equals("--verbose") || arg.equals("-v"))
				{
					logger.info("Setting verbose on");
					verbose = true;
				}
				if (arg.equals("--port") || arg.equals("-p"))
				{
					if (argIt.hasNext())
					{
						// search for port number in next argument
						Integer portInteger = readInt(argIt.next());
						if (portInteger != null)
						{
							int readPort = portInteger.intValue();
							if (readPort >= 1024)
							{
								port = readPort;
							}
							else
							{
								logger.severe(Failure.INVALID_PORT.toString() + ":" + port);
								System.exit(Failure.INVALID_PORT.toInteger());
							}
						}
						logger.fine("Setting port to: " + port);
					}
					else
					{
						logger.warning("Setting port to nothing, invalid value");
					}
				}
				if (arg.equals("--host") || arg.equals("-h"))
				{
					if (argIt.hasNext())
					{
						// parse next arg for in port value
						host = argIt.next();
						logger.info("Setting host to " + host);
					}
					else
					{
						logger.warning("Setting host to: nothing, invalid value");
					}
				}
				if (arg.equals("--name") || arg.equals("-n"))
				{
					if (argIt.hasNext())
					{
						// parse next arg for in port value
						name = argIt.next();
						logger.info("Setting user name to: " + name);
					}
					else
					{
						logger.warning("Setting user name to: nothing, invalid value");
					}
				}
			}
		}

		if (host == null) // use localhost if there is no specified host
		{
			try
			{
				host = InetAddress.getLocalHost().getHostName();
			}
			catch (UnknownHostException e)
			{
				logger.severe(Failure.NO_LOCAL_HOST.toString());
				logger.severe(e.getLocalizedMessage());
				System.exit(Failure.NO_LOCAL_HOST.toInteger());
			}
		}

		if (name == null) // use current user name if there is no specified user name
		{
			try
			{
				// Try LOGNAME on unix type systems
				name = System.getenv("LOGNAME");
			}
			catch (NullPointerException npe)
			{
				logger.warning("no LOGNAME found, trying USERNAME");
				try
				{
					// Try USERNAME on other systems
					name = System.getenv("USERNAME");
				}
				catch (NullPointerException npe2)
				{
					logger.severe(Failure.NO_USER_NAME + " abort");
					System.exit(Failure.NO_USER_NAME.toInteger());
				}
			}
			catch (SecurityException se)
			{
				logger.severe(Failure.NO_ENV_ACCESS + " !");
				System.exit(Failure.NO_ENV_ACCESS.toInteger());
			}
		}
	}

	/**
	 * Utility method to read number from string with exception handling
	 * @param s the string to parse for number
	 * @return the parsed Integer or null if number could not be parsed
	 * from string
	 */
	protected Integer readInt(String s)
	{
		try
		{
			Integer value = Integer.parseInt(s); // Autoboxing
			return value;
		}
		catch (NumberFormatException e)
		{
			// System.err.println("readInt: " + s + " is not a number");
			logger.warning("readInt: " + s + " is not a number");
			return null;
		}
	}
}
