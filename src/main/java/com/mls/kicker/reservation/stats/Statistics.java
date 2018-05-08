package com.mls.kicker.reservation.stats;

public class Statistics {
	private Long numberOfDays;
	
	private Long numberOfMatchesTotal;
	private Double numberOfMatchesPerDay;
	private Long playingTimeTotal;
	private Long playingTimeAverage;
	private Long reservationTimeAverage;
	
	public Long getNumberOfDays() {
		return this.numberOfDays;
	}
	
	public void setNumberOfDays( Long numberOfDays ) {
		this.numberOfDays = numberOfDays;
	}
	
	public Long getNumberOfMatchesTotal() {
		return this.numberOfMatchesTotal;
	}
	
	public void setNumberOfMatchesTotal( Long numberOfMatchesTotal ) {
		this.numberOfMatchesTotal = numberOfMatchesTotal;
	}
	
	public Double getNumberOfMatchesPerDay() {
		return this.numberOfMatchesPerDay;
	}
	
	public void setNumberOfMatchesPerDay( Double numberOfMatchesPerDay ) {
		this.numberOfMatchesPerDay = numberOfMatchesPerDay;
	}
	
	public Long getPlayingTimeTotal() {
		return this.playingTimeTotal;
	}
	
	public void setPlayingTimeTotal( Long playingTimeTotal ) {
		this.playingTimeTotal = playingTimeTotal;
	}
	
	public Long getPlayingTimeAverage() {
		return this.playingTimeAverage;
	}
	
	public void setPlayingTimeAverage( Long playingTimeAverage ) {
		this.playingTimeAverage = playingTimeAverage;
	}
	
	public Long getReservationTimeAverage() {
		return this.reservationTimeAverage;
	}
	
	public void setReservationTimeAverage( Long reservationTimeAverage ) {
		this.reservationTimeAverage = reservationTimeAverage;
	}
	
}
