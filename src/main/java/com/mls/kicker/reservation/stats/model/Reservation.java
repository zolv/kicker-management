package com.mls.kicker.reservation.stats.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Reservation {
	
	@Id
	@GeneratedValue( strategy = GenerationType.AUTO )
	private Long id;
	
	private Date started;
	
	public void setTaken( boolean taken ) {
		this.taken = taken;
	}
	
	private Long time;
	
	private boolean taken;
	
	public boolean isTaken() {
		return this.taken;
	}
	
	public Long getId() {
		return id;
	}
	
	public void setId( Long id ) {
		this.id = id;
	}
	
	public Date getStarted() {
		return started;
	}
	
	public void setStarted( Date started ) {
		this.started = started;
	}
	
	public Long getTime() {
		return this.time;
	}
	
	public void setTime( Long time ) {
		this.time = time;
	}
	
}
