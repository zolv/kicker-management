package com.mls.kicker.reservation.engine;

import com.mls.kicker.reservation.engine.Referee.TableStatus;

public class StateChangedEvent {
	
	private final TableStatus previousStatus;
	
	private final TableStatus currentStatus;
	
	private final String userId;
	
	private final Long timeLeft;
	
	public StateChangedEvent( TableStatus previousStatus, TableStatus currentStatus, String userId ) {
		this( previousStatus, currentStatus, userId, null );
	}
	
	public StateChangedEvent( TableStatus previousStatus, TableStatus currentStatus, String userId, Long timeLeft ) {
		super();
		this.previousStatus = previousStatus;
		this.currentStatus = currentStatus;
		this.userId = userId;
		this.timeLeft = timeLeft;
	}
	
	public TableStatus getCurrentStatus() {
		return currentStatus;
	}
	
	public TableStatus getPreviousStatus() {
		return previousStatus;
	}
	
	public String getUserId() {
		return userId;
	}
	
	public Long getTimeLeft() {
		return timeLeft;
	}
	
}
