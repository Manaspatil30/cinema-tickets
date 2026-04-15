# Cinema Tickets - Manas Patil

The code checks everything
upfront before touching any calculations or external services.
If something's wrong with the request, it fails fast with a
clear message. The payment and seat reservation services are
injected through the constructor to keep the code loosely
coupled and easy to test.

## Tests
18 tests covering valid purchases, invalid account IDs,
bad ticket requests, and all business rule violations.