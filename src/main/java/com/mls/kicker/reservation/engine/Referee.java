package com.mls.kicker.reservation.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope( value = "singleton" )
public class Referee {
	
	private static Logger log = LoggerFactory.getLogger( Referee.class );
	
	public static final long ONE_SECOND = 1000;
	
	public static final long ONE_MINUTE = 60 * ONE_SECOND;
	
	public static final long ONE_HOUR = 60 * ONE_MINUTE;
	
	public static final long ONE_DAY = 24 * ONE_HOUR;
	
	public static final long RESERVATION_TIME = 180 * ONE_SECOND;// 60
	
	public static final long MAX_PLAYING_TIME = 1200 * ONE_SECOND;// 900;
	
	public static final long PLAYING_EXTENTION_TIME = 300 * ONE_SECOND;// 900;
	
	private Timer timer;
	
	private class ReservationTask extends TimerTask {
		
		public ReservationTask() {
			super();
			Referee.this.reservationTimePassed = 0;
			Referee.this.reservationTimeLeft = RESERVATION_TIME;
		}
		
		@Override
		public void run() {
			try {
				decreaseReservationTime();
			} catch ( Exception e ) {
				e.printStackTrace();
				this.cancel();
			}
		}
	}
	
	private class PlayingTask extends TimerTask {
		
		public PlayingTask(long playingTime, boolean extend) {
			super();
			if ( extend ) {
				Referee.this.playingTimePassed += playingTime;
			} else {
				Referee.this.playingTimePassed = 0;
			}
			Referee.this.playingTimeLeft = playingTime;
		}
		
		@Override
		public void run() {
			try {
				decreasePlayingTime();
			} catch ( Exception e ) {
				e.printStackTrace();
				this.cancel();
			}
		}
		
	}
	
	private class FreeTask extends TimerTask {
		
		public FreeTask() {
			super();
			Referee.this.freeTimePassed = 0;
		}
		
		@Override
		public void run() {
			try {
				increaseFreeTime();
			} catch ( Exception e ) {
				e.printStackTrace();
				this.cancel();
			}
		}
	}
	
	public static enum TableStatus {
		FREE, RESERVED, OCCUPIED
	}
	
	public static enum PlayResult {
		GAME_STARTED, GAME_STARTED_RESERVATION, GAME_EXTENDED, NO_ACTION;
	}
	
	private volatile TableStatus tableStatus = TableStatus.FREE;
	
	private volatile String userId;
	
	private volatile long playingTimePassed;
	
	private volatile long playingTimeLeft;
	
	private volatile long reservationTimePassed;
	
	private volatile long reservationTimeLeft = RESERVATION_TIME;
	
	private volatile long freeTimePassed;
	
	private final List< TimeoutHandler > reservationTimeoutHandlers = new ArrayList<>( 3 );
	
	private final List< TimeoutHandler > playTimeoutHandlers = new ArrayList<>( 3 );
	
	private final List< StateChangeHandler > stateHandlers = new ArrayList<>( 3 );
	
	private volatile ReservationTask reservationTask;
	
	private volatile PlayingTask playingTask;
	
	private volatile FreeTask freeTask;
	
	public Referee() {
	}
	
	public synchronized StateChangedEvent reserve( String byUser ) {
		final StateChangedEvent result;
		switch ( this.tableStatus ) {
			case FREE:
				cancelTasks();
				this.tableStatus = TableStatus.RESERVED;
				this.userId = byUser;
				this.reservationTask = new ReservationTask();
				this.timer.schedule( this.reservationTask, ONE_SECOND, ONE_SECOND );
				result = new StateChangedEvent( TableStatus.FREE, TableStatus.RESERVED, byUser, 0L, RESERVATION_TIME );
				notifyStateListeners( result );
				break;
			case OCCUPIED:
				result = new StateChangedEvent( this.tableStatus, this.tableStatus, this.userId, this.playingTimePassed, this.playingTimeLeft );
				break;
			case RESERVED:
				result = new StateChangedEvent( this.tableStatus, this.tableStatus, this.userId, this.reservationTimePassed, this.reservationTimeLeft );
				break;
			default:
				result = new StateChangedEvent( this.tableStatus, this.tableStatus, this.userId, this.reservationTimePassed, this.reservationTimeLeft );
				break;
		}
		return result;
	}
	
	public synchronized StateChangedEvent cancel( String requestUserId ) {
		final StateChangedEvent result;
		switch ( this.tableStatus ) {
			case FREE:
			case OCCUPIED:
			default:
				result = new StateChangedEvent( this.tableStatus, this.tableStatus, this.userId, this.playingTimePassed, this.playingTimeLeft );
				break;
			case RESERVED:
				if ( ( this.userId == null ) || this.userId.equals( requestUserId ) ) {
					cancelTasks();
					result = new StateChangedEvent( this.tableStatus, TableStatus.FREE, this.userId, this.reservationTimePassed, this.reservationTimeLeft );
					this.tableStatus = TableStatus.FREE;
					this.userId = requestUserId;
					startFreeTask();
					notifyStateListeners( result );
				} else {
					result = new StateChangedEvent( this.tableStatus, this.tableStatus, this.userId, this.reservationTimePassed, this.reservationTimeLeft );
				}
				break;
		}
		return result;
	}
	
	private synchronized void reservationTimeout() {
		switch ( this.tableStatus ) {
			case RESERVED:
				cancelTasks();
				final StateChangedEvent event = new StateChangedEvent( this.tableStatus, TableStatus.FREE, this.userId );
				this.tableStatus = TableStatus.FREE;
				this.startFreeTask();
				notifyReservationTimeoutHandlers();
				notifyStateListeners( event );
				break;
			case FREE:
			case OCCUPIED:
			default:
				break;
		}
	}
	
	public synchronized void addReservationTimeoutHandler( TimeoutHandler handler ) {
		if ( handler != null ) {
			this.reservationTimeoutHandlers.add( handler );
		}
	}
	
	public synchronized void removeReservationTimeoutHandler( TimeoutHandler handler ) {
		if ( handler != null ) {
			this.reservationTimeoutHandlers.remove( handler );
		}
	}
	
	private void notifyReservationTimeoutHandlers() {
		
		for ( TimeoutHandler reservationTimeoutHandler : Referee.this.reservationTimeoutHandlers ) {
			reservationTimeoutHandler.timedOut();
		}
	}
	
	public synchronized StateChangedEvent play( String requestUserId ) {
		final StateChangedEvent result;
		switch ( this.tableStatus ) {
			default:
			case FREE:
				cancelTasks();
				this.tableStatus = TableStatus.OCCUPIED;
				this.userId = requestUserId;
				this.playingTask = new PlayingTask( MAX_PLAYING_TIME, false );
				this.timer.schedule( this.playingTask, ONE_SECOND, ONE_SECOND );
				result = new StateChangedEvent( TableStatus.FREE, TableStatus.OCCUPIED, requestUserId, 0L, MAX_PLAYING_TIME );
				break;
			case RESERVED:
				if ( ( requestUserId == null ) || requestUserId.equals( "-1" ) || requestUserId.equals( this.userId ) ) {
					cancelTasks();
					this.tableStatus = TableStatus.OCCUPIED;
					this.userId = ( requestUserId == null ) || ( requestUserId.equals( "-1" ) ) ? this.userId : requestUserId;
					this.playingTask = new PlayingTask( MAX_PLAYING_TIME, false );
					this.timer.schedule( this.playingTask, ONE_SECOND, ONE_SECOND );
					result = new StateChangedEvent( TableStatus.RESERVED, TableStatus.OCCUPIED, this.userId, 0L, MAX_PLAYING_TIME );
				} else {
					/*
					 * Not used.
					 */
					result = new StateChangedEvent( this.tableStatus, TableStatus.RESERVED, this.userId, this.reservationTimePassed, this.reservationTimeLeft );
				}
				break;
			case OCCUPIED:
				if ( this.playingTimeLeft < PLAYING_EXTENTION_TIME ) {
					cancelTasks();
					// this.userId = requestUserId;
					this.playingTask = new PlayingTask( PLAYING_EXTENTION_TIME, true );
					this.timer.schedule( this.playingTask, ONE_SECOND, ONE_SECOND );
					result = new StateChangedEvent( TableStatus.OCCUPIED, TableStatus.OCCUPIED, this.userId, this.playingTimePassed, PLAYING_EXTENTION_TIME );
				} else {
					result = new StateChangedEvent( TableStatus.OCCUPIED, TableStatus.OCCUPIED, this.userId, this.playingTimePassed, this.playingTimeLeft );
				}
				break;
		}
		notifyStateListeners( result );
		return result;
	}
	
	private synchronized void playTimeout() {
		switch ( this.tableStatus ) {
			case RESERVED:
			case FREE:
				break;
			case OCCUPIED:
			default:
				this.cancelTasks();
				final StateChangedEvent event = new StateChangedEvent( this.tableStatus, TableStatus.FREE, this.userId, this.playingTimePassed, this.playingTimeLeft );
				this.tableStatus = TableStatus.FREE;
				this.startFreeTask();
				notifyPlayTimeoutHandlers();
				notifyStateListeners( event );
				break;
		}
	}
	
