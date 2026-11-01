package com.attendance.storage;

import java.io.IOException;

/** A checked exception that explains a persistent-storage operation failure. */
public class DataStoreException extends IOException {
    public DataStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
