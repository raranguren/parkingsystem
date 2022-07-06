package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.Ticket;

import static java.lang.Math.round;

public class DiscountCalculatorService {

    private TicketDAO ticketDAO;

    public DiscountCalculatorService(TicketDAO ticketDAO) {
        this.ticketDAO = ticketDAO;
    }

    public void calculateDiscount(Ticket ticket) {
        String regNumber = ticket.getVehicleRegNumber();
        if (regNumber != null && !regNumber.isEmpty()) {
            int timesParkedWithSameRegNumber = ticketDAO.countTicketsByRegNumber(regNumber);
            if (timesParkedWithSameRegNumber > 1) {
                double price = ticket.getPrice();
                price *= 1- Fare.RATE_PERCENT_DISCOUNT_FOR_RECURRING_USERS;
                price = round(price * 100) / 100.0; // rounding to 2 decimals
                ticket.setPrice(price);
            }
        }
    }
}
