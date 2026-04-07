package com.stablespringbootproject.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.stablespringbootproject.Entity.Vehicleresponcemappings;

public interface Vehicleresponcemappingsrepo extends JpaRepository<Vehicleresponcemappings, Long> {

    List<Vehicleresponcemappings> findByVendorId(Long vendorId);

    List<Vehicleresponcemappings> findByApiId(Long apiId);

    List<Vehicleresponcemappings> findByVendorIdAndApiId(Long vendorId, Long apiId);
}
