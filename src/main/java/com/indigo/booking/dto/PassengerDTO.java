package com.indigo.booking.dto;

import com.indigo.booking.model.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Passenger information")
public class PassengerDTO {

    @Schema(description = "Full name of the passenger", example = "John Doe", required = true)
    @NotBlank(message = "Passenger name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @Schema(description = "Age of the passenger", example = "30", required = true)
    @NotNull(message = "Age is required")
    @Min(value = 0, message = "Age must be at least 0")
    @Max(value = 150, message = "Age must not exceed 150")
    private Integer age;

    @Schema(description = "Gender (M/F/O)", example = "M", required = true)
    @NotNull(message = "Gender is required")
    private Gender gender;

    @Schema(description = "ID proof number (Passport/Aadhaar/etc)", example = "ABC123456", required = true)
    @NotBlank(message = "ID proof number is required")
    @Size(max = 50, message = "ID proof number must not exceed 50 characters")
    private String idProofNumber;
}
