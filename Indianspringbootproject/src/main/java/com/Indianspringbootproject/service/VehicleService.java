package com.Indianspringbootproject.service;

import java.lang.reflect.Field;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.Indianspringbootproject.Entity.VehicleInfo;
import com.Indianspringbootproject.Entity.Vehicleresponcemappings;
import com.Indianspringbootproject.Repository.VehicleDataRepository;
import com.Indianspringbootproject.Repository.VehicleRepository;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class VehicleService {

	@Autowired
	private VehicleRepository mappingRepository;

	@Autowired
	private VehicleDataRepository dataRepository;

	public VehicleInfo saveDynamicVehicle(JsonNode root, Long apiId) {
		// 1. Fetch rules
		List<Vehicleresponcemappings> mappings = mappingRepository.findByApiId(apiId);
		if (mappings.isEmpty())
			return null;

		Vehicleresponcemappings mapConfig = mappings.get(0);
		VehicleInfo newVehicle = new VehicleInfo();

		// 2. Loop through mapping fields
		for (Field mappingField : mapConfig.getClass().getDeclaredFields()) {
			try {
				mappingField.setAccessible(true);
				String fieldName = mappingField.getName();

				// ONLY skip the technical database IDs
				if (fieldName.equals("id") || fieldName.equals("apiId") || fieldName.equals("serialVersionUID")) {
					continue;
				}

				// 3. Get the path from DB
				Object dbPathValue = mappingField.get(mapConfig);
				if (dbPathValue == null)
					continue;

				String path = dbPathValue.toString();
				JsonNode jsonValue;

				// 4. Extract from JSON
				if (path.startsWith("/")) {
					jsonValue = root.at(path); // Nested
				} else {
					jsonValue = root.path(path); // Flat
				}

				// 5. Map to VehicleInfo
				if (!jsonValue.isMissingNode() && !jsonValue.isNull()) {
					setFieldValue(newVehicle, fieldName, jsonValue.asText());
				}
			} catch (Exception e) {
				System.out.println("Error mapping field: " + mappingField.getName());
			}
		}
		return dataRepository.save(newVehicle);
	}

	private void setFieldValue(Object object, String fieldName, String value) {
		try {
			// This looks for the EXACT same name in VehicleInfo.java
			Field field = object.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(object, value);
		} catch (NoSuchFieldException e) {
			System.out.println("Mismatch: Field '" + fieldName + "' exists in Mappings but NOT in VehicleInfo.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public VehicleInfo getVehicleInfo(String vehicleNumber, String vendorname) {
		return dataRepository.findByVehicleNumberAndVendorname(vehicleNumber, vendorname);
	}
}
