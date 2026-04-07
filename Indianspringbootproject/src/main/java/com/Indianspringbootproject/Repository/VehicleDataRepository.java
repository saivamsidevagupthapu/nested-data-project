package com.Indianspringbootproject.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.Indianspringbootproject.Entity.VehicleInfo;

@Repository
public interface VehicleDataRepository extends JpaRepository<VehicleInfo, Long> {
    
    // Move the search method here!
    VehicleInfo findByVehicleNumberAndVendorname(String vehicleNumber, String vendorname);
}