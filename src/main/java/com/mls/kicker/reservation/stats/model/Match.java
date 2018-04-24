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
	private Date finished;
}
