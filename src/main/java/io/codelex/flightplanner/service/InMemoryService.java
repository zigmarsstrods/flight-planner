package io.codelex.flightplanner.service;

import io.codelex.flightplanner.domain.*;
import io.codelex.flightplanner.dto.AddFlightRequest;
import io.codelex.flightplanner.dto.PageResult;
import io.codelex.flightplanner.dto.SearchFlightsRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

@Service
@ConditionalOnProperty(prefix = "flight-planner", name = "store-type", havingValue = "in-memory")
public class InMemoryService extends FlightPlannerService {

    private final List<Flight> flightList = new ArrayList<>();

    private int flightId = 0;

    public Flight addFlight(final AddFlightRequest flightRequest) {
        Flight flightToAdd = getFlightCarrierAndTimesFromRequest(flightRequest);
        Airport flightFrom = flightRequest.getFrom();
        Airport flightTo = flightRequest.getTo();
        setAirports(flightToAdd, flightFrom, flightTo);
        if (flightList.stream()
                .anyMatch(flight -> flight.areFlightsEqual(flightToAdd))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Can not add 2 identical flights!");
        }
        flightToAdd.setId(flightId);
        flightList.add(flightToAdd);
        flightId++;
        return flightToAdd;
    }

    public Flight fetchFlight(final int id) {
        return flightList.stream().
                filter(flight -> flight.getId() == id)
                .findAny()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Requested flight not found in the DB!!!"));
    }

    public PageResult<Flight> searchFlights(final SearchFlightsRequest request) {
        LocalDate formattedRequestDepartureDate = getFormattedRequestDepartureDate(request);
        List<Flight> flightsFound = flightList.stream()
                .filter(flight -> flight.getFrom()
                        .getAirport()
                        .equals(request.getFrom())
                        && flight.getTo()
                        .getAirport()
                        .equals(request.getTo())
                        && flight.getDepartureTime()
                        .toLocalDate()
                        .equals(formattedRequestDepartureDate))
                .toList();
        return new PageResult<>(0, flightsFound.size(), flightsFound);
    }

    public Set<Airport> searchAirports(final String search) {
        String normalizedSearch = getNormalizedSearch(search);
        Optional<Airport> matchedAirport = flightList.stream()
                .flatMap(flight -> Stream.of(flight.getFrom(), flight.getTo()))
                .distinct()
                .filter(airport -> airport.getCountry()
                        .toLowerCase()
                        .startsWith(normalizedSearch)
                        || airport.getCity()
                        .toLowerCase()
                        .startsWith(normalizedSearch)
                        || airport.getAirport()
                        .toLowerCase()
                        .startsWith(normalizedSearch))
                .findAny();
        matchedAirport.ifPresent(matchedAirportSet::add);
        return matchedAirportSet;
    }

    public void deleteFlight(final int id) {
        flightList.removeIf(flight -> flight.getId() == id);
    }

    public void clear() {
        flightList.clear();
    }
}
