package com.cts.customerservices.client;
import org.springframework.stereotype.Component;

@Component
public class BranchClient {

    public boolean isBranchActive(String ifscCode) {

        /*
        In real system this will call:

        Branch Service API

        GET /branches/{ifsc}/status
        */

        if (ifscCode == null)
            return false;

        return ifscCode.startsWith("SBIN");

    }

}
