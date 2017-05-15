package com.mls.kicker.reservation.slack;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.mls.kicker.reservation.engine.Referee;
import com.mls.kicker.reservation.engine.Referee.TableStatus;
import com.mls.kicker.reservation.engine.StateChangedEvent;
import com.mls.kicker.reservation.engine.TimeoutHandler;
import com.mls.kicker.reservation.lcd.PiScreen;
import com.mls.kicker.reservation.led.PiLeds;

import allbegray.slack.SlackClientFactory;
import allbegray.slack.bot.SlackbotClient;
import allbegray.slack.rtm.CloseListener;
import allbegray.slack.rtm.Event;
import allbegray.slack.rtm.EventListener;
import allbegray.slack.rtm.FailureListener;
import allbegray.slack.rtm.SlackRealTimeMessagingClient;
import allbegray.slack.type.Channel;
import allbegray.slack.type.History;
import allbegray.slack.type.Message;
import allbegray.slack.type.Profile;
import allbegray.slack.type.User;
import allbegray.slack.webapi.SlackWebApiClient;
import allbegray.slack.webhook.SlackWebhookClient;

@Component
public class Slack {
	
	private static final int HISTORY_SIZE = 6;

	private static final int HISTORY_ENTRIES_TO_CLEAR = 1000;
	
	private static Logger log = LoggerFactory.getLogger( Slack.class );
	
	private static final String CMD_STATUS_SHORT = "st";
	
	private static final String CMD_STATUS_LONG = "status";
	
	private static final String CMD_RELEASE_SHORT = "rel";
	
	private static final String CMD_RELEASE_LONG = "release";
	
	private static final String CMD_PLAY_LONG = "play";
	
	private static final String CMD_CANCEL_LONG = "cancel";
	
	private static final String CMD_RESERVE_SHORT = "res";
	
	private static final String CMD_RESERVE_LONG = "reserve";

	private static final String CMD_CLEAR = "clear";
	
	private static final Set< String > allCommands = new HashSet<>();
	
	private static final long CONNECTION_CHECK_PERIOD = 5000;
	
	static {
		allCommands.add( CMD_CANCEL_LONG );
		allCommands.add( CMD_PLAY_LONG );
		allCommands.add( CMD_RELEASE_LONG );
		allCommands.add( CMD_RELEASE_SHORT );
		allCommands.add( CMD_RESERVE_LONG );
		allCommands.add( CMD_RESERVE_SHORT );
		allCommands.add( CMD_STATUS_LONG );
		allCommands.add( CMD_STATUS_SHORT );
		allCommands.add( CMD_CLEAR );
		
	}
	
	private String channelName = "kickertest2";
	
	private final AtomicLong counter = new AtomicLong();
	
	private String slackBotToken;
	
	private String webhookUrl;
	
	private String slackbotUrl;
	
	private SlackRealTimeMessagingClient rtmClient;
	
	private SlackbotClient slackbotClient;
	
	private SlackWebApiClient webApiClient;
	
	private SlackWebhookClient webhookClient;
	
	@Autowired
	private Referee referee;
	
	@Autowired
	private PiLeds piLeds;

	@Autowired
	private PiScreen piScreen;
	
	private Channel channel;
	
	private Map< String, User > users;
	
	private TimeoutHandler reservationTimeoutHandler;
	
	private TimeoutHandler playTimeoutHandler;
	
	private String botId;
	
	private List< String > postedMessages = Collections.synchronizedList( new ArrayList<>() );
	
	private volatile boolean rtmClientOpen;
	
	private Timer timer;
	
	private TimerTask connectionChecker;
	
	public void postMessageToUser( String userId, String message ) {
		this.webApiClient.postMessage( this.channel.getName(), message, "mlsbot", true );
		deleteOldMessages();
	}
	
	public void postMessageToChannel( String message ) {
		try {
			this.webApiClient.postMessage( this.channel.getName(), message, "mlsbot", true );
		} catch ( Exception e ) {
			log.error( e.getMessage(), e );
		}
		deleteOldMessages();
	}
	
	private void deleteOldMessages() {
		deleteOldMessage();
		deleteOldMessage();
	}
	
