package com.stablespringbootproject.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stablespringbootproject.Dto.Stablerequest;
import com.stablespringbootproject.Dto.Stableresponse;
import com.stablespringbootproject.Entity.*;
import com.stablespringbootproject.repository.*;

@Service
public class Stableservice {

    private final RestTemplate restTemplate;
    private final Countryrepo countryRepo;
    private final Countryservicerepo stableRepo;
    private final Vendorrepo vendorRepo;
    private final Vendorapirepo vendorApiRepository;
    private final VendorJsonMappingrepo jsonMappingRepo;
    private final Vehiclerequestmappingrepo vehicleRequestMappingRepo;
    private final Vehicleresponcemappingsrepo nestedMappingRepo;

    // Added ObjectMapper for dynamic path navigation
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Stableservice(RestTemplate restTemplate, Countryrepo countryRepo, Countryservicerepo stableRepo,
            Vendorrepo vendorRepo, Vendorapirepo vendorApiRepository, VendorJsonMappingrepo jsonMappingRepo,
            Vehiclerequestmappingrepo vehicleRequestMappingRepo, Vehicleresponcemappingsrepo nestedMappingRepo) {
        this.restTemplate = restTemplate;
        this.countryRepo = countryRepo;
        this.stableRepo = stableRepo;
        this.vendorRepo = vendorRepo;
        this.vendorApiRepository = vendorApiRepository;
        this.jsonMappingRepo = jsonMappingRepo;
        this.vehicleRequestMappingRepo = vehicleRequestMappingRepo;
        this.nestedMappingRepo = nestedMappingRepo;
    }

    public Stableresponse fetchVehicle(Stablerequest request) {
        Countryentity country = countryRepo.findByCountryCode(request.getCountry())
                .orElseThrow(() -> new RuntimeException("Country Not Found"));

        Countryserviceentity service = stableRepo
                .findFirstByCountryCodeAndActiveTrue(country.getCountryCode())
                .orElseThrow(() -> new RuntimeException("No active service found"));

        Vendorentity vendor = vendorRepo.findByVendorNameIgnoreCaseAndPhoneNumber(request.getVendorname(),
                request.getPhone_number());

        if (vendor == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor Not Found");

        List<Vendorapis> vendorApis = vendorApiRepository.findByVendorIdAndApiType(vendor.getId(),
                request.getApi_usage_type());

        for (Vendorapis vendorApi : vendorApis) {
            List<vehiclerequestmapping> mappings = vehicleRequestMappingRepo.findByVendorIdAndApiId(vendor.getId(),
                    vendorApi.getApiId());

            Map<String, Object> vendorResponse = callVendor(service, vendorApi, mappings, request);

            if (vendorResponse != null) {
                List<?> activeMappings = null;

                // Fetch flat response mappings according to the vendor ID
                List<Vehicleresponcemapping> flatMappings = jsonMappingRepo.findByVendorId(vendor.getId());
                if (flatMappings == null || flatMappings.isEmpty()) {
                    flatMappings = jsonMappingRepo.findByApiId(vendorApi.getApiId());
                }

                if (flatMappings != null && !flatMappings.isEmpty()) {
                    activeMappings = flatMappings;
                } else {
                    // Check for nested mappings if no flat mapping exists
                    List<Vehicleresponcemappings> nestedMappings = nestedMappingRepo.findByVendorId(vendor.getId());
                    if (nestedMappings == null || nestedMappings.isEmpty()) {
                        nestedMappings = nestedMappingRepo.findByApiId(vendorApi.getApiId());
                    }
                    activeMappings = nestedMappings;
                }

                Stableresponse response = mapVendorResponse(vendorResponse, activeMappings);
                response.setCountry(country.getCountryCode());

                // Field filtering logic remains same
                if (request.getFields() != null && !request.getFields().isEmpty()) {
                    Map<String, String> filtered = new HashMap<>();
                    for (String field : request.getFields()) {
                        if (response.getVehicleDetails().containsKey(field)) {
                            filtered.put(field, response.getVehicleDetails().get(field));
                        }
                    }
                    response.setVehicleDetails(filtered);
                }
                return response;
            }
        }
        throw new RuntimeException("Vehicle not found");
    }

    /**
     * DYNAMIC RESPONSE MAPPING:
     * Uses Jackson Pointers to navigate any JSON structure based on DB paths.
     */
    private Stableresponse mapVendorResponse(Map<String, Object> vendorResponse,
            List<?> mappings) {

        Stableresponse response = new Stableresponse();
        Map<String, String> vehicleDetails = new HashMap<>();

        if (mappings == null || mappings.isEmpty()) {
            System.out.println("DEBUG: No mappings found in the database for this vendor/api!");
            return response;
        }

        Object mapConfig = mappings.get(0);
        System.out.println("DEBUG: Found mappings configuration class: " + mapConfig.getClass().getSimpleName());

        // Convert Map to JsonNode tree for deep path searching
        JsonNode root = objectMapper.valueToTree(vendorResponse);
        System.out.println("DEBUG: Vendor JSON Response -> " + root.toString());

        try {
            for (Field field : mapConfig.getClass().getDeclaredFields()) {
                // Skip ID/Metadata fields
                if (Set.of("mappingId", "id", "apiId", "vendorId", "countryId").contains(field.getName()))
                    continue;

                field.setAccessible(true);
                Object pathInDb = field.get(mapConfig);
                System.out.println(
                        "DEBUG: Examining field -> " + field.getName() + " with DB path mapping -> " + pathInDb);

                if (pathInDb != null) {
                    String path = pathInDb.toString();
                    // Auto-prepend '/' if missing — supports both "make" and
                    // "/vehicleinfo/general/0/make"
                    if (!path.startsWith("/")) {
                        path = "/" + path;
                    }

                    // Navigate to the path (e.g., /vehicleinfo/general/0/number)
                    JsonNode valueNode = root.at(path);
                    System.out.println("DEBUG: Json Pointer Path -> " + path + " result Node -> " + valueNode);

                    if (!valueNode.isMissingNode() && !valueNode.isNull()) {
                        vehicleDetails.put(field.getName(), valueNode.asText());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("DEBUG: Final vehicleDetails mapped -> " + vehicleDetails);
        response.setVehicleDetails(vehicleDetails);
        return response;
    }

    // --- UTILS (Unchanged Logic) ---

    private Map<String, Object> callVendor(Countryserviceentity service, Vendorapis vendorApi,
            List<vehiclerequestmapping> mappings, Stablerequest request) {
        String url = service.getBaseUrl() + vendorApi.getApiUrl();
        Map<String, String> requestMap = convertRequestToMap(request);
        Map<String, String> pathVars = new HashMap<>();
        Map<String, String> queryParams = new HashMap<>();
        Map<String, String> headersMap = new HashMap<>();
        Map<String, Object> bodyJson = new HashMap<>();

        for (vehiclerequestmapping m : mappings) {
            String value = m.getConstantValue() != null && !m.getConstantValue().isEmpty()
                    ? m.getConstantValue()
                    : getIgnoreCase(requestMap, m.getStableField());
            if (value == null)
                continue;
            switch (m.getLocation()) {
                case PATH -> pathVars.put(m.getExternalName(), value);
                case QUERY -> queryParams.put(m.getExternalName(), value);
                case HEADER -> headersMap.put(m.getExternalName(), value);
                case BODY_JSON -> bodyJson.put(m.getExternalName(), value);
            }
        }

        url = resolveUrl(url, pathVars);
        if (!queryParams.isEmpty()) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
            queryParams.forEach(builder::queryParam);
            url = builder.toUriString();
        }

        System.out.println("DEBUG: Resolved URL -> " + url);
        System.out.println("DEBUG: PathVars -> " + pathVars);
        System.out.println("DEBUG: QueryParams -> " + queryParams);
        System.out.println("DEBUG: HeadersMap -> " + headersMap);
        System.out.println("DEBUG: BodyJson being sent -> " + bodyJson);
        System.out.println("DEBUG: RequestMap -> " + requestMap);
        for (vehiclerequestmapping m : mappings) {
            System.out.println("DEBUG: Mapping -> DB StableField=" + m.getStableField()
                    + ", ExtName=" + m.getExternalName()
                    + ", Loc=" + m.getLocation()
                    + ", Const=" + m.getConstantValue());
        }

        HttpHeaders headers = new HttpHeaders();
        headersMap.forEach(headers::add);
        HttpMethod method = HttpMethod.valueOf(vendorApi.getHttpMethod().toUpperCase());
        HttpEntity<?> entity = method == HttpMethod.GET
                ? new HttpEntity<>(headers)
                : new HttpEntity<>(bodyJson, headers);

        try {
            ResponseEntity<Map> resp = restTemplate.exchange(URI.create(url), method, entity, Map.class);
            System.out.println("DEBUG: Response status -> " + resp.getStatusCode());
            System.out.println("DEBUG: Response body -> " + resp.getBody());
            return resp.getBody();
        } catch (Exception e) {
            System.out.println("DEBUG: Exception calling vendor -> " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String resolveUrl(String url, Map<String, String> pathVars) {
        Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(url);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String val = getIgnoreCase(pathVars, matcher.group(1));
            matcher.appendReplacement(result, URLEncoder.encode(val != null ? val : "", StandardCharsets.UTF_8));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private Map<String, String> convertRequestToMap(Object obj) {
        Map<String, String> map = new HashMap<>();
        for (Field field : obj.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object val = field.get(obj);
                if (val != null)
                    map.put(field.getName(), val.toString());
            } catch (Exception ignored) {
            }
        }
        return map;
    }

    private String getIgnoreCase(Map<String, String> map, String key) {
        return map.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(key))
                .map(Map.Entry::getValue)
                .findFirst().orElse(null);
    }

    private static String firstNonBlank(String... values) {
        for (String v : values)
            if (v != null && !v.trim().isEmpty())
                return v.trim();
        return null;
    }
}