package com.mls.kicker.reservation.led;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mls.kicker.reservation.engine.Referee;
import com.mls.kicker.reservation.engine.Referee.TableStatus;
import com.mls.kicker.reservation.engine.StateChangeHandler;
import com.mls.kicker.reservation.engine.StateChangedEvent;
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

@Component
public class PiLeds {

	private static final PinState SWITCH_OFF = PinState.LOW;

	private static final PinState SWITCH_ON = PinState.HIGH;

	private static Logger log = LoggerFactory.getLogger(PiLeds.class);

	@Autowired
	private Referee referee;

	private StateChangeHandler stateChangeHandler;

	private GpioController gpio;

	private GpioPinDigitalOutput greenLedPin;

	private GpioPinDigitalOutput yellowLedPin;

	private GpioPinDigitalOutput redLedPin;

	public PiLeds() {
	}

	@PostConstruct
	public void initialize() {
		initializeGpio();

		this.stateChangeHandler = new StateChangeHandler() {

			@Override
			public void stateChanged(StateChangedEvent event) {
				TableStatus currentStatus = event.getCurrentStatus();
				updateStatus(currentStatus);
			}
		};
		this.referee.addStateChangedHandler(this.stateChangeHandler);

		updateStatus(this.referee.status());
	}

	private void initializeGpio() {
		this.gpio = GpioFactory.getInstance();

		this.greenLedPin = this.gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "GreenLED", SWITCH_OFF);
		this.greenLedPin.setShutdownOptions(true, SWITCH_OFF);
		this.yellowLedPin = this.gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "YellowLED", SWITCH_OFF);
		this.yellowLedPin.setShutdownOptions(true, SWITCH_OFF);
		this.redLedPin = this.gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "RedLED", SWITCH_OFF);
		this.redLedPin.setShutdownOptions(true, SWITCH_OFF);
	}

	@PreDestroy
	public synchronized void deinitialize() {
		this.referee.removeStateChangedHandler(this.stateChangeHandler);
		this.gpio.shutdown();
	}

	public void updateStatus() {
		this.updateStatus(this.referee.status());
	}

	public void updateStatus(TableStatus currentStatus) {
		if(isGpioAlive()) {
			switch(currentStatus){
				case FREE:
					lightGreen();
					break;
				case OCCUPIED:
					lightRed();
					break;
				case RESERVED:
					lightYellow();
					break;
				default:
					break;
			}
		} else {
			log.info("GPIO is dead.");
		}
	}

	public synchronized void lightGreen() {
		PiLeds.this.yellowLedPin.high();
		PiLeds.this.redLedPin.high();
		PiLeds.this.greenLedPin.low();
	}

	public synchronized void lightRed() {
		PiLeds.this.yellowLedPin.high();
		PiLeds.this.greenLedPin.high();
		PiLeds.this.redLedPin.low();
	}

	public synchronized void lightYellow() {
		PiLeds.this.redLedPin.high();
		PiLeds.this.greenLedPin.high();
		PiLeds.this.yellowLedPin.low();
	}

	public void lightsDown() {
		light(false, false, false);
	}

	public synchronized void light(boolean g, boolean y, boolean r) {
		if(!g) {
			this.greenLedPin.high();
		}
		if(!y) {
			this.yellowLedPin.high();
		}
		if(!r) {
			this.redLedPin.high();
		}

		if(g) {
			this.greenLedPin.low();
		}
		if(y) {
			this.yellowLedPin.low();
		}
		if(r) {
			this.redLedPin.low();
		}
	}

	private void light(GpioPinDigitalOutput pin, boolean switchOn) {
		if(switchOn) {
			pin.low();
		} else {
			pin.high();
		}
	}

	private synchronized boolean isGpioAlive() {
		if((this.gpio == null) || this.gpio.isShutdown()) {
			initializeGpio();
		}
		return !this.gpio.isShutdown();
	}

}
