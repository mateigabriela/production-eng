package ro.unibuc.prodeng.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "service_orders")
public record ServiceOrderEntity(
        @Id String id,
        String clientId,
        String carId,
        String carName,
        String mechanicId,
        String serviceName,
        String description,
        BigDecimal laborCost,
        BigDecimal partsCost,
        BigDecimal totalCost,
        List<RequiredPart> requiredParts,
        List<String> selectedOperations,
        boolean consultationRequested,
        Integer estimatedDurationMinutes,
        LocalDateTime scheduledAt,
        LocalDateTime estimatedEndAt,
        LocalDateTime completedAt,
        OrderStatus status
) {

    public ServiceOrderEntity {
        selectedOperations = selectedOperations == null ? List.of() : selectedOperations;
    }

    public ServiceOrderEntity(
            String id,
            String carId,
            String mechanicId,
            String serviceName,
            String description,
            BigDecimal laborCost,
            BigDecimal partsCost,
            BigDecimal totalCost,
            List<RequiredPart> requiredParts,
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

    public ServiceOrderEntity(
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
            List<RequiredPart> requiredParts,
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

        public static ServiceOrderEntity createFull(
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
                        List<RequiredPart> requiredParts,
                        List<String> selectedOperations,
                        boolean consultationRequested,
                        Integer estimatedDurationMinutes,
                        LocalDateTime scheduledAt,
                        LocalDateTime estimatedEndAt,
                        LocalDateTime completedAt,
                        OrderStatus status
        ) {
                return new ServiceOrderEntity(
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
