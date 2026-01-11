# Movie Booking Domain - Low-Level Design (LLD)

## Document Information
| Attribute | Value |
|-----------|-------|
| Domain | Movie Ticket Booking (BookMyShow-style) |
| Version | 1.0 |
| Dependencies | Master HLD |

---

## 1. Domain Overview

### 1.1 What We're Testing

The Booking domain covers the complete movie ticket booking lifecycle:

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                           BOOKING DOMAIN SCOPE                                           │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  ┌───────────────────────────────────────────────────────────────────────────────────┐  │
│  │                                                                                    │  │
│  │   DISCOVERY         SELECTION          BOOKING            POST-BOOKING           │  │
│  │   ─────────         ─────────          ───────            ────────────           │  │
│  │                                                                                    │  │
│  │   • Search movie    • Choose cinema    • Enter details    • View ticket          │  │
│  │   • Filter by       • Select showtime  • Apply offers     • Download ticket      │  │
│  │     - Location      • Pick seats       • Pay              • Cancel booking       │  │
│  │     - Language      • Add F&B          • Confirm          • Refund               │  │
│  │     - Genre         • Review order                        • Reschedule           │  │
│  │   • View ratings                                                                  │  │
│  │                                                                                    │  │
│  └───────────────────────────────────────────────────────────────────────────────────┘  │
│                                                                                          │
│  UNIQUE CHALLENGES:                                                                      │
│  ──────────────────                                                                      │
│  1. TIME-SENSITIVE INVENTORY: Seats can be booked by others during test                │
│  2. SEAT LOCKING: Seats locked for X minutes, must handle expiry                        │
│  3. REAL-TIME AVAILABILITY: Seat map changes dynamically                                │
│  4. SHOWTIME DEPENDENCY: Tests depend on future showtimes being available               │
│  5. LOCATION-BASED: Results vary by user location                                       │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 Testing Philosophy for Bookings

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                         BOOKING TESTING PHILOSOPHY                                       │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  PRINCIPLE: "Reserve via API, Confirm via UI, Verify via API"                          │
│                                                                                          │
│  WHY?                                                                                    │
│  ────                                                                                    │
│  • Seat selection is RACE-PRONE - API gives guaranteed inventory                        │
│  • Seat map UI is critical UX - must test visual selection                              │
│  • Payment is same as e-commerce - reuse payment patterns                               │
│  • Booking confirmation via API is reliable verification                                │
│                                                                                          │
│  THE SEAT AVAILABILITY PROBLEM:                                                          │
│  ──────────────────────────────                                                          │
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │  PROBLEM: If tests search and select seats via UI, another user might book     │   │
│  │           those seats between selection and payment → FLAKY TEST               │   │
│  │                                                                                  │   │
│  │  SOLUTION: Use API to:                                                          │   │
│  │            1. Find available show with sufficient seats                         │   │
│  │            2. Lock/hold specific seats before UI test                           │   │
│  │            3. Test UI with guaranteed seat availability                         │   │
│  │            4. Release locks in teardown if test fails                           │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
│  HYBRID APPROACH:                                                                        │
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │                                                                                  │   │
│  │  API                              UI                          API               │   │
│  │  ───                              ──                          ───               │   │
│  │  1. Get available shows           4. Navigate to seat map     8. Verify order   │   │
│  │  2. Filter by test criteria       5. Visual seat selection    9. Get ticket     │   │
│  │  3. Hold/lock seats               6. Confirm selection        10. Validate      │   │
│  │                                   7. Complete payment             booking       │   │
│  │                                                                                  │   │
│  │  Time: ~1s                        Time: ~5s                   Time: ~0.5s       │   │
│  │                                                                                  │   │
│  │  TOTAL: ~6.5s vs Traditional 30-45s                                             │   │
│  │                                                                                  │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Component Architecture

