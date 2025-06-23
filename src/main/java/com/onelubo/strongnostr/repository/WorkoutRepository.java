package com.onelubo.strongnostr.repository;

import com.onelubo.strongnostr.model.workout.Workout;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WorkoutRepository extends MongoRepository<Workout, String> {
}
