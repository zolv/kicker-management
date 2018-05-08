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
import com.mls.kicker.reservation.stats.model.Reservation;
import com.mls.kicker.reservation.stats.repository.MatchRepository;
import com.mls.kicker.reservation.stats.repository.ReservationRepository;
import com.mls.kicker.reservation.util.TimeFormatUtil;

@Component
public class Stats {
	
	private static Logger log = LoggerFactory.getLogger( Stats.class );
	
	public static final long MIN_PLAYING_TIME = 60000;
	
	@Autowired
	private Referee referee;
	
	@Autowired
	private MatchRepository matchRepository;
	
	@Autowired
	private ReservationRepository reservationRepository;
	
	private StateChangeHandler stateChangeHandler;
	
	public Stats() {
	}
	
	@PostConstruct
	public void initialize() {
		
		this.stateChangeHandler = new StateChangeHandler() {
			
			@Override
			public void stateChanged( StateChangedEvent event ) {
				updateStatus( event );
			}
		};
		this.referee.addStateChangedHandler( this.stateChangeHandler );
		
	}
	
	@PreDestroy
	public synchronized void deinitialize() {
		this.referee.removeStateChangedHandler( this.stateChangeHandler );
	}
	
	public void updateStatus( StateChangedEvent stateChangedEvent ) {
		try {
			final Date now = new Date();
			switch ( stateChangedEvent.getCurrentStatus() ) {
				case FREE:
					switch ( stateChangedEvent.getPreviousStatus() ) {
						case FREE:
							break;
						case OCCUPIED:
							final Match match = new Match();
							match.setStarted( new Date( now.getTime() - stateChangedEvent.getTimePassed() ) );
							match.setTime( stateChangedEvent.getTimePassed() );
							this.matchRepository.save( match );
							break;
						case RESERVED:
							final Reservation reservationCanceled = new Reservation();
							reservationCanceled.setStarted( new Date( now.getTime() - stateChangedEvent.getTimePassed() ) );
							reservationCanceled.setTime( stateChangedEvent.getTimePassed() );
							reservationCanceled.setTaken( false );
							this.reservationRepository.save( reservationCanceled );
							break;
						default:
							break;
					}
					break;
				case OCCUPIED:
					switch ( stateChangedEvent.getPreviousStatus() ) {
						case FREE:
							break;
						case OCCUPIED:
							break;
						case RESERVED:
							final Reservation reservationCanceled = new Reservation();
							reservationCanceled.setStarted( new Date( now.getTime() - stateChangedEvent.getTimePassed() ) );
							reservationCanceled.setTime( stateChangedEvent.getTimePassed() );
							reservationCanceled.setTaken( true );
							this.reservationRepository.save( reservationCanceled );
							break;
						default:
							break;
					}
					break;
				case RESERVED:
					break;
				default:
					break;
			}
		} catch ( Exception e ) {
			log.error( "Repository exception. ", e );
		}
		
	}
	
	public Statistics getStatistics() {
		final Statistics stats = new Statistics();
		stats.setNumberOfMatchesTotal( this.matchRepository.count() );
		stats.setPlayingTimeTotal( this.matchRepository.getPlayingTimeTotal() );
		stats.setNumberOfDays( this.matchRepository.getNumberOfDays() );
		stats.setMaxNumberOfMatchesInSingleDay( this.matchRepository.maxNumberOfMatchesInOneDay() );
		
		stats.setNumberOfMatchesPerDay( stats.getNumberOfDays().longValue() > 0 ? Double.valueOf( stats.getNumberOfMatchesTotal().doubleValue() / stats.getNumberOfDays().doubleValue() ) : null );
		stats.setPlayingTimeAverage( stats.getNumberOfMatchesTotal().longValue() > 0 ? Long.valueOf( stats.getPlayingTimeTotal().longValue() / stats.getNumberOfMatchesTotal().longValue() ) : null );
		return stats;
	}
}
