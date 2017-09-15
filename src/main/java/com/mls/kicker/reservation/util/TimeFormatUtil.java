package com.mls.kicker.reservation.util;

import com.mls.kicker.reservation.engine.Referee;

public class TimeFormatUtil {
	
	public static String createTimeString( Long timeLeft ) {
		return "Time left: " + formatTime(timeLeft);
	}

	public static String createSimpleTimeString( long timeLeft ) {
		return formatTime(timeLeft) + "s";
	}
	
	public static String formatTime(Long timeLeft) {
		final String result;
		if ( timeLeft != null ) {
			result = formatTime(timeLeft.longValue());
		} else {
			result = "??:??";
		}
		return result;
	}

	public static String formatTime(long timeLeftMilis) {
		final String result;
		long minutes = timeLeftMilis / Referee.ONE_MINUTE;
		long seconds = ( timeLeftMilis % Referee.ONE_MINUTE ) / Referee.ONE_SECOND;
		result = ( minutes < 10 ? "0" + minutes : Long.valueOf( minutes ) ) + ":" + ( seconds < 10 ? "0" + seconds : Long.valueOf( seconds ) );
		return result;
	}
}
