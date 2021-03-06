package com.mls.kicker.reservation.stats.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

	@Entity
	public class Match {
		
		@Id
		@GeneratedValue( strategy = GenerationType.AUTO )
		private Long id;
		
		private Date started;
		
		private Long time;
	
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
