package com.aicoach.dto;

public class CodingResponse {

    private boolean success;
    private String result;

    public CodingResponse() {
    }

    public CodingResponse(boolean success, String result) {
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