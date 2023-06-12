package com.bdcom.appdialer.network_utils

// import com.orhanobut.logger.Logger
import com.bdcom.appdialer.models.*
import retrofit2.Call
import retrofit2.Callback

class ApiHelper {

    companion object {

        const val DIRECTORY_IPTKOTHA = "iptkotha"
        const val DIRECTORY_APPS = "apps"
        const val DIRECTORY_API = "api"

        const val FCM_TOKEN_REGISTER = "/$DIRECTORY_IPTKOTHA/$DIRECTORY_APPS/$DIRECTORY_API/apps_device_token"
        const val COMPANY_SECRET = "/$DIRECTORY_IPTKOTHA/$DIRECTORY_APPS/$DIRECTORY_API/company_key"
        const val COMPANY_SECRET_VERIFY = "/$DIRECTORY_IPTKOTHA/$DIRECTORY_APPS/$DIRECTORY_API/company_key_check"

        fun login(request: LoginRequest, listener: ApiCallbackListener<LoginDataResponse>) {
            val apiService = com.bdcom.appdialer.network_utils.APIClient.getClient().create(APIInterface::class.java)
//            val call = apiService.login(request.apikey, request.userid, request.mobileno, request.company, request.password, "", request.imei, request.lat, request.long, request.platform)
            val call = apiService.login(request.apikey, request.userid, request.mobileno, request.password, "", request.imei, request.lat, request.long, request.platform)
            call?.enqueue(object : Callback<LoginDataResponse?> {
                override fun onResponse(
                    call: Call<LoginDataResponse?>?,
                    response: retrofit2.Response<LoginDataResponse?>?
                ) {

                    // Logger.d(response?.body()?.data.toString())
                    if (response?.code() == 200) {
                        listener.onDataFetched(response.body())
                    } else {
                        listener.onFailed(response?.code())
                    }
                }

                override fun onFailure(call: Call<LoginDataResponse?>?, t: Throwable?) {
                    // Logger.d(t?.message)
                    listener.onFailed(0)
                }
            })
        }

        fun registerUser(
            request: RegisterRequest,
            listener: ApiCallbackListener<RegistrationDataResponse>
        ) {
            val apiService = com.bdcom.appdialer.network_utils.APIClient.getClient().create(APIInterface::class.java)
            val call = apiService.register(request.apikey, request.userid, request.mobileno, request.company, request.password, request.name, request.email, request.imei, request.lat, request.long, request.platform
            )
            call?.enqueue(object : Callback<RegistrationDataResponse?> {
                override fun onResponse(
                    call: Call<RegistrationDataResponse?>?,
                    response: retrofit2.Response<RegistrationDataResponse?>?
                ) {
                    // Logger.d(response?.body()?.data.toString())
                   // if (response?.code() == 200) {
                    if (response != null) {
                        listener.onDataFetched(response.body())
                    }
                }

                override fun onFailure(call: Call<RegistrationDataResponse?>?, t: Throwable?) {
                    // Logger.d(t?.message)
                    listener.onFailed(0)
                }
            })
        }
        fun verifyUser(
            request: VerificationRequest,
            listener: ApiCallbackListener<VerificationSuccessData>
        ) {
            val apiService = com.bdcom.appdialer.network_utils.APIClient.getClient().create(APIInterface::class.java)
            val call = apiService.verify(request)

            call?.enqueue(object : Callback<Response<VerificationSuccessData?>?> {

                override fun onResponse(
                    call: Call<Response<VerificationSuccessData?>?>?,
                    response: retrofit2.Response<Response<VerificationSuccessData?>?>?
                ) {
                    // Logger.d(response?.body()?.data.toString())
                    if (response?.code() == 200) {
                       // listener.onDataFetched(response?.body())
                    } else {
                        listener.onFailed(response?.code())
                    }
                }

                override fun onFailure(
                    call: Call<Response<VerificationSuccessData?>?>?,
                    t: Throwable?
                ) {
                    // Logger.d(t?.message)
                    listener.onFailed(0)
                }
            })
        }

        fun registerFirebaseToken(
            request: FCMTokenRegisterRequest,
            listener: ApiCallbackListener<FCMTokenRegistrationResponse>
        ) {
            val apiService = com.bdcom.appdialer.network_utils.APIClient.getApiClient().create(APIInterface::class.java)
            val call = apiService.registerFCMToken(request.userid, request.apikey, request.mobile_no, request.company, request.token,
                    request.imei_number, request.latitude, request.longitude, request.platform)
            call?.enqueue(object : Callback<FCMTokenRegistrationResponse?> {
                override fun onResponse(
                    call: Call<FCMTokenRegistrationResponse?>?,
                    response: retrofit2.Response<FCMTokenRegistrationResponse?>?
                ) {
                    if (response?.code() == 200) {
                        listener.onDataFetched(response.body())
                    } else {
                        listener.onFailed(response?.code())
                    }
                }

                override fun onFailure(call: Call<FCMTokenRegistrationResponse?>?, t: Throwable?) {
                    listener.onFailed(0)
                }
            })
        }

        fun getCompanySecret(
            request: CompanySecretRequest,
            listener: ApiCallbackListener<CompanySecretResponse>
        ) {
            val apiService = com.bdcom.appdialer.network_utils.APIClient.getApiClient().create(APIInterface::class.java)
            val call = apiService.getCompanySecret(request.userid, request.apikey, request.mobile_no, request.company)
            call?.enqueue(object : Callback<CompanySecretResponse?> {
                override fun onResponse(
                    call: Call<CompanySecretResponse?>?,
                    response: retrofit2.Response<CompanySecretResponse?>?
                ) {
                    if (response?.code() == 200) {
                        listener.onDataFetched(response.body())
                    } else {
                        listener.onFailed(response?.code())
                    }
                }

                override fun onFailure(call: Call<CompanySecretResponse?>?, t: Throwable?) {
                    listener.onFailed(0)
                }
            })
        }

        fun verifyCompanySecret(
            request: CompanySecretVerifyRequest,
            listener: ApiCallbackListener<CompanySecretVerifyResponse>
        ) {
            val apiService = com.bdcom.appdialer.network_utils.APIClient.getApiClient().create(APIInterface::class.java)
            val call = apiService.verifyCompanySecret(request.userid, request.apikey, request.mobile_no, request.company, request.company_key)
            call?.enqueue(object : Callback<CompanySecretVerifyResponse?> {
                override fun onResponse(
                    call: Call<CompanySecretVerifyResponse?>?,
                    response: retrofit2.Response<CompanySecretVerifyResponse?>?
                ) {
                    if (response?.code() == 200) {
                        listener.onDataFetched(response.body())
                    } else {
                        listener.onFailed(response?.code())
                    }
                }

                override fun onFailure(call: Call<CompanySecretVerifyResponse?>?, t: Throwable?) {
                    listener.onFailed(0)
                }
            })
        }
    }

    interface ApiCallbackListener<T> {
        fun onDataFetched(response: T?)
        fun onFailed(status: Int?)
    }
}
