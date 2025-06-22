package com.onelubo.strongnostr.repository;

import com.onelubo.strongnostr.model.Exercise;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExerciseRepository extends MongoRepository<Exercise, String> {

    Optional<Exercise> findByNameAndDescriptionAndEquipment(String name, String description, String equipment);
}
