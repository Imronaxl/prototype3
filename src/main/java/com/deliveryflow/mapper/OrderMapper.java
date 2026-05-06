package com.deliveryflow.mapper;

import com.deliveryflow.api.dto.response.OrderResponse;
import com.deliveryflow.domain.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderMapper {

    OrderResponse toResponse(Order order);
}
