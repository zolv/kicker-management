package com.mls.kicker.reservation.stats.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.mls.kicker.reservation.stats.model.Match;

@Repository
public interface MatchRepository extends CrudRepository<Match, Long> {
 
}