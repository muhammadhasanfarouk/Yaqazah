package com.yaqazah.session.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionUploadRequest {
    private SessionPayload session;
    private List<LogPayload> logs;
}
