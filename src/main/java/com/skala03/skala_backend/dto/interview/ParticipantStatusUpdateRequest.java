package com.skala03.skala_backend.dto.interview;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantStatusUpdateRequest {
    private String status; // "IN_PROGRESS", "WAITING", "OFFLINE"
}