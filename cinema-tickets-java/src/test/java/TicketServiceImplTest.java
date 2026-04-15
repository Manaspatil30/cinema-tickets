import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.TicketServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import static org.mockito.Mockito.verify;
public class TicketServiceImplTest {
    private TicketPaymentService ticketPaymentService;
    private SeatReservationService seatReservationService;
    private TicketServiceImpl ticketService;

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


}
