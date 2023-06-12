package com.bdcom.appdialer.models

data class LoginRequest(
    var apikey: String,
    var userid: String,
    var company: String,
    var mobileno: String,
    var password: String,
    var email: String,
    var imei: String,
    var lat: String,
    var long: String,
    var platform: String
)
data class RegisterRequest(
    var apikey: String,
    var name: String,
    var userid: String,
    var company: String,
    var mobileno: String,
    var password: String,
    var email: String,
    var imei: String,
    var lat: String,
    var long: String,
    var platform: String
)
data class FCMTokenRegisterRequest(
    val userid: String,
    val apikey: String,
    val mobile_no: String,
    val company: String,
    val token: String,
    val imei_number: String,
    val latitude: String,
    val longitude: String,
    val platform: String
)

data class Response<X>(var statuscode: String, var status: String, var message: String, var data: X)

data class RegisterSuccessData(
    var name: String,
    var company_uniqueid: String,
    var extension: String,
    var secret: String,
    var server_ip: String,
    var server_port: String?,
    var company_key: String,
    var company_key_date: String
)

data class VerificationRequest(var msisdn: String, var otp: String, var secret: String)

data class VerificationSuccessData(var sip: String, var secret: String, var domain: String)
data class LoginDataResponse(
    val name: String?,
    val company_uniqueid: String?,
    val extension: String?,
    val secret: String?,
    val server_ip: String?,
    val server_port: String?,
    val company_key: String?,
    val company_key_date: String?
)

data class RegistrationDataResponse(val success: String?)
