package com.onelubo.strongnostr.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "workouts")
public class Workout {

    @Id
    private String id;

    @NotNull(message = "User ID is required")
    @Indexed(unique = true)
    private String userId;

    @NotNull(message = "Workout date is required")
    @Indexed
    private OffsetDateTime workoutDate;

    @NotEmpty(message = "At least one exercise is required")
    private List<WorkoutExercise> exercises = new ArrayList<>();

    private Integer durationSeconds;
    private String notes;

    private Boolean isPublic = false;

    private Double totalVolume;
    private Integer totalSets;
    private Integer totalReps;
    private Double averageRpe;

    @CreatedDate
    private OffsetDateTime createdAt;

    @LastModifiedDate
    private OffsetDateTime updatedAt;

    public Workout() {}

    public void calculateMetrics() {
        if (exercises == null || exercises.isEmpty()) {
            this.totalVolume = 0.0;
            this.totalSets = 0;
            this.totalReps = 0;
            this.averageRpe = null;
            return;
        }

        this.totalVolume = exercises.stream()
                .mapToDouble(WorkoutExercise::getTotalVolume)
                .sum();

        this.totalSets = exercises.stream()
                .mapToInt(WorkoutExercise::getTotalSets)
                .sum();

        this.totalReps = exercises.stream()
                .mapToInt(WorkoutExercise::getTotalReps)
                .sum();

        this.averageRpe = exercises.stream()
                .filter(exercise -> exercise.getAverageRpe() != null)
                .mapToDouble(WorkoutExercise::getAverageRpe)
                .average()
                .orElse(0.0);

        this.durationSeconds = exercises.stream()
                .mapToInt(WorkoutExercise::getTotalRestTimeSeconds)
                .sum();
    }

    public List<WorkoutExercise> getExercises() {
        return exercises;
    }

    public void updateWorkout(WorkoutExercise newExercise) {
        exercises.stream()
                .filter(exercise -> exercise.getExerciseId().equals(newExercise.getExerciseId()))
                .findFirst()
                .ifPresentOrElse(
                        exercise -> exercise.addWorkoutSet(newExercise.getHeaviestSet()),
                        () -> exercises.add(newExercise)
                                );
        this.calculateMetrics();
    }

    public void addExercise(WorkoutExercise exercise) {
        this.exercises.add(exercise);
        this.calculateMetrics();
    }

    public void removeExercise(WorkoutExercise exercise) {
        this.exercises.removeIf(existingExercise ->
                                        existingExercise.getExerciseId().equals(exercise.getExerciseId())
                               );
        this.calculateMetrics();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Workout{" +
                "workoutDate=" + workoutDate +
                ", exercises=" + exercises +
                ", totalVolume=" + totalVolume +
                '}';
    }
}
