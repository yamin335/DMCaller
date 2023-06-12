package com.bdcom.appdialer.models

data class CompanySecretRequest(
    val userid: String,
    val apikey: String,
    val mobile_no: String,
    val company: String
)

data class CompanySecretVerifyRequest(
    val userid: String,
    val apikey: String,
    val mobile_no: String,
    val company: String,
    val company_key: String
)

data class CompanySecretResponse(val company_key: String?)
data class CompanySecretVerifyResponse(val messages: String?)
