package com.indigo.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlightSearchResponseDTO {

    private List<FlightSearchResultDTO> directFlights;
    private List<IndirectFlightResultDTO> indirectFlights;
    private Integer totalResults;

    public FlightSearchResponseDTO(List<FlightSearchResultDTO> directFlights,
                                    List<IndirectFlightResultDTO> indirectFlights) {
        this.directFlights = directFlights;
        this.indirectFlights = indirectFlights;
        this.totalResults = directFlights.size() + indirectFlights.size();
    }
}
