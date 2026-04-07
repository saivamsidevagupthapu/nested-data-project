package com.Indianspringbootproject.Controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.Indianspringbootproject.Entity.VehicleInfo;
import com.Indianspringbootproject.service.VehicleService;
import com.fasterxml.jackson.databind.JsonNode;

@RestController
@RequestMapping("/vehicle")
public class VehicleController {

    @Autowired
    private VehicleService service;

    /**
     * UNIVERSAL DYNAMIC SAVE
     * This replaces the static call with the injected 'service' instance.
     */
    @PostMapping("/dynamic-save/{apiId}")
    public ResponseEntity<VehicleInfo> save(@RequestBody JsonNode payload, @PathVariable Long apiId) {
        // FIX: Use 'service' (the instance) instead of 'VehicleService' (the class)
        VehicleInfo saved = service.saveDynamicVehicle(payload, apiId);
        return ResponseEntity.ok(saved);
    }

    // --- EXISTING FLOWS (STAY UNCHANGED) ---

//    @PostMapping
//    public VehicleInfo createVehicle(@RequestBody VehicleInfo vehicle) {
//        // Ensure your service has a saveVehicle method that takes the entity directly
//        return service.saveVehicle(vehicle);
//    }

    @GetMapping("/{vendorname}/{vehiclenumber}")
    public VehicleInfo getVehicle(@PathVariable String vehiclenumber,
                                 @PathVariable String vendorname) {
        return service.getVehicleInfo(vehiclenumber, vendorname);
    }

    @GetMapping("/detailsByVendor")
    public VehicleInfo detailsByVendor(@RequestParam("plateNumber") String plateNumber,
                                       @RequestParam("vendorname") String vendorname) {
        return service.getVehicleInfo(plateNumber, vendorname);
    }

    @GetMapping("/vechidetails")
    public VehicleInfo vechidetails(@RequestParam("vno") String vehiclenumber,
                                   @RequestParam("ownerName") String ownerName) {
        return service.getVehicleInfo(vehiclenumber, ownerName);
    }

    @GetMapping("/header")
    public VehicleInfo getVehicleFromHeaders(
            @RequestHeader("vehiclenumber") String vehiclenumber,
            @RequestHeader("vendorname") String vendorname) {
        return service.getVehicleInfo(vehiclenumber, vendorname);
    }

    @PostMapping("/body")
    public VehicleInfo getVehicleFromBody(@RequestBody Map<String, String> body) {
        String vehicleNumber = body.get("vehiclenumber");
        String vendorname = body.get("vendorname");
        System.out.println("DEBUG: /body received vehicleNumber=" + vehicleNumber + " vendorname=" + vendorname);
        return service.getVehicleInfo(vehicleNumber, vendorname);
    }

    @GetMapping("/query")
    public VehicleInfo getVehicleFromQuery(
            @RequestParam("vehicleno") String vehicleno,
            @RequestParam("vendorname") String vendorname) {
        return service.getVehicleInfo(vehicleno, vendorname);
    }
}