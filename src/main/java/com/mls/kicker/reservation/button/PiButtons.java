package com.mls.kicker.reservation.button;

import java.util.Date;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mls.kicker.reservation.engine.Referee;
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
	
	private static final int FREEZE_PERIOD = 500;

	@Autowired
	private Referee referee;
	
	@Autowired
	private Slack slack;
	
	private GpioController gpio;
	
	private GpioPinDigitalInput redButtonPin;
	
	private GpioPinDigitalInput greenButtonPin;
	
	private Date lastButtonHitTime = new Date();
	
	public PiButtons() {
	}
	
	@PostConstruct
	public void initialize() {
		initializeGpio();
		
	}
	
	private void initializeGpio() {
		this.gpio = GpioFactory.getInstance();
		
		initializeRedButton();
		initializeGreenButton();
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
				System.out.println( " --> GPIO PIN STATE CHANGE: " + event.getPin() + " = " + event.getState() );
				if ( event.getState().isHigh() ) {
					final Date now = new Date();
					if ( Math.abs( now.getTime() - lastButtonHitTime.getTime() ) > FREEZE_PERIOD ) {
						lastButtonHitTime = now;
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
				System.out.println( " --> GPIO PIN STATE CHANGE: " + event.getPin() + " = " + event.getState() );
				if ( event.getState().isHigh() ) {
					final Date now = new Date();
					if ( Math.abs( now.getTime() - lastButtonHitTime.getTime() ) > FREEZE_PERIOD ) {
						lastButtonHitTime = now;
						PiButtons.this.slack.release( "-1" );
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
