package com.skala03.skala_backend.entity;

import jakarta.persistence.*;

// Question
@Entity
@Table(name = "questions")
public class Question {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Integer questionId;

    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;
}