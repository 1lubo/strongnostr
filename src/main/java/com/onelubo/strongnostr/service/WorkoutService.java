package com.onelubo.strongnostr.service;

import com.onelubo.strongnostr.exception.WorkoutNotFoundException;
import com.onelubo.strongnostr.model.workout.Exercise;
import com.onelubo.strongnostr.model.workout.Workout;
import com.onelubo.strongnostr.model.workout.WorkoutExercise;
import com.onelubo.strongnostr.model.workout.WorkoutSet;
import com.onelubo.strongnostr.repository.WorkoutRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class WorkoutService {

    private final ExerciseService exerciseService;
    private final WorkoutRepository workoutRepository;

    public WorkoutService(ExerciseService exerciseService, WorkoutRepository workoutRepository) {
        this.exerciseService = exerciseService;
        this.workoutRepository = workoutRepository;
    }

    public Workout createWorkout(Exercise exercise, WorkoutSet set) {
        Objects.requireNonNull(exercise);
        Objects.requireNonNull(set);

        Workout workout = new Workout();
        Exercise existingExercise = exerciseService.addExercise(exercise);
        WorkoutExercise workoutExercise = new WorkoutExercise(existingExercise.getId(),
                                                              existingExercise.getName(),
                                                              existingExercise.getEquipment(), List.of(set));
        workout.addExercise(workoutExercise);
        return workoutRepository.save(workout);
    }

    public Workout addExerciseToWorkout(Workout workout, Exercise exercise, WorkoutSet set) {
        Objects.requireNonNull(exercise);
        Objects.requireNonNull(workout);
        Objects.requireNonNull(set);

        Exercise existingExercise = exerciseService.addExercise(exercise);
        WorkoutExercise workoutExercise = new WorkoutExercise(existingExercise.getId(),
                                                              existingExercise.getName(),
                                                              existingExercise.getEquipment(), List.of(set));
        workout.updateWorkout(workoutExercise);

        return workoutRepository.save(workout);
    }

    public Workout removeExerciseFromWorkout(Workout workout, Exercise exercise) {
        workout.getExercises()
               .stream()
               .filter(wex -> wex.getExerciseId().equals(exercise.getId()))
               .findFirst()
               .ifPresentOrElse(workout::removeExercise, () -> {
                   throw new IllegalArgumentException(
                           String.format("Exercise '%s' with id '%s' not found in workout",
                                         exercise.getName(), exercise.getId()));
               });
        return workoutRepository.save(workout);
    }

    public Workout getWorkoutById(String workoutID) {
        return workoutRepository.findById(workoutID)
                .orElseThrow(() -> new WorkoutNotFoundException(workoutID));
    }
}
