package com.demo.check;

import java.util.Date;

public class HealthCheck {

    public final Date time;
    public final int statusCode;
    public final String description;

    public HealthCheck(Date time, int statusCode, String description) {
        this.time = time;
        this.statusCode = statusCode;
        this.description = description;
    }

    public HealthCheck(Date time, int statusCode) {
        this(time, statusCode, "");
    }
}
