package com.yaqazah.session.controller;

import com.yaqazah.session.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {
    @Autowired
    private SessionService sessionService;

    @PostMapping("/start/{driverId}")
    public void createSession(@PathVariable UUID driverId) {
        sessionService.startSession(driverId);
    }

    @PutMapping("/pause/{sessionId}")
    public void pauseSession(@PathVariable UUID sessionId) {
        sessionService.pauseSession(sessionId);
    }

    @PutMapping("/resume/{sessionId}")
    public void resumeSession(@PathVariable UUID sessionId) {
        sessionService.resumeSession(sessionId);
    }

    @PutMapping("/end/{sessionId}")
    public void endSession(@PathVariable UUID sessionId) {
        sessionService.endSession(sessionId);
    }
}