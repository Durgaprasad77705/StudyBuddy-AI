package com.aicoach.dto;

public class InterviewResponse {

    private boolean success;
    private String result;

    public InterviewResponse() {
    }

    public InterviewResponse(
            boolean success,
            String result) {

        this.success = success;
        this.result = result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }
}