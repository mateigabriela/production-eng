package ro.unibuc.prodeng.service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ro.unibuc.prodeng.exception.EntityNotFoundException;
import ro.unibuc.prodeng.model.CarEntity;
import ro.unibuc.prodeng.model.MechanicEntity;
import ro.unibuc.prodeng.model.OrderStatus;
import ro.unibuc.prodeng.model.PartEntity;
import ro.unibuc.prodeng.model.RequiredPart;
import ro.unibuc.prodeng.model.ServiceOrderEntity;
import ro.unibuc.prodeng.repository.PartRepository;
import ro.unibuc.prodeng.repository.ServiceOrderRepository;
import ro.unibuc.prodeng.request.CreateServiceOrderRequest;
import ro.unibuc.prodeng.request.RequiredPartRequest;
import ro.unibuc.prodeng.response.RequiredPartResponse;
import ro.unibuc.prodeng.response.ServiceOperationOptionResponse;
import ro.unibuc.prodeng.response.ServiceOrderResponse;

@Service
public class ServiceOrderService {

    private static final int DEFAULT_CONSULTATION_MINUTES = 30;
    private static final Map<String, Integer> OPERATION_DURATION_MINUTES = Map.ofEntries(
        Map.entry("WHEEL_CHANGE", 60),
        Map.entry("OIL_CHANGE", 45),
        Map.entry("BRAKE_INSPECTION", 45),
        Map.entry("BRAKE_PADS_REPLACEMENT", 90),
        Map.entry("ENGINE_DIAGNOSTIC", 60),
        Map.entry("BATTERY_REPLACEMENT", 30),
        Map.entry("AC_SERVICE", 75)
    );
    private static final Map<String, String> OPERATION_LABELS = Map.ofEntries(
        Map.entry("WHEEL_CHANGE", "Schimbare roti"),
        Map.entry("OIL_CHANGE", "Schimb ulei"),
        Map.entry("BRAKE_INSPECTION", "Inspectie frane"),
        Map.entry("BRAKE_PADS_REPLACEMENT", "Schimb placute frana"),
        Map.entry("ENGINE_DIAGNOSTIC", "Diagnostic motor"),
        Map.entry("BATTERY_REPLACEMENT", "Schimb baterie"),
        Map.entry("AC_SERVICE", "Service aer conditionat")
    );

    private final AutoServiceCatalogService catalogService;
    private final ServiceOrderRepository serviceOrderRepository;
    private final PartRepository partRepository;

    @Autowired
    public ServiceOrderService(
            AutoServiceCatalogService catalogService,
            ServiceOrderRepository serviceOrderRepository,
            PartRepository partRepository) {
        this.catalogService = catalogService;
        this.serviceOrderRepository = serviceOrderRepository;
        this.partRepository = partRepository;
    }

    public ServiceOrderResponse createServiceOrder(CreateServiceOrderRequest request) {
        catalogService.ensureClientExists(request.clientId());
        MechanicEntity mechanic = catalogService.ensureMechanicExists(request.mechanicId());
        CarEntity car = catalogService.ensureCarExists(request.carId());
        String carName = formatCarName(car);

        List<String> selectedOperations = normalizeOperations(request.selectedOperations());
        int estimatedDurationMinutes = estimateDurationMinutes(selectedOperations, request.consultationRequested());
        LocalDateTime estimatedEndAt = request.scheduledAt().plus(Duration.ofMinutes(estimatedDurationMinutes));

        validateMechanicAvailability(mechanic, request.scheduledAt(), estimatedEndAt);

        BigDecimal finalLaborCost = request.laborCost() != null && request.laborCost().signum() > 0
                ? request.laborCost()
                : estimatePrice(estimatedDurationMinutes);

        List<PartEntity> partsToUpdate = new ArrayList<>();
        List<RequiredPart> requiredParts = new ArrayList<>();
        BigDecimal totalPartsCost = BigDecimal.ZERO;

        for (RequiredPartRequest requiredPartRequest : request.requiredParts()) {
            PartEntity part = catalogService.ensurePartExists(requiredPartRequest.partId());
            if (part.availableStock() < requiredPartRequest.quantity()) {
                throw new IllegalArgumentException("Not enough stock for part " + part.id()
                        + ". Required: " + requiredPartRequest.quantity()
                        + ", available: " + part.availableStock());
            }

            partsToUpdate.add(new PartEntity(
                    part.id(),
                    part.name(),
                    part.availableStock() - requiredPartRequest.quantity(),
                    part.unitPrice(),
                    part.supplierId()
            ));

            requiredParts.add(new RequiredPart(part.id(), requiredPartRequest.quantity(), part.unitPrice()));
            totalPartsCost = totalPartsCost.add(part.unitPrice().multiply(BigDecimal.valueOf(requiredPartRequest.quantity())));
        }

        partRepository.saveAll(partsToUpdate);

        BigDecimal totalCost = finalLaborCost.add(totalPartsCost);
        ServiceOrderEntity saved = serviceOrderRepository.save(ServiceOrderEntity.createFull(
                null,
                request.clientId(),
                request.carId(),
                carName,
                request.mechanicId(),
                request.serviceName(),
                request.description(),
                finalLaborCost,
                totalPartsCost,
                totalCost,
                requiredParts,
                selectedOperations,
                request.consultationRequested(),
                estimatedDurationMinutes,
                request.scheduledAt(),
                estimatedEndAt,
                null,
                OrderStatus.IN_PROGRESS
            ));

        return toResponse(saved);
    }

