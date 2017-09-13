package com.mls.kicker.reservation.util;

import com.mls.kicker.reservation.engine.Referee;

public class TimeFormatUtil {
	
	public static String formatTime( Long timeLeft ) {
		final String result;
		if ( timeLeft != null ) {
			long timeLeftMilis = timeLeft.longValue();
			long minutes = timeLeftMilis / Referee.ONE_MINUTE;
			long seconds = ( timeLeft.longValue() % Referee.ONE_MINUTE ) / Referee.ONE_SECOND;
			result = ( minutes < 10 ? "0" + minutes : Long.valueOf( minutes ) ) + ":" + ( seconds < 10 ? "0" + seconds : Long.valueOf( seconds ) );
		} else {
			result = "??:??";
		}
		return "Time left: " + result;
	}
}
