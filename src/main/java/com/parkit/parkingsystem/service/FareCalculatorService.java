package com.parkit.parkingsystem.service;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.model.Ticket;

import static java.lang.Math.round;

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
        double price = billableHours * fare;
        price = round(price * 100) / 100.0; // rounding to 2 decimals
        ticket.setPrice(price);
    }

}