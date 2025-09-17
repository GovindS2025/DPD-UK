package com.dpd.uk.routing.repository;

import com.dpd.uk.common.model.Route;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RouteRepository extends MongoRepository<Route, String> {
    
    List<Route> findByDepotIdAndStatus(String depotId, Route.RouteStatus status);
    
    List<Route> findByDriverIdAndStatus(String driverId, Route.RouteStatus status);
    
    List<Route> findByVehicleIdAndStatus(String vehicleId, Route.RouteStatus status);
    
    @Query("{ 'depotId': ?0, 'status': { $in: [?1, ?2] } }")
    List<Route> findByDepotIdAndStatusIn(String depotId, Route.RouteStatus status1, Route.RouteStatus status2);
    
    @Query("{ 'plannedStartTime': { $gte: ?0, $lte: ?1 }, 'status': ?2 }")
    List<Route> findByPlannedStartTimeBetweenAndStatus(LocalDateTime start, LocalDateTime end, Route.RouteStatus status);
    
    @Query("{ 'driverId': ?0, 'status': { $in: [?1, ?2] }, 'plannedStartTime': { $gte: ?3 } }")
    List<Route> findActiveRoutesByDriver(String driverId, Route.RouteStatus status1, Route.RouteStatus status2, LocalDateTime from);
    
    @Query("{ 'depotId': ?0, 'status': ?1, 'totalDistanceKm': { $lte: ?2 } }")
    List<Route> findByDepotIdAndStatusAndMaxDistance(String depotId, Route.RouteStatus status, Double maxDistance);
    
    Optional<Route> findByRouteIdAndStatus(String routeId, Route.RouteStatus status);
    
    long countByDepotIdAndStatus(String depotId, Route.RouteStatus status);
    
    long countByDriverIdAndStatus(String driverId, Route.RouteStatus status);
    
    @Query("{ 'status': ?0, 'lastUpdated': { $lt: ?1 } }")
    List<Route> findStaleRoutes(Route.RouteStatus status, LocalDateTime threshold);
}
