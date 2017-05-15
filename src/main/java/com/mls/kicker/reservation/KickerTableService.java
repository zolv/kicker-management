package com.mls.kicker.reservation;
//package kicker;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.atomic.AtomicLong;
//
//import javax.annotation.PostConstruct;
//import javax.annotation.PreDestroy;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import com.fasterxml.jackson.databind.JsonNode;
//
//import allbegray.slack.SlackClientFactory;
//import allbegray.slack.bot.SlackbotClient;
//import allbegray.slack.rtm.Event;
//import allbegray.slack.rtm.EventListener;
//import allbegray.slack.rtm.SlackRealTimeMessagingClient;
//import allbegray.slack.type.Channel;
//import allbegray.slack.type.Payload;
//import allbegray.slack.type.Profile;
//import allbegray.slack.type.User;
//import allbegray.slack.webapi.SlackWebApiClient;
//import allbegray.slack.webhook.SlackWebhookClient;
//import kicker.Referee.TableStatus;
//
//@RestController
//public class KickerTableService {
//	
//	@Autowired
//	private Referee referee;
//
//	@Autowired
//	private Slack slack;
//	
//	@PostConstruct
//	public void initialize() {
//		
//	}
//	
//	@PreDestroy
//	public void destroy() {
//		
//	}
//	
//	@RequestMapping ( "/reserve" )
//	public void reserve( User user ) {
//		if ( this.referee.reserve() ) {
//			slack.postMessage( "Kicker status: " + TableStatus.RESERVED + " by " + getUserName( user ));
//		} else {
//			slack.postMessage( "Reservation <span style=\"color:red\">rejected</span>. Kicker is reserved by " + getUserName( user ));
//		}
//	}
//	
//	private String getUserName( User user ) {
//		final String userName;
//		if ( user != null ) {
//			final Profile profile = user.getProfile();
//			userName = ( profile != null ? profile.getReal_name() : "" ) + "(" + user.getName() + ")";
//		} else {
//			userName = "somebody";
//		}
//		return userName;
//	}
//	
//	@RequestMapping ( "/cancel" )
//	public Greeting cancel( User user ) {
//		if ( this.referee.cancel() ) {
//			postMessage( "Kicker table reservation was canceled by " + getUserName( user ) + "." );
//		} else {
//			postMessage( "Kicker table is not reserved." );
//		}
//		return new Greeting( this.counter.incrementAndGet(), "Kicker table reservation was canceled by " + getUserName( user ) + "." );
//	}
//	
//	@RequestMapping ( "/play" )
//	public Greeting play( User user ) {
//		switch ( this.referee.play() ) {
//			default:
//			case GAME_STARTED:
//				postMessage( "Kicker match has just started..." );
//				break;
//			case GAME_EXTENDED:
//				postMessage( "Kicker match time was just extended..." );
//				break;
//			case GAME_STARTED_RESERVATION:
//				postMessage( "Kicker match has just started..." );
//				break;
//		}
//		return new Greeting( this.counter.incrementAndGet(), "Kicker table is reserved for " + getUserName( user ) + "." );
//	}
//	
//	@RequestMapping ( "/release" )
//	public Greeting release( User user ) {
//		if ( this.referee.release() ) {
//			postMessage( "Kicker table is now free." );
//		} else {
//			postMessage( "Kicker table is now free." );
//		}
//		return new Greeting( this.counter.incrementAndGet(), "Kicker table is reserved for " + getUserName( user ) + "." );
//	}
//	
//	@RequestMapping ( "/status" )
//	public Greeting status( User user ) {
//		if ( this.referee.status() ) {
//			postMessage( "Kicker table is now free." );
//		} else {
//			postMessage( "Kicker table is now free." );
//		}
//		return new Greeting( this.counter.incrementAndGet(), "Kicker table is reserved for " + getUserName( user ) + "." );
//	}
//	
//
//	@RequestMapping ( "/disconnect" )
//	public Greeting disconnect() {
//		if ( this.slackbotClient != null ) {
//			this.slackbotClient.shutdown();
//		}
//		if ( this.rtmClient != null ) {
//			this.rtmClient.close();
//		}
//		return new Greeting( this.counter.incrementAndGet(), "Disconnected from Slack." );
//	}
//}
