package com.example.customerapi.customer.application.port.in;

import java.util.List;

import com.example.customerapi.customer.domain.StateLocation;

public interface GroupCustomersByLocationUseCase {

	/** Customer counts per state (by UF) and per city (pt-BR order). */
	List<StateLocation> groupByLocation();

}
