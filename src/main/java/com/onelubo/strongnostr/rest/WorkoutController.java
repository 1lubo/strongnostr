package com.onelubo.strongnostr.rest;

import com.onelubo.strongnostr.dto.ExerciseSetDto;
import com.onelubo.strongnostr.exception.WorkoutNotFoundException;
import com.onelubo.strongnostr.model.workout.Workout;
import com.onelubo.strongnostr.service.WorkoutService;
import jakarta.validation.Valid;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("workout")
public class WorkoutController {

    private final WorkoutService workoutService;

    public WorkoutController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @GetMapping("/{workoutId}")
    public ResponseEntity<?> getWorkoutById(@PathVariable("workoutId") String workoutId) {
        try {
            Workout workout = workoutService.getWorkoutById(workoutId);
            return ResponseEntity.ok(workout);
        } catch (WorkoutNotFoundException e) {
            return ResponseEntity.status(404).body(String.format("Workout with ID '%s' not found", workoutId));
        }
    }

    @PostMapping()
    public ResponseEntity<?> createWorkout(@Valid @RequestBody ExerciseSetDto workoutDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).toList());
        }

        Workout createdWorkout = workoutService.createWorkout(workoutDto.getExercise(), workoutDto.getWorkoutSet());
        return ResponseEntity.ok(createdWorkout);
    }

    @PostMapping("/addExercise/{workoutId}")
    public ResponseEntity<?> addExercise(@Valid @RequestBody ExerciseSetDto workoutDto,
                                               @PathVariable("workoutId") String workoutId) {
        try {
            Workout existingWorkout = workoutService.getWorkoutById(workoutId);
            Workout updatedWorkout = workoutService.addExerciseToWorkout(existingWorkout, workoutDto.getExercise(),
                                                       workoutDto.getWorkoutSet());
            return ResponseEntity.ok(updatedWorkout);

        } catch (WorkoutNotFoundException e) {
            return ResponseEntity.status(404).body("Workout with ID " + workoutId + " not found");
        }
    }
}
