package ru.samgtu.pm.stress_approximation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ru.samgtu.pm.stress_approximation.dto.Params;
import ru.samgtu.pm.stress_approximation.dto.Result;
import ru.samgtu.pm.stress_approximation.service.ApproximationService;

@RestController
public class ApproximationController {

    private final ApproximationService approximationService;

    public ApproximationController(ApproximationService approximationService) {
        this.approximationService = approximationService;
    }

    @GetMapping("/graph")
    public synchronized ResponseEntity<Result> getResult() {
        Result result = approximationService.getResult();
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/run")
    public synchronized ResponseEntity<?> runOptimization(@RequestBody Params params) {
        approximationService.cancelOptimization();
        params.init();
        approximationService.runOptimization(params);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/cancel")
    public synchronized ResponseEntity<?> cancelOptimization() {
        approximationService.cancelOptimization();
        return ResponseEntity.ok().build();
    }

}