### 2.1 Booking Module Structure

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                          BOOKING MODULE COMPONENTS                                       │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  booking/                                                                                │
│  ├── api/                              # API Layer                                      │
│  │   ├── MovieApiClient.java           # Movie catalog queries                          │
│  │   ├── CinemaApiClient.java          # Cinema and showtime data                       │
│  │   ├── SeatApiClient.java            # Seat availability and locking                  │
│  │   ├── BookingApiClient.java         # Booking CRUD                                   │
│  │   └── TicketApiClient.java          # Ticket retrieval                               │
│  │                                                                                       │
│  ├── pages/                            # UI Layer (Page Objects)                        │
│  │   ├── MovieListPage.java            # Movie search and listings                      │
│  │   ├── MovieDetailPage.java          # Movie info and showtimes                       │
│  │   ├── SeatSelectionPage.java        # Seat map interaction                           │
│  │   ├── BookingSummaryPage.java       # Order review                                   │
│  │   ├── TicketPage.java               # View/download ticket                           │
│  │   └── components/                   # Reusable UI components                         │
│  │       ├── SeatMapComponent.java     # Interactive seat map                           │
│  │       ├── ShowtimeSelector.java     # Date/time picker                               │
│  │       ├── CinemaCard.java           # Cinema display card                            │
│  │       └── TicketWidget.java         # Ticket display                                 │
│  │                                                                                       │
│  ├── models/                           # Data Models                                    │
│  │   ├── Movie.java                                                                     │
│  │   ├── Cinema.java                                                                    │
│  │   ├── Show.java                                                                      │
│  │   ├── Seat.java                                                                      │
│  │   ├── SeatLayout.java               # 2D seat arrangement                            │
│  │   ├── Booking.java                                                                   │
│  │   ├── Ticket.java                                                                    │
│  │   └── builders/                                                                      │
│  │       ├── ShowQueryBuilder.java     # Fluent show search                             │
│  │       └── BookingBuilder.java       # Test booking creation                          │
│  │                                                                                       │
│  ├── playbooks/                        # Reusable Workflows                             │
│  │   ├── ShowFinderPlaybook.java       # Find suitable show for testing                 │
│  │   ├── SeatReservationPlaybook.java  # Reserve seats via API                          │
│  │   ├── BookingPlaybook.java          # Complete booking flow                          │
│  │   └── CancellationPlaybook.java     # Cancel and refund flow                         │
│  │                                                                                       │
│  ├── tests/                            # Test Classes                                   │
│  │   ├── MovieSearchTests.java         # Discovery flow tests                           │
│  │   ├── SeatSelectionTests.java       # Seat map interaction tests                     │
│  │   ├── BookingFlowTests.java         # E2E booking tests                              │
│  │   ├── CancellationTests.java        # Cancel/refund tests                            │
│  │   └── ConcurrencyTests.java         # Race condition tests                           │
│  │                                                                                       │
│  └── data/                             # Test Data                                      │
│      ├── test-movies.json              # Test movie catalog                             │
│      ├── seat-configs.json             # Seat layout patterns                           │
│      └── booking-scenarios.json        # Pre-defined test scenarios                     │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 Seat Map Architecture

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                             SEAT MAP ARCHITECTURE                                        │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  THE SEAT MAP CHALLENGE:                                                                 │
│  ───────────────────────                                                                 │
│  • Real-time availability                                                               │
│  • Complex 2D layout (rows, gaps, aisles)                                              │
│  • Different seat types (regular, premium, recliner)                                   │
│  • Price variations by position                                                         │
│  • Visual representation must match API state                                           │
│                                                                                          │
│  SEAT MAP DATA MODEL:                                                                    │
│  ─────────────────────                                                                   │
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │  {                                                                               │   │
│  │    "showId": "SHOW-2024-001",                                                    │   │
│  │    "screenLayout": {                                                             │   │
│  │      "rows": 15,                                                                 │   │
│  │      "seatsPerRow": 20,                                                          │   │
│  │      "aisles": [5, 15],    // Column gaps                                        │   │
│  │      "screen": "top"                                                             │   │
│  │    },                                                                            │   │
│  │    "seatCategories": [                                                           │   │
│  │      { "name": "RECLINER", "rows": [1, 2], "price": 500 },                       │   │
│  │      { "name": "PREMIUM", "rows": [3, 4, 5], "price": 350 },                     │   │
│  │      { "name": "REGULAR", "rows": [6, 7, 8, 9, 10], "price": 200 },              │   │
│  │      { "name": "FRONT", "rows": [11, 12, 13, 14, 15], "price": 150 }             │   │
│  │    ],                                                                            │   │
│  │    "seats": [                                                                    │   │
│  │      { "id": "A1", "row": 1, "col": 1, "status": "AVAILABLE" },                  │   │
│  │      { "id": "A2", "row": 1, "col": 2, "status": "BOOKED" },                     │   │
│  │      { "id": "A3", "row": 1, "col": 3, "status": "LOCKED", "lockExpiry": "..." } │   │
│  │    ]                                                                             │   │
│  │  }                                                                               │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
│  SEAT STATUS STATE MACHINE:                                                              │
│  ──────────────────────────                                                              │
│                                                                                          │
│       ┌──────────────┐                                                                  │
│       │              │                                                                  │
│       │  AVAILABLE   │◄───────────────────────────────────┐                             │
│       │              │                                    │                             │
│       └──────┬───────┘                                    │                             │
│              │                                            │                             │
│              │ User clicks                                │ Lock expires OR             │
│              │ or API lock                                │ booking cancelled           │
│              ▼                                            │                             │
│       ┌──────────────┐                                    │                             │
│       │              │                                    │                             │
│       │   LOCKED     │────────────────────────────────────┘                             │
│       │  (5 min TTL) │                                                                  │
│       │              │                                                                  │
│       └──────┬───────┘                                                                  │
│              │                                                                          │
│              │ Payment success                                                          │
│              ▼                                                                          │
│       ┌──────────────┐                                                                  │
│       │              │                                                                  │
│       │   BOOKED     │                                                                  │
│       │              │                                                                  │
│       └──────────────┘                                                                  │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Page Objects Design

