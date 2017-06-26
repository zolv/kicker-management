package com.mls.kicker.reservation.button;

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
import com.mls.kicker.reservation.engine.Referee.TableStatus;
import com.mls.kicker.reservation.slack.Slack;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;

@Component
public class PiButtons {
	
	private static Logger log = LoggerFactory.getLogger( Referee.class );
	
	private static final long BUTTON_HIT_FREEZE_PERIOD = 500;
	
	private static final long RESERVATION_FREEZE_PERIOD = 5000;

	@Autowired
	private Referee referee;
	
	@Autowired
	private Slack slack;
	
	private GpioController gpio;
	
	private GpioPinDigitalInput redButtonPin;
	
	private GpioPinDigitalInput greenButtonPin;
	
	private Date lastButtonHitTime = new Date();

	private Date lastReservationTime = new Date(0);

	private StateChangeHandler stateChangeHandler;
	
	public PiButtons() {
	}
	
	@PostConstruct
	public void initialize() {
		initializeGpio();
		
	}
	
	private void initializeGpio() {
		this.gpio = GpioFactory.getInstance();
		
		initializeReservationHandler();
		initializeRedButton();
		initializeGreenButton();
	}
	
	private void initializeReservationHandler() {
		this.stateChangeHandler = new StateChangeHandler() {

			@Override
			public void stateChanged(StateChangedEvent event) {
				final TableStatus currentStatus = event.getCurrentStatus();
				switch(currentStatus) {
					case FREE:
					case OCCUPIED:
					default:
						PiButtons.this.lastReservationTime = new Date(0);
						break;
					case RESERVED:
						/*
						 * Update only on changing state.
						 */
						if( TableStatus.FREE.equals(event.getPreviousStatus())) {
							PiButtons.this.lastReservationTime = new Date();
							System.out.println( "Last reservation time updated to " + PiButtons.this.lastReservationTime );
						}
						break;
				}
			}
		};
		this.referee.addStateChangedHandler(this.stateChangeHandler);		
	}

	private void initializeRedButton() {
		this.redButtonPin = this.gpio.provisionDigitalInputPin( RaspiPin.GPIO_05, PinPullResistance.PULL_DOWN );
		
		// set shutdown state for this input pin
		this.redButtonPin.setShutdownOptions( true );
		
		// create and register gpio pin listener
		this.redButtonPin.addListener( new GpioPinListenerDigital() {
			
			@Override
			public void handleGpioPinDigitalStateChangeEvent( GpioPinDigitalStateChangeEvent event ) {
				// display pin state on console
				log.debug( " --> GPIO PIN STATE CHANGE: " + event.getPin() + " = " + event.getState() );
				if ( event.getState().isHigh() ) {
					final Date now = new Date();
					final long nowMilis = now.getTime();
					final boolean buttonHitCondition = Math.abs( nowMilis - PiButtons.this.lastButtonHitTime.getTime() ) > BUTTON_HIT_FREEZE_PERIOD;
					final boolean reservationCondition = Math.abs( nowMilis - PiButtons.this.lastReservationTime.getTime() ) > RESERVATION_FREEZE_PERIOD;
					if ( buttonHitCondition && reservationCondition ) {
						PiButtons.this.lastButtonHitTime = now;
						PiButtons.this.slack.play( "-1" );
					}
				}
			}
			
		} );
	}
	
	private void initializeGreenButton() {
		this.greenButtonPin = this.gpio.provisionDigitalInputPin( RaspiPin.GPIO_04, PinPullResistance.PULL_DOWN );
		
		// set shutdown state for this input pin
		this.greenButtonPin.setShutdownOptions( true );
		
		// create and register gpio pin listener
		this.greenButtonPin.addListener( new GpioPinListenerDigital() {
			
			@Override
			public void handleGpioPinDigitalStateChangeEvent( GpioPinDigitalStateChangeEvent event ) {
				// display pin state on console
				log.debug( " --> GPIO PIN STATE CHANGE: " + event.getPin() + " = " + event.getState() );
				if ( event.getState().isHigh() ) {
					final Date now = new Date();
					final long nowMilis = now.getTime();
					final boolean buttonHitCondition = Math.abs( nowMilis - PiButtons.this.lastButtonHitTime.getTime() ) > BUTTON_HIT_FREEZE_PERIOD;
					if ( buttonHitCondition ) {
						final boolean reservationCondition = Math.abs( nowMilis - PiButtons.this.lastReservationTime.getTime() ) > RESERVATION_FREEZE_PERIOD;
						if ( reservationCondition ) {
							PiButtons.this.lastButtonHitTime = now;
							PiButtons.this.slack.release( "-1" );
						} else {
							log.trace( "Buttos are freezed due to reservation." );
						}
					} else {
						log.trace( "Buttos are freezed due to last button hit." );
					}
				}
			}
			
		} );
	}
	
	@PreDestroy
	public void deinitialize() {
		this.gpio.shutdown();
	}
	
}
