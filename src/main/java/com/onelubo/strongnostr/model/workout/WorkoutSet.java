package com.onelubo.strongnostr.model.workout;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public class WorkoutSet {
    @NotNull(message = "Weight is required")
    @PositiveOrZero(message = "Weight must be zero or positive")
    private Double weight;

    @NotNull(message = "Reps is required")
    @Positive(message = "Reps must be zero or positive")
    private Integer reps;

    @PositiveOrZero(message = "Rest time must be zero or positive")
    private Integer restTimeSeconds;

    @PositiveOrZero(message = "RPE must be zero or positive")
    private Double rpe;

    private String notes;
    private Boolean isWarmup = false;

    public WorkoutSet(Double weight, Integer reps) {
        this.weight = weight;
        this.reps = reps;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    public Integer getReps() {
        return reps;
    }

    public void setReps(Integer reps) {
        this.reps = reps;
    }

    public Integer getRestTimeSeconds() {
        return restTimeSeconds;
    }

    public void setRestTimeSeconds(Integer restTimeSeconds) {
        this.restTimeSeconds = restTimeSeconds;
    }

    public Double getRpe() {
        return rpe;
    }

    public void setRpe(Double rpe) {
        this.rpe = rpe;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Boolean getWarmup() {
        return isWarmup;
    }

    public void setWarmup(Boolean warmup) {
        isWarmup = warmup;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("WorkoutSet{")
                .append("weight=").append(weight)
                .append(", reps=").append(reps);
        if (restTimeSeconds != null && restTimeSeconds > 0) {
            builder.append(", restTimeSeconds=").append(restTimeSeconds);
        }
        if (rpe != null && rpe > 0) {
            builder.append(", rpe=").append(rpe);
        }
        if (notes != null && !notes.isEmpty()) {
            builder.append(", notes='").append(notes).append('\'');
        }
        builder.append('}');
        return builder.toString();
    }
}
