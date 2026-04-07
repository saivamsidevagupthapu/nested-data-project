package com.Indianspringbootproject.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.Indianspringbootproject.Entity.Vehicleresponcemappings;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicleresponcemappings, Long> {
    // This is correct for this table
    List<Vehicleresponcemappings> findByApiId(Long apiId);
    
    // REMOVE findByVehicleNumberAndVendorname from here! 
    // It doesn't belong to this entity.
}