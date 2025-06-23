package com.onelubo.strongnostr.dto;

import com.onelubo.strongnostr.model.workout.Exercise;
import com.onelubo.strongnostr.model.workout.WorkoutSet;
import jakarta.validation.constraints.NotNull;

public class ExerciseSetDto {
    @NotNull(message = "Exercise cannot be null")
    private Exercise exercise;
    @NotNull(message = "Workout set cannot be null")
    private WorkoutSet workoutSet;

    public ExerciseSetDto() {}

    public ExerciseSetDto(Exercise exercise, WorkoutSet workoutSet) {
        this.exercise = exercise;
        this.workoutSet = workoutSet;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public WorkoutSet getWorkoutSet() {
        return workoutSet;
    }
}
