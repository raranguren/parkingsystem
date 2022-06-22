package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.FareCalculatorService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.lang.Math.round;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Date;

public class FareCalculatorServiceTest {

    private static FareCalculatorService fareCalculatorService;
    private Ticket ticket;

    @BeforeAll
    private static void setUp() {
        fareCalculatorService = new FareCalculatorService();
    }

    @BeforeEach
    private void setUpPerTest() {
        ticket = new Ticket();
    }

    private void arrangeTestTicket(Date inTime, ParkingType parkingType) {
        ParkingSpot parkingSpot = new ParkingSpot(1, parkingType,false);
        ticket = new Ticket();
        ticket.setParkingSpot(parkingSpot);
        ticket.setInTime(inTime);
        ticket.setOutTime(new Date());
    }

    // Not a test - Gives correct price to be expected from tests
    private double predictCorrectPrice(double hours, double pricePerHour) {
        double billableHours = hours - Fare.FREE_FIRST_HOURS;
        if (billableHours < 0) billableHours = 0;
        double price =  billableHours * pricePerHour;
        // Prices rounded to 2 decimals
        return round(price * 100.0) / 100.0;
    }
    
    // Not a test - Gives a time that happened before
    private Date removeHoursFromCurrentTime(double hoursPassedSince) {
        return new Date(System.currentTimeMillis() - (long)(hoursPassedSince * 60 * 60 * 1000));
    }
    private Date removeMinutesFromCurrentTime(int minutesPassedSince) {
        return new Date(System.currentTimeMillis() - (long)minutesPassedSince * 60 * 1000);
    }

    @Test
    public void calculateFareCar(){
        arrangeTestTicket(removeHoursFromCurrentTime(1),ParkingType.CAR);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(ticket.getPrice(), predictCorrectPrice(1,Fare.CAR_RATE_PER_HOUR));
    }

    @Test
    public void calculateFareBike(){
        arrangeTestTicket(removeHoursFromCurrentTime(1),ParkingType.BIKE);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(ticket.getPrice(), predictCorrectPrice(1,Fare.BIKE_RATE_PER_HOUR));
    }

    @Test
    public void calculateFareUnknownType(){
        arrangeTestTicket(removeHoursFromCurrentTime(1),null);
        assertThrows(NullPointerException.class, () -> fareCalculatorService.calculateFare(ticket));
    }

    @Test
    public void calculateFareBikeWithFutureInTime(){
        Date futureDate = new Date(System.currentTimeMillis() + 60 * 60 * 1000); // adding time
        arrangeTestTicket(futureDate, ParkingType.BIKE);
        assertThrows(IllegalArgumentException.class, () -> fareCalculatorService.calculateFare(ticket));
    }

    @Test
    public void calculateFareBikeWithLessThanOneHourParkingTime(){
        arrangeTestTicket(removeMinutesFromCurrentTime(45), //45 minutes parking time
                ParkingType.BIKE);
        fareCalculatorService.calculateFare(ticket);
        assertEquals(predictCorrectPrice(0.75, Fare.BIKE_RATE_PER_HOUR), ticket.getPrice() );
    }

    @Test
    public void calculateFareCarWithLessThanOneHourParkingTime(){
        arrangeTestTicket(removeMinutesFromCurrentTime(45), //45 minutes parking time
                ParkingType.CAR);
        fareCalculatorService.calculateFare(ticket);
        assertEquals( predictCorrectPrice(0.75,Fare.CAR_RATE_PER_HOUR) , ticket.getPrice());
    }

    @Test
    public void calculateFareCarWithMoreThanADayParkingTime(){
        arrangeTestTicket(removeHoursFromCurrentTime(24), ParkingType.CAR);
        fareCalculatorService.calculateFare(ticket);
        assertEquals( predictCorrectPrice(24, Fare.CAR_RATE_PER_HOUR) , ticket.getPrice());
    }

    @Test
    public void calculateFareCarWithLessThanFreeTime() {
        arrangeTestTicket(removeHoursFromCurrentTime(Fare.FREE_FIRST_HOURS), ParkingType.CAR);
        fareCalculatorService.calculateFare(ticket);
        assertEquals( 0, ticket.getPrice());
    }

    @Test
    public void calculateFareBikeWithLessThanFreeTime() {
        arrangeTestTicket(removeHoursFromCurrentTime(Fare.FREE_FIRST_HOURS), ParkingType.BIKE);
        fareCalculatorService.calculateFare(ticket);
        assertEquals( 0, ticket.getPrice());
    }

}
