package com.example.getsms.modul;

public class Response {
    public String name;
    public String date;
    public int status_code;

    public Response(String name, String date, int status_code) {
        this.name = name;
        this.date = date;
        this.status_code = status_code;
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }

    public int getStatus_code() {
        return status_code;
    }

}