### 3.1 SeatSelectionPage Implementation

```java
/**
 * SeatSelectionPage - Handles interactive seat map
 *
 * Design Considerations:
 * 1. Seat map is a complex 2D grid - need careful locator strategy
 * 2. Real-time updates via WebSocket - must wait for stability
 * 3. Visual state (color) indicates availability
 * 4. Mobile responsive - different layouts possible
 */
public class SeatSelectionPage extends BasePage {

    // ═══════════════════════════════════════════════════════════════════════════
    // LOCATORS
    // ═══════════════════════════════════════════════════════════════════════════

    // Seat Map Container
    private static final String SEAT_MAP_CONTAINER = "[data-testid='seat-map']";
    private static final String SEAT_ROW = "[data-testid='seat-row-%s']";
    private static final String SEAT = "[data-testid='seat-%s']";

    // Seat States (CSS classes or data attributes)
    private static final String SEAT_AVAILABLE = "[data-status='available']";
    private static final String SEAT_SELECTED = "[data-status='selected']";
    private static final String SEAT_BOOKED = "[data-status='booked']";
    private static final String SEAT_LOCKED = "[data-status='locked']";

    // Summary Panel
    private static final String SELECTED_SEATS_COUNT = "[data-testid='selected-count']";
    private static final String TOTAL_PRICE = "[data-testid='total-price']";
    private static final String PROCEED_BUTTON = "[data-testid='proceed-btn']";

    // Loading States
    private static final String SEAT_MAP_LOADING = ".seat-map-loading";
    private static final String SEAT_UPDATING = ".seat-updating";

    // ═══════════════════════════════════════════════════════════════════════════
    // CONSTRUCTOR & NAVIGATION
    // ═══════════════════════════════════════════════════════════════════════════

    public SeatSelectionPage(Page page) {
        super(page);
    }

    public SeatSelectionPage navigate(String showId) {
        page.navigate(baseUrl + "/show/" + showId + "/seats");
        waitForSeatMapLoad();
        return this;
    }

    private void waitForSeatMapLoad() {
        // Wait for loading spinner to disappear
        page.waitForSelector(SEAT_MAP_LOADING,
            new Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.HIDDEN)
                .setTimeout(10000));

        // Wait for seat map to be visible
        page.waitForSelector(SEAT_MAP_CONTAINER);

        // Wait for at least some seats to be rendered
        page.waitForFunction("document.querySelectorAll('[data-testid^=\"seat-\"]').length > 0");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SEAT SELECTION
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Selects a specific seat by ID (e.g., "A5", "B12")
     */
    public SeatSelectionPage selectSeat(String seatId) {
        String locator = String.format(SEAT, seatId);
        Locator seat = page.locator(locator);

        // Verify seat is available
        if (!isSeatAvailable(seatId)) {
            throw new SeatNotAvailableException("Seat " + seatId + " is not available");
        }

        seat.click();

        // Wait for selection to register
        page.waitForSelector(locator + "[data-status='selected']");

        return this;
    }

    /**
     * Selects multiple seats
     */
    public SeatSelectionPage selectSeats(List<String> seatIds) {
        for (String seatId : seatIds) {
            selectSeat(seatId);
        }
        return this;
    }

    /**
     * Auto-selects N available seats from preferred category
     * Used when specific seats don't matter
     */
    public List<String> autoSelectSeats(int count, SeatCategory category) {
        List<String> selectedSeats = new ArrayList<>();

        // Get all available seats in category
        String categoryLocator = String.format(
            "%s[data-category='%s']",
            SEAT_AVAILABLE,
            category.name()
        );

        List<Locator> availableSeats = page.locator(categoryLocator).all();

        if (availableSeats.size() < count) {
            throw new InsufficientSeatsException(
                "Only " + availableSeats.size() + " seats available in " + category);
        }

        // Select consecutive seats if possible for better UX
        List<String> consecutiveGroup = findConsecutiveSeats(availableSeats, count);
        if (consecutiveGroup != null) {
            for (String seatId : consecutiveGroup) {
                selectSeat(seatId);
                selectedSeats.add(seatId);
            }
        } else {
            // Fallback: select first N available
            for (int i = 0; i < count; i++) {
                String seatId = availableSeats.get(i).getAttribute("data-testid")
                    .replace("seat-", "");
                selectSeat(seatId);
                selectedSeats.add(seatId);
            }
        }

        return selectedSeats;
    }

    private List<String> findConsecutiveSeats(List<Locator> seats, int count) {
        // Group seats by row
        Map<String, List<Integer>> rowSeats = new TreeMap<>();

        for (Locator seat : seats) {
            String seatId = seat.getAttribute("data-testid").replace("seat-", "");
            String row = seatId.replaceAll("\\d", "");
            int col = Integer.parseInt(seatId.replaceAll("\\D", ""));

            rowSeats.computeIfAbsent(row, k -> new ArrayList<>()).add(col);
        }

        // Find a row with N consecutive seats
        for (Map.Entry<String, List<Integer>> entry : rowSeats.entrySet()) {
            String row = entry.getKey();
            List<Integer> cols = entry.getValue();
            Collections.sort(cols);

            int consecutive = 1;
            int start = cols.get(0);

            for (int i = 1; i < cols.size(); i++) {
                if (cols.get(i) == cols.get(i - 1) + 1) {
                    consecutive++;
                    if (consecutive >= count) {
                        List<String> result = new ArrayList<>();
                        for (int j = 0; j < count; j++) {
                            result.add(row + (start + j));
                        }
                        return result;
                    }
                } else {
                    consecutive = 1;
                    start = cols.get(i);
                }
            }
        }

        return null;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SEAT STATE QUERIES
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean isSeatAvailable(String seatId) {
        String locator = String.format(SEAT, seatId);
        return page.locator(locator).getAttribute("data-status").equals("available");
    }

    public boolean isSeatSelected(String seatId) {
        String locator = String.format(SEAT, seatId);
        return page.locator(locator).getAttribute("data-status").equals("selected");
    }

    public boolean isSeatBooked(String seatId) {
        String locator = String.format(SEAT, seatId);
        String status = page.locator(locator).getAttribute("data-status");
        return status.equals("booked") || status.equals("locked");
    }

    public List<String> getSelectedSeats() {
        List<String> selected = new ArrayList<>();
        List<Locator> selectedSeats = page.locator(SEAT_SELECTED).all();

        for (Locator seat : selectedSeats) {
            String seatId = seat.getAttribute("data-testid").replace("seat-", "");
            selected.add(seatId);
        }

        return selected;
    }

    public int getAvailableSeatCount() {
        return page.locator(SEAT_AVAILABLE).count();
    }

    public int getAvailableSeatCount(SeatCategory category) {
        String locator = String.format(
            "%s[data-category='%s']",
            SEAT_AVAILABLE,
            category.name()
        );
        return page.locator(locator).count();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // SUMMARY & CHECKOUT
    // ═══════════════════════════════════════════════════════════════════════════

    public int getSelectedSeatCount() {
        return Integer.parseInt(page.locator(SELECTED_SEATS_COUNT).textContent());
    }

    public BigDecimal getTotalPrice() {
        String priceText = page.locator(TOTAL_PRICE).textContent();
        // Remove currency symbol and parse
        return new BigDecimal(priceText.replaceAll("[^\\d.]", ""));
    }

    public BookingSummaryPage proceedToBooking() {
        page.click(PROCEED_BUTTON);
        return new BookingSummaryPage(page);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // REAL-TIME UPDATES
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Waits for seat map to refresh (e.g., after another user books)
     */
    public SeatSelectionPage waitForSeatMapRefresh() {
        // Seat maps typically update via WebSocket
        // Wait for any "updating" state to clear
        page.waitForSelector(SEAT_UPDATING,
            new Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.HIDDEN)
                .setTimeout(5000));
        return this;
    }

    /**
     * Checks if selected seats are still valid (not booked by someone else)
     */
    public boolean areSelectedSeatsStillValid() {
        List<String> selected = getSelectedSeats();
        for (String seatId : selected) {
            String status = page.locator(String.format(SEAT, seatId))
                .getAttribute("data-status");
            if (!status.equals("selected")) {
                return false;
            }
        }
        return true;
    }
}
```

