package com.example.userservice.mapper;

import com.example.userservice.dto.PaymentCardRequestDto;
import com.example.userservice.dto.PaymentCardResponseDto;
import com.example.userservice.entity.PaymentCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PaymentCardMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PaymentCard toEntity(PaymentCardRequestDto dto);

    @Mapping(target = "userId", source = "user.id")
    PaymentCardResponseDto toDto(PaymentCard card);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(
            PaymentCardRequestDto dto,
            @MappingTarget PaymentCard card
    );
}