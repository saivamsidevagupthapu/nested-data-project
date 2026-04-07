package com.Indianspringbootproject.Entity;

import jakarta.persistence.*;

@Entity
@Table(name = "vehicleresponcemappings")
public class Vehicleresponcemappings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long apiId;
    
    // The following names MUST exactly match the variable names in VehicleInfo.java
    private String make;           // Stores JSON path (e.g., /data/make)
    private String model;          // Stores JSON path
    private String year;           // Stores JSON path
    private String vehicleNumber;  // Stores JSON path (e.g., /data/v_no)

    // --- GETTERS AND SETTERS ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getApiId() {
        return apiId;
    }

    public void setApiId(Long apiId) {
        this.apiId = apiId;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }
}