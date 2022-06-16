package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.DiscountCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DiscountCalculatorServiceTest {

    private static DiscountCalculatorService discountCalculatorService;
    Ticket ticket;

    @Mock
    private static TicketDAO ticketDAO;

    @BeforeEach
    private void setUpPerTest() {
        ticket = new Ticket();
        try {
            Ticket previousTicket = new Ticket();
            previousTicket.setVehicleRegNumber("ABCDEF");
            when(ticketDAO.getTicket(anyString())).thenAnswer( invocationOnMock ->
                    invocationOnMock.getArguments()[0] == "ABCDEF" ? previousTicket : null );
            discountCalculatorService = new DiscountCalculatorService(ticketDAO);
        } catch (Exception e) {
            e.printStackTrace();
            throw  new RuntimeException("Failed to set up test mock objects");
        }
    }

    @Test
    public void calculateDiscountSecondVisit() {
        // ARRANGE
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setPrice(100);
        // ACT
        discountCalculatorService.calculateDiscount(ticket);
        // ASSERT
        assertEquals(
                100 * (1 - Fare.RATE_PERCENT_DISCOUNT_FOR_RECURRING_USERS),
                ticket.getPrice());
    }

    @Test
    public void calculateDiscountFirstVisit() {
        // ARRANGE
        ticket.setVehicleRegNumber("SOMETHINGELSE");
        ticket.setPrice(100);
        // ACT
        discountCalculatorService.calculateDiscount(ticket);
        // ASSERT
        assertEquals(100, ticket.getPrice());
    }
}
