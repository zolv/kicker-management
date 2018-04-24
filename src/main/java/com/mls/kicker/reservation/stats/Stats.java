package com.mls.kicker.reservation.stats;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mls.kicker.reservation.engine.Referee;
import com.mls.kicker.reservation.engine.StateChangeHandler;
import com.mls.kicker.reservation.engine.StateChangedEvent;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

@Component
public class Stats {

	private static Logger log = LoggerFactory.getLogger(Stats.class);

	@Autowired
	private Referee referee;

	
	
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
		
		
	}

}
