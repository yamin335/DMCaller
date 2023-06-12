package com.bdcom.appdialer.models.recharge_info;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class RechargeInfoResponse {

    @SerializedName("statuscode")
    @Expose
    private String statuscode;

    @SerializedName("status")
    @Expose
    private String status;

    @SerializedName("message")
    @Expose
    private String message;

    public String getStatuscode() {
        return statuscode;
    }

    public void setStatuscode(String statuscode) {
        this.statuscode = statuscode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
