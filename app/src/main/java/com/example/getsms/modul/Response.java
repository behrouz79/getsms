package com.example.getsms.modul;

public class Response {
    public int id;
    public String name;
    public String body;
    public String date;
    public int status_code;

    public Response(int id ,String name, String date, int status_code, String body) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.status_code = status_code;
        this.body = body;
    }

    public int getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDate() {
        return date;
    }

    public String getBody() {return body;}

    public int getStatus_code() {
        return status_code;
    }

}
