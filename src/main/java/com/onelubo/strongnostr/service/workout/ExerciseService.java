package com.onelubo.strongnostr.service.workout;

import com.onelubo.strongnostr.exception.ExerciseNotFoundException;
import com.onelubo.strongnostr.model.workout.Exercise;
import com.onelubo.strongnostr.repository.ExerciseRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;

    public ExerciseService(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    public Exercise createExercise(String exerciseName, String exerciseDescription, String equipment) {
        return createExerciseInternal(exerciseName, exerciseDescription, equipment, false, null);
    }

    public Exercise updateExercise(String exerciseId, String exerciseName, String exerciseDescription,
                                   String equipment) {
        Optional<Exercise> existingExercise = exerciseRepository.findById(exerciseId);

        if (existingExercise.isPresent()) {
            existingExercise.get().setName(exerciseName);
            existingExercise.get().setDescription(exerciseDescription);
            existingExercise.get().setEquipment(equipment);
            exerciseRepository.save(existingExercise.get());
            return existingExercise.get();
        }
        throw new ExerciseNotFoundException(exerciseId);
    }

    public void deleteExercise(Exercise exercise) {
        Optional<Exercise> existingExercise = exerciseRepository.findById(exercise.getId());

        existingExercise.ifPresentOrElse(
                exerciseRepository::delete,
            () -> { throw new ExerciseNotFoundException(exercise.getId()); }
        );
    }

    public Exercise addExercise(Exercise exercise) {
        Optional<Exercise> existingExercise = exerciseRepository.findByNameAndDescriptionAndEquipment(
                exercise.getName(), exercise.getDescription(), exercise.getEquipment());

        return existingExercise.orElseGet(() -> exerciseRepository.save(exercise));
    }

    protected Exercise createExerciseInternal(String exerciseName, String exerciseDescription, String equipment,
                                              boolean isCustom, String createdByUserId) {
        Exercise exercise = new Exercise(exerciseName, exerciseDescription, equipment, isCustom, createdByUserId);
        return exerciseRepository.save(exercise);
    }
}
