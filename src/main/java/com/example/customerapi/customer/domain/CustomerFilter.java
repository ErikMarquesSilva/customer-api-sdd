package com.example.customerapi.customer.domain;

/**
 * Optional list filters; a null or blank value means "no filter".
 */
public record CustomerFilter(String name, String email) {
}
