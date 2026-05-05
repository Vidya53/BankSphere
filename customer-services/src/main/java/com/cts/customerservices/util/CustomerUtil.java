package com.cts.customerservices.util;

import java.util.UUID;

public class CustomerUtil {

    public static String generateCustomerNo() {

        return "CUST-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase();

    }

}



