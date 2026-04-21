package ro.unibuc.prodeng.controller;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import ro.unibuc.prodeng.IntegrationTestBase;
import ro.unibuc.prodeng.model.CarEntity;
import ro.unibuc.prodeng.model.ClientEntity;
import ro.unibuc.prodeng.model.MechanicEntity;
import ro.unibuc.prodeng.model.OrderStatus;
import ro.unibuc.prodeng.model.PartEntity;
import ro.unibuc.prodeng.model.ServiceOrderEntity;
import ro.unibuc.prodeng.model.SupplierEntity;
import ro.unibuc.prodeng.repository.CarRepository;
import ro.unibuc.prodeng.repository.ClientRepository;
import ro.unibuc.prodeng.repository.MechanicRepository;
import ro.unibuc.prodeng.repository.PartRepository;
import ro.unibuc.prodeng.repository.ServiceOrderRepository;
import ro.unibuc.prodeng.repository.SupplierRepository;

@DisplayName("Auto Service Integration Tests")
class AutoServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ServiceOrderRepository serviceOrderRepository;

    @Autowired
    private PartRepository partRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private MechanicRepository mechanicRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @BeforeEach
    void cleanUp() {
        serviceOrderRepository.deleteAll();
        partRepository.deleteAll();
        carRepository.deleteAll();
        clientRepository.deleteAll();
        mechanicRepository.deleteAll();
        supplierRepository.deleteAll();
    }

    @Test
    void createServiceOrder_shouldDeductPartStockAndPersistOrder() throws Exception {
        seedCatalog(10);

        String createOrderPayload = """
                {
                                                                        "clientId": "client-1",
                  "carId": "car-1",
                  "mechanicId": "mechanic-1",
                  "serviceName": "Revizie completa",
                  "description": "Schimb ulei si filtru",
                  "laborCost": 200.0,
                  "scheduledAt": "2026-03-05T10:00:00",
                  "requiredParts": [
                    {
                      "partId": "part-1",
                      "quantity": 2
                    }
                  ]
                }
                """;

        String responseBody = mockMvc.perform(post("/api/service-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.partsCost").value(90))
                .andExpect(jsonPath("$.totalCost").value(290))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String orderId = objectMapper.readTree(responseBody).get("id").asText();

        PartEntity updatedPart = partRepository.findById("part-1").orElseThrow();
        assertEquals(8, updatedPart.availableStock());

        ServiceOrderEntity savedOrder = serviceOrderRepository.findById(orderId).orElseThrow();
        assertEquals(OrderStatus.IN_PROGRESS, savedOrder.status());
        assertEquals(BigDecimal.valueOf(290.0), savedOrder.totalCost());
        assertEquals(1, savedOrder.requiredParts().size());
        assertEquals("part-1", savedOrder.requiredParts().getFirst().partId());
    }

    @Test
    void receiveDelivery_shouldIncreasePartStockAndPersist() throws Exception {
        seedCatalog(5);

        String deliveryPayload = """
                {
                  "partId": "part-1",
                  "supplierId": "supplier-1",
                  "quantity": 7,
                  "deliveredAt": "2026-03-05T08:30:00"
                }
                """;

        mockMvc.perform(patch("/api/deliveries/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deliveryPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("part-1"))
                .andExpect(jsonPath("$.availableStock").value(12));

        PartEntity updatedPart = partRepository.findById("part-1").orElseThrow();
        assertEquals(12, updatedPart.availableStock());
    }

    @Test
    void completeOrder_shouldPersistCompletedStatusAndBeReturnedByStatusFilter() throws Exception {
        seedCatalog(10);

        String createOrderPayload = """
                {
                                                                        "clientId": "client-1",
                  "carId": "car-1",
                  "mechanicId": "mechanic-1",
                  "serviceName": "Revizie completa",
                  "description": "Schimb ulei si filtru",
                  "laborCost": 200.0,
                  "scheduledAt": "2026-03-05T10:00:00",
                  "requiredParts": [
                    {
                      "partId": "part-1",
                      "quantity": 2
                    }
                  ]
                }
                """;

        String responseBody = mockMvc.perform(post("/api/service-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderPayload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String orderId = objectMapper.readTree(responseBody).get("id").asText();

        mockMvc.perform(patch("/api/service-orders/" + orderId + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").exists());

        ServiceOrderEntity updatedOrder = serviceOrderRepository.findById(orderId).orElseThrow();
        assertEquals(OrderStatus.COMPLETED, updatedOrder.status());

        mockMvc.perform(get("/api/service-orders").param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(orderId))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void receiveDelivery_whenSupplierDoesNotMatch_shouldReturnBadRequestWithoutChangingStock() throws Exception {
        seedCatalog(5);
        supplierRepository.save(new SupplierEntity(
                "supplier-2",
                "Another Supplier",
                "Bd. Unirii 10",
                "0210000000"
        ));

        String deliveryPayload = """
                {
                  "partId": "part-1",
                  "supplierId": "supplier-2",
                  "quantity": 7,
                  "deliveredAt": "2026-03-05T08:30:00"
                }
                """;

        mockMvc.perform(patch("/api/deliveries/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deliveryPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        PartEntity unchangedPart = partRepository.findById("part-1").orElseThrow();
        assertEquals(5, unchangedPart.availableStock());
    }

    @Test
    void createClientCarSupplierPartFlow_shouldPersistCatalogData() throws Exception {
        String clientPayload = """
                {
                  "firstName": "Mihai",
                  "lastName": "Ionescu",
                  "phone": "0722111222",
                  "email": "mihai.flow@example.com",
                  "address": "Bucuresti"
                }
                """;

        String clientResponse = mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(clientPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("mihai.flow@example.com"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String clientId = objectMapper.readTree(clientResponse).get("id").asText();

        String carPayload = """
                {
                  "brand": "Dacia",
                  "model": "Logan",
                  "fabricationYear": 2020,
                  "plateNumber": "B-55-FLW",
                  "clientId": "%s"
                }
                """.formatted(clientId);

        mockMvc.perform(post("/api/cars")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(carPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clientId").value(clientId));

        String supplierPayload = """
                {
                  "name": "Flow Supplier",
                  "address": "Calea Victoriei 1",
                  "phone": "0211234567"
                }
                """;

        String supplierResponse = mockMvc.perform(post("/api/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(supplierPayload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String supplierId = objectMapper.readTree(supplierResponse).get("id").asText();

        String partPayload = """
                {
                  "name": "Filtru aer",
                  "availableStock": 9,
                  "unitPrice": 55.0,
                  "supplierId": "%s"
                }
                """.formatted(supplierId);

        mockMvc.perform(post("/api/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(partPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.supplierId").value(supplierId))
                .andExpect(jsonPath("$.availableStock").value(9));

        mockMvc.perform(get("/api/clients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/cars/by-client/" + clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/suppliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/parts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void createServiceOrder_whenStockInsufficient_shouldReturnBadRequestWithoutChangingStock() throws Exception {
        seedCatalog(1);

        String createOrderPayload = """
                {
                                                                        "clientId": "client-1",
                  "carId": "car-1",
                  "mechanicId": "mechanic-1",
                  "serviceName": "Revizie completa",
                  "description": "Schimb ulei si filtru",
                  "laborCost": 200.0,
                  "scheduledAt": "2026-03-05T10:00:00",
                  "requiredParts": [
                    {
                      "partId": "part-1",
                      "quantity": 2
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/service-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createOrderPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        PartEntity unchangedPart = partRepository.findById("part-1").orElseThrow();
        assertEquals(1, unchangedPart.availableStock());
        assertTrue(serviceOrderRepository.findAll().isEmpty());
    }

    private void seedCatalog(int partStock) {
        clientRepository.save(new ClientEntity(
                "client-1",
                "Mihai",
                "Ionescu",
                "0722111222",
                "mihai@example.com",
                "Bucuresti"
        ));

        carRepository.save(new CarEntity(
                "car-1",
                "Dacia",
                "Duster",
                2021,
                "B-42-XYZ",
                "client-1"
        ));

        mechanicRepository.save(new MechanicEntity(
                "mechanic-1",
                "Andrei",
                "Pop",
                "0733222111"
        ));

        supplierRepository.save(new SupplierEntity(
                "supplier-1",
                "Auto Parts Distribution",
                "Bd. Timisoara 8",
                "0215552233"
        ));

        partRepository.save(new PartEntity(
                "part-1",
                "Filtru ulei",
                partStock,
                BigDecimal.valueOf(45.0),
                "supplier-1"
        ));
    }
}
