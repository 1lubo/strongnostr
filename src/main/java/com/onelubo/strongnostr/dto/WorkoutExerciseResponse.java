package com.onelubo.strongnostr.dto;

import com.onelubo.strongnostr.model.workout.WorkoutSet;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Exercise withtin a workout with sets and metrics")
public class WorkoutExerciseResponse {

    @Schema(description = "Exercise Id", example = "exercise12345")
    private String exerciseId;

    @Schema(description = "Name of the exercise", example = "Bench Press")
    private String exerciseName;

    @Schema(description = "Equipment used for the exercise", example = "Barbell")
    private String equipment;

    @Schema(description = "List of sets performed for the exercise")
    private List<WorkoutSet> sets;

    @Schema(description = "Total volume for the exercise (weight * reps)", example = "1500.0")
    private Double totalVolume;

    @Schema(description = "Average Rate of Perceived Exertion (RPE) for the sets", example = "7.5")
    private Double averageRpe;

    @Schema(description = "Total number of repetitions performed for the exercise", example = "100")
    private Integer totalReps;

    @Schema(description = "Total rest time in seconds between sets", example = "120")
    private Integer totalRestTimeSeconds = 0;

    @Schema(description = "Additional notes or comments about the exercise")
    private String notes;

    public WorkoutExerciseResponse(String exerciseId, String exerciseName, String equipment, List<WorkoutSet> sets, Double totalVolume, Double averageRpe, Integer totalReps, Integer totalRestTimeSeconds, String notes) {
        this.exerciseId = exerciseId;
        this.exerciseName = exerciseName;
        this.equipment = equipment;
        this.sets = sets;
        this.totalVolume = totalVolume;
        this.averageRpe = averageRpe;
        this.totalReps = totalReps;
        this.totalRestTimeSeconds = totalRestTimeSeconds;
        this.notes = notes;
    }
}
