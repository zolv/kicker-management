package com.mls.kicker.reservation.util;

import com.mls.kicker.reservation.engine.Referee;

public class TimeFormatUtil {
	
	public static String createTimeString( Long timeLeft ) {
		return "Time left: " + formatTime(timeLeft);
	}

	public static String createSimpleTimeString( long timeLeft ) {
		return formatTime(timeLeft) + "s";
	}

	public static String createDayTimeString( long timeLeftMilis ) {
		long days = timeLeftMilis / Referee.ONE_DAY;
		long hours = timeLeftMilis % Referee.ONE_DAY  / Referee.ONE_HOUR;
		long minutes = ( timeLeftMilis % Referee.ONE_HOUR ) / Referee.ONE_MINUTE;
		long seconds = ( timeLeftMilis % Referee.ONE_MINUTE ) / Referee.ONE_SECOND;
		final String daysString = days == 0 ? "" : Long.valueOf( days ) + "d ";
		final String hourString = ( days == 0 && hours == 0 ? "" : get2digits(  hours ) + "h ");
		final String minutesString = (days == 0 && hours == 0 && minutes == 0) ? "" : (get2digits( minutes ) + "min ");
		final String secondsString = get2digits(seconds);
		final String result =  daysString + hourString + minutesString + secondsString + "sec";
		return result;
	}

	private static String get2digits( long minutes ) {
		return ( minutes < 10) ? "0" + Long.valueOf( minutes ).toString() : Long.valueOf( minutes ).toString();
	}
	
	public static String createHourTimeString( long timeLeftMilis ) {
		long hours = timeLeftMilis / Referee.ONE_HOUR;
		long minutes = ( timeLeftMilis % Referee.ONE_HOUR ) / Referee.ONE_MINUTE;
		long seconds = ( timeLeftMilis % Referee.ONE_MINUTE ) / Referee.ONE_SECOND;
		final String hourString = ( hours == 0 ? "" : ( Long.valueOf( hours ) ) + "h ");
		final String minutesString = (hours == 0 && minutes == 0) ? "" : (( (hours > 0 && minutes < 10) ? "0" + minutes : Long.valueOf( minutes ) ) + "min ");
		final String result =  hourString + minutesString + ( seconds < 10 ? "0" + seconds : Long.valueOf( seconds ) ) + "sec";
		return result;
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
