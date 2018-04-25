package com.mls.kicker.reservation.stats;

import java.util.Date;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mls.kicker.reservation.engine.Referee;
import com.mls.kicker.reservation.engine.StateChangeHandler;
import com.mls.kicker.reservation.engine.StateChangedEvent;
import com.mls.kicker.reservation.stats.model.Match;
import com.mls.kicker.reservation.stats.repository.MatchRepository;

@Component
public class Stats {

	private static Logger log = LoggerFactory.getLogger(Stats.class);

	@Autowired
	private Referee referee;
	
	@Autowired
  private MatchRepository repository;
  
	private StateChangeHandler stateChangeHandler;
	
	public Stats() {
	}

	@PostConstruct
	public void initialize() {

		this.stateChangeHandler = new StateChangeHandler() {

			@Override
			public void stateChanged(StateChangedEvent event) {
				updateStatus(event);
			}
		};
		this.referee.addStateChangedHandler(this.stateChangeHandler);

		updateStatus(this.referee.status());
	}

	@PreDestroy
	public synchronized void deinitialize() {
		this.referee.removeStateChangedHandler(this.stateChangeHandler);
	}

	public void updateStatus() {
		this.updateStatus(this.referee.status());
	}

	public void updateStatus(StateChangedEvent stateChangedEvent) {
		switch(stateChangedEvent.getCurrentStatus()) {
			case FREE:
				switch(stateChangedEvent.getPreviousStatus()) {
					case FREE:
						break;
					case OCCUPIED:
						final Match match = new Match();
						final Date now = new Date();
						match.setStarted( new Date(now.getTime() - stateChangedEvent.getTimePassed() ));
						match.setFinished( new Date(now.getTime() - stateChangedEvent.getTimePassed() ));
						repository.save( match );
						break;
					case RESERVED:
						break;
					default:
						break;
					
				}
				break;
			case OCCUPIED:
				break;
			case RESERVED:
				break;
			default:
				break;
			
		}
		
	}

}
