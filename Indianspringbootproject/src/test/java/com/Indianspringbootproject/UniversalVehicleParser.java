package com.Indianspringbootproject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

public class UniversalVehicleParser {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws Exception {

        // --- 1. MOCK DATA: ALL 4 SCENARIOS ---
        
        // Rajesh (Standard)
        String jsonRajesh = "{\"vehicleinfo\": {\"general\": [{\"number\": \"TS09EA1234\", \"MMVYInfo\": {\"make\": \"Tata\", \"model\": \"Safari\", \"year\": \"2023\"}}]}}";
        
        // Suresh (Custom key: 'pennadetials')
        String jsonSuresh = "{\"vahan_vehicle_info\": {\"pennadetials\": [{\"number\": \"KA01MG5566\", \"MMVYInfo\": {\"make\": \"Mahindra\", \"model\": \"XUV700\", \"year\": \"2024\"}}]}}";
        
        // Anjali (Custom key: '2W_vehicle_info')
        String jsonAnjali = "{\"2W_vehicle_info\": {\"general\": [{\"number\": \"DL04CJ9988\", \"MMVYInfo\": {\"make\": \"Hyundai\", \"model\": \"Creta\", \"year\": \"2022\"}}]}}";
        
        // Flow 4: DIFFICULT (Deeply nested with different keys)
        String jsonDifficult = "{\"payload\":{\"records\":[{\"unit_details\":{\"registrations\":[{\"id_values\":{\"license_plate\":\"AP31AQ5555\"}}]}}]}}";

        // --- 2. DYNAMIC PATHS (Instructions from DB) ---
        
        Map<String, String> rajeshPaths = Map.of(
            "make", "/vehicleinfo/general/0/MMVYInfo/make", 
            "number", "/vehicleinfo/general/0/number"
        );

        Map<String, String> sureshPaths = Map.of(
            "make", "/vahan_vehicle_info/pennadetials/0/MMVYInfo/make", 
            "number", "/vahan_vehicle_info/pennadetials/0/number"
        );

        Map<String, String> anjaliPaths = Map.of(
            "make", "/2W_vehicle_info/general/0/MMVYInfo/make", 
            "number", "/2W_vehicle_info/general/0/number"
        );

        Map<String, String> difficultPaths = Map.of(
            "make", "/payload/records/0/unit_details/registrations/0/id_values/license_plate", // Map to number field
            "number", "/payload/records/0/unit_details/registrations/0/id_values/license_plate"
        );

        // --- 3. EXECUTION: All 4 flows passing through the SAME method ---
        
        System.out.println("--- FLOW 1: RAJESH ---");
        processUniversalJson(jsonRajesh, rajeshPaths);

        System.out.println("\n--- FLOW 2: SURESH ---");
        processUniversalJson(jsonSuresh, sureshPaths);

        System.out.println("\n--- FLOW 3: ANJALI ---");
        processUniversalJson(jsonAnjali, anjaliPaths);

        System.out.println("\n--- FLOW 4: DIFFICULT ---");
        processUniversalJson(jsonDifficult, difficultPaths);
    }

    /**
     * THE STABLE METHOD: 
     * This logic handles any depth or key name because it uses Jackson's 'at' 
     * to navigate based on the path instructions provided.
     */
    public static void processUniversalJson(String rawJson, Map<String, String> paths) {
        try {
            JsonNode root = mapper.readTree(rawJson);
            Map<String, String> stableResponse = new HashMap<>();

            // Dynamic extraction: The GPS (paths) tells the code where to go
            paths.forEach((fieldName, path) -> {
                JsonNode valueNode = root.at(path);
                
                // If path is valid, extract text. Missing nodes are ignored (no crash).
                if (!valueNode.isMissingNode() && !valueNode.isNull()) {
                    stableResponse.put(fieldName, valueNode.asText());
                }
            });

            // Convert to a clean, flat JSON for Frontend/DB
            String finalJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(stableResponse);
            System.out.println(finalJson);

        } catch (Exception e) {
            System.err.println("Critical Parsing Error: " + e.getMessage());
        }
    }
}