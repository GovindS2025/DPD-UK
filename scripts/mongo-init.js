// MongoDB initialization script
db = db.getSiblingDB('logistics');

// Create collections
db.createCollection('eta_calculations');
db.createCollection('routes');
db.createCollection('return_requests');

// Create indexes for better performance
db.eta_calculations.createIndex({ "parcelId": 1, "isActive": 1 });
db.eta_calculations.createIndex({ "depotId": 1, "isActive": 1 });
db.eta_calculations.createIndex({ "driverId": 1, "isActive": 1 });
db.eta_calculations.createIndex({ "estimatedArrival": 1, "isActive": 1 });

db.routes.createIndex({ "depotId": 1, "status": 1 });
db.routes.createIndex({ "driverId": 1, "status": 1 });
db.routes.createIndex({ "vehicleId": 1, "status": 1 });
db.routes.createIndex({ "plannedStartTime": 1, "status": 1 });

db.return_requests.createIndex({ "returnId": 1, "isActive": 1 });
db.return_requests.createIndex({ "status": 1, "isActive": 1 });
db.return_requests.createIndex({ "customerId": 1, "isActive": 1 });
db.return_requests.createIndex({ "parcelId": 1, "isActive": 1 });
db.return_requests.createIndex({ "ttlExpiry": 1, "isActive": 1 });

print('MongoDB initialization completed successfully');
