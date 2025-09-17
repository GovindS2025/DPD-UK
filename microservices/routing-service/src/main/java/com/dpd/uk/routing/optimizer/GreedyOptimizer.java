package com.dpd.uk.routing.optimizer;

import com.dpd.uk.routing.model.RouteOptimizationRequest;
import com.dpd.uk.routing.model.RouteOptimizationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class GreedyOptimizer {
    
    public RouteOptimizationResult optimize(RouteOptimizationRequest request, 
                                          Map<String, Map<String, Double>> distanceMatrix,
                                          Map<String, Map<String, Integer>> timeMatrix) {
        
        long startTime = System.currentTimeMillis();
        log.info("Starting greedy optimization for {} stops", request.getStops().size());
        
        int numStops = request.getStops().size();
        List<Integer> solution = new ArrayList<>();
        Set<Integer> unvisited = new HashSet<>();
        
        // Initialize unvisited set
        for (int i = 0; i < numStops; i++) {
            unvisited.add(i);
        }
        
        // Start from depot (first stop)
        int currentStop = 0;
        solution.add(currentStop);
        unvisited.remove(currentStop);
        
        // Greedy construction
        while (!unvisited.isEmpty()) {
            int nextStop = findNearestNeighbor(currentStop, unvisited, distanceMatrix);
            solution.add(nextStop);
            unvisited.remove(nextStop);
            currentStop = nextStop;
        }
        
        long optimizationTime = System.currentTimeMillis() - startTime;
        
        log.info("Greedy optimization completed in {}ms", optimizationTime);
        
        return createResult(solution, distanceMatrix, timeMatrix, optimizationTime);
    }
    
    private int findNearestNeighbor(int currentStop, Set<Integer> unvisited, 
                                   Map<String, Map<String, Double>> distanceMatrix) {
        
        int nearestStop = -1;
        double minDistance = Double.MAX_VALUE;
        
        for (int stop : unvisited) {
            Double distance = getDistance(distanceMatrix, currentStop, stop);
            if (distance != null && distance < minDistance) {
                minDistance = distance;
                nearestStop = stop;
            }
        }
        
        return nearestStop;
    }
    
    private Double getDistance(Map<String, Map<String, Double>> distanceMatrix, int from, int to) {
        Map<String, Double> fromMap = distanceMatrix.get(String.valueOf(from));
        return fromMap != null ? fromMap.get(String.valueOf(to)) : null;
    }
    
    private Integer getTime(Map<String, Map<String, Integer>> timeMatrix, int from, int to) {
        Map<String, Integer> fromMap = timeMatrix.get(String.valueOf(from));
        return fromMap != null ? fromMap.get(String.valueOf(to)) : null;
    }
    
    private RouteOptimizationResult createResult(List<Integer> solution, 
                                               Map<String, Map<String, Double>> distanceMatrix,
                                               Map<String, Map<String, Integer>> timeMatrix,
                                               long optimizationTime) {
        
        double totalDistance = calculateTotalDistance(solution, distanceMatrix);
        int totalTime = calculateTotalTime(solution, timeMatrix);
        
        return RouteOptimizationResult.builder()
            .optimizedSequence(solution)
            .totalDistance(totalDistance)
            .totalTime(totalTime)
            .optimizationTime(optimizationTime)
            .iterations(1) // Greedy is single-pass
            .fitness(1.0 / (1.0 + totalDistance))
            .algorithm("GREEDY")
            .status("SUCCESS")
            .isOptimal(false)
            .optimalityGap(0.0)
            .totalStops(solution.size())
            .routeEfficiency(1.0 / (1.0 + totalDistance))
            .build();
    }
    
    private double calculateTotalDistance(List<Integer> solution, Map<String, Map<String, Double>> distanceMatrix) {
        double totalDistance = 0.0;
        for (int i = 0; i < solution.size() - 1; i++) {
            Double distance = getDistance(distanceMatrix, solution.get(i), solution.get(i + 1));
            if (distance != null) {
                totalDistance += distance;
            }
        }
        return totalDistance;
    }
    
    private int calculateTotalTime(List<Integer> solution, Map<String, Map<String, Integer>> timeMatrix) {
        int totalTime = 0;
        for (int i = 0; i < solution.size() - 1; i++) {
            Integer time = getTime(timeMatrix, solution.get(i), solution.get(i + 1));
            if (time != null) {
                totalTime += time;
            }
        }
        return totalTime;
    }
}
