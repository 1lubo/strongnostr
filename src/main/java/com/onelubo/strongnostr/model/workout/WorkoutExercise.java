package com.onelubo.strongnostr.model.workout;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Represents an exercise within a workout session.
 * This is an embedded document that contains both exercise reference
 * and the actual sets performed for that exercise.
 */
public class WorkoutExercise {

    @NotNull(message = "Exercise ID is required")
    private String exerciseId;

    @NotBlank(message = "Exercise name cannot be blank")
    private String exerciseName;

    @NotBlank(message = "Exercise equipment is required")
    private String equipment;

    @NotEmpty(message = "At least one set is required for the exercise")
    @Valid
    private List<WorkoutSet> sets;

    private String notes;
    private Integer totalRestTimeSeconds = 0;

    private Double totalVolume; // Total volume for the exercise (weight * reps)
    private Double averageRpe; // Average RPE for the sets
    private Integer totalReps; // Total reps across all sets

    public WorkoutExercise(String exerciseId, String exerciseName, String equipment, List<WorkoutSet> sets) {
        this.exerciseId = exerciseId;
        this.exerciseName = exerciseName;
        this.equipment = equipment;
        this.sets = new ArrayList<>(sets);
        this.calculateMetrics();
    }

    public void calculateMetrics() {
        if (sets == null || sets.isEmpty()) {
            this.totalVolume = 0.0;
            this.averageRpe = null;
            this.totalReps = 0;
            return;
        }

        this.totalVolume = sets.stream()
                               .mapToDouble(sets -> sets.getWeight() * sets.getReps())
                               .sum();

        this.totalReps = sets.stream()
                             .mapToInt(WorkoutSet::getReps)
                             .sum();

        this.averageRpe = sets.stream()
                              .filter(set -> set.getRpe() != null)
                              .mapToDouble(WorkoutSet::getRpe)
                              .average()
                              .orElse(0.0);
    }

    public WorkoutSet getHighestRepSet() {
            return sets.stream()
                    .max(Comparator.comparingInt(WorkoutSet::getReps))
                    .orElse(null);
    }

    public WorkoutSet getHeaviestSet() {
            return sets.stream()
                    .max(Comparator.comparingDouble(WorkoutSet::getWeight))
                    .orElse(null);
    }

    public Integer getTotalReps() {
        return totalReps;
    }

    public Double getAverageRpe() {
        return averageRpe;
    }

    public Double getTotalVolume() {
        return totalVolume;
    }

    public Integer getTotalRestTimeSeconds() {
        return totalRestTimeSeconds;
    }

    public Integer getTotalSets() {
        return sets.size();
    }

    public String getExerciseId() {
        return exerciseId;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public void addWorkoutSet(WorkoutSet set) {
        if (set != null) {
            sets.add(set);
            this.calculateMetrics();
        }
    }

    @Override
    public String toString() {
        return "WorkoutExercise{" +
                "exerciseName=" +  exerciseName +
                ", equipment='" + equipment + '\'' +
                ", totalVolume='" + totalVolume + '\'' +
                ", totalReps=" + totalReps +
                '}';
    }
}
