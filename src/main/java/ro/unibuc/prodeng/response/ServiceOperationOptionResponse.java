package ro.unibuc.prodeng.response;

public record ServiceOperationOptionResponse(
        String code,
        String label,
        int estimatedDurationMinutes
) {
}
