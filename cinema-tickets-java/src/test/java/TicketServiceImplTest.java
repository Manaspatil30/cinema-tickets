import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.TicketServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class TicketServiceImplTest {
    private TicketPaymentService ticketPaymentService;
    private SeatReservationService seatReservationService;
    private TicketServiceImpl ticketService;

    //Valid Ticket Purchase Scenarios
    @BeforeEach
    void setup(){
        ticketPaymentService = Mockito.mock(TicketPaymentService.class);
        seatReservationService = Mockito.mock(SeatReservationService.class);
        ticketService = new TicketServiceImpl(ticketPaymentService, seatReservationService);
    }

    @Test
    @DisplayName("Adult only - correct payment and seat count")
    void purchaseAdultTicketsOnly() {
        ticketService.purchaseTickets(1L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 3));

        verify(ticketPaymentService).makePayment(1L, 75); // 3 x £25
        verify(seatReservationService).reserveSeat(1L, 3);
    }

    @Test
    @DisplayName("Adult and child tickets - correct payment and seats")
    void purchaseAdultAndChildTickets() {
        ticketService.purchaseTickets(1L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 3));

        verify(ticketPaymentService).makePayment(1L, 95); // 2x25 + 3x15
        verify(seatReservationService).reserveSeat(1L, 5);
    }

    @Test
    @DisplayName("Adult, child and infant tickets - infants pay nothing and get no seat")
    void purchaseAdultChildAndInfantTickets() {
        ticketService.purchaseTickets(1L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2),
                new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1),
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2));

        verify(ticketPaymentService).makePayment(1L, 65); // 2x25 + 1x15 + 2x0
        verify(seatReservationService).reserveSeat(1L, 3); // adults + children only
    }

    @Test
    @DisplayName("Maximum 25 tickets allowed")
    void purchaseMaximumTickets() {
        ticketService.purchaseTickets(1L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 25));

        verify(ticketPaymentService).makePayment(1L, 625);
        verify(seatReservationService).reserveSeat(1L, 25);
    }

    @Test
    @DisplayName("Infant count equal to adult count is valid")
    void infantCountEqualToAdultCount() {
        ticketService.purchaseTickets(1L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 3),
                new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 3));

        verify(ticketPaymentService).makePayment(1L, 75); // 3x25, infants free
        verify(seatReservationService).reserveSeat(1L, 3); // only adults
    }

    @Test
    @DisplayName("Large account ID is valid")
    void largeAccountId() {
        ticketService.purchaseTickets(999999L,
                new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1));

        verify(ticketPaymentService).makePayment(999999L, 25);
        verify(seatReservationService).reserveSeat(999999L, 1);
    }

    //Invalid account ID Scenarios

    @Test
    @DisplayName("Account ID zero is rejected")
    void accountIdZeroThrows() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(0L,
                        new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1)));

        verifyNoInteractions(ticketPaymentService, seatReservationService);
    }

    @Test
    @DisplayName("Negative account ID is rejected")
    void negativeAccountIdThrows() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(-1L,
                        new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1)));

        verifyNoInteractions(ticketPaymentService, seatReservationService);
    }

    @Test
    @DisplayName("Null account is rejected")
    void nullAccountIdThrows() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(null,
                        new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1)));

        verifyNoInteractions(ticketPaymentService, seatReservationService);
    }

    //Invalid Ticket request Scenarios

    @Test
    @DisplayName("No ticket requests throws InvalidPurchaseException")
    void noTicketRequestsThrows() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L));

        verifyNoInteractions(ticketPaymentService, seatReservationService);
    }

    @Test
    @DisplayName("Null ticket requests array throws InvalidPurchaseException")
    void nullTicketRequestsThrows() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L, (TicketTypeRequest[]) null));

        verifyNoInteractions(ticketPaymentService, seatReservationService);
    }

    @Test
    @DisplayName("Zero quantity in a ticket request throws InvalidPurchaseException")
    void zeroQuantityTicketRequestThrows() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                        new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0)));

        verifyNoInteractions(ticketPaymentService, seatReservationService);
    }

    //Business Rules

    @Test
    @DisplayName("More than 25 tickets throws InvalidPurchaseException")
    void moreThan25TicketsThrows() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                        new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 26)));

        verifyNoInteractions(ticketPaymentService, seatReservationService);
    }

    @Test
    @DisplayName("Child ticket without adult throws InvalidPurchaseException")
    void childTicketWithoutAdultThrows() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                        new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2)));

        verifyNoInteractions(ticketPaymentService, seatReservationService);
    }

    @Test
    @DisplayName("Infant ticket without adult throws InvalidPurchaseException")
    void infantTicketWithoutAdultThrows() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                        new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1)));

        verifyNoInteractions(ticketPaymentService, seatReservationService);
    }

    @Test
    @DisplayName("More infants than adults throws InvalidPurchaseException")
    void moreInfantsThanAdultsThrows() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                        new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2),
                        new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 3)));

        verifyNoInteractions(ticketPaymentService, seatReservationService);
    }

    @Test
    @DisplayName("Child and infant without adult throws InvalidPurchaseException")
    void childAndInfantWithoutAdultThrows() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                        new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2),
                        new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1)));

        verifyNoInteractions(ticketPaymentService, seatReservationService);
    }

    @Test
    @DisplayName("Mixed ticket types exceeding 25 total throws InvalidPurchaseException")
    void mixedTicketsExceeding25Throws() {
        assertThrows(InvalidPurchaseException.class, () ->
                ticketService.purchaseTickets(1L,
                        new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 10),
                        new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 10),
                        new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 6)));

        verifyNoInteractions(ticketPaymentService, seatReservationService);
    }

}