### 3.2 SeatMapComponent (Reusable)

```java
/**
 * SeatMapComponent - Reusable seat map widget
 *
 * Can be embedded in different pages (selection, review, confirmation)
 * with different capabilities (interactive vs read-only)
 */
public class SeatMapComponent {

    private final Page page;
    private final String containerSelector;
    private final boolean isInteractive;

    public SeatMapComponent(Page page, String containerSelector, boolean isInteractive) {
        this.page = page;
        this.containerSelector = containerSelector;
        this.isInteractive = isInteractive;
    }

    /**
     * Creates a visual snapshot of current seat map state
     * Useful for debugging and test evidence
     */
    public SeatMapSnapshot captureSnapshot() {
        SeatMapSnapshot snapshot = new SeatMapSnapshot();

        // Get all seats and their states
        page.locator(containerSelector + " [data-testid^='seat-']").all()
            .forEach(seat -> {
                String id = seat.getAttribute("data-testid").replace("seat-", "");
                String status = seat.getAttribute("data-status");
                String category = seat.getAttribute("data-category");

                snapshot.addSeat(new SeatState(id, status, category));
            });

        return snapshot;
    }

    /**
     * Compares two seat map snapshots
     * Detects changes between API state and UI state
     */
    public static SeatMapDiff compare(SeatMapSnapshot expected, SeatMapSnapshot actual) {
        SeatMapDiff diff = new SeatMapDiff();

        for (String seatId : expected.getSeatIds()) {
            SeatState expectedState = expected.getSeat(seatId);
            SeatState actualState = actual.getSeat(seatId);

            if (actualState == null) {
                diff.addMissing(seatId);
            } else if (!expectedState.getStatus().equals(actualState.getStatus())) {
                diff.addMismatch(seatId, expectedState.getStatus(), actualState.getStatus());
            }
        }

        return diff;
    }

    /**
     * Gets seat element bounding box for visual testing
     */
    public BoundingBox getSeatBoundingBox(String seatId) {
        String locator = containerSelector + " [data-testid='seat-" + seatId + "']";
        return page.locator(locator).boundingBox();
    }
}
```

