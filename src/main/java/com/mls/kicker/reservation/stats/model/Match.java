package com.mls.kicker.reservation.stats.model;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Match {
	
	@Id
	@GeneratedValue ( strategy = GenerationType.AUTO )
	private Long id;
	
	private Date started;
	
	private Date finished;
	
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
	
	public Date getFinished() {
		return finished;
	}
	
	public void setFinished( Date finished ) {
		this.finished = finished;
	}
	
}
