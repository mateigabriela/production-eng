package ro.unibuc.prodeng.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import ro.unibuc.prodeng.model.OrderStatus;

public record ServiceOrderResponse(
        String id,
        String clientId,
        String carId,
        String carName,
        String mechanicId,
        String serviceName,
        String description,
        BigDecimal laborCost,
        BigDecimal partsCost,
        BigDecimal totalCost,
        List<RequiredPartResponse> requiredParts,
        List<String> selectedOperations,
        boolean consultationRequested,
        Integer estimatedDurationMinutes,
        LocalDateTime scheduledAt,
        LocalDateTime estimatedEndAt,
        LocalDateTime completedAt,
        OrderStatus status
) {

    public ServiceOrderResponse {
        selectedOperations = selectedOperations == null ? List.of() : selectedOperations;
    }

    public ServiceOrderResponse(
            String id,
            String carId,
            String mechanicId,
            String serviceName,
            String description,
            BigDecimal laborCost,
            BigDecimal partsCost,
            BigDecimal totalCost,
            List<RequiredPartResponse> requiredParts,
            LocalDateTime scheduledAt,
            LocalDateTime completedAt,
            OrderStatus status
    ) {
        this(
                id,
                carId,
                carId,
                carId,
                mechanicId,
                serviceName,
                description,
                laborCost,
                partsCost,
                totalCost,
                requiredParts,
                List.of(),
                false,
                0,
                scheduledAt,
                scheduledAt,
                completedAt,
                status
        );
    }

    public ServiceOrderResponse(
            String id,
            String clientId,
            String carId,
            String carName,
            String mechanicId,
            String serviceName,
            String description,
            BigDecimal laborCost,
            BigDecimal partsCost,
            BigDecimal totalCost,
            List<RequiredPartResponse> requiredParts,
            LocalDateTime scheduledAt,
            LocalDateTime completedAt,
            OrderStatus status
    ) {
        this(
                id,
                clientId,
                carId,
                carName,
                mechanicId,
                serviceName,
                description,
                laborCost,
                partsCost,
                totalCost,
                requiredParts,
                List.of(),
                false,
                0,
                scheduledAt,
                scheduledAt,
                completedAt,
                status
        );
    }

        public static ServiceOrderResponse createFull(
                        String id,
                        String clientId,
                        String carId,
                        String carName,
                        String mechanicId,
                        String serviceName,
                        String description,
                        BigDecimal laborCost,
                        BigDecimal partsCost,
                        BigDecimal totalCost,
                        List<RequiredPartResponse> requiredParts,
                        List<String> selectedOperations,
                        boolean consultationRequested,
                        Integer estimatedDurationMinutes,
                        LocalDateTime scheduledAt,
                        LocalDateTime estimatedEndAt,
                        LocalDateTime completedAt,
                        OrderStatus status
        ) {
                return new ServiceOrderResponse(
                                id,
                                clientId,
                                carId,
                                carName,
                                mechanicId,
                                serviceName,
                                description,
                                laborCost,
                                partsCost,
                                totalCost,
                                requiredParts,
                                selectedOperations,
                                consultationRequested,
                                estimatedDurationMinutes,
                                scheduledAt,
                                estimatedEndAt,
                                completedAt,
                                status
                );
        }
}
