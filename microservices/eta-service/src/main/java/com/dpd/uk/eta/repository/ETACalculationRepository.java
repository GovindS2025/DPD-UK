package com.dpd.uk.eta.repository;

import com.dpd.uk.eta.model.ETACalculation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ETACalculationRepository extends MongoRepository<ETACalculation, String> {
    
    Optional<ETACalculation> findByParcelIdAndIsActiveTrue(String parcelId);
    
    List<ETACalculation> findByDepotIdAndIsActiveTrue(String depotId);
    
    List<ETACalculation> findByDriverIdAndIsActiveTrue(String driverId);
    
    List<ETACalculation> findByVehicleIdAndIsActiveTrue(String vehicleId);
    
    @Query("{ 'estimatedArrival': { $gte: ?0, $lte: ?1 }, 'isActive': true }")
    List<ETACalculation> findActiveByEstimatedArrivalBetween(LocalDateTime start, LocalDateTime end);
    
    @Query("{ 'parcelId': { $in: ?0 }, 'isActive': true }")
    List<ETACalculation> findActiveByParcelIds(List<String> parcelIds);
    
    @Query("{ 'depotId': ?0, 'estimatedArrival': { $gte: ?1 }, 'isActive': true }")
    List<ETACalculation> findActiveByDepotIdAndEstimatedArrivalAfter(String depotId, LocalDateTime after);
    
    void deleteByParcelIdAndIsActiveFalse(String parcelId);
    
    long countByDepotIdAndIsActiveTrue(String depotId);
}
