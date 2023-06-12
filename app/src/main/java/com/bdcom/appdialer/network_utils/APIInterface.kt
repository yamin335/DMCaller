package com.bdcom.appdialer.network_utils

import com.bdcom.appdialer.models.*
import com.bdcom.appdialer.models.CommonApiResponse
import com.bdcom.appdialer.models.RegistrationResponse
import com.bdcom.appdialer.models.current_balance.CurrentBalanceResponse
import com.bdcom.appdialer.models.recharge_info.RechargeInfoResponse
import com.bdcom.appdialer.models.user_info.UserInfoResponse
import com.bdcom.appdialer.network_utils.ApiHelper.Companion.COMPANY_SECRET
import com.bdcom.appdialer.network_utils.ApiHelper.Companion.COMPANY_SECRET_VERIFY
import com.bdcom.appdialer.network_utils.ApiHelper.Companion.FCM_TOKEN_REGISTER
import retrofit2.Call
import retrofit2.http.*

interface APIInterface {
    @POST("/registrationStage01.php")
    @FormUrlEncoded
    fun userRegistration(
        @Field("apikey") apiKey: String?,
        @Field("mobileno") mobileNo: String?,
        @Field("clientname") clientName: String?
    ): Call<com.bdcom.appdialer.models.RegistrationResponse?>?

    @POST("/registrationStage02.php")
    @FormUrlEncoded
    fun userRegistrationOTP(
        @Field("apikey") apiKey: String?,
        @Field("mobileno") mobileNo: String?,
        @Field("otpCode") otpCode: String?
    ): Call<com.bdcom.appdialer.models.CommonApiResponse?>?

    @POST("/login.php")
    @FormUrlEncoded
    fun userLogin(
        @Field("apikey") apiKey: String?,
        @Field("mobileno") mobileNo: String?
    ): Call<com.bdcom.appdialer.models.RegistrationResponse?>?

    @POST("/getUserInfo.php")
    @FormUrlEncoded
    fun getUserInfo(
        @Field("apikey") apiKey: String?,
        @Field("mobileno") mobileNo: String?,
        @Field("otpCode") otpCode: String?
    ): Call<com.bdcom.appdialer.models.user_info.UserInfoResponse?>?

    @POST("/currentBalance.php")
    @FormUrlEncoded
    fun getCurrentBalance(
        @Field("apikey") apiKey: String?,
        @Field("username") mobileNo: String?,
        @Field("secret") otpCode: String?
    ): Call<com.bdcom.appdialer.models.current_balance.CurrentBalanceResponse?>?

    @POST("/addRechargeInfo.php")
    @FormUrlEncoded
    fun sendRechargeInfo(
        @Field("apikey") apiKey: String?,
        @Field("username") userName: String?,
        @Field("mobileno") mobileNo: String?,
        @Field("rechargeAmount") rechargeAmount: String?,
        @Field("txid") tranId: String?,
        @Field("riskLevel") riskLevel: String?
    ): Call<com.bdcom.appdialer.models.recharge_info.RechargeInfoResponse?>?

    @POST("/updateClientData.php")
    @FormUrlEncoded
    fun updateUserInfo(
        @Field("apikey") apiKey: String?,
        @Field("mobileno") mobileNo: String?,
        @Field("secret") secret: String?,
        @Field("clientname") clientName: String?
    ): Call<com.bdcom.appdialer.models.CommonApiResponse?>?

//    @GET("login")
//    fun login(
//        @Query("apikey") apikey: String?,
//        @Query("userid") userid: String?,
//        @Query("mobile_no") mobile_no: String?,
//        @Query("company") company: String?,
//        @Query("password") password: String?,
//        @Query("name") name: String?,
//        @Query("imei_number") imei_number: String?,
//        @Query("latitude") latitude: String?,
//        @Query("longitude") longitude: String?,
//        @Query("platform") platform: String? // longitude=24.9&platform=ios
//    ): Call<LoginDataResponse?>?

    @GET("login")
    fun login(
        @Query("apikey") apikey: String?,
        @Query("userid") userid: String?,
        @Query("mobile_no") mobile_no: String?,
        @Query("password") password: String?,
        @Query("name") name: String?,
        @Query("imei_number") imei_number: String?,
        @Query("latitude") latitude: String?,
        @Query("longitude") longitude: String?,
        @Query("platform") platform: String? // longitude=24.9&platform=ios
    ): Call<LoginDataResponse?>?

    @GET("register")
    fun register(
        @Query("apikey") apikey: String?,
        @Query("userid") userid: String?,
        @Query("mobile_no") mobile_no: String?,
        @Query("company") company: String?,
        @Query("password") password: String?,
        @Query("name") name: String?,
        @Query("email") email: String?,
        @Query("imei_number") imei_number: String?,
        @Query("latitude") latitude: String?,
        @Query("longitude") longitude: String?,
        @Query("platform") platform: String? // longitude=24.9&platform=ios
    ): Call<RegistrationDataResponse?>?

    @POST("http://119.40.81.8/api/confirm")
    fun verify(@Body data: VerificationRequest?): Call<Response<VerificationSuccessData?>?>?

    @POST(FCM_TOKEN_REGISTER)
    fun registerFCMToken(
        @Query("userid") userid: String?,
        @Query("apikey") apikey: String?,
        @Query("mobile_no") mobile_no: String?,
        @Query("company") company: String?,
        @Query("token") token: String?,
        @Query("imei_number") imei_number: String?,
        @Query("latitude") latitude: String?,
        @Query("longitude") longitude: String?,
        @Query("platform") platform: String?
    ): Call<FCMTokenRegistrationResponse?>?

    @POST(COMPANY_SECRET)
    fun getCompanySecret(
        @Query("userid") userid: String?,
        @Query("apikey") apikey: String?,
        @Query("mobile_no") mobile_no: String?,
        @Query("company") company: String?
    ): Call<CompanySecretResponse?>?

    @POST(COMPANY_SECRET_VERIFY)
    fun verifyCompanySecret(
        @Query("userid") userid: String?,
        @Query("apikey") apikey: String?,
        @Query("mobile_no") mobile_no: String?,
        @Query("company") company: String?,
        @Query("company_key") company_key: String?
    ): Call<CompanySecretVerifyResponse?>?
}
