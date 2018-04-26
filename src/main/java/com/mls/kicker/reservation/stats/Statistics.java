package com.mls.kicker.reservation.stats;

public class Statistics {
	private long numberOfDays;
	
	private long numberOfMatchesTotal;
	private double numberOfMatchesPerDay;
	private long playingTimeTotal;
	private long playingTimeAverage;
	private long reservationTimeAverage;
	
	public long getNumberOfDays() {
		return this.numberOfDays;
	}
	
	public void setNumberOfDays( long numberOfDays ) {
		this.numberOfDays = numberOfDays;
	}
	
	public long getNumberOfMatchesTotal() {
		return this.numberOfMatchesTotal;
	}
	
	public void setNumberOfMatchesTotal( long numberOfMatchesTotal ) {
		this.numberOfMatchesTotal = numberOfMatchesTotal;
	}
	
	public double getNumberOfMatchesPerDay() {
		return this.numberOfMatchesPerDay;
	}
	
	public void setNumberOfMatchesPerDay( double numberOfMatchesPerDay ) {
		this.numberOfMatchesPerDay = numberOfMatchesPerDay;
	}
	
	public long getPlayingTimeTotal() {
		return this.playingTimeTotal;
	}
	
	public void setPlayingTimeTotal( long playingTimeTotal ) {
		this.playingTimeTotal = playingTimeTotal;
	}
	
	public long getPlayingTimeAverage() {
		return this.playingTimeAverage;
	}
	
	public void setPlayingTimeAverage( long playingTimeAverage ) {
		this.playingTimeAverage = playingTimeAverage;
	}
	
	public long getReservationTimeAverage() {
		return this.reservationTimeAverage;
	}
	
	public void setReservationTimeAverage( long reservationTimeAverage ) {
		this.reservationTimeAverage = reservationTimeAverage;
	}
	
}
