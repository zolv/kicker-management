package com.mls.kicker.reservation.util;

import org.junit.Assert;
import org.junit.Test;

import com.mls.kicker.reservation.engine.Referee;


public class TimeFormatUtilTest {
	
	@Test
	public void testCreateDayTimeString() {
		Assert.assertEquals( "00sec", TimeFormatUtil.createDayTimeString( 0 ));
		Assert.assertEquals( "56sec", TimeFormatUtil.createDayTimeString( Referee.ONE_SECOND * 56));
		Assert.assertEquals( "45min 56sec", TimeFormatUtil.createDayTimeString( Referee.ONE_MINUTE * 45 + Referee.ONE_SECOND * 56));
		Assert.assertEquals( "45min 00sec", TimeFormatUtil.createDayTimeString( Referee.ONE_MINUTE * 45 ));
		Assert.assertEquals( "03h 45min 56sec", TimeFormatUtil.createDayTimeString( Referee.ONE_HOUR * 3 + Referee.ONE_MINUTE * 45 + Referee.ONE_SECOND * 56));
		Assert.assertEquals( "03h 00min 56sec", TimeFormatUtil.createDayTimeString( Referee.ONE_HOUR * 3 + Referee.ONE_SECOND * 56));
		Assert.assertEquals( "03h 00min 00sec", TimeFormatUtil.createDayTimeString( Referee.ONE_HOUR * 3 ));
		Assert.assertEquals( "1d 01h 00min 00sec", TimeFormatUtil.createDayTimeString( Referee.ONE_HOUR * 25 ));
		Assert.assertEquals( "2d 03h 45min 56sec", TimeFormatUtil.createDayTimeString( Referee.ONE_DAY * 2 + Referee.ONE_HOUR * 3 + Referee.ONE_MINUTE * 45 + Referee.ONE_SECOND * 56));
		Assert.assertEquals( "123d 03h 45min 56sec", TimeFormatUtil.createDayTimeString( Referee.ONE_DAY * 123 + Referee.ONE_HOUR * 3 + Referee.ONE_MINUTE * 45 + Referee.ONE_SECOND * 56));
		Assert.assertEquals( "100d 00h 00min 00sec", TimeFormatUtil.createDayTimeString( Referee.ONE_DAY * 100));
	}
	
}
