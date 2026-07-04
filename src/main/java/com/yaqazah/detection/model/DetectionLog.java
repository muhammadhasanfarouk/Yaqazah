package com.yaqazah.detection.model;

import com.yaqazah.common.util.EncryptionConverter;
import com.yaqazah.session.model.Session;
import com.yaqazah.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "detection_log")
public class DetectionLog {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID eventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String title;
    private String subtitle;
    private String timestamp;

    private int alertId;
    private int riskId;

    @Convert(converter = EncryptionConverter.class)
//    @Column(columnDefinition = "TEXT")
    private String snapshotUrl;

    private String insertionTimestamp;
}