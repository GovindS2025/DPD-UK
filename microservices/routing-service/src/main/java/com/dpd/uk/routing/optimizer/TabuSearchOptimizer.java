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
public class TabuSearchOptimizer {
    
    private static final int DEFAULT_TABU_LIST_SIZE = 10;
    private static final int DEFAULT_MAX_ITERATIONS = 1000;
    private static final int DEFAULT_MAX_ITERATIONS_WITHOUT_IMPROVEMENT = 100;
    
    public RouteOptimizationResult optimize(RouteOptimizationRequest request, 
                                          Map<String, Map<String, Double>> distanceMatrix,
                                          Map<String, Map<String, Integer>> timeMatrix) {
        
        long startTime = System.currentTimeMillis();
        log.info("Starting tabu search optimization for {} stops", request.getStops().size());
        
        int tabuListSize = request.getTabuListSize() != null ? request.getTabuListSize() : DEFAULT_TABU_LIST_SIZE;
        int maxIterations = request.getMaxIterations() != null ? request.getMaxIterations() : DEFAULT_MAX_ITERATIONS;
        int maxIterationsWithoutImprovement = DEFAULT_MAX_ITERATIONS_WITHOUT_IMPROVEMENT;
        
        // Initialize solution
        List<Integer> currentSolution = initializeSolution(request.getStops().size());
        List<Integer> bestSolution = new ArrayList<>(currentSolution);
        
        double currentCost = calculateCost(currentSolution, distanceMatrix, timeMatrix, request);
        double bestCost = currentCost;
        
        // Initialize tabu list
        Queue<Move> tabuList = new LinkedList<>();
        int iterationsWithoutImprovement = 0;
        int iterations = 0;
        
        for (int i = 0; i < maxIterations && iterationsWithoutImprovement < maxIterationsWithoutImprovement; i++) {
            // Generate all possible moves
            List<Move> moves = generateMoves(currentSolution);
            
            // Find best non-tabu move
            Move bestMove = null;
            double bestMoveCost = Double.MAX_VALUE;
            
            for (Move move : moves) {
                if (!isTabu(move, tabuList)) {
                    List<Integer> neighborSolution = applyMove(currentSolution, move);
                    double neighborCost = calculateCost(neighborSolution, distanceMatrix, timeMatrix, request);
                    
                    if (neighborCost < bestMoveCost) {
                        bestMove = move;
                        bestMoveCost = neighborCost;
                    }
                }
            }
            
            // If no non-tabu move found, select best move anyway (aspiration criterion)
            if (bestMove == null) {
                bestMove = moves.stream()
                    .min(Comparator.comparing(move -> calculateCost(applyMove(currentSolution, move), distanceMatrix, timeMatrix, request)))
                    .orElse(moves.get(0));
                bestMoveCost = calculateCost(applyMove(currentSolution, bestMove), distanceMatrix, timeMatrix, request);
            }
            
            // Apply move
            currentSolution = applyMove(currentSolution, bestMove);
            currentCost = bestMoveCost;
            
            // Update tabu list
            tabuList.offer(bestMove);
            if (tabuList.size() > tabuListSize) {
                tabuList.poll();
            }
            
            // Update best solution
            if (currentCost < bestCost) {
                bestSolution = new ArrayList<>(currentSolution);
                bestCost = currentCost;
                iterationsWithoutImprovement = 0;
            } else {
                iterationsWithoutImprovement++;
            }
            
            iterations = i + 1;
        }
        
        long optimizationTime = System.currentTimeMillis() - startTime;
        
        log.info("Tabu search completed in {}ms after {} iterations", optimizationTime, iterations);
        
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
    
    private List<Move> generateMoves(List<Integer> solution) {
        List<Move> moves = new ArrayList<>();
        
        // Generate 2-opt moves
        for (int i = 0; i < solution.size() - 1; i++) {
            for (int j = i + 1; j < solution.size(); j++) {
                moves.add(new Move(i, j, MoveType.TWO_OPT));
            }
        }
        
        // Generate swap moves
        for (int i = 0; i < solution.size(); i++) {
            for (int j = i + 1; j < solution.size(); j++) {
                moves.add(new Move(i, j, MoveType.SWAP));
            }
        }
        
        return moves;
    }
    
    private boolean isTabu(Move move, Queue<Move> tabuList) {
        return tabuList.contains(move);
    }
    
    private List<Integer> applyMove(List<Integer> solution, Move move) {
        List<Integer> newSolution = new ArrayList<>(solution);
        
        switch (move.getType()) {
            case TWO_OPT -> {
                int i = move.getI();
                int j = move.getJ();
                while (i < j) {
                    Collections.swap(newSolution, i, j);
                    i++;
                    j--;
                }
            }
            case SWAP -> {
                Collections.swap(newSolution, move.getI(), move.getJ());
            }
        }
        
        return newSolution;
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
            .algorithm("TABU_SEARCH")
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
    
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class Move {
        private int i;
        private int j;
        private MoveType type;
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Move move = (Move) obj;
            return i == move.i && j == move.j && type == move.type;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(i, j, type);
        }
    }
    
    private enum MoveType {
        TWO_OPT, SWAP
    }
}
