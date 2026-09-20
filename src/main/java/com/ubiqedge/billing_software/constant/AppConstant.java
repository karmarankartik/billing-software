package com.ubiqedge.billing_software.constant;

import org.springframework.data.mapping.model.FieldNamingStrategy;

import java.lang.reflect.Field;

public class AppConstant {

    //SEED ADMIN USER
    public static final String SEED_ADMIN_USERNAME = "app-admin";
    public static final String SEED_ADMIN_PASSWORD = "adminuser";


    //USER ROLES
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "CUSTOMER";

    public static final String SUCCESS = "success";
    public static final String FAILURE = "failed";


    //APP ERROR MESSAGES
    public static final String  INVOICE_NOT_FOUND = "inc=voice not found";
    public static final String INSUFFICIENT_READING_DATA = "isufficient reading data";
    public static final String INVALID_TOTAL_READING = "invalid total reading";
    public static final String INVALID_BILLING_PERIOD = "invalid billing period";
    public static final String WATER_METER_READING_ALREADY_EXISTS = "water meter reading already exists";
    public static final String WATER_METER_NOT_ASSIGNED = "water meter not assigned";
    public static final String INVALID_BILLING_PLAN_REQUEST = "invalid billing type";
    public static final String BILLING_PLAN_ALREADY_EXISTS = "billing plan already exists";
    public static final String BILLING_PLAN_NOT_FOUND = "billing plan not found";
    public static final String INVALID_WATER_METER_REQUEST = "invalid water meter request";
    public static final String WATER_METER_NOT_FOUND = "water meter not found";
    public static final String WATER_METER_BILLING_PLAN_NOT_FOUND ="water meter billing plan not found";
    public static final String WATER_METER_ALREADY_EXISTS = "water meter already exists";
    public static final String INVALID_ROLE_TYPE = "invalid role type";
    public static final String USER_CREATED_SUCCESSFULLY = "user created";
    public static final String USER_NOT_FOUND = "username not found";
    public static final String USERNAME_ALREADY_EXISTS = "username already exists";
    public static final String SESSION_EXPIRED = "session expired, login again";
    public static final String INVALID_SESSION = "invalid session";
    public static final String LOGIN_SUCCESSFUL = "login successful";
    public static final String LOGOUT_SUCCESSFUL = "logout successful";
    public  static final String INVALID_USERNAME_PASSWORD = "invalid username or password";
    public static final String GENRIC_ERROR_MESSAGE = "something went wrong, please try again";

}
