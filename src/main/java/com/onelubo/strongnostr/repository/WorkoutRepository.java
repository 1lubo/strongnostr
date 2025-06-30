package com.onelubo.strongnostr.repository;

import com.onelubo.strongnostr.model.workout.Workout;
import org.springframework.data.mongodb.repository.MongoRepository;

import org.springframework.data.domain.Pageable;
import java.util.List;

public interface WorkoutRepository extends MongoRepository<Workout, String> {
    List<Workout> findByUserNPub(String npub, Pageable page);
}
