package com.mls.kicker.reservation.engine;

import com.mls.kicker.reservation.engine.Referee.TableStatus;

public class StateChangedEvent {
	
	private final TableStatus previousStatus;
	
	private final TableStatus currentStatus;
	
	private final String userId;
	
	private final Long timePassed;
	
	private final Long timeLeft;
	
	public StateChangedEvent( TableStatus previousStatus, TableStatus currentStatus, String userId ) {
		this( previousStatus, currentStatus, userId, null, null );
	}
	
	public StateChangedEvent( TableStatus previousStatus, TableStatus currentStatus, String userId, long timePassed, long timeLeft ) {
		this( previousStatus, currentStatus, userId, Long.valueOf( timePassed ), Long.valueOf( timeLeft ) );
	}
	
	public StateChangedEvent( TableStatus previousStatus, TableStatus currentStatus, String userId, Long timePassed, Long timeLeft ) {
		super();
		this.previousStatus = previousStatus;
		this.currentStatus = currentStatus;
		this.userId = userId;
		this.timePassed = timePassed;
		this.timeLeft = timeLeft;
	}
	
	public TableStatus getCurrentStatus() {
		return this.currentStatus;
	}
	
	public TableStatus getPreviousStatus() {
		return this.previousStatus;
	}
	
	public String getUserId() {
		return this.userId;
	}
	
	public Long getTimeLeft() {
		return this.timeLeft;
	}
	
	public Long getTimePassed() {
		return this.timePassed;
	}
	
}
