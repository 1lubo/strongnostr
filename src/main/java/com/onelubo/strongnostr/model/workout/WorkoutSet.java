package com.onelubo.strongnostr.model.workout;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public class WorkoutSet {

    @Schema(description = "Weight lifted in kilograms", example = "80.0")
    @NotNull(message = "Weight is required")
    @PositiveOrZero(message = "Weight must be zero or positive")
    private Double weight;

    @Schema(description = "Number of repetitions performed", example = "8")
    @NotNull(message = "Reps is required")
    @Positive(message = "Reps must be zero or positive")
    private Integer reps;

    @Schema(description = "Rest time after this set in seconds", example = "180")
    @PositiveOrZero(message = "Rest time must be zero or positive")
    private Integer restTimeSeconds;

    @Schema(description = "Rate of Perceived Exertion (1-10 scale)", example = "7.5")
    @PositiveOrZero(message = "RPE must be zero or positive")
    private Double rpe;

    @Schema(description = "Notes about this set", example = "Felt strong, good form")
    private String notes;

    @Schema(description = "Whether this set was a warmup", example = "false")
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
