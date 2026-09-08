package com.aicoach.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "answers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    @JsonIgnore
    private Question question;

    @Lob
    @Column(nullable = false)
    private String answerText;

    private Integer timeTakenSeconds;

    private Double aiScore;

    @Lob
    private String aiFeedback;

    @Builder.Default
    private LocalDateTime answeredAt = LocalDateTime.now();
}
