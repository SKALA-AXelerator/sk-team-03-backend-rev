package com.skala03.skala_backend.entity.interview;

import lombok.*;

import java.io.Serializable;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoomParticipantId implements Serializable {
    private String roomId;
    private String userId;
}