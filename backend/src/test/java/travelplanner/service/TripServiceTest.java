package travelplanner.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import travelplanner.dto.trip.TripCreateRequest;
import travelplanner.dto.trip.TripResponse;
import travelplanner.entity.Itinerary;
import travelplanner.entity.Place;
import travelplanner.entity.Tag;
import travelplanner.entity.Trip;
import travelplanner.entity.TripDay;
import travelplanner.entity.User;
import travelplanner.exception.EntityNotFoundException;
import travelplanner.mapper.TripMapper;
import travelplanner.repository.TripRepository;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripMapper tripMapper;

    @InjectMocks
    private TripService tripService;

    @Test
    void cloneTrip_Success_PublicTripOfAnotherUser() {
        User originalOwner = new User();
        originalOwner.setUserId(UUID.randomUUID());

        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());

        Place place = new Place();
        place.setPlaceId(UUID.randomUUID());
        place.setName("Eiffel Tower");

        UUID originalTripId = UUID.randomUUID();
        Trip originalTrip = new Trip();
        originalTrip.setTripId(originalTripId);
        originalTrip.setTitle("Paris Tour");
        originalTrip.setDestination("Paris");
        originalTrip.setStartDate(LocalDate.of(2026, 9, 1));
        originalTrip.setEndDate(LocalDate.of(2026, 9, 5));
        originalTrip.setDescription("A wonderful tour");
        originalTrip.setBudget(BigDecimal.valueOf(1500));
        originalTrip.setCurrency("EUR");
        originalTrip.setCoverUrl("http://example.com/cover.jpg");
        originalTrip.setIsPublic(true);
        originalTrip.setOwner(originalOwner);

        Tag tag = new Tag();
        tag.setTagId(UUID.randomUUID());
        tag.setName("Adventure");
        originalTrip.getTags().add(tag);

        TripDay originalDay = new TripDay();
        originalDay.setDayId(UUID.randomUUID());
        originalDay.setDayNumber(1);
        originalDay.setDate(LocalDate.of(2026, 9, 1));
        originalDay.setTrip(originalTrip);
        originalTrip.getTripDays().add(originalDay);

        Itinerary originalItinerary = new Itinerary();
        originalItinerary.setItineraryId(UUID.randomUUID());
        originalItinerary.setVisitOrder(1);
        originalItinerary.setNotes("First stop");
        originalItinerary.setTimeSpentMinutes(120);
        originalItinerary.setPlace(place);
        originalItinerary.setTripDay(originalDay);
        originalDay.getItineraries().add(originalItinerary);

        TripResponse expectedResponse = new TripResponse(
                UUID.randomUUID(),
                "Paris Tour (Copy)",
                "Paris",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 5),
                "A wonderful tour",
                BigDecimal.valueOf(1500),
                "EUR",
                "http://example.com/cover.jpg",
                false,
                LocalDateTime.now()
        );

        when(tripRepository.findById(originalTripId)).thenReturn(Optional.of(originalTrip));
        when(tripRepository.save(any(Trip.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(tripMapper.toResponse(any(Trip.class))).thenReturn(expectedResponse);

        TripResponse result = tripService.cloneTrip(originalTripId, currentUser);

        assertNotNull(result);
        assertEquals(expectedResponse.title(), result.title());
        assertEquals(expectedResponse.destination(), result.destination());
        assertEquals(expectedResponse.startDate(), result.startDate());
        assertEquals(expectedResponse.endDate(), result.endDate());
        assertFalse(result.isPublic());

        ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository, times(1)).save(tripCaptor.capture());
        Trip clonedTrip = tripCaptor.getValue();

        assertNull(clonedTrip.getTripId());
        assertEquals("Paris Tour (Copy)", clonedTrip.getTitle());
        assertEquals("Paris", clonedTrip.getDestination());
        assertEquals(LocalDate.of(2026, 9, 1), clonedTrip.getStartDate());
        assertEquals(LocalDate.of(2026, 9, 5), clonedTrip.getEndDate());
        assertEquals("A wonderful tour", clonedTrip.getDescription());
        assertEquals(BigDecimal.valueOf(1500), clonedTrip.getBudget());
        assertEquals("EUR", clonedTrip.getCurrency());
        assertEquals("http://example.com/cover.jpg", clonedTrip.getCoverUrl());
        assertSame(currentUser, clonedTrip.getOwner());
        assertFalse(clonedTrip.getIsPublic());

        assertEquals(1, clonedTrip.getTags().size());
        org.junit.jupiter.api.Assertions.assertTrue(
                clonedTrip.getTags().contains(tag));
        org.junit.jupiter.api.Assertions.assertNotSame(
                originalTrip.getTags(), clonedTrip.getTags());

        assertEquals(1, clonedTrip.getTripDays().size());
        TripDay clonedDay = clonedTrip.getTripDays().get(0);
        assertNull(clonedDay.getDayId());
        assertSame(clonedTrip, clonedDay.getTrip());
        assertEquals(1, clonedDay.getDayNumber());
        assertEquals(LocalDate.of(2026, 9, 1), clonedDay.getDate());

        assertEquals(1, clonedDay.getItineraries().size());
        Itinerary clonedItinerary = clonedDay.getItineraries().get(0);
        assertNull(clonedItinerary.getItineraryId());
        assertSame(clonedDay, clonedItinerary.getTripDay());
        assertEquals(1, clonedItinerary.getVisitOrder());
        assertEquals("First stop", clonedItinerary.getNotes());
        assertEquals(120, clonedItinerary.getTimeSpentMinutes());
        assertSame(place, clonedItinerary.getPlace());
    }

    @Test
    void cloneTrip_Success_OwnPrivateTrip() {
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());

        UUID originalTripId = UUID.randomUUID();
        Trip originalTrip = new Trip();
        originalTrip.setTripId(originalTripId);
        originalTrip.setTitle("My Trip");
        originalTrip.setIsPublic(false);
        originalTrip.setOwner(currentUser);

        when(tripRepository.findById(originalTripId)).thenReturn(Optional.of(originalTrip));
        when(tripRepository.save(any(Trip.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        tripService.cloneTrip(originalTripId, currentUser);

        verify(tripRepository, times(1)).save(any(Trip.class));
    }

    @Test
    void cloneTrip_Failure_PrivateTripOfAnotherUser() {
        User originalOwner = new User();
        originalOwner.setUserId(UUID.randomUUID());

        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());

        UUID originalTripId = UUID.randomUUID();
        Trip originalTrip = new Trip();
        originalTrip.setTripId(originalTripId);
        originalTrip.setTitle("Secret Trip");
        originalTrip.setIsPublic(false);
        originalTrip.setOwner(originalOwner);

        when(tripRepository.findById(originalTripId)).thenReturn(Optional.of(originalTrip));

        assertThrows(AccessDeniedException.class,
                () -> tripService.cloneTrip(originalTripId, currentUser));
    }

    @Test
    void cloneTrip_Failure_TripNotFound() {
        User currentUser = new User();

        UUID originalTripId = UUID.randomUUID();
        when(tripRepository.findById(originalTripId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class,
                () -> tripService.cloneTrip(originalTripId, currentUser));
    }

    @Test
    void createTrip_Success_WithDates_GeneratesTripDays() {
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());

        Trip mappedTrip = new Trip();
        mappedTrip.setTitle("Italy Trip");
        mappedTrip.setDestination("Rome");
        mappedTrip.setStartDate(LocalDate.of(2026, 6, 1));
        mappedTrip.setEndDate(LocalDate.of(2026, 6, 3));
        mappedTrip.setDescription("Summer holiday");
        mappedTrip.setBudget(BigDecimal.valueOf(2000));
        mappedTrip.setCurrency("EUR");
        mappedTrip.setIsPublic(true);

        TripResponse expectedResponse = new TripResponse(
                UUID.randomUUID(),
                "Italy Trip",
                "Rome",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 3),
                "Summer holiday",
                BigDecimal.valueOf(2000),
                "EUR",
                null,
                true,
                LocalDateTime.now()
        );

        final TripCreateRequest request = new TripCreateRequest(
                "Italy Trip",
                "Rome",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 3),
                "Summer holiday",
                BigDecimal.valueOf(2000),
                "EUR",
                null,
                true
        );

        when(tripMapper.toEntity(request)).thenReturn(mappedTrip);
        when(tripRepository.save(any(Trip.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(tripMapper.toResponse(any(Trip.class))).thenReturn(expectedResponse);

        TripResponse response = tripService.createTrip(request, currentUser);

        assertNotNull(response);
        ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository, times(1)).save(tripCaptor.capture());
        Trip savedTrip = tripCaptor.getValue();

        assertSame(currentUser, savedTrip.getOwner());
        assertEquals("Rome", savedTrip.getDestination());
        assertEquals(LocalDate.of(2026, 6, 1), savedTrip.getStartDate());
        assertEquals(LocalDate.of(2026, 6, 3), savedTrip.getEndDate());

        assertEquals(3, savedTrip.getTripDays().size());
        assertEquals(1, savedTrip.getTripDays().get(0).getDayNumber());
        assertEquals(LocalDate.of(2026, 6, 1), savedTrip.getTripDays().get(0).getDate());
        assertSame(savedTrip, savedTrip.getTripDays().get(0).getTrip());

        assertEquals(2, savedTrip.getTripDays().get(1).getDayNumber());
        assertEquals(LocalDate.of(2026, 6, 2), savedTrip.getTripDays().get(1).getDate());

        assertEquals(3, savedTrip.getTripDays().get(2).getDayNumber());
        assertEquals(LocalDate.of(2026, 6, 3), savedTrip.getTripDays().get(2).getDate());
    }

    @Test
    void createTrip_Success_WithoutDates_NoTripDays() {
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());

        Trip mappedTrip = new Trip();
        mappedTrip.setTitle("Berlin Weekend");
        mappedTrip.setDestination("Berlin");

        final TripCreateRequest request = new TripCreateRequest(
                "Berlin Weekend",
                "Berlin",
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(tripMapper.toEntity(request)).thenReturn(mappedTrip);
        when(tripRepository.save(any(Trip.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        tripService.createTrip(request, currentUser);

        ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository, times(1)).save(tripCaptor.capture());
        Trip savedTrip = tripCaptor.getValue();

        assertEquals(0, savedTrip.getTripDays().size());
        assertFalse(savedTrip.getIsPublic());
    }

    @Test
    void createTrip_Success_StartDateAfterEndDate_NoTripDays() {
        User currentUser = new User();
        currentUser.setUserId(UUID.randomUUID());

        Trip mappedTrip = new Trip();
        mappedTrip.setTitle("Invalid Dates Trip");
        mappedTrip.setDestination("Tokyo");
        mappedTrip.setStartDate(LocalDate.of(2026, 6, 10));
        mappedTrip.setEndDate(LocalDate.of(2026, 6, 1));

        final TripCreateRequest request = new TripCreateRequest(
                "Invalid Dates Trip",
                "Tokyo",
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 1),
                null,
                null,
                null,
                null,
                false
        );

        when(tripMapper.toEntity(request)).thenReturn(mappedTrip);
        when(tripRepository.save(any(Trip.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        tripService.createTrip(request, currentUser);

        ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository, times(1)).save(tripCaptor.capture());
        Trip savedTrip = tripCaptor.getValue();

        assertEquals(0, savedTrip.getTripDays().size());
    }
}
