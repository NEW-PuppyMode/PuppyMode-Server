package com.umc.puppymode2.domain.temp.converter;

import com.umc.puppymode2.domain.temp.dto.TempRequestDTO;
import com.umc.puppymode2.domain.temp.dto.TempResponseDTO;
import com.umc.puppymode2.domain.temp.entity.Temp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class TempConverter {

    public static TempResponseDTO.TempDTO toTempDTO(Temp temp) {
        return TempResponseDTO.TempDTO.builder()
                .tempId(temp.getId())
                .name(temp.getName())
                .description(temp.getDescription())
                .status(temp.getStatus())
                .createdAt(temp.getCreatedAt())
                .updatedAt(temp.getUpdatedAt())
                .build();
    }

    public static TempResponseDTO.CreateTempResultDTO toCreateTempResultDTO(Long tempId) {
        return TempResponseDTO.CreateTempResultDTO.builder()
                .tempId(tempId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static TempResponseDTO.TempListDTO toTempListDTO(List<Temp> tempList) {
        List<TempResponseDTO.TempDTO> tempDTOList = tempList.stream()
                .map(TempConverter::toTempDTO)
                .collect(Collectors.toList());

        return TempResponseDTO.TempListDTO.builder()
                .tempList(tempDTOList)
                .listSize(tempDTOList.size())
                .build();
    }

    public static Temp toTemp(TempRequestDTO.CreateTempDTO request) {
        return Temp.builder()
                .name(request.getName())
                .description(request.getDescription())
                .status(request.getStatus())
                .build();
    }
}