	public synchronized void addPlayTimeoutHandler( TimeoutHandler handler ) {
		if ( handler != null ) {
			this.playTimeoutHandlers.add( handler );
		}
	}
	
	public synchronized void removePlayTimeoutHandler( TimeoutHandler handler ) {
		if ( handler != null ) {
			this.playTimeoutHandlers.remove( handler );
		}
	}
	
	private void notifyPlayTimeoutHandlers() {
		for ( TimeoutHandler reservationTimeoutHandler : Referee.this.playTimeoutHandlers ) {
			reservationTimeoutHandler.timedOut();
		}
	}
	
	public synchronized void addStateChangedHandler( StateChangeHandler handler ) {
		if ( handler != null ) {
			this.stateHandlers.add( handler );
		}
	}
	
	public synchronized void removeStateChangedHandler( StateChangeHandler handler ) {
		if ( handler != null ) {
			this.stateHandlers.remove( handler );
		}
	}
	
	private void notifyStateListeners( final StateChangedEvent event ) {
		for ( StateChangeHandler handler : Referee.this.stateHandlers ) {
			handler.stateChanged( event );
		}
	}
	
	public synchronized StateChangedEvent release( String requestUserId ) {
		final StateChangedEvent result;
		switch ( this.tableStatus ) {
			case OCCUPIED:
				cancelTasks();
				result = new StateChangedEvent( this.tableStatus, TableStatus.FREE, requestUserId, this.playingTimePassed, this.playingTimeLeft );
				this.userId = requestUserId;
				this.tableStatus = TableStatus.FREE;
				startFreeTask();
				notifyStateListeners( result );
				break;
			case FREE:
			default:
			case RESERVED:
				result = new StateChangedEvent( this.tableStatus, this.tableStatus, this.userId, this.reservationTimePassed, this.reservationTimeLeft );
				break;
			
		}
		return result;
	}
	
	@PostConstruct
	private void init() {
		this.timer = new Timer( "ReservationTimer" );
		startFreeTask();
	}
	
	private void cancelTasks() {
		cancelPlayingTask();
		cancelReservationTask();
		cancelFreeTask();
	}
	
	private void cancelPlayingTask() {
		if ( this.playingTask != null ) {
			this.playingTask.cancel();
			this.playingTask = null;
		}
	}
	
	private void cancelReservationTask() {
		if ( this.reservationTask != null ) {
			this.reservationTask.cancel();
			this.reservationTask = null;
		}
	}
	
	private void cancelFreeTask() {
		if ( this.freeTask != null ) {
			this.freeTask.cancel();
			this.freeTask = null;
		}
	}
	
	private void startFreeTask() {
		this.freeTask = new FreeTask();
		this.timer.schedule( this.freeTask, ONE_SECOND, ONE_SECOND );
	}
	
	private synchronized void decreaseReservationTime() {
		this.reservationTimePassed += ONE_SECOND;
		this.reservationTimeLeft -= ONE_SECOND;
		if ( this.reservationTimeLeft <= 0 ) {
			cancelReservationTask();
			reservationTimeout();
		} else {
			notifyStateListeners( new StateChangedEvent( this.tableStatus, this.tableStatus, this.userId, this.reservationTimePassed, this.reservationTimeLeft ) );
		}
	}
	
	private synchronized void decreasePlayingTime() {
		this.playingTimePassed += ONE_SECOND;
		this.playingTimeLeft -= ONE_SECOND;
		if ( this.playingTimeLeft <= 0 ) {
			cancelPlayingTask();
			playTimeout();
		} else {
			notifyStateListeners( new StateChangedEvent( this.tableStatus, this.tableStatus, this.userId, this.playingTimePassed, this.playingTimeLeft ) );
		}
	}
	
	private synchronized void increaseFreeTime() {
		this.freeTimePassed += ONE_SECOND;
	}
	
	public synchronized StateChangedEvent status() {
		final StateChangedEvent result;
		switch ( this.tableStatus ) {
			case FREE:
				result = new StateChangedEvent( this.tableStatus, this.tableStatus, this.userId, Long.valueOf( this.freeTimePassed ), null );
				break;
			case OCCUPIED:
				result = new StateChangedEvent( this.tableStatus, this.tableStatus, this.userId, this.playingTimePassed, this.playingTimeLeft );
				break;
			case RESERVED:
				result = new StateChangedEvent( this.tableStatus, this.tableStatus, this.userId, this.reservationTimePassed, this.reservationTimeLeft );
				break;
			default:
				result = new StateChangedEvent( this.tableStatus, this.tableStatus, this.userId, Long.valueOf( -1 ), Long.valueOf( -1 ) );
				break;
		}
		return result;
	}
}