---

## 4. Playbooks Design

### 4.1 ShowFinderPlaybook

```java
/**
 * ShowFinderPlaybook - Finds suitable shows for testing
 *
 * Critical for avoiding flaky tests due to:
 * - Shows selling out
 * - Shows not available in certain locations
 * - Time-specific shows (matinee, night)
 */
@Component
public class ShowFinderPlaybook {

    private final MovieApiClient movieApi;
    private final CinemaApiClient cinemaApi;
    private final SeatApiClient seatApi;

    @Autowired
    public ShowFinderPlaybook(MovieApiClient movieApi,
                              CinemaApiClient cinemaApi,
                              SeatApiClient seatApi) {
        this.movieApi = movieApi;
        this.cinemaApi = cinemaApi;
        this.seatApi = seatApi;
    }

    /**
     * Finds a show with guaranteed seat availability
     * Criteria:
     * - At least minSeats available
     * - Within next 7 days
     * - In specified city
     */
    public Show findAvailableShow(ShowCriteria criteria) {
        // Get movies currently playing
        List<Movie> movies = movieApi.getNowPlaying(criteria.getCity());

        if (criteria.getMovieTitle() != null) {
            movies = movies.stream()
                .filter(m -> m.getTitle().contains(criteria.getMovieTitle()))
                .collect(Collectors.toList());
        }

        // For each movie, find shows with availability
        for (Movie movie : movies) {
            List<Show> shows = cinemaApi.getShows(
                movie.getId(),
                criteria.getCity(),
                LocalDate.now(),
                LocalDate.now().plusDays(7)
            );

            for (Show show : shows) {
                // Check seat availability
                SeatAvailability availability = seatApi.getAvailability(show.getId());

                if (availability.getAvailableCount() >= criteria.getMinSeats()) {
                    // Check if preferred category has enough seats
                    if (criteria.getPreferredCategory() != null) {
                        int categoryAvailable = availability
                            .getAvailableByCategory(criteria.getPreferredCategory());
                        if (categoryAvailable >= criteria.getMinSeats()) {
                            return show;
                        }
                    } else {
                        return show;
                    }
                }
            }
        }

        throw new TestSetupException(
            "No shows found matching criteria: " + criteria);
    }

    /**
     * Finds show and pre-locks seats to guarantee availability
     */
    public ShowWithSeats findAndLockSeats(ShowCriteria criteria) {
        Show show = findAvailableShow(criteria);

        // Lock seats via API to prevent race conditions
        List<String> lockedSeats = seatApi.lockSeats(
            show.getId(),
            criteria.getMinSeats(),
            criteria.getPreferredCategory()
        );

        return new ShowWithSeats(show, lockedSeats);
    }
}

/**
 * Criteria builder for show search
 */
public class ShowCriteria {
    private String city = "Mumbai";  // Default
    private String movieTitle;
    private int minSeats = 2;
    private SeatCategory preferredCategory;
    private LocalTime preferredTime;

    public static ShowCriteria builder() {
        return new ShowCriteria();
    }

    public ShowCriteria inCity(String city) {
        this.city = city;
        return this;
    }

    public ShowCriteria forMovie(String title) {
        this.movieTitle = title;
        return this;
    }

    public ShowCriteria withMinSeats(int count) {
        this.minSeats = count;
        return this;
    }

    public ShowCriteria preferCategory(SeatCategory category) {
        this.preferredCategory = category;
        return this;
    }

    public ShowCriteria aroundTime(LocalTime time) {
        this.preferredTime = time;
        return this;
    }

    // Getters...
}
```

### 4.2 BookingPlaybook

