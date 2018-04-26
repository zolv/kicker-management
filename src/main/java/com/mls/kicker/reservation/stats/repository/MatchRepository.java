package com.mls.kicker.reservation.stats.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.mls.kicker.reservation.stats.model.Match;

@Repository
public interface MatchRepository extends CrudRepository< Match, Long > {
	
	@Query( nativeQuery = true, value = "SELECT IFNULL(sum(m.time), 0) FROM Match m where m.time >= 180000 AND m.time <= 1200000" )
	long getPlayingTimeTotal();
	
	@Query( nativeQuery = true, value = "select count(*) from (select FORMATDATETIME(m.started, 'yyyy.MM.dd') from Match m where m.time >= 180000 AND m.time <= 1200000 GROUP BY FORMATDATETIME(m.started, 'yyyy.MM.dd'))" )
	long getNumberOfDays();
}