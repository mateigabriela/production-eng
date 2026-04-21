package ro.unibuc.prodeng.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import ro.unibuc.prodeng.model.OrderStatus;
import ro.unibuc.prodeng.model.MechanicEntity;
import ro.unibuc.prodeng.request.CreateServiceOrderRequest;
import ro.unibuc.prodeng.response.ServiceOperationOptionResponse;
import ro.unibuc.prodeng.response.ServiceOrderResponse;
import ro.unibuc.prodeng.service.ServiceOrderService;

@RestController
@RequestMapping("/api/service-orders")
public class ServiceOrderController {

    @Autowired
    private ServiceOrderService serviceOrderService;

    @PostMapping
    public ResponseEntity<ServiceOrderResponse> createServiceOrder(
            @Valid @RequestBody CreateServiceOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceOrderService.createServiceOrder(request));
    }

    @PatchMapping("/{orderId}/complete")
    public ResponseEntity<ServiceOrderResponse> completeOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(serviceOrderService.completeOrder(orderId));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ServiceOrderResponse> getOrderById(@PathVariable String orderId) {
        return ResponseEntity.ok(serviceOrderService.getOrderById(orderId));
    }

    @GetMapping
    public ResponseEntity<List<ServiceOrderResponse>> getOrdersByStatus(@RequestParam OrderStatus status) {
        return ResponseEntity.ok(serviceOrderService.getOrdersByStatus(status));
    }

    @GetMapping("/operation-catalog")
    public ResponseEntity<List<ServiceOperationOptionResponse>> getOperationCatalog() {
        return ResponseEntity.ok(serviceOrderService.getServiceOperationCatalog());
    }

    @GetMapping("/available-slots")
    public ResponseEntity<List<Map<String, Object>>> getAvailableSlots(
            @RequestParam String mechanicId,
            @RequestParam String date,
            @RequestParam(required = false, defaultValue = "") String operation) {
        LocalDateTime dateStart = LocalDate.parse(date).atStartOfDay();
        return ResponseEntity.ok(serviceOrderService.getAvailableSlots(mechanicId, dateStart, operation));
    }

    @GetMapping("/available-mechanics")
    public ResponseEntity<List<MechanicEntity>> getAvailableMechanics(
            @RequestParam String date,
            @RequestParam String time,
            @RequestParam(required = false, defaultValue = "") String operation) {
        LocalDateTime startTime = LocalDateTime.of(LocalDate.parse(date), LocalTime.parse(time));
        return ResponseEntity.ok(serviceOrderService.getAvailableMechanics(startTime, operation));
    }
}
