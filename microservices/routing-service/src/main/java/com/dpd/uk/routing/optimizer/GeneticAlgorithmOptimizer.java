package com.dpd.uk.routing.optimizer;

import com.dpd.uk.routing.model.RouteOptimizationRequest;
import com.dpd.uk.routing.model.RouteOptimizationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeneticAlgorithmOptimizer {
    
    private static final int DEFAULT_POPULATION_SIZE = 50;
    private static final int DEFAULT_MAX_ITERATIONS = 1000;
    private static final double DEFAULT_MUTATION_RATE = 0.1;
    private static final double DEFAULT_CROSSOVER_RATE = 0.8;
    
    public RouteOptimizationResult optimize(RouteOptimizationRequest request, 
                                          Map<String, Map<String, Double>> distanceMatrix,
                                          Map<String, Map<String, Integer>> timeMatrix) {
        
        long startTime = System.currentTimeMillis();
        log.info("Starting genetic algorithm optimization for {} stops", request.getStops().size());
        
        int populationSize = request.getPopulationSize() != null ? request.getPopulationSize() : DEFAULT_POPULATION_SIZE;
        int maxIterations = request.getMaxIterations() != null ? request.getMaxIterations() : DEFAULT_MAX_ITERATIONS;
        double mutationRate = request.getMutationRate() != null ? request.getMutationRate() : DEFAULT_MUTATION_RATE;
        double crossoverRate = request.getCrossoverRate() != null ? request.getCrossoverRate() : DEFAULT_CROSSOVER_RATE;
        
        // Initialize population
        List<Individual> population = initializePopulation(request.getStops().size(), populationSize);
        
        Individual bestIndividual = null;
        int iterations = 0;
        
        for (int generation = 0; generation < maxIterations; generation++) {
            // Evaluate fitness
            evaluatePopulation(population, distanceMatrix, timeMatrix, request);
            
            // Find best individual
            Individual currentBest = population.stream()
                .max(Comparator.comparing(Individual::getFitness))
                .orElse(population.get(0));
            
            if (bestIndividual == null || currentBest.getFitness() > bestIndividual.getFitness()) {
                bestIndividual = currentBest;
            }
            
            // Check convergence
            if (isConverged(population)) {
                log.info("Genetic algorithm converged after {} generations", generation);
                break;
            }
            
            // Create new generation
            List<Individual> newPopulation = new ArrayList<>();
            
            // Elitism: keep best individual
            newPopulation.add(new Individual(bestIndividual.getSequence()));
            
            // Generate offspring
            while (newPopulation.size() < populationSize) {
                Individual parent1 = tournamentSelection(population);
                Individual parent2 = tournamentSelection(population);
                
                if (Math.random() < crossoverRate) {
                    Individual[] offspring = crossover(parent1, parent2);
                    newPopulation.add(offspring[0]);
                    if (newPopulation.size() < populationSize) {
                        newPopulation.add(offspring[1]);
                    }
                } else {
                    newPopulation.add(new Individual(parent1.getSequence()));
                    if (newPopulation.size() < populationSize) {
                        newPopulation.add(new Individual(parent2.getSequence()));
                    }
                }
            }
            
            // Apply mutations
            for (int i = 1; i < newPopulation.size(); i++) { // Skip elite individual
                if (Math.random() < mutationRate) {
                    mutate(newPopulation.get(i));
                }
            }
            
            population = newPopulation;
            iterations = generation + 1;
        }
        
        long optimizationTime = System.currentTimeMillis() - startTime;
        
        log.info("Genetic algorithm completed in {}ms after {} iterations", optimizationTime, iterations);
        
        return createResult(bestIndividual, distanceMatrix, timeMatrix, optimizationTime, iterations);
    }
    
    private List<Individual> initializePopulation(int numStops, int populationSize) {
        List<Individual> population = new ArrayList<>();
        
        for (int i = 0; i < populationSize; i++) {
            List<Integer> sequence = new ArrayList<>();
            for (int j = 0; j < numStops; j++) {
                sequence.add(j);
            }
            Collections.shuffle(sequence);
            population.add(new Individual(sequence));
        }
        
        return population;
    }
    
    private void evaluatePopulation(List<Individual> population, 
                                   Map<String, Map<String, Double>> distanceMatrix,
                                   Map<String, Map<String, Integer>> timeMatrix,
                                   RouteOptimizationRequest request) {
        
        for (Individual individual : population) {
            double fitness = calculateFitness(individual, distanceMatrix, timeMatrix, request);
            individual.setFitness(fitness);
        }
    }
    
    private double calculateFitness(Individual individual, 
                                  Map<String, Map<String, Double>> distanceMatrix,
                                  Map<String, Map<String, Integer>> timeMatrix,
                                  RouteOptimizationRequest request) {
        
        List<Integer> sequence = individual.getSequence();
        double totalDistance = 0.0;
        int totalTime = 0;
        
        // Calculate total distance and time
        for (int i = 0; i < sequence.size() - 1; i++) {
            int from = sequence.get(i);
            int to = sequence.get(i + 1);
            
            // Get distance and time from matrices
            Double distance = getDistance(distanceMatrix, from, to);
            Integer time = getTime(timeMatrix, from, to);
            
            if (distance != null) totalDistance += distance;
            if (time != null) totalTime += time;
        }
        
        // Calculate fitness (higher is better)
        // Use inverse of total distance as base fitness
        double baseFitness = 1.0 / (1.0 + totalDistance);
        
        // Apply penalties for constraint violations
        double penalty = 0.0;
        
        if (request.getMaxDistanceKm() != null && totalDistance > request.getMaxDistanceKm()) {
            penalty += (totalDistance - request.getMaxDistanceKm()) * 0.1;
        }
        
        if (request.getMaxDurationHours() != null && totalTime > request.getMaxDurationHours() * 60) {
            penalty += (totalTime - request.getMaxDurationHours() * 60) * 0.01;
        }
        
        return baseFitness - penalty;
    }
    
    private Double getDistance(Map<String, Map<String, Double>> distanceMatrix, int from, int to) {
        Map<String, Double> fromMap = distanceMatrix.get(String.valueOf(from));
        return fromMap != null ? fromMap.get(String.valueOf(to)) : null;
    }
    
    private Integer getTime(Map<String, Map<String, Integer>> timeMatrix, int from, int to) {
        Map<String, Integer> fromMap = timeMatrix.get(String.valueOf(from));
        return fromMap != null ? fromMap.get(String.valueOf(to)) : null;
    }
    
    private boolean isConverged(List<Individual> population) {
        // Check if population has converged (all individuals have similar fitness)
        double avgFitness = population.stream()
            .mapToDouble(Individual::getFitness)
            .average()
            .orElse(0.0);
        
        double variance = population.stream()
            .mapToDouble(ind -> Math.pow(ind.getFitness() - avgFitness, 2))
            .average()
            .orElse(0.0);
        
        return variance < 0.001; // Convergence threshold
    }
    
    private Individual tournamentSelection(List<Individual> population) {
        int tournamentSize = Math.min(5, population.size());
        List<Individual> tournament = new ArrayList<>();
        
        for (int i = 0; i < tournamentSize; i++) {
            tournament.add(population.get((int) (Math.random() * population.size())));
        }
        
        return tournament.stream()
            .max(Comparator.comparing(Individual::getFitness))
            .orElse(tournament.get(0));
    }
    
    private Individual[] crossover(Individual parent1, Individual parent2) {
        List<Integer> sequence1 = new ArrayList<>(parent1.getSequence());
        List<Integer> sequence2 = new ArrayList<>(parent2.getSequence());
        
        int size = sequence1.size();
        int start = (int) (Math.random() * size);
        int end = start + (int) (Math.random() * (size - start));
        
        // Order crossover (OX)
        List<Integer> child1 = new ArrayList<>(Collections.nCopies(size, -1));
        List<Integer> child2 = new ArrayList<>(Collections.nCopies(size, -1));
        
        // Copy middle section
        for (int i = start; i <= end; i++) {
            child1.set(i, sequence1.get(i));
            child2.set(i, sequence2.get(i));
        }
        
        // Fill remaining positions
        fillRemaining(child1, sequence2, start, end);
        fillRemaining(child2, sequence1, start, end);
        
        return new Individual[]{
            new Individual(child1),
            new Individual(child2)
        };
    }
    
    private void fillRemaining(List<Integer> child, List<Integer> parent, int start, int end) {
        int parentIndex = 0;
        for (int i = 0; i < child.size(); i++) {
            if (child.get(i) == -1) {
                while (parentIndex < parent.size()) {
                    int value = parent.get(parentIndex++);
                    if (!child.contains(value)) {
                        child.set(i, value);
                        break;
                    }
                }
            }
        }
    }
    
    private void mutate(Individual individual) {
        List<Integer> sequence = individual.getSequence();
        
        // Swap mutation
        int index1 = (int) (Math.random() * sequence.size());
        int index2 = (int) (Math.random() * sequence.size());
        
        if (index1 != index2) {
            Collections.swap(sequence, index1, index2);
        }
    }
    
    private RouteOptimizationResult createResult(Individual bestIndividual, 
                                               Map<String, Map<String, Double>> distanceMatrix,
                                               Map<String, Map<String, Integer>> timeMatrix,
                                               long optimizationTime, 
                                               int iterations) {
        
        List<Integer> sequence = bestIndividual.getSequence();
        double totalDistance = calculateTotalDistance(sequence, distanceMatrix);
        int totalTime = calculateTotalTime(sequence, timeMatrix);
        
        return RouteOptimizationResult.builder()
            .optimizedSequence(sequence)
            .totalDistance(totalDistance)
            .totalTime(totalTime)
            .optimizationTime(optimizationTime)
            .iterations(iterations)
            .fitness(bestIndividual.getFitness())
            .algorithm("GENETIC_ALGORITHM")
            .status("SUCCESS")
            .isOptimal(false) // Genetic algorithms don't guarantee optimality
            .optimalityGap(0.0) // Unknown for genetic algorithms
            .totalStops(sequence.size())
            .routeEfficiency(1.0 / (1.0 + totalDistance))
            .build();
    }
    
    private double calculateTotalDistance(List<Integer> sequence, Map<String, Map<String, Double>> distanceMatrix) {
        double totalDistance = 0.0;
        for (int i = 0; i < sequence.size() - 1; i++) {
            Double distance = getDistance(distanceMatrix, sequence.get(i), sequence.get(i + 1));
            if (distance != null) {
                totalDistance += distance;
            }
        }
        return totalDistance;
    }
    
    private int calculateTotalTime(List<Integer> sequence, Map<String, Map<String, Integer>> timeMatrix) {
        int totalTime = 0;
        for (int i = 0; i < sequence.size() - 1; i++) {
            Integer time = getTime(timeMatrix, sequence.get(i), sequence.get(i + 1));
            if (time != null) {
                totalTime += time;
            }
        }
        return totalTime;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class Individual {
        private List<Integer> sequence;
        private double fitness;
        
        public Individual(List<Integer> sequence) {
            this.sequence = new ArrayList<>(sequence);
            this.fitness = 0.0;
        }
    }
}
