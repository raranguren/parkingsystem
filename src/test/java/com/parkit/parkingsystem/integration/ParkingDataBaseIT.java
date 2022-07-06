package com.parkit.parkingsystem.integration;

import com.parkit.parkingsystem.constants.Fare;
import com.parkit.parkingsystem.constants.ParkingType;
import com.parkit.parkingsystem.dao.ParkingSpotDAO;
import com.parkit.parkingsystem.dao.TicketDAO;
import com.parkit.parkingsystem.integration.config.DataBaseTestConfig;
import com.parkit.parkingsystem.integration.service.DataBasePrepareService;
import com.parkit.parkingsystem.model.ParkingSpot;
import com.parkit.parkingsystem.model.Ticket;
import com.parkit.parkingsystem.service.ParkingService;
import com.parkit.parkingsystem.util.InputReaderUtil;
import org.apache.logging.log4j.core.util.SystemMillisClock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;

import static java.lang.Math.round;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ParkingDataBaseIT {

    private static DataBaseTestConfig dataBaseTestConfig = new DataBaseTestConfig();
    private static ParkingSpotDAO parkingSpotDAO;
    private static TicketDAO ticketDAO;
    private static DataBasePrepareService dataBasePrepareService;
    private ParkingService parkingService;

    @Mock
    private static InputReaderUtil inputReaderUtil;

    @BeforeAll
    private static void setUp() throws Exception{
        parkingSpotDAO = new ParkingSpotDAO();
        parkingSpotDAO.dataBaseConfig = dataBaseTestConfig;
        ticketDAO = new TicketDAO();
        ticketDAO.dataBaseConfig = dataBaseTestConfig;
        dataBasePrepareService = new DataBasePrepareService();
    }

    @BeforeEach
    private void setUpPerTest() throws Exception {
        when(inputReaderUtil.readSelection()).thenReturn(1); // always select 1 : CAR
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF");
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){

    }

    @Test
    public void testParkingACar(){
        // ARRANGE
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        int slotNumberAvailableBefore = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
        // ACT
        parkingService.processIncomingVehicle();
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        ParkingSpot parkingSpot = ticket.getParkingSpot();
        int SlotNumberAvailableAfter = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
        // ASSERT
        assertNotNull(ticket.getInTime());
        assertNull(ticket.getOutTime());
        assertEquals(parkingSpot.getId(),slotNumberAvailableBefore);
        assertNotEquals(SlotNumberAvailableAfter,slotNumberAvailableBefore);
    }

    @Test
    public void testParkingLotExit(){
        // ARRANGE
        Date fiveHoursBefore = new Date(System.currentTimeMillis() - 5 * 60 * 60 * 1000);
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        ticket.setInTime(fiveHoursBefore);
        ticketDAO.updateTicket(ticket);
        // ACT
        parkingService.processExitingVehicle();
        ticket = ticketDAO.getTicket("ABCDEF");
        // ASSERT
        assertNotNull(ticket.getOutTime());
        assertNotEquals(ticket.getPrice(),0);
    }

    @Test
    public void testPriceFirstTime() {
        // ARRANGE
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        Ticket ticket = ticketDAO.getTicket("ABCDEF");

        long freeTimeInMillis = (long)(Fare.FREE_FIRST_HOURS * 60 * 60 * 1000);
        long paidTimeInMillis = 60 * 60 * 1000; // to be billed for 1 hour
        ticket.setInTime(new Date(System.currentTimeMillis() - paidTimeInMillis - freeTimeInMillis));
        ticketDAO.updateTicket(ticket);

        // ACT
        parkingService.processExitingVehicle();
        ticket = ticketDAO.getTicket("ABCDEF");
        // ASSERT
        assertEquals(Fare.CAR_RATE_PER_HOUR, ticket.getPrice());
    }

    @Test
    public void testPriceSecondTime() {
        // ARRANGE
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);

        parkingService.processIncomingVehicle();
        parkingService.processExitingVehicle();
        Ticket previousTicket = ticketDAO.getTicket("ABCDEF");
        Date previousDay = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        previousTicket.setInTime(previousDay);
        previousTicket.setOutTime(previousDay);
        ticketDAO.updateTicket(previousTicket);

        parkingService.processIncomingVehicle();
        Ticket ticket = ticketDAO.getTicket("ABCDEF");
        long freeTimeInMillis = (long)(Fare.FREE_FIRST_HOURS * 60 * 60 * 1000);
        long paidTimeInMillis = 60 * 60 * 1000; // to be billed for 1 hour
        ticket.setInTime(new Date(System.currentTimeMillis() - paidTimeInMillis - freeTimeInMillis));
        ticketDAO.updateTicket(ticket);

        // ACT
        parkingService.processExitingVehicle();
        ticket = ticketDAO.getTicket("ABCDEF");
        // ASSERT
        double expectedPriceNotRounded = Fare.CAR_RATE_PER_HOUR * (1 - Fare.RATE_PERCENT_DISCOUNT_FOR_RECURRING_USERS);
        double expectedPriceRounded = round(expectedPriceNotRounded * 100.0) / 100.0;
        assertEquals(expectedPriceRounded, ticket.getPrice());
    }


}
