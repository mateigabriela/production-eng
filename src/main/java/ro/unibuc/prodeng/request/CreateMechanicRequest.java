package ro.unibuc.prodeng.request;

import jakarta.validation.constraints.NotBlank;

public record CreateMechanicRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String phone,
        String workingStartTime,
        String workingEndTime
) {
        public CreateMechanicRequest(String firstName, String lastName, String phone) {
                this(firstName, lastName, phone, null, null);
        }
}