	private synchronized void deleteOldMessage() {
		try {
			if ( this.postedMessages.size() > HISTORY_SIZE ) {
				Iterator< String > messagesIterator = this.postedMessages.iterator();
				String oldestMessage = messagesIterator.next();
				messagesIterator.remove();
				log.info( "Deleting message with ts=" + oldestMessage );
				boolean isOk = this.webApiClient.deleteMessage( this.channel.getId(), oldestMessage );
				log.info( "Deleting message with ts=" + oldestMessage + ", result=" + isOk );
				log.info( "Old messages to remove in the future: " + postedMessages.size() );
			}
		} catch ( Throwable e ) {
			
		}
	}
	
	private synchronized void addToMessagesToRemove( String messageTs ) {
		this.postedMessages.add( messageTs );
	}
	
	@PostConstruct
	public synchronized void initialize() throws IOException {
		
		String propFileName = "/config.properties";
		
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream( propFileName );
		
		Properties prop = new Properties();
		if ( inputStream != null ) {
			prop.load( inputStream );
		} else {
			throw new FileNotFoundException( "property file '" + propFileName + "' not found in the classpath" );
		}
		
		log.info( "slackBotToken=" + prop.getProperty( "slackBotToken" ) );
		channelName = prop.getProperty( "channelName" );
		slackBotToken = prop.getProperty( "slackBotToken" );
		webhookUrl = prop.getProperty( "webhookUrl" );
		slackbotUrl = "https://marketlogicsoftware.slack.com/services/hooks/slackbot?token=" + slackBotToken;
		
		this.reservationTimeoutHandler = new TimeoutHandler() {
			
			@Override
			public void timedOut() {
				postMessageToChannel( "Reservation timed out.\n" + createStatusString( TableStatus.FREE ) );
			}
		};
		this.referee.addReservationTimeoutHandler( this.reservationTimeoutHandler );
		
		this.playTimeoutHandler = new TimeoutHandler() {
			
			@Override
			public void timedOut() {
				postMessageToChannel( "Playtime timed out.\n" + createStatusString( TableStatus.FREE ) );
			}
		};
		this.referee.addPlayTimeoutHandler( this.playTimeoutHandler );
		
		this.slackbotClient = SlackClientFactory.createSlackbotClient( slackbotUrl );
		this.webhookClient = SlackClientFactory.createWebhookClient( webhookUrl );
		this.webApiClient = SlackClientFactory.createWebApiClient( slackBotToken );
		// log.info( webApiClient.getBotInfo().getId() );
		
		reloadReservationChannelInfo();
		reloadUsers();

		clearHistory();
		
		initializeRTMClient();
		
		
		
		timer = new Timer( true );
		
		connectionChecker = new TimerTask() {
			
			@Override
			public void run() {
				checkConnection();
			}
		};
		timer.schedule( connectionChecker, CONNECTION_CHECK_PERIOD, CONNECTION_CHECK_PERIOD );
	}
	
	private synchronized void initializeRTMClient() {
		
		if ( rtmClient != null ) {
			rtmClient.close();
		}
		JsonNode welcomeMessage = this.webApiClient.startRealTimeMessagingApi();
		String webSocketUrl = welcomeMessage.findPath( "url" ).asText();
		this.botId = welcomeMessage.findPath( "self" ).findPath( "id" ).asText();
		this.rtmClient = new SlackRealTimeMessagingClient( webSocketUrl );
		
		// this.rtmClient = SlackClientFactory.createSlackRealTimeMessagingClient(
		// slackBotToken );
		
		this.rtmClient.addListener( Event.MESSAGE, new EventListener() {
			
			@Override
			public void onMessage( JsonNode message ) {
				String channelName = message.get( "channel" ).asText();
				if ( Slack.this.channel.getId().equals( channelName ) ) {
					JsonNode jsonNode = message.get( "user" );
					if ( jsonNode != null ) {
						String userId = jsonNode.asText();
						boolean haveInfoAboutUser = ( Slack.this.users != null ) && Slack.this.users.containsKey( userId );
						final User user;
						if ( haveInfoAboutUser ) {
							user = Slack.this.users.get( userId );
						} else {
							user = null;
						}
						log.info( "Got message on " + channelName + " channel." );
						String text = message.get( "text" ).asText();
						String command = text.toLowerCase();
						switch ( command ) {
							case CMD_RESERVE_LONG:
							case CMD_RESERVE_SHORT:
								reserve( user );
								break;
							case CMD_CANCEL_LONG:
								cancel( user );
								break;
							case CMD_PLAY_LONG:
//								play( user );
								break;
							case CMD_RELEASE_LONG:
							case CMD_RELEASE_SHORT:
//								release( user );
								break;
							case CMD_STATUS_LONG:
							case CMD_STATUS_SHORT:
								status();
								break;
							case CMD_CLEAR:
								clearHistory();
								break;
						}
						if ( !haveInfoAboutUser ) {
							reloadUsers();
						}
						boolean isCommand = allCommands.contains( command );
						log.info( "isCommand=" + isCommand );
						if ( ( Slack.this.botId != null ) && Slack.this.botId.equals( userId ) ) {
							final String messageTs = message.get( "ts" ).asText();
							log.info( "Adding message to remove later ts=" + messageTs + ", isCommand=" + isCommand );
							addToMessagesToRemove( messageTs );
						}
						deleteOldMessages();
					} else {
						log.info( "No user info. Ignoring." );
						
					}
				} else {
					log.info( "Got message, but not on " + channelName + " channel." );
				}
			}
		} );
		
		this.rtmClient.addListener( Event.HELLO, new EventListener() {
			
			@Override
			public void onMessage( JsonNode message ) {
				log.info( "Hello message received" );
				rtmClientOpen = true;
				postMessageToChannel( "Service is :arrow_forward:" );
				piLeds.updateStatus();
			}
		} );
		this.rtmClient.addFailureListener( new FailureListener() {
			
			@Override
			public void onFailure( Throwable t ) {
				log.info( "Failure received." );
				// rtmClient.close();
				// rtmClient.connect();
			}
		} );
		this.rtmClient.addCloseListener( new CloseListener() {
			
			@Override
			public void onClose() {
				log.info( "Close received." );
				rtmClientOpen = false;
				postMessageToChannel( "Service is :black_square_for_stop:" );
			}
		} );
		this.rtmClient.connect();
	}
	
