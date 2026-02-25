package com.yaqazah.session.service;

import com.yaqazah.session.model.Session;
import com.yaqazah.session.repository.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class SessionService {
    @Autowired
    private SessionRepository sessionRepository;

    public void save(Session session) { sessionRepository.save(session); }
    public void startSession(UUID driverId) { /* Logic */ }
    public void pauseSession(UUID sessionId) { /* Logic */ }
    public void resumeSession(UUID sessionId) { /* Logic */ }
    public void endSession(UUID sessionId) { /* Logic */ }
    public List<Session> getSessions(UUID driverId) { return sessionRepository.findByUserId(driverId); }
    public Session getSession(UUID sessionId) { return sessionRepository.findById(sessionId).orElse(null); }
}