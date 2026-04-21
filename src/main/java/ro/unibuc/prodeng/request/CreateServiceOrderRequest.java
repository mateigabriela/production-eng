package ro.unibuc.prodeng.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateServiceOrderRequest(
        @NotBlank String clientId,
    @JsonAlias("vehicleId")
        @NotBlank String carId,
        @NotBlank String mechanicId,
        @NotBlank String serviceName,
        @NotBlank String description,
        BigDecimal laborCost,
        @NotNull LocalDateTime scheduledAt,
        @NotNull List<@Valid RequiredPartRequest> requiredParts,
        List<String> selectedOperations,
        boolean consultationRequested
) {

    public CreateServiceOrderRequest {
        if (clientId == null || clientId.isBlank()) {
            clientId = carId;
        }
        if (carId == null || carId.isBlank()) {
            carId = clientId;
        }
        requiredParts = requiredParts == null ? List.of() : requiredParts;
        selectedOperations = selectedOperations == null ? List.of() : selectedOperations;
    }

    public CreateServiceOrderRequest(
            String carId,
            String mechanicId,
            String serviceName,
            String description,
            BigDecimal laborCost,
            LocalDateTime scheduledAt,
            List<@Valid RequiredPartRequest> requiredParts
    ) {
        this(carId, carId, mechanicId, serviceName, description, laborCost, scheduledAt, requiredParts, List.of(), false);
    }

    public CreateServiceOrderRequest(
            String clientId,
            String carId,
            String mechanicId,
            String serviceName,
            String description,
            BigDecimal laborCost,
            LocalDateTime scheduledAt,
            List<@Valid RequiredPartRequest> requiredParts
    ) {
        this(clientId, carId, mechanicId, serviceName, description, laborCost, scheduledAt, requiredParts, List.of(), false);
    }
}
