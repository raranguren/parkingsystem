package com.parkit.parkingsystem;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.DiscountCalculatorService;
import com.parkit.parkingsystem.service.ParkingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
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
        discountCalculatorService = new DiscountCalculatorService(ticketDAO);
    }

    @Test
    public void calculateDiscountSecondVisit() {
        // ARRANGE
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setPrice(100);
        when(ticketDAO.countTicketsByRegNumber(anyString())).thenReturn(2);
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
        when(ticketDAO.countTicketsByRegNumber(anyString())).thenReturn(1);
        // ACT
        discountCalculatorService.calculateDiscount(ticket);
        // ASSERT
        assertEquals(100, ticket.getPrice()); // no discount on first visit
    }

    @Test
    public void calculateDiscountWhenRegNumberIsNotDefined() {
        // ARRANGE
        ticket.setPrice(100);
        // ACT
        discountCalculatorService.calculateDiscount(ticket);
        // ASSERT
        assertEquals(100, ticket.getPrice());
    }

    @Test
    public void calculateDiscountWhenPriceIsZero() {
        // ARRANGE
        ticket.setVehicleRegNumber("ABCDEF");
        ticket.setPrice(0);
        when(ticketDAO.countTicketsByRegNumber(anyString())).thenReturn(2);
        // ACT
        discountCalculatorService.calculateDiscount(ticket);
        // ASSERT
        assertEquals(0, ticket.getPrice());
    }

    @Test
    public void calculateDiscountWithNeglagiblePrice() {
        // ARRANGE
        ticket.setPrice(0.01);
        ticket.setVehicleRegNumber("ABCDEF");
        when(ticketDAO.countTicketsByRegNumber(anyString())).thenReturn(2);
        // ACT
        discountCalculatorService.calculateDiscount(ticket);
        // ASSERT
        if (Fare.RATE_PERCENT_DISCOUNT_FOR_RECURRING_USERS < 0.5) {
            assertEquals(0.01,ticket.getPrice());
        } else {
            assertEquals(0.00,ticket.getPrice());
        }
    }

}