    public ServiceOrderResponse completeOrder(String orderId) {
        ServiceOrderEntity existing = getOrderEntity(orderId);
        if (existing.status() == OrderStatus.COMPLETED) {
            throw new IllegalArgumentException("Order is already completed: " + orderId);
        }

        ServiceOrderEntity completed = ServiceOrderEntity.createFull(
                existing.id(),
                existing.clientId(),
                existing.carId(),
                existing.carName(),
                existing.mechanicId(),
                existing.serviceName(),
                existing.description(),
                existing.laborCost(),
                existing.partsCost(),
                existing.totalCost(),
                existing.requiredParts(),
                existing.selectedOperations(),
                existing.consultationRequested(),
                existing.estimatedDurationMinutes(),
                existing.scheduledAt(),
                existing.estimatedEndAt(),
                LocalDateTime.now(),
                OrderStatus.COMPLETED
        );

        return toResponse(serviceOrderRepository.save(completed));
    }

    public ServiceOrderResponse getOrderById(String orderId) {
        return toResponse(getOrderEntity(orderId));
    }

    public List<ServiceOrderResponse> getOrdersByStatus(OrderStatus status) {
        return serviceOrderRepository.findByStatus(status).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ServiceOperationOptionResponse> getServiceOperationCatalog() {
        List<ServiceOperationOptionResponse> options = new ArrayList<>();
        for (Map.Entry<String, Integer> operation : OPERATION_DURATION_MINUTES.entrySet()) {
            options.add(new ServiceOperationOptionResponse(
                    operation.getKey(),
                    OPERATION_LABELS.getOrDefault(operation.getKey(), operation.getKey()),
                    operation.getValue()
            ));
        }
        options.add(new ServiceOperationOptionResponse(
                "CONSULTATION",
                "Consult initial (nu stiu exact problema)",
                DEFAULT_CONSULTATION_MINUTES
        ));
        return options;
    }

    public List<MechanicEntity> getAvailableMechanics(LocalDateTime startTime, String operation) {
        int serviceDurationMinutes = OPERATION_DURATION_MINUTES.getOrDefault(operation, DEFAULT_CONSULTATION_MINUTES);
        LocalDateTime endTime = startTime.plusMinutes(serviceDurationMinutes + 15);

        List<MechanicEntity> availableMechanics = new ArrayList<>();
        for (MechanicEntity mechanic : catalogService.getAllMechanics()) {
            if (isMechanicAvailable(mechanic, startTime, endTime)) {
                availableMechanics.add(mechanic);
            }
        }

        return availableMechanics;
    }

    private ServiceOrderEntity getOrderEntity(String orderId) {
        return serviceOrderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("ServiceOrder " + orderId));
    }

    private ServiceOrderResponse toResponse(ServiceOrderEntity serviceOrderEntity) {
        List<RequiredPartResponse> requiredPartResponses = serviceOrderEntity.requiredParts().stream()
                .map(part -> new RequiredPartResponse(part.partId(), part.quantity(), part.unitPrice()))
                .toList();

        return ServiceOrderResponse.createFull(
                serviceOrderEntity.id(),
                serviceOrderEntity.clientId(),
                serviceOrderEntity.carId(),
                serviceOrderEntity.carName(),
                serviceOrderEntity.mechanicId(),
                serviceOrderEntity.serviceName(),
                serviceOrderEntity.description(),
                serviceOrderEntity.laborCost(),
                serviceOrderEntity.partsCost(),
                serviceOrderEntity.totalCost(),
                requiredPartResponses,
                serviceOrderEntity.selectedOperations(),
                serviceOrderEntity.consultationRequested(),
                serviceOrderEntity.estimatedDurationMinutes(),
                serviceOrderEntity.scheduledAt(),
                serviceOrderEntity.estimatedEndAt(),
                serviceOrderEntity.completedAt(),
                serviceOrderEntity.status()
        );
    }

    private List<String> normalizeOperations(List<String> selectedOperations) {
        if (selectedOperations == null || selectedOperations.isEmpty()) {
            return List.of();
        }

        Map<String, String> unique = new LinkedHashMap<>();
        for (String selectedOperation : selectedOperations) {
            if (selectedOperation == null || selectedOperation.isBlank()) {
                continue;
            }
            String normalized = selectedOperation.trim().toUpperCase();
            if (OPERATION_DURATION_MINUTES.containsKey(normalized)) {
                unique.put(normalized, normalized);
            }
        }

        return new ArrayList<>(unique.values());
    }

