package com.ubiqedge.billing_software.constant;

import org.springframework.data.mapping.model.FieldNamingStrategy;

import java.lang.reflect.Field;
import java.time.ZoneId;

public class AppConstant {

    //SEED ADMIN USER
    public static final String SEED_ADMIN_USERNAME = "app-admin";
    public static final String SEED_ADMIN_PASSWORD = "adminuser";


    //USER ROLES
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "CUSTOMER";

    public static final String SUCCESS = "success";
    public static final String FAILURE = "failed";

    public static final String FIXED = "FIXED";
    public static final String SLAB ="SLAB";

    public static final String READING_FLOW = "FLOW";
    public static final String READING_TOTAL = "TOTAL";



    //APP ERROR MESSAGES

    public static final String WATER_METER_ALREADY_ASSIGNED =
            "Water meter is already assigned";

    public static final String WATER_METER_NOT_ASSIGNED =
            "Water meter is not assigned";

    public static final String WATER_METER_ASSIGNMENT_USER_MISMATCH =
            "Water meter is assigned to a different user";


    public static final String INVALID_WATER_METER_ID =
            "invalid water meter id";

    public static final String DUPLICATE_WATER_METER_INGESTION =
            "duplicate water meter ingestion";

    public static final String DUPLICATE_WATER_METER_READING =
            "duplicate water meter reading";

    public static final String INVALID_WATER_METER_READING =
            "invalid water meter reading";



    public static final String INVOICES_GENERATED_SUCCESSFULLY =
            "Invoices generated successfully";

    public static final String NO_ASSIGNMENTS_FOUND_FOR_BILLING_PERIOD =
            "No assignments found for the billing period";

    public static final String BILLING_GENERATION_ALREADY_RUNNING = "billing generation already running";

    public static final String BILLING_GENERATION_FAILED = "billing generation failed";

    public static final String NO_NEW_INVOICES_GENERATED =
            "No new invoices were generated for the billing period";
    public static  final  String INVOICE_ALREADY_EXISTS = "invoice already exists";
    public static  final  String ASSIGNMENT_NOT_ACTIVE_FOR_BILLING_PERIOD = "assignment not active for billing period";
    public static  final  String BILLING_PLAN_BOUNDARY_READING_NOT_FOUND = "billing plan boundary not found";
    public static  final  String TO_TOTAL_READING_NOT_FOUND = "to total not found";
    public static  final  String FROM_TOTAL_READING_NOT_FOUND = "from total not found";
   public static  final String CONSUMPTION_CALCULATION_FAILED ="consumption calcuation failed";
   public  static  final String BILLING_GENERATION_JOB_NOT_FOUND ="billing generation job not found";
    public static final String WATER_METER_READING_REQUEST_REQUIRED = "water meter reading request is required";
    public static final String WATER_METER_ID_REQUIRED = "water meter id is required";
    public static final String WATER_METER_READING_TYPE_REQUIRED = "water meter reading type is required";
    public static final String INVALID_WATER_METER_READING_TYPE = "invalid water meter reading type";
    public static final String WATER_METER_READING_VALUE_REQUIRED = "water meter reading value is required";
    public static final String WATER_METER_READING_AT_REQUIRED = "water meter reading at is required";
    public static final String WATER_METER_INGESTION_KEY_REQUIRED = "water meter ingestion key is required";
    public static final String WATER_METER_BILLING_PLAN_REQUIRED =
            "Water meter must have a billing plan before assignment";
    public static final String INVALID_BILLING_SLAB = "invalid billing slab";
    public static final String INVOICE_OPENING_READING_NOT_FOUND = "invoice opening reading not found";
    public static final String INVOICE_CLOSING_READING_NOT_FOUND ="invoice closing reading not found";
    public static final String  INVOICE_NOT_FOUND = "inc=voice not found";
    public static final String INSUFFICIENT_READING_DATA = "insufficient reading data";
    public static final String INVALID_TOTAL_READING = "invalid total reading";
    public static final String INVALID_BILLING_PERIOD = "invalid billing period";
    public static final String WATER_METER_READING_ALREADY_EXISTS = "water meter reading already exists";
    public static final String NEGATIVE_VALUE_NOT_ALLOWED = "NEGTAIVE READING NOT ALLOWED";
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

    public static final String INVALID_BILLING_YEAR = "invalid year";
    public static final String INVALID_BILLING_MONTH = "invalid month";


    public static final String BILLING_DATE_RANGE_OUT_OF_BOUNDS =
            "Billing date range is out of bounds for the meter";



    public static final String RUNNING = "RUNNING";
    public static final String COMPLETED = "COMPLETED";
    public static final String FAILED = "FAILED";

    public static final ZoneId BILLING_ZONE = ZoneId.of("Asia/Kolkata");

}