```java
/**
 * BookingPlaybook - Complete booking workflow
 *
 * Orchestrates the full flow from show selection to ticket confirmation
 */
@Component
public class BookingPlaybook {

    private final ShowFinderPlaybook showFinder;
    private final SeatApiClient seatApi;
    private final Page page;

    public BookingPlaybook(ShowFinderPlaybook showFinder,
                          SeatApiClient seatApi,
                          Page page) {
        this.showFinder = showFinder;
        this.seatApi = seatApi;
        this.page = page;
    }

    /**
     * Complete booking with API-guaranteed seats + UI payment
     */
    public BookingResult completeBooking(BookingRequest request) {
        // PHASE 1: API Setup - Find and lock seats
        ShowWithSeats showWithSeats = showFinder.findAndLockSeats(
            ShowCriteria.builder()
                .inCity(request.getCity())
                .forMovie(request.getMovieTitle())
                .withMinSeats(request.getSeatCount())
                .preferCategory(request.getPreferredCategory())
        );

        String showId = showWithSeats.getShow().getId();
        List<String> lockedSeats = showWithSeats.getLockedSeats();

        try {
            // PHASE 2: UI - Navigate to seat selection with pre-locked seats
            SeatSelectionPage seatPage = new SeatSelectionPage(page);
            seatPage.navigate(showId);

            // Verify our locked seats show as "selected" (or select them)
            for (String seatId : lockedSeats) {
                if (!seatPage.isSeatSelected(seatId)) {
                    seatPage.selectSeat(seatId);
                }
            }

            // PHASE 3: UI - Proceed through checkout
            BookingSummaryPage summaryPage = seatPage.proceedToBooking();
            summaryPage.verifySeats(lockedSeats);
            summaryPage.applyOfferIfAvailable(request.getOfferCode());

            // PHASE 4: UI - Payment
            PaymentPage paymentPage = summaryPage.proceedToPayment();
            paymentPage.payWithCard(request.getPaymentCard());

            // PHASE 5: Capture confirmation
            TicketPage ticketPage = new TicketPage(page);
            String bookingId = ticketPage.getBookingId();

            // PHASE 6: API - Verify booking
            Booking booking = bookingApi.getBooking(bookingId);
            assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

            return BookingResult.success(booking, lockedSeats);

        } catch (Exception e) {
            // Cleanup: Release locked seats on failure
            seatApi.releaseSeats(showId, lockedSeats);
            throw new BookingException("Booking failed", e);
        }
    }

    /**
     * Quick booking with minimal UI interaction
     * Used when booking details don't matter, just need a completed booking
     */
    public Booking quickBook(String city, int seatCount) {
        return completeBooking(BookingRequest.builder()
            .city(city)
            .seatCount(seatCount)
            .preferredCategory(SeatCategory.REGULAR)
            .paymentCard(TestCards.VISA_SUCCESS)
            .build()
        ).getBooking();
    }
}
```

---

## 5. Test Case Design

### 5.1 Seat Selection Tests

