package com.example.demo.vo;

public class RestObject<T> {

    private int code;
    private String message;
    private boolean success;
    private T data;

    public static <T> RestObject<T> newOk(T data) {
        RestObject<T> response = new RestObject<T>();
        response.setCode(200);
        response.setMessage("success");
        response.setSuccess(true);
        response.setData(data);
        return response;
    }

    public static <T> RestObject<T> newFail(int code, String message, T data) {
        RestObject<T> response = new RestObject<T>();
        response.setCode(code);
        response.setMessage(message);
        response.setSuccess(false);
        response.setData(data);
        return response;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
