package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {

    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    public TicketServiceImpl(TicketPaymentService ticketPaymentService, SeatReservationService seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {
        int adultCount = countTickets(ticketTypeRequests, TicketTypeRequest.Type.ADULT);
        int childCount = countTickets(ticketTypeRequests, TicketTypeRequest.Type.CHILD);
        int infantCount = countTickets(ticketTypeRequests, TicketTypeRequest.Type.INFANT);

        int totalAmountToPay = (adultCount * 25) + (childCount * 15);

        int totalSeatsToReserve = adultCount + childCount;

        ticketPaymentService.makePayment(accountId, totalAmountToPay);
        seatReservationService.reserveSeat(accountId, totalSeatsToReserve);
    }

    private int countTickets(TicketTypeRequest[] requests, TicketTypeRequest.Type type){
        int total = 0;
        for (TicketTypeRequest request : requests){
            if (request.getTicketType() == type) {
                total += request.getNoOfTickets();
            }
        }
        return total;
    }

}
