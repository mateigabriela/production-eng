package ro.unibuc.prodeng.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "mechanics")
public record MechanicEntity(
        @Id String id,
        String firstName,
        String lastName,
        String phone,
        String workingStartTime,
        String workingEndTime
) {
        public MechanicEntity(String id, String firstName, String lastName, String phone) {
                this(id, firstName, lastName, phone, "08:00", "17:00");
        }
}
