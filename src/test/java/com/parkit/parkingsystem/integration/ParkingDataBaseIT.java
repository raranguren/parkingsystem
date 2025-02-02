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
import java.util.concurrent.TimeUnit;

import static java.lang.Math.round;
import static org.mockito.ArgumentMatchers.anyString;
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
        dataBasePrepareService.clearDataBaseEntries();
    }

    @AfterAll
    private static void tearDown(){

    }

    @Test
    public void testParkingACar() throws Exception {
        // ARRANGE
        when(inputReaderUtil.readSelection()).thenReturn(1); // select 1 : CAR
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF1");
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        int slotNumberAvailableBefore = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
        // ACT
        parkingService.processIncomingVehicle();
        Ticket ticket = ticketDAO.getTicket("ABCDEF1");
        ParkingSpot parkingSpot = ticket.getParkingSpot();
        int SlotNumberAvailableAfter = parkingSpotDAO.getNextAvailableSlot(ParkingType.CAR);
        // ASSERT
        assertNotNull(ticket.getInTime());
        assertNull(ticket.getOutTime());
        assertEquals(parkingSpot.getId(),slotNumberAvailableBefore);
        assertNotEquals(SlotNumberAvailableAfter,slotNumberAvailableBefore);
    }

    @Test
    public void testParkingABike() throws Exception {
        // ARRANGE
        when(inputReaderUtil.readSelection()).thenReturn(2); // select 2 : BIKE
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF2");
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        int slotNumberAvailableBefore = parkingSpotDAO.getNextAvailableSlot(ParkingType.BIKE);
        // ACT
        parkingService.processIncomingVehicle();
        Ticket ticket = ticketDAO.getTicket("ABCDEF2");
        ParkingSpot parkingSpot = ticket.getParkingSpot();
        int SlotNumberAvailableAfter = parkingSpotDAO.getNextAvailableSlot(ParkingType.BIKE);
        // ASSERT
        assertNotNull(ticket.getInTime());
        assertNull(ticket.getOutTime());
        assertEquals(parkingSpot.getId(),slotNumberAvailableBefore);
        assertNotEquals(SlotNumberAvailableAfter,slotNumberAvailableBefore);
    }

    @Test
    public void testParkingLotExit() throws Exception {
        // ARRANGE
        when(inputReaderUtil.readSelection()).thenReturn(1); // select 1 : CAR
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF3");
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        Ticket ticket = ticketDAO.getTicket("ABCDEF3");
        Date fiveHoursBefore = new Date(System.currentTimeMillis() - 5 * 60 * 60 * 1000);
        ticket.setInTime(fiveHoursBefore);
        ticketDAO.updateTicket(ticket);
        TimeUnit.SECONDS.sleep(1);
        // ACT
        parkingService.processExitingVehicle();
        ticket = ticketDAO.getTicket("ABCDEF3");
        // ASSERT
        assertNotNull(ticket.getOutTime());
        assertNotEquals(ticket.getPrice(),0);
    }

    @Test
    public void testPriceFirstTimeWithCar() throws Exception {
        // ARRANGE
        when(inputReaderUtil.readSelection()).thenReturn(1); // select 1 : CAR
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF4");
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        Ticket ticket = ticketDAO.getTicket("ABCDEF4");

        long freeTimeInMillis = (long)(Fare.FREE_FIRST_HOURS * 60 * 60 * 1000);
        long paidTimeInMillis = 60 * 60 * 1000; // to be billed for 1 hour
        ticket.setInTime(new Date(System.currentTimeMillis() - paidTimeInMillis - freeTimeInMillis));
        ticketDAO.updateTicket(ticket);
        TimeUnit.SECONDS.sleep(1);

        // ACT
        parkingService.processExitingVehicle();
        ticket = ticketDAO.getTicket("ABCDEF4");
        // ASSERT
        assertEquals(Fare.CAR_RATE_PER_HOUR, ticket.getPrice());
    }

    @Test
    public void testPriceSecondTimeWithCar() throws Exception {
        // ARRANGE
        when(inputReaderUtil.readSelection()).thenReturn(1); // select 1 : CAR
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF5");
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        TimeUnit.SECONDS.sleep(1);
        parkingService.processExitingVehicle();
        TimeUnit.SECONDS.sleep(1);
        Ticket previousTicket = ticketDAO.getTicket("ABCDEF5");
        Date previousDay = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        previousTicket.setInTime(previousDay);
        previousTicket.setOutTime(previousDay);
        ticketDAO.updateTicket(previousTicket);
        TimeUnit.SECONDS.sleep(1);
        parkingService.processIncomingVehicle();
        Ticket ticket = ticketDAO.getTicket("ABCDEF5");
        long freeTimeInMillis = (long)(Fare.FREE_FIRST_HOURS * 60 * 60 * 1000);
        long paidTimeInMillis = 60 * 60 * 1000; // to be billed for 1 hour
        ticket.setInTime(new Date(System.currentTimeMillis() - paidTimeInMillis - freeTimeInMillis));
        ticketDAO.updateTicket(ticket);
        TimeUnit.SECONDS.sleep(1);

        // ACT
        parkingService.processExitingVehicle();
        ticket = ticketDAO.getTicket("ABCDEF5");
        // ASSERT
        double expectedPriceNotRounded = Fare.CAR_RATE_PER_HOUR * (1 - Fare.RATE_PERCENT_DISCOUNT_FOR_RECURRING_USERS);
        double expectedPriceRounded = round(expectedPriceNotRounded * 100.0) / 100.0;
        assertEquals(expectedPriceRounded, ticket.getPrice());
    }

    @Test
    public void testPriceFirstTimeWithBike() throws Exception {
        // ARRANGE
        when(inputReaderUtil.readSelection()).thenReturn(2); // select 2 : BIKE
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF6");
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        Ticket ticket = ticketDAO.getTicket("ABCDEF6");

        long freeTimeInMillis = (long)(Fare.FREE_FIRST_HOURS * 60 * 60 * 1000);
        long paidTimeInMillis = 60 * 60 * 1000; // to be billed for 1 hour
        ticket.setInTime(new Date(System.currentTimeMillis() - paidTimeInMillis - freeTimeInMillis));
        ticketDAO.updateTicket(ticket);
        TimeUnit.SECONDS.sleep(1);

        // ACT
        parkingService.processExitingVehicle();
        ticket = ticketDAO.getTicket("ABCDEF6");
        // ASSERT
        assertEquals(Fare.BIKE_RATE_PER_HOUR, ticket.getPrice());
    }

    @Test
    public void testPriceSecondTimeWithBike() throws Exception {
        // ARRANGE
        when(inputReaderUtil.readSelection()).thenReturn(2); // select 2 : BIKE
        when(inputReaderUtil.readVehicleRegistrationNumber()).thenReturn("ABCDEF7");
        parkingService = new ParkingService(inputReaderUtil, parkingSpotDAO, ticketDAO);
        parkingService.processIncomingVehicle();
        TimeUnit.SECONDS.sleep(1);
        parkingService.processExitingVehicle();
        TimeUnit.SECONDS.sleep(1);
        Ticket previousTicket = ticketDAO.getTicket("ABCDEF7");
        Date previousDay = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000);
        previousTicket.setInTime(previousDay);
        previousTicket.setOutTime(previousDay);
        ticketDAO.updateTicket(previousTicket);
        parkingService.processIncomingVehicle();
        TimeUnit.SECONDS.sleep(1);
        Ticket ticket = ticketDAO.getTicket("ABCDEF7");
        long freeTimeInMillis = (long)(Fare.FREE_FIRST_HOURS * 60 * 60 * 1000);
        long paidTimeInMillis = 60 * 60 * 1000; // to be billed for 1 hour
        ticket.setInTime(new Date(System.currentTimeMillis() - paidTimeInMillis - freeTimeInMillis));
        ticketDAO.updateTicket(ticket);
        TimeUnit.SECONDS.sleep(1);

        // ACT
        parkingService.processExitingVehicle();
        ticket = ticketDAO.getTicket("ABCDEF7");
        // ASSERT
        double expectedPriceNotRounded = Fare.BIKE_RATE_PER_HOUR * (1 - Fare.RATE_PERCENT_DISCOUNT_FOR_RECURRING_USERS);
        double expectedPriceRounded = round(expectedPriceNotRounded * 100.0) / 100.0;
        assertEquals(expectedPriceRounded, ticket.getPrice());
    }

}