```java
/**
 * SeatSelectionTests - Tests for seat map interactions
 */
@Test(groups = {"booking", "ui", "seat-selection"})
public class SeatSelectionTests extends BaseBookingTest {

    private ShowFinderPlaybook showFinder;
    private SeatApiClient seatApi;

    @BeforeMethod
    public void setupShow() {
        showFinder = getBean(ShowFinderPlaybook.class);
        seatApi = getBean(SeatApiClient.class);

        // Find a show with good availability for testing
        Show show = showFinder.findAvailableShow(
            ShowCriteria.builder()
                .withMinSeats(10)
                .preferCategory(SeatCategory.REGULAR)
        );
        testDataCache.put("showId", show.getId());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // POSITIVE SCENARIOS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "User can select available seats")
    public void selectAvailableSeats() {
        String showId = testDataCache.get("showId");

        SeatSelectionPage seatPage = new SeatSelectionPage(page);
        seatPage.navigate(showId);

        // Select 2 seats
        List<String> selected = seatPage.autoSelectSeats(2, SeatCategory.REGULAR);

        // Verify
        assertThat(selected).hasSize(2);
        assertThat(seatPage.getSelectedSeatCount()).isEqualTo(2);
        assertThat(seatPage.getTotalPrice()).isPositive();
    }

    @Test(description = "Consecutive seat selection finds adjacent seats")
    public void selectConsecutiveSeats() {
        String showId = testDataCache.get("showId");

        SeatSelectionPage seatPage = new SeatSelectionPage(page);
        seatPage.navigate(showId);

        List<String> selected = seatPage.autoSelectSeats(3, SeatCategory.REGULAR);

        // Verify seats are consecutive
        // Extract row and column
        String row = selected.get(0).replaceAll("\\d", "");
        List<Integer> cols = selected.stream()
            .map(s -> Integer.parseInt(s.replaceAll("\\D", "")))
            .sorted()
            .collect(Collectors.toList());

        // Check all same row
        for (String seat : selected) {
            assertThat(seat.replaceAll("\\d", "")).isEqualTo(row);
        }

        // Check consecutive columns
        for (int i = 1; i < cols.size(); i++) {
            assertThat(cols.get(i)).isEqualTo(cols.get(i - 1) + 1);
        }
    }

    @Test(description = "Price updates correctly when selecting premium seats")
    public void priceUpdatesByCategory() {
        String showId = testDataCache.get("showId");

        SeatSelectionPage seatPage = new SeatSelectionPage(page);
        seatPage.navigate(showId);

        // Get prices from API for comparison
        SeatPricing pricing = seatApi.getPricing(showId);

        // Select regular seat
        seatPage.autoSelectSeats(1, SeatCategory.REGULAR);
        BigDecimal regularPrice = seatPage.getTotalPrice();
        assertThat(regularPrice).isEqualTo(pricing.getPrice(SeatCategory.REGULAR));

        // Deselect and select premium
        page.reload();  // Reset selection
        seatPage.autoSelectSeats(1, SeatCategory.PREMIUM);
        BigDecimal premiumPrice = seatPage.getTotalPrice();
        assertThat(premiumPrice).isEqualTo(pricing.getPrice(SeatCategory.PREMIUM));

        // Premium should cost more
        assertThat(premiumPrice).isGreaterThan(regularPrice);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // NEGATIVE SCENARIOS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Cannot select already booked seats")
    public void cannotSelectBookedSeats() {
        String showId = testDataCache.get("showId");

        // Book some seats via API first
        List<String> bookedSeats = seatApi.lockSeats(showId, 2, SeatCategory.REGULAR);
        testDataCache.put("bookedSeats", bookedSeats);

        SeatSelectionPage seatPage = new SeatSelectionPage(page);
        seatPage.navigate(showId);

        // Verify booked seats show as unavailable
        for (String seatId : bookedSeats) {
            assertThat(seatPage.isSeatBooked(seatId)).isTrue();

            // Attempting to click should have no effect
            page.locator("[data-testid='seat-" + seatId + "']").click();
            assertThat(seatPage.isSeatSelected(seatId)).isFalse();
        }
    }

    @Test(description = "Selection limit enforced")
    public void maxSeatSelectionEnforced() {
        String showId = testDataCache.get("showId");
        int maxSeats = 10;  // Typical limit

        SeatSelectionPage seatPage = new SeatSelectionPage(page);
        seatPage.navigate(showId);

        // Select max seats
        seatPage.autoSelectSeats(maxSeats, SeatCategory.REGULAR);
        assertThat(seatPage.getSelectedSeatCount()).isEqualTo(maxSeats);

        // Try to select one more - should show error or be blocked
        List<Locator> availableSeats = page.locator(
            "[data-status='available'][data-category='REGULAR']").all();

        if (!availableSeats.isEmpty()) {
            availableSeats.get(0).click();

            // Either still at max, or error shown
            boolean limitEnforced = seatPage.getSelectedSeatCount() == maxSeats ||
                                   page.locator(".seat-limit-error").isVisible();
            assertThat(limitEnforced).isTrue();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // RACE CONDITION TESTS
    // ═══════════════════════════════════════════════════════════════════════════

    @Test(description = "Handle seat booked by another user during selection")
    public void handleConcurrentBooking() {
        String showId = testDataCache.get("showId");

        SeatSelectionPage seatPage = new SeatSelectionPage(page);
        seatPage.navigate(showId);

        // Select some seats
        List<String> selected = seatPage.autoSelectSeats(2, SeatCategory.REGULAR);
        String firstSeat = selected.get(0);

        // Simulate another user booking one of our selected seats via API
        seatApi.forceLock(showId, List.of(firstSeat), "other-user-session");

        // Trigger refresh (simulate WebSocket update or manual refresh)
        seatPage.waitForSeatMapRefresh();

        // Our selection should be invalidated
        assertThat(seatPage.areSelectedSeatsStillValid()).isFalse();

        // UI should show feedback
        assertThat(page.locator(".seat-conflict-message").isVisible()).isTrue();
    }

    @AfterMethod
    public void cleanup() {
        // Release any seats locked during test
        String showId = testDataCache.get("showId");
        List<String> bookedSeats = testDataCache.get("bookedSeats");
        if (bookedSeats != null && showId != null) {
            seatApi.releaseSeats(showId, bookedSeats);
        }
    }
}
```

---

## 6. Handling Time-Sensitive Tests

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│                         TIME-SENSITIVE TEST HANDLING                                     │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                          │
│  PROBLEM: Movie showtimes are time-dependent                                            │
│  ─────────────────────────────────────────                                              │
│  • Tests written today may fail tomorrow (showtime passed)                              │
│  • Tests may fail at different times of day (no matinee shows at night)                │
│  • Tests may fail on weekends (different schedule)                                      │
│                                                                                          │
│  SOLUTION: Dynamic Show Selection                                                        │
│  ─────────────────────────────────                                                       │
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │  // DON'T: Hard-coded show                                                       │   │
│  │  @Test                                                                           │   │
│  │  public void badTest() {                                                         │   │
│  │      seatPage.navigate("SHOW-2024-01-15-10AM");  // ❌ Will fail after Jan 15   │   │
│  │  }                                                                               │   │
│  │                                                                                  │   │
│  │  // DO: Dynamic show discovery                                                   │   │
│  │  @Test                                                                           │   │
│  │  public void goodTest() {                                                        │   │
│  │      Show show = showFinder.findAvailableShow(                                   │   │
│  │          ShowCriteria.builder()                                                  │   │
│  │              .withMinSeats(2)                                                    │   │
│  │              .aroundTime(LocalTime.of(14, 0))  // Prefer 2 PM shows             │   │
│  │      );                                                                          │   │
│  │      seatPage.navigate(show.getId());  // ✅ Always finds valid show            │   │
│  │  }                                                                               │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
│  TEST DATA SEEDING FOR CI:                                                               │
│  ─────────────────────────                                                               │
│                                                                                          │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐   │
│  │  @BeforeSuite                                                                    │   │
│  │  public void ensureTestShows() {                                                 │   │
│  │      // In test environment, seed shows for next 30 days                         │   │
│  │      if (isTestEnvironment()) {                                                  │   │
│  │          testDataApi.seedShows(                                                  │   │
│  │              movie = "Test Movie",                                               │   │
│  │              cinema = "Test Cinema",                                             │   │
│  │              dates = LocalDate.now() to LocalDate.now().plusDays(30),            │   │
│  │              times = List.of(                                                    │   │
│  │                  LocalTime.of(10, 0),   // Morning                               │   │
│  │                  LocalTime.of(14, 0),   // Afternoon                             │   │
│  │                  LocalTime.of(18, 0),   // Evening                               │   │
│  │                  LocalTime.of(21, 0)    // Night                                 │   │
│  │              ),                                                                  │   │
│  │              seatsPerShow = 100                                                  │   │
│  │          );                                                                      │   │
│  │      }                                                                           │   │
│  │  }                                                                               │   │
│  └─────────────────────────────────────────────────────────────────────────────────┘   │
│                                                                                          │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Visual Testing for Seat Maps

```java
/**
 * Visual regression testing for seat map component
 *
 * Seat maps are highly visual - pixel comparison helps catch:
 * - Seat color/status display bugs
 * - Layout issues
 * - Responsive design problems
 */
@Test(groups = {"booking", "visual"})
public class SeatMapVisualTests extends BaseBookingTest {

    @Test(description = "Seat map renders correctly with mixed availability")
    public void seatMapRendersCorrectly() {
        // Setup show with known seat states
        String showId = setupShowWithMixedStates();

        SeatSelectionPage seatPage = new SeatSelectionPage(page);
        seatPage.navigate(showId);

        // Capture seat map screenshot
        byte[] screenshot = page.locator("[data-testid='seat-map']")
            .screenshot(new Locator.ScreenshotOptions()
                .setType(ScreenshotType.PNG));

        // Compare with baseline
        // Using Applitools or similar visual testing tool
        eyes.checkRegion(
            By.cssSelector("[data-testid='seat-map']"),
            "Seat Map - Mixed Availability"
        );
    }

    @Test(description = "Seat selection visual feedback")
    public void seatSelectionVisualFeedback() {
        String showId = testDataCache.get("showId");

        SeatSelectionPage seatPage = new SeatSelectionPage(page);
        seatPage.navigate(showId);

        // Baseline - no selection
        eyes.checkRegion(By.cssSelector("[data-testid='seat-map']"),
            "Seat Map - No Selection");

        // Select seats
        seatPage.autoSelectSeats(3, SeatCategory.REGULAR);

        // With selection - verify visual highlight
        eyes.checkRegion(By.cssSelector("[data-testid='seat-map']"),
            "Seat Map - With Selection");
    }

    private String setupShowWithMixedStates() {
        // Create show via API with specific seat states for consistent visual testing
        return testDataApi.createTestShow(TestShowConfig.builder()
            .bookedSeats(List.of("A1", "A2", "B5", "C10"))
            .lockedSeats(List.of("D1", "D2"))
            // Rest are available
            .build()
        ).getId();
    }
}
```

---

## 8. Key Test Scenarios Summary

| Category | Test | API/UI | Priority |
|----------|------|--------|----------|
| **Discovery** | Search movies by title | API | P2 |
| **Discovery** | Filter by language/genre | UI | P2 |
| **Selection** | Select available seats | UI | P1 |
| **Selection** | Handle booked seats | UI | P1 |
| **Selection** | Consecutive seat algorithm | UI | P2 |
| **Concurrency** | Seat locked by other user | Hybrid | P1 |
| **Concurrency** | Lock expiry handling | Hybrid | P1 |
| **Booking** | Complete booking flow | Hybrid | P0 |
| **Booking** | Apply offer code | Hybrid | P2 |
| **Payment** | Card payment for tickets | UI | P1 |
| **Cancellation** | Cancel and refund | Hybrid | P1 |
| **Visual** | Seat map rendering | UI | P2 |

---

*Document End - Booking LLD v1.0*
