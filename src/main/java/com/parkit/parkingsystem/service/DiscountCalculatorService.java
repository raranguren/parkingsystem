package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.Ticket;

public class DiscountCalculatorService {

    private TicketDAO ticketDAO;

    public DiscountCalculatorService(TicketDAO ticketDAO) {
        this.ticketDAO = ticketDAO;
    }

    public void calculateDiscount(Ticket ticket) {
        String regNumber = ticket.getVehicleRegNumber();
        if (regNumber != null && !regNumber.isEmpty()) {
            Ticket previousTicket = ticketDAO.getTicket(regNumber);
            if (previousTicket != null && previousTicket.getVehicleRegNumber().equals(regNumber)) {
                double price = ticket.getPrice();
                price *= 1- Fare.RATE_PERCENT_DISCOUNT_FOR_RECURRING_USERS;
                ticket.setPrice(price);
            }
        }
    }
}
