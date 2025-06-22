package com.onelubo.strongnostr.rest;

import com.onelubo.strongnostr.model.Exercise;
import com.onelubo.strongnostr.model.Workout;
import com.onelubo.strongnostr.model.WorkoutSet;
import com.onelubo.strongnostr.service.WorkoutService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("workout")
public class WorkoutController {

    private final WorkoutService workoutService;

    public WorkoutController(WorkoutService workoutService) {
        this.workoutService = workoutService;
    }

    @PostMapping()
    public ResponseEntity<Workout> createWorkout(@RequestBody Exercise exercise, WorkoutSet workoutSet) {
        if (exercise == null || exercise.getName() == null || exercise.getName().isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Workout createdWorkout = workoutService.createWorkout(exercise, workoutSet);
        return ResponseEntity.ok(createdWorkout);
    }
}
