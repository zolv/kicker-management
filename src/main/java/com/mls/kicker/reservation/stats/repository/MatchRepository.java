package com.mls.kicker.reservation.stats.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.mls.kicker.reservation.engine.Referee;
import com.mls.kicker.reservation.stats.Stats;
import com.mls.kicker.reservation.stats.model.Match;

@Repository
public interface MatchRepository extends CrudRepository< Match, Long > {
	
	@Query( nativeQuery = true, value = "SELECT IFNULL(sum(m.time), 0) FROM Match m where m.time >= " + Stats.MIN_PLAYING_TIME + " AND m.time < " + Referee.MAX_PLAYING_TIME )
	long getPlayingTimeTotal();
	
	@Query( nativeQuery = true, value = "select count(*) from (select FORMATDATETIME(m.started, 'yyyy.MM.dd') from Match m where m.time >= " + Stats.MIN_PLAYING_TIME + " AND m.time < " + Referee.MAX_PLAYING_TIME + " GROUP BY FORMATDATETIME(m.started, 'yyyy.MM.dd'))" )
	long getNumberOfDays();

	@Query( nativeQuery = true, value = "select count(*) from Match m where m.time >= " + Stats.MIN_PLAYING_TIME + " AND m.time < " + + Referee.MAX_PLAYING_TIME )
	long count();
	
}