    private int estimateDurationMinutes(List<String> selectedOperations, boolean consultationRequested) {
        int totalDurationMinutes = selectedOperations.stream()
                .map(operationCode -> OPERATION_DURATION_MINUTES.getOrDefault(operationCode, 0))
                .reduce(0, Integer::sum);

        if (consultationRequested) {
            totalDurationMinutes += DEFAULT_CONSULTATION_MINUTES;
        }

        return totalDurationMinutes > 0 ? totalDurationMinutes : DEFAULT_CONSULTATION_MINUTES;
    }

    private BigDecimal estimatePrice(int estimatedDurationMinutes) {
        BigDecimal hourlyRate = BigDecimal.valueOf(50);
        BigDecimal hours = BigDecimal.valueOf(estimatedDurationMinutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
        return hourlyRate.multiply(hours);
    }

    private void validateMechanicAvailability(MechanicEntity mechanic, LocalDateTime startTime, LocalDateTime endTime) {
        if (!isMechanicAvailable(mechanic, startTime, endTime)) {
            throw new IllegalArgumentException("Mechanic " + mechanic.id() + " is not available during this time slot");
        }
    }

    private boolean isMechanicAvailable(MechanicEntity mechanic, LocalDateTime startTime, LocalDateTime endTime) {
        if (!isWithinWorkingHours(mechanic, startTime, endTime)) {
            return false;
        }

        List<ServiceOrderEntity> existingOrders = serviceOrderRepository.findByMechanicIdAndStatus(mechanic.id(), OrderStatus.IN_PROGRESS);
        for (ServiceOrderEntity order : existingOrders) {
            if (timeSlotOverlaps(startTime, endTime, order.scheduledAt(), order.estimatedEndAt())) {
                return false;
            }
        }

        return true;
    }

    private boolean isWithinWorkingHours(MechanicEntity mechanic, LocalDateTime startTime, LocalDateTime endTime) {
        DayOfWeek dayOfWeek = startTime.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }

        LocalTime workingStart = parseWorkingTime(mechanic.workingStartTime(), LocalTime.of(8, 0));
        LocalTime workingEnd = parseWorkingTime(mechanic.workingEndTime(), LocalTime.of(17, 0));
        return !startTime.toLocalTime().isBefore(workingStart) && !endTime.toLocalTime().isAfter(workingEnd);
    }

    private String formatCarName(CarEntity car) {
        return car.brand() + " " + car.model() + " [" + car.plateNumber() + "]";
    }

    private LocalTime parseWorkingTime(String value, LocalTime defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return LocalTime.parse(value.trim());
    }

    private boolean timeSlotOverlaps(LocalDateTime newStart, LocalDateTime newEnd, LocalDateTime existingStart, LocalDateTime existingEnd) {
        LocalDateTime safeExistingEnd = existingEnd != null ? existingEnd : existingStart;
        return newStart.isBefore(safeExistingEnd) && newEnd.isAfter(existingStart);
    }

    public List<Map<String, Object>> getAvailableSlots(String mechanicId, LocalDateTime dateStart, String operation) {
        MechanicEntity mechanic = catalogService.getMechanicById(mechanicId);
        if (dateStart.getDayOfWeek() == DayOfWeek.SATURDAY || dateStart.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return List.of();
        }
        int serviceDurationMinutes = OPERATION_DURATION_MINUTES.getOrDefault(operation, 30);
        int bufferMinutes = 15;
        int totalDurationMinutes = serviceDurationMinutes + bufferMinutes;
        
        List<Map<String, Object>> slots = new ArrayList<>();
        LocalTime workingStart = parseWorkingTime(mechanic.workingStartTime(), LocalTime.of(8, 0));
        LocalTime workingEnd = parseWorkingTime(mechanic.workingEndTime(), LocalTime.of(17, 0));
        LocalDateTime slotTime = dateStart.toLocalDate().atTime(workingStart);
        LocalDateTime dayEnd = dateStart.toLocalDate().atTime(workingEnd);

        List<ServiceOrderEntity> existingOrders = serviceOrderRepository.findByMechanicIdAndStatus(mechanicId, OrderStatus.IN_PROGRESS);
        
        while (slotTime.isBefore(dayEnd)) {
            LocalDateTime slotEnd = slotTime.plusMinutes(totalDurationMinutes);
            if (slotEnd.isAfter(dayEnd)) {
                break;
            }
            
            boolean isAvailable = true;
            for (ServiceOrderEntity order : existingOrders) {
                if (timeSlotOverlaps(slotTime, slotEnd, order.scheduledAt(), order.estimatedEndAt())) {
                    isAvailable = false;
                    break;
                }
            }

            Map<String, Object> slot = new java.util.LinkedHashMap<>();
            slot.put("startTime", slotTime);
            slot.put("endTime", slotEnd);
            slot.put("available", isAvailable);
            slot.put("duration", serviceDurationMinutes);
            slots.add(slot);

            slotTime = slotTime.plusMinutes(30);
        }

        return slots;
    }

    public List<Map<String, Object>> getAvailableSlots(String mechanicId, LocalDateTime dateStart) {
        return getAvailableSlots(mechanicId, dateStart, "");
    }
}