	private void reloadReservationChannelInfo() {
		List< Channel > channelList = this.webApiClient.getChannelList();
		for ( final Channel channel : channelList ) {
			if ( channel.getName().equals( channelName ) ) {
				this.channel = channel;
			}
		}
	}
	
	private void reloadUsers() {
		final List< User > usersList = this.webApiClient.getUserList();
		this.users = Collections.synchronizedMap( new HashMap<>( usersList.size() ) );
		for ( final User user : usersList ) {
			this.users.put( user.getId(), user );
		}
	}
	
	private synchronized void clearHistory() {
		final History history = this.webApiClient.getChannelHistory( channel.getId(), HISTORY_ENTRIES_TO_CLEAR );
		log.info( "History entries: " + history.getMessages().size() );
		for ( final Message message : history.getMessages() ) {
			if ( message.getUser().equals( botId ) ) {
				addToMessagesToRemove( message.getTs() );
				deleteOldMessage();
			}
		}
	}
	
	@PreDestroy
	public synchronized void destroy() {
		if ( this.slackbotClient != null ) {
			this.slackbotClient.shutdown();
		}
		if ( this.rtmClient != null ) {
			this.rtmClient.close();
		}
		this.referee.removePlayTimeoutHandler( this.playTimeoutHandler );
		this.referee.removeReservationTimeoutHandler( this.reservationTimeoutHandler );
	}
	
	public void reserve( User user ) {
		final StateChangedEvent reservationData = this.referee.reserve( user.getId() );
		final TableStatus tableState = reservationData.getCurrentStatus();
		switch ( tableState ) {
			case FREE:
				postMessageToChannel( createStatusString( tableState ) );
				break;
			case OCCUPIED:
			default:
				postMessageToChannel( "Reservation rejected!\n" + createStatusString( TableStatus.OCCUPIED ) );
				break;
			case RESERVED:
				if ( user.getId().equals( reservationData.getUserId() ) ) {
					postMessageToChannel( createStatusString( TableStatus.RESERVED ) + "  by " + getUserFullName( user ) );
				} else {
					postMessageToChannel(
						"Reservation rejected!\n" + createStatusString( TableStatus.RESERVED ) + "  by " + getUserFullNameByUserId( reservationData.getUserId() ) );
				}
				break;
		}
	}
	
	public void cancel( User user ) {
		final StateChangedEvent reservationData = this.referee.cancel( user.getId() );
		final TableStatus tableState = reservationData.getCurrentStatus();
		switch ( tableState ) {
			case FREE:
			case OCCUPIED:
			case RESERVED:
			default:
				postMessageToChannel( createStatusString( tableState ) );
				break;
		}
	}
	
	public void play( User user ) {
		final String userId;
		if ( user != null ) {
			userId = user.getId();
		} else {
			userId = "-1";
		}
		play( userId );
		
	}
	
