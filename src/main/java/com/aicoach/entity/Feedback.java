package com.aicoach.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "feedbacks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_feedback_session",
                        columnNames = "session_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "session_id",
            nullable = false,
            unique = true
    )
    @JsonIgnore
    private InterviewSession session;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String strengths;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String weaknesses;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String improvementPlan;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String summary;
}