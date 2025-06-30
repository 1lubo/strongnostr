package com.onelubo.strongnostr.dto;

import com.onelubo.strongnostr.model.workout.Exercise;
import com.onelubo.strongnostr.model.workout.WorkoutSet;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to create a new workout")
public class ExerciseSetDto {

    @Schema(description = "Initial exercise for the workout")
    @NotNull(message = "Exercise cannot be null")
    @Valid
    private Exercise exercise;

    @Schema(description = "Initial set for the exercise")
    @NotNull(message = "Workout set cannot be null")
    @Valid
    private WorkoutSet workoutSet;

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
