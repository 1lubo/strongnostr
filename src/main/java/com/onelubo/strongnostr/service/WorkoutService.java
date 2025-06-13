package com.onelubo.strongnostr.service;

import com.onelubo.strongnostr.model.Exercise;
import com.onelubo.strongnostr.model.Workout;

public class WorkoutService {

    public Workout createWorkout(Exercise exercise) {
        // Logic to create a workout with the given exercise
        Workout workout = new Workout();
        workout.addExercise(exercise);
        return workout;
    }

    public Workout addExerciseToWorkout(Workout workout, Exercise exercise) {
        // Logic to add an exercise to an existing workout
        workout.addExercise(exercise);
        return workout;
    }

    public Workout removeExerciseFromWorkout(Workout workout, Exercise exercise) {
        // Logic to remove an exercise from a workout by name
        workout.getExercises()
               .stream()
               .filter(ex -> ex.equals(exercise))
               .findFirst()
               .ifPresent(ex -> workout.getExercises().remove(ex));
        return workout;
    }
}
