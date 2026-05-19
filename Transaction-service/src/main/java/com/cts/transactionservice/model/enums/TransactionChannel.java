package com.cts.transactionservice.model.enums;

public enum TransactionChannel {
    MOBILE_APP,
    NET_BANKING,
    ATM,
    BRANCH,
    UPI,
    API,

    // Same-bank transfers initiated from the customer app or staff console.
    // account-service.TransferService.recordCompletedLedger sends this for every
    // intra-BankSphere transfer that doesn't ride an external rail.
    INTERNAL,

    // Cash counter deposits and withdrawals — sent by account-service.CashService.
    CASH
}

