package com.indigo.booking.dto;

import com.indigo.booking.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDTO {
    private int status;
    private ErrorCode error;
    private String message;
    private LocalDateTime timestamp;
}
