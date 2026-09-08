package com.aicoach.service.impl;

import com.aicoach.entity.InterviewSession.InterviewType;

import java.util.*;

/**
 * Offline fallback so the product works end-to-end even without an AI API key configured.
 */
public final class StaticQuestionBank {

    private static final Map<InterviewType, List<String>> BANK = new EnumMap<>(InterviewType.class);

    static {
        BANK.put(InterviewType.HR, List.of(
                "Tell me about yourself and your career journey so far.",
                "Why do you want to work for this company / in this role?",
                "What are your greatest strengths and how have you used them at work?",
                "Describe a weakness you have and what you are doing to improve it.",
                "Where do you see yourself in five years?",
                "How do you handle stress and pressure at work?",
                "Why should we hire you over other candidates?",
                "What motivates you to do your best work?",
                "Describe your ideal work environment.",
                "What are your salary expectations for this role?"
        ));
        BANK.put(InterviewType.TECHNICAL, List.of(
                "Explain the difference between an abstract class and an interface.",
                "How does garbage collection work in Java?",
                "What is the time complexity of common operations on a HashMap and why?",
                "Explain SOLID principles with a short example.",
                "How would you design a REST API for a bookstore application?",
                "What is the difference between SQL and NoSQL databases, and when would you use each?",
                "Explain how indexing improves database query performance.",
                "What is dependency injection and why is it useful?",
                "Describe how you would debug a memory leak in a production application.",
                "Explain the CAP theorem and its implications for distributed systems."
        ));
        BANK.put(InterviewType.BEHAVIORAL, List.of(
                "Tell me about a time you disagreed with a teammate. How did you resolve it?",
                "Describe a situation where you had to meet a tight deadline.",
                "Give an example of a time you failed at something. What did you learn?",
                "Tell me about a time you had to learn a new skill quickly.",
                "Describe a time you led a project or initiative.",
                "Tell me about a time you received difficult feedback. How did you respond?",
                "Give an example of when you went above and beyond for a project.",
                "Describe a time you had to manage multiple priorities at once.",
                "Tell me about a time you made a mistake at work. What did you do next?",
                "Describe a situation where you had to persuade someone to see your point of view."
        ));
    }

    private StaticQuestionBank() {}

    public static List<String> pick(InterviewType type, int count) {
        List<String> pool = new ArrayList<>(BANK.getOrDefault(type, BANK.get(InterviewType.HR)));
        Collections.shuffle(pool);
        return pool.subList(0, Math.min(count, pool.size()));
    }
}
