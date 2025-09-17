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
public class SimulatedAnnealingOptimizer {
    
    private static final double DEFAULT_INITIAL_TEMPERATURE = 1000.0;
    private static final double DEFAULT_COOLING_RATE = 0.95;
    private static final int DEFAULT_MAX_ITERATIONS = 1000;
    
    public RouteOptimizationResult optimize(RouteOptimizationRequest request, 
                                          Map<String, Map<String, Double>> distanceMatrix,
                                          Map<String, Map<String, Integer>> timeMatrix) {
        
        long startTime = System.currentTimeMillis();
        log.info("Starting simulated annealing optimization for {} stops", request.getStops().size());
        
        double initialTemperature = request.getCoolingRate() != null ? 
            (1.0 - request.getCoolingRate()) * 1000 : DEFAULT_INITIAL_TEMPERATURE;
        double coolingRate = request.getCoolingRate() != null ? request.getCoolingRate() : DEFAULT_COOLING_RATE;
        int maxIterations = request.getMaxIterations() != null ? request.getMaxIterations() : DEFAULT_MAX_ITERATIONS;
        
        // Initialize solution
        List<Integer> currentSolution = initializeSolution(request.getStops().size());
        List<Integer> bestSolution = new ArrayList<>(currentSolution);
        
        double currentCost = calculateCost(currentSolution, distanceMatrix, timeMatrix, request);
        double bestCost = currentCost;
        
        double temperature = initialTemperature;
        int iterations = 0;
        
        for (int i = 0; i < maxIterations; i++) {
            // Generate neighbor solution
            List<Integer> neighborSolution = generateNeighbor(currentSolution);
            double neighborCost = calculateCost(neighborSolution, distanceMatrix, timeMatrix, request);
            
            // Accept or reject neighbor
            if (neighborCost < currentCost || Math.random() < Math.exp((currentCost - neighborCost) / temperature)) {
                currentSolution = neighborSolution;
                currentCost = neighborCost;
                
                // Update best solution
                if (currentCost < bestCost) {
                    bestSolution = new ArrayList<>(currentSolution);
                    bestCost = currentCost;
                }
            }
            
            // Cool down
            temperature *= coolingRate;
            iterations = i + 1;
            
            // Early termination if temperature is too low
            if (temperature < 0.1) {
                log.info("Simulated annealing terminated early due to low temperature after {} iterations", iterations);
                break;
            }
        }
        
        long optimizationTime = System.currentTimeMillis() - startTime;
        
        log.info("Simulated annealing completed in {}ms after {} iterations", optimizationTime, iterations);
        
        return createResult(bestSolution, distanceMatrix, timeMatrix, optimizationTime, iterations, bestCost);
    }
    
    private List<Integer> initializeSolution(int numStops) {
        List<Integer> solution = new ArrayList<>();
        for (int i = 0; i < numStops; i++) {
            solution.add(i);
        }
        Collections.shuffle(solution);
        return solution;
    }
    
    private List<Integer> generateNeighbor(List<Integer> currentSolution) {
        List<Integer> neighbor = new ArrayList<>(currentSolution);
        
        // 2-opt swap
        int i = (int) (Math.random() * neighbor.size());
        int j = (int) (Math.random() * neighbor.size());
        
        if (i > j) {
            int temp = i;
            i = j;
            j = temp;
        }
        
        // Reverse the segment between i and j
        while (i < j) {
            Collections.swap(neighbor, i, j);
            i++;
            j--;
        }
        
        return neighbor;
    }
    
    private double calculateCost(List<Integer> solution, 
                               Map<String, Map<String, Double>> distanceMatrix,
                               Map<String, Map<String, Integer>> timeMatrix,
                               RouteOptimizationRequest request) {
        
        double totalDistance = 0.0;
        int totalTime = 0;
        
        // Calculate total distance and time
        for (int i = 0; i < solution.size() - 1; i++) {
            int from = solution.get(i);
            int to = solution.get(i + 1);
            
            Double distance = getDistance(distanceMatrix, from, to);
            Integer time = getTime(timeMatrix, from, to);
            
            if (distance != null) totalDistance += distance;
            if (time != null) totalTime += time;
        }
        
        // Calculate cost (lower is better)
        double cost = totalDistance;
        
        // Apply penalties for constraint violations
        if (request.getMaxDistanceKm() != null && totalDistance > request.getMaxDistanceKm()) {
            cost += (totalDistance - request.getMaxDistanceKm()) * 10.0;
        }
        
        if (request.getMaxDurationHours() != null && totalTime > request.getMaxDurationHours() * 60) {
            cost += (totalTime - request.getMaxDurationHours() * 60) * 0.1;
        }
        
        return cost;
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
                                               long optimizationTime, 
                                               int iterations,
                                               double cost) {
        
        double totalDistance = calculateTotalDistance(solution, distanceMatrix);
        int totalTime = calculateTotalTime(solution, timeMatrix);
        
        return RouteOptimizationResult.builder()
            .optimizedSequence(solution)
            .totalDistance(totalDistance)
            .totalTime(totalTime)
            .optimizationTime(optimizationTime)
            .iterations(iterations)
            .fitness(1.0 / (1.0 + cost))
            .algorithm("SIMULATED_ANNEALING")
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