	public void play( final String userId ) {
		final StateChangedEvent reservationData = this.referee.play( userId );
		final TableStatus prevState = reservationData.getPreviousStatus();
		switch ( prevState ) {
			case FREE:
			default:
				postMessageToChannel( "Kicker match has just started.\n" + createStatusString( TableStatus.OCCUPIED ) );
				break;
			case RESERVED:
				final TableStatus currState = reservationData.getCurrentStatus();
				switch ( currState ) {
					case FREE:
					default:
						break;
					case RESERVED:
						postMessageToChannel( createStatusString( TableStatus.FREE ) + " by " + getUserFullNameByUserId( reservationData.getUserId() ) );
						break;
					case OCCUPIED:
						postMessageToChannel(
							"Kicker match has just started.\n" + createStatusString( TableStatus.OCCUPIED ) + " by "
								+ getUserFullNameByUserId( reservationData.getUserId() ) );
						break;
				}
				break;
			case OCCUPIED:
				// We don't send any info about extending match.
				break;
		}
	}
	
	public void release( User user ) {
		
		final String userId;
		if ( user != null ) {
			userId = user.getId();
		} else {
			userId = "-1";
		}
		release( userId );
	}
	
	public void release( final String userId ) {
		final StateChangedEvent reservationData = this.referee.release( userId );
		final TableStatus prevState = reservationData.getPreviousStatus();
		switch ( prevState ) {
			case FREE:
			default:
				// postMessage( createStatusString( TableStatus.FREE ) );
				break;
			case RESERVED:
				// We don't send any info when somebody pressed red button during
				// reservation.
				// Reservation needs to be canceled by command and not by a button
				break;
			case OCCUPIED:
				postMessageToChannel( "Kicker match has finished.\n" + createStatusString( TableStatus.FREE ) );
				break;
		}
	}
	
	public void status() {
		postMessageToChannel( createStatusString( this.referee.status() ) );
	}
	
	private String createStatusString( TableStatus status ) {
		final StringBuilder sb = new StringBuilder( 32 );
		sb.append( "Kicker status: " );
		switch ( status ) {
			case FREE:
				sb.append( ":white_check_mark: FREE" );
				break;
			case OCCUPIED:
				sb.append( ":no_entry: OCCUPIED" );
				break;
			case RESERVED:
				sb.append( ":warning: RESERVED" );
				break;
			default:
				sb.append( ":interrobang: UNKNOWN" );
				break;
		}
		return sb.toString();
	}
	
	private String getUserFullNameByUserId( String userId ) {
		final String userName;
		if ( ( userId != null ) && this.users.containsKey( userId ) ) {
			final User user = this.users.get( userId );
			userName = getUserFullName( user );
		} else {
			userName = "somebody";
		}
		return userName;
	}
	
	private String getUserFullName( User user ) {
		final String userName;
		if ( user != null ) {
			final Profile profile = user.getProfile();
			userName = profile != null ? profile.getReal_name() + " (" + user.getName() + ")" : user.getName();
		} else {
			userName = "somebody";
		}
		return userName;
	}
	
	public String getUserNameByUserId( String userId ) {
		final String userName;
		if ( ( userId != null ) && this.users.containsKey( userId ) ) {
			final User user = this.users.get( userId );
			userName = getUserName( user );
		} else {
			userName = "???";
		}
		return userName;
	}
	
	private String getUserName( User user ) {
		final String userName;
		if ( user != null ) {
			userName = user.getName();
		} else {
			userName = "???";
		}
		return userName;
	}
	
	public void disconnect() {
		postMessageToChannel( "Kicker table reservation system is DOWN" );
		if ( this.slackbotClient != null ) {
			this.slackbotClient.shutdown();
		}
		if ( this.rtmClient != null ) {
			this.rtmClient.close();
		}
	}
	
	public boolean isRtmClientOpen() {
		return rtmClientOpen;
	}
	
	public synchronized void checkConnection() {
		//log.info( "Checking connection, rtmClientOpen: " + rtmClientOpen );
		if ( !rtmClientOpen ) {
			log.info( "RTMClient is not open. Reinitializing..." );
			piLeds.lightsDown();
			piScreen.display2Lines( "Connection", "Problem", true );
			try {
				initializeRTMClient();
				postMessageToChannel( "Respawn after connection lost." );
			} catch(Exception e) {
				log.warn( "Exception in reinitializing..." );
				e.printStackTrace();
			}
		}
	}
	
}
