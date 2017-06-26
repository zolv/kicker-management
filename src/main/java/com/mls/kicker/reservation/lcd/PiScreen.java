package com.mls.kicker.reservation.lcd;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;

import com.mls.kicker.reservation.engine.Referee;
import com.mls.kicker.reservation.engine.StateChangeHandler;
import com.mls.kicker.reservation.engine.StateChangedEvent;
import com.mls.kicker.reservation.engine.Referee.TableStatus;
import com.mls.kicker.reservation.slack.Slack;

@Component
public class PiScreen {
	
	private static final int SCREEN_SCRIPT_TIMEOUT = 2;

	private static Logger log = LoggerFactory.getLogger( PiScreen.class );
	
	private static final String LCDI2C_PATH = "/home/pi/services";
	
	private static final String LCDI2C_SCIPT_NAME = "lcdi2c";
	
	@Autowired
	private Referee referee;
	
	@Autowired
	private Slack slack;
	
	private StateChangeHandler stateChangeHandler;
	
	private Date lastDisplayTime = new Date();
	
	private String previousLine1 = "";
	
	private String previousLine2 = "";
	
	public PiScreen() {
	}
	
	@PostConstruct
	public void initialize() {
		this.stateChangeHandler = new StateChangeHandler() {
			
			@Override
			public void stateChanged( StateChangedEvent event ) {
				updateStatus( event );
			}
		};
		this.referee.addStateChangedHandler( this.stateChangeHandler );
		
		initializeDisplay();
		display2Lines( "Kicker Manager", "Initializing...", false );
		
		updateStatus( new StateChangedEvent( TableStatus.FREE, TableStatus.FREE, "" ) );
		
	}

	private void initializeDisplay() {
		display( "-i" );
	}
	
	public synchronized void display2Lines( String line1, String line2, boolean initialize ) {
		display1stLine( line1, initialize );
		display2ndLine( line2 );
	}
	
	public void display1stLine( String line, boolean initialize ) {
		final String lineFixed = fixLineValue( line );
		if ( !this.previousLine1.equals( lineFixed ) ) {
			displayLine( 0, lineFixed, initialize );
			this.previousLine1 = lineFixed;
		}
	}
	
	public void display2ndLine( String line ) {
		final String lineFixed = fixLineValue( line );
		if ( !this.previousLine2.equals( lineFixed ) ) {
			displayLine( 1, lineFixed, false );
			this.previousLine2 = lineFixed;
		}
	}
	
	private void displayLine( int lineNumber, String lineFixed, boolean initialize ) {
		if(initialize) {
			display( "-i", "-b", "1", "-x", "0", "-y", Integer.toString( lineNumber ), lineFixed );
		} else {
			display( "-b", "1", "-x", "0", "-y", Integer.toString( lineNumber ), lineFixed );
		}
	}
	
	private String fixLineValue( String line ) {
		final String lineExtended = line + "                ";
		return lineExtended.substring( 0, 16 );
	}
	
	private synchronized void display( String... options ) {
		try {
//			log.info( "Executing: " + LCDI2C_PATH + "/" + LCDI2C_SCIPT_NAME + " " + Arrays.toString( options ) );
			final List< String > optionsList = Arrays.asList( options );
			final List< String > cmd = new ArrayList<>( options.length + 1 );
			cmd.add( LCDI2C_PATH + "/" + LCDI2C_SCIPT_NAME );
			cmd.addAll( optionsList );
			final Date now = new Date();
			long timeDiff = Math.abs( now.getTime() - this.lastDisplayTime.getTime() );
			if ( timeDiff < 100 ) {
				Thread.sleep( timeDiff );
			}
			this.lastDisplayTime = now;
			new ProcessExecutor().command( cmd.toArray( new String[ cmd.size() ] ) ).timeout( SCREEN_SCRIPT_TIMEOUT, TimeUnit.SECONDS ).execute();
		} catch ( InvalidExitValueException | IOException | InterruptedException | TimeoutException e ) {
			e.printStackTrace();
		}
	}
	
	public void updateStatus( StateChangedEvent event ) {
		final TableStatus currentStatus = event.getCurrentStatus();
		switch ( currentStatus ) {
			case FREE:
				display2Lines( "Free to play", "Press red button", false );
				break;
			case OCCUPIED:
				display2Lines( "Occupied by " + getUserValue( event ), formatTime( event.getTimeLeft() ), false );
				break;
			case RESERVED:
				display2Lines( "Reserved by " + getUserValue( event ), formatTime( event.getTimeLeft() ), true );
				break;
			default:
				break;
		}
	}
	
	private String getUserValue( StateChangedEvent event ) {
		String userName = this.slack.getUserNameByUserId( event.getUserId() );
		String userValue = "???".equals( userName ) ? "You" : userName;
		return userValue;
	}
	
	private String formatTime( Long timeLeft ) {
		final String result;
		if ( timeLeft != null ) {
			long timeLeftMilis = timeLeft.longValue();
			long minutes = timeLeftMilis / Referee.ONE_MINUTE;
			long seconds = ( timeLeft.longValue() % Referee.ONE_MINUTE ) / Referee.ONE_SECOND;
			result = ( minutes < 10 ? "0" + minutes : Long.valueOf(minutes) ) + ":" + ( seconds < 10 ? "0" + seconds : Long.valueOf(seconds) );
		} else {
			result = "??:??";
		}
		return "Time left: " + result;
	}
	
	@PreDestroy
	public void deinitialize() {
		display2Lines( "Turned", "off", false );
		this.referee.removeStateChangedHandler( this.stateChangeHandler );
	}
	
}
