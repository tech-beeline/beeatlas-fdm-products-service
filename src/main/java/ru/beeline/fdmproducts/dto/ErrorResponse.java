package ru.beeline.fdmproducts.dto;

public class ErrorResponse {
    private String message;
    private String timestamp;

    public ErrorResponse(String message) {
        this.message = message;
        this.timestamp = java.time.Instant.now().toString();
    }

    // геттеры/сеттеры или lombok
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}