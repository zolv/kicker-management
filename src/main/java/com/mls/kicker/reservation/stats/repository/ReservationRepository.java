package com.mls.kicker.reservation.stats.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.mls.kicker.reservation.stats.model.Reservation;

@Repository
public interface ReservationRepository extends CrudRepository< Reservation, Long > {
	
}