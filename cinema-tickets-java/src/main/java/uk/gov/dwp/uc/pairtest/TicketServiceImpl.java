package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

public class TicketServiceImpl implements TicketService {

    private static final int MAX_TICKETS = 25;
    private static final int ADULT_TICKET_PRICE = 25;
    private static final int CHILD_TICKET_PRICE = 15;

    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    public TicketServiceImpl(TicketPaymentService ticketPaymentService, SeatReservationService seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {

        validateAccountId(accountId);
        validateTicketRequests(ticketTypeRequests);

        int adultCount = countTickets(ticketTypeRequests, TicketTypeRequest.Type.ADULT);
        int childCount = countTickets(ticketTypeRequests, TicketTypeRequest.Type.CHILD);
        int infantCount = countTickets(ticketTypeRequests, TicketTypeRequest.Type.INFANT);

        int totalTickets = adultCount + childCount + infantCount;

        validateBusinessRules(adultCount,childCount,infantCount, totalTickets);

        int totalAmountToPay = (adultCount * ADULT_TICKET_PRICE)
                + (childCount * CHILD_TICKET_PRICE);

        //Infants sit on adult's lap therefore no seat allocated
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

    private void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new InvalidPurchaseException("Invalid account ID: account ID must be greater than zero.");
        }
    }

    private void validateTicketRequests(TicketTypeRequest[] ticketTypeRequests) {
        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException("No ticket requests provided.");
        }
        for (TicketTypeRequest request : ticketTypeRequests) {
            if (request == null) {
                throw new InvalidPurchaseException("Ticket request cannot be null.");
            }
            if (request.getNoOfTickets() <= 0) {
                throw new InvalidPurchaseException(
                        "Number of tickets must be greater than zero for type: " + request.getTicketType());
            }
        }
    }

    private void validateBusinessRules(int adultCount, int childCount,
                                       int infantCount, int totalTickets) {
        if (totalTickets > MAX_TICKETS) {
            throw new InvalidPurchaseException(
                    "Cannot purchase more than " + MAX_TICKETS + " tickets at a time.");
        }
        if (adultCount == 0 && (childCount > 0 || infantCount > 0)) {
            throw new InvalidPurchaseException(
                    "Child and Infant tickets cannot be purchased without at least one Adult ticket.");
        }
        if (infantCount > adultCount) {
            throw new InvalidPurchaseException(
                    "Number of Infants cannot exceed the number of Adults (each infant sits on an adult's lap).");
        }
    }

}
