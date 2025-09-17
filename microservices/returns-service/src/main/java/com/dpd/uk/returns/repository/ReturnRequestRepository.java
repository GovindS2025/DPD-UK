package com.dpd.uk.returns.repository;

import com.dpd.uk.returns.model.ReturnRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends MongoRepository<ReturnRequest, String> {
    
    Optional<ReturnRequest> findByReturnIdAndIsActiveTrue(String returnId);
    
    List<ReturnRequest> findByStatusAndIsActiveTrue(ReturnRequest.ReturnStatus status);
    
    List<ReturnRequest> findByCustomerIdAndIsActiveTrue(String customerId);
    
    List<ReturnRequest> findByParcelIdAndIsActiveTrue(String parcelId);
    
    List<ReturnRequest> findByDepotIdAndIsActiveTrue(String depotId);
    
    List<ReturnRequest> findByAssignedDriverIdAndIsActiveTrue(String driverId);
    
    @Query("{ 'ttlExpiry': { $lt: ?0 }, 'isActive': true }")
    List<ReturnRequest> findByTtlExpiryBeforeAndIsActiveTrue(LocalDateTime expiryTime);
    
    @Query("{ 'status': { $in: [?0, ?1] }, 'isActive': true }")
    List<ReturnRequest> findByStatusInAndIsActiveTrue(ReturnRequest.ReturnStatus status1, ReturnRequest.ReturnStatus status2);
    
    @Query("{ 'requestedAt': { $gte: ?0, $lte: ?1 }, 'isActive': true }")
    List<ReturnRequest> findByRequestedAtBetweenAndIsActiveTrue(LocalDateTime start, LocalDateTime end);
    
    @Query("{ 'priority': ?0, 'status': ?1, 'isActive': true }")
    List<ReturnRequest> findByPriorityAndStatusAndIsActiveTrue(ReturnRequest.ReturnPriority priority, ReturnRequest.ReturnStatus status);
    
    long countByStatusAndIsActiveTrue(ReturnRequest.ReturnStatus status);
    
    long countByCustomerIdAndIsActiveTrue(String customerId);
    
    @Query("{ 'status': ?0, 'lastUpdated': { $lt: ?1 }, 'isActive': true }")
    List<ReturnRequest> findStaleReturns(ReturnRequest.ReturnStatus status, LocalDateTime threshold);
}
