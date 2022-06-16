package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.model.Ticket;

import java.time.Duration;
import java.time.temporal.Temporal;
import java.util.concurrent.TimeUnit;

public class FareCalculatorService {

    public void calculateFare(Ticket ticket){
        if( (ticket.getOutTime() == null) || (ticket.getOutTime().before(ticket.getInTime())) ){
            throw new IllegalArgumentException("Out time provided is incorrect:"+ticket.getOutTime().toString());
        }

        // Using timestamps in milliseconds to account for midnight and daylight saving
        long inTimestamp = ticket.getInTime().getTime();
        long outTimestamp = ticket.getOutTime().getTime();
        long durationInMinutes = (outTimestamp - inTimestamp) / 60_000;
        double durationInHours = durationInMinutes / 60.0;

        // Parking is free for short periods of time
        double billableHours = durationInHours - Fare.FREE_FIRST_HOURS;
        if (billableHours < 0) billableHours = 0;

        // Calculating fare based on parking type
        double fare;
        switch (ticket.getParkingSpot().getParkingType()){
            case CAR: {
                fare = Fare.CAR_RATE_PER_HOUR;
                break;
            }
            case BIKE: {
                fare = Fare.BIKE_RATE_PER_HOUR;
                break;
            }
            default: throw new IllegalArgumentException("Unkown Parking Type");
        }

        // Applying price to ticket
        ticket.setPrice(billableHours * fare);
    }

    public void calculateFare(Ticket ticket, TicketDAO ticketDAO) {
        // First, calculate fare without discount for being recurrent user
        calculateFare(ticket);
        // Then, find out if there was a previous entry for the same registration number
        String regNumber = ticket.getVehicleRegNumber();
        if (regNumber != null && !regNumber.isEmpty()) {
            Ticket previousTicket = ticketDAO.getTicket(regNumber);
            if (previousTicket.getVehicleRegNumber().equals(regNumber)) {
                // If not the first time, then apply discount for recurrent users
                double price = ticket.getPrice();
                price *= 1-Fare.RATE_PERCENT_DISCOUNT_FOR_RECURRING_USERS;
                ticket.setPrice(price);
            }
        }
    }
}