# HKUST Marketplace — Group 48

A campus marketplace for HKUST students and staff to **buy, borrow and give away** items. One JavaFX desktop app holds three portals that share the same data:

| Portal | Task | Owner | Subtasks |
|---|---|---|---|
| **Buyer Portal** | Task 1 | Saumik Vij | 1.1–1.10 |
| **Seller Portal** | Task 2 | Aryan Batra (group leader) | 2.1–2.9 |
| **Admin Portal** | Task 3 | Hasnain Mohammed | 3.1–3.8 |

COMP3111 Software Engineering · Fall 2026 · TA: Muhammad Usman

Java 21 · JavaFX 21 · Maven · JUnit 5 · JaCoCo

> **Assessors: start with [SUBMISSION.md](SUBMISSION.md).** It links every required item: screenshots with sample inputs and outputs, the unit test report, the coverage report, the JavaDoc and the supplementary notes.

## At a glance

| | |
|---|---|
| Subtasks implemented | **27 / 27** (Buyer 10, Seller 9, Admin 8) |
| Unit tests | **132 / 132 passed** (Buyer 106, Seller 11, Admin 15) · [report](docs/final/test-report/index.html) |
| Branch coverage | **90.5 %** (line 98.6 %) · [JaCoCo report](docs/final/coverage/index.html) |
| JavaDoc | Every class, including private members · [JavaDoc](docs/final/javadoc/index.html) |
| Screenshots | [Task 1](docs/screenshots) · [Task 2](docs/final/screenshots/task2-seller) · [Task 3](docs/final/screenshots/task3-admin) · [all three together](docs/final/screenshots/integration) |
| Supplementary notes | [Task 1 (PDF)](docs/Task1_Supplementary_Notes.pdf) · [Task 2](docs/Task2_Supplementary_Notes.md) · [Task 3](docs/task3/README.md) · [integrated system](SUBMISSION.md#4-supplementary-notes-integrated-system) |

## Run it

**IntelliJ IDEA**
1. File → Open → choose this folder (the one with `pom.xml`) → open as a Maven project.
2. Project Structure → SDK: **JDK 21** or newer.
3. Maven tool window → **Plugins → javafx → `javafx:run`**. You can also run `hkust.marketplace.Launcher` with the green arrow.

**Command line**
```bash
mvn javafx:run                  # start the app with demo data
mvn javafx:run -Pclean-start    # start with only the admin account
mvn verify                      # unit tests + JaCoCo → target/site/jacoco/index.html
mvn javadoc:javadoc             # JavaDoc → target/site/apidocs/index.html
```

**Data and resetting.** Everything is saved to `marketplace-data.ser` in the project folder after each change. **Delete that file to reset the marketplace.** When no data file exists, the app starts in one of two modes:

| Mode | How | What you get |
|---|---|---|
| Demo (default) | `javafx:run` | The accounts below, plus sample listings, deals, chats and reviews |
| Clean | IntelliJ: Maven → **Profiles → tick `clean-start`**, then `javafx:run` | Only the admin account. Buyers and sellers register themselves. |

## Demo accounts

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `Admin1234` (exists in both modes) |
| Seller | `alice_w`, `bob_c` | `Seller123` |
| Buyer | `demo_buyer`, `chris_l` | `Buyer123` |

In demo mode `demo_buyer` starts with a **countered offer** on the headphones, an **accepted giveaway claim** on the hoodie, a **4-minute loan** of the calculator (live countdown, "Due soon" reminder, automatic return) and a finished, **unrated purchase** in Deal History. Alice has a monitor waiting in the admin review queue.

## A 5-minute tour across all three portals

1. **Seller** (`alice_w`): Create Listing → Sell, "Mechanical keyboard", HK$320 → **Submit for review**. My Listings shows it as Pending review, and buyers cannot see it yet.
2. **Admin** (`admin`): Review queue → select the keyboard → **View details & photos** → **Approve**.
3. **Buyer** (`demo_buyer`): Browse → search "keyboard" → open it → **Make Offer** HK$300 with a message.
4. **Seller**: Offers and Requests → select the offer → **Counter** HK$310. Open **Messages** to see the conversation.
5. **Buyer**: Notifications shows the counter-offer → My Deals → **Accept counter-offer** → Mark complete → rate the seller.
6. **Seller**: Reviews → **Reply**. **Admin**: Seller ratings shows the new review and All deals shows the finished deal (export it to CSV).

## Features

### Shared across portals
- **One data model and one data file.** A listing a seller submits goes to the admin queue, appears for buyers once approved, and every deal, chat, review and notification is visible to the right people in each portal.
- **Messages tab** for buyers and sellers: every conversation, newest first, with the item, the other person and the last message. Double-click to open the chat. New messages appear without refreshing.
- **Chat** with offer and counter-offer cards showing their live status, and search within one chat or across all of a user's chats.
- **Notification boards** with categories, unread counts and links to the related deal, chat or listing.
- **Role-checked logins.** Each portal only accepts its own role, and every service checks the caller's role and account status itself, not only the UI.

### Task 1 · Buyer Portal (Saumik Vij)

| Subtask | What it does | Main classes |
|---|---|---|
| 1.1 Register | Username, name, password rules, residence hall, all errors shown at once | `AuthService.registerBuyer`, `BuyerRegisterView` |
| 1.2 Login | Buyer-only login with a generic error message | `AuthService.login`, `BuyerLoginView` |
| 1.3 Browse | Keyword search, category, type, condition, price range, sorting | `ListingService.browse`, `BrowseQuery`, `BrowseView` |
| 1.4 Listing detail | Photo viewer, seller rating and reviews, report a listing | `ListingService`, `ListingDetailDialog` |
| 1.5 Offer, borrow, claim, chat | Offers, borrow requests to the second, one-click giveaway claims, chat with the seller | `DealService`, `ChatService`, `DealDialogs`, `ChatWindow` |
| 1.6 My Deals | Accept or decline counters, withdraw, return, mark complete, live loan countdown | `DealService`, `MyDealsView` |
| 1.7 Manage Profile | Edit name and hall, change password | `ProfileService`, `ProfileView` |
| 1.8 Notification Board | Category filter, unread only, mark all read, open related item | `NotificationService`, `NotificationsView` |
| 1.9 Deal History | Keyword, date range, category and type filters, view a finished deal | `DealService.history`, `HistoryQuery`, `HistoryView` |
| 1.10 Rate & review | 1–5 stars and a comment after a finished deal, My Reviews | `ReviewService`, `ReviewDialog`, `MyReviewsView` |

### Task 2 · Seller Portal (Aryan Batra)

| Subtask | What it does | Main classes |
|---|---|---|
| 2.1 Register | Seller account with bio and preferred meet-up locations, cross-role duplicate check | `AuthService.registerSeller`, `SellerRegistrationView` |
| 2.2 Login | Seller-only login | `AuthService.login`, `BuyerLoginView` (seller mode) |
| 2.3 Create listing | Sell, Lend or Giveaway, up to 6 photos, submitted for admin review | `SellerListingService.create`, `SellerListingForm` |
| 2.4 My Listings | View details, edit and resubmit, delete when allowed | `SellerListingService`, `SellerListingsView` |
| 2.5 Offers and Requests | Accept, decline or counter offers, borrow requests and claims; open the chat | `DealService`, `SellerRequestsView` |
| 2.6 Manage Profile | Name, bio, meet-up locations, password | `ProfileService`, `SellerProfileView` |
| 2.7 Notifications | Timestamped request, deal and review notifications | `NotificationService`, `SellerNotificationsView` |
| 2.8 Seller Stats | Views, requests, completed deals, average rating, bar and pie charts | `Marketplace.sellerStats`, `SellerStats`, `SellerStatsView` |
| 2.9 Reviews | Reply to a review or flag it for the admin | `ReviewService.reply/flag`, `SellerReviewsView` |

### Task 3 · Admin Portal (Hasnain Mohammed)

| Subtask | What it does | Main classes |
|---|---|---|
| 3.1 Login | Seeded admin account, other roles rejected | `AuthService.login` |
| 3.2 Review queue | View details and photos, approve, or reject with a reason; the seller is notified | `AdminService`, `AdminPortal` |
| 3.3 Users | Search and filter, add and edit accounts, deactivate, reactivate, delete with confirmation | `AdminService`, `AdminForms` |
| 3.4 My profile | Edit details after confirming the current password | `AdminService`, `AdminPortal` |
| 3.5 All deals | Every deal with buyer, seller, type, dates and status; filter and **export to CSV** | `AdminRecords`, `AdminPortal` |
| 3.6 Notifications | Category and date filters, unread counts, archive and restore | `NotificationService`, `AdminPortal` |
| 3.7 All listings and reports | Add or edit listings for a seller, review buyer reports, take a listing down with a reason | `AdminService`, `AdminForms` |
| 3.8 Seller ratings | Flagged ratings first, filters, hide and restore ratings; averages update everywhere | `AdminService`, `AdminRecords` |

## Design

| Layer | Responsibility |
|---|---|
| `model` | Serializable entities (User → Buyer, Seller, Admin; Listing; Deal; ChatThread; Review; Notification; Report) and status enums |
| `data` | `DataStore` keeps every record and saves the whole store after each change. It writes to a temp file and then moves it into place, so a crash never leaves a half-written file. `transact()` rolls back a change that fails part-way. `SeedData` creates the demo and clean datasets. |
| `service` | All business rules behind the `Marketplace` facade: `AuthService`, `ProfileService`, `ListingService`, `SellerListingService`, `DealService`, `ChatService`, `NotificationService`, `ReviewService`, `AdminService`. No JavaFX, so everything is unit-testable. |
| `util` | Credential rules, salted PBKDF2-HMAC-SHA256 password hashing, ids, and `ValidationException`, which reports every broken rule at once |
| `ui` | JavaFX screens for the three portals and one shared stylesheet. A 1-second timer handles loan reminders, automatic returns and live updates. |

- **Concurrency:** claim, accept and counter actions run inside `synchronized (store)`, so two buyers can never both claim the same giveaway. A unit test checks this with two threads.
- **Testable time:** every service takes a `java.time.Clock`, so tests move time forward to check reminders, due dates and automatic returns without waiting.
- **Deal lifecycle:** Pending → Countered → Accepted/Borrowed → Completed/Returned/Auto-returned, or Declined/Withdrawn/Cancelled. When one request is accepted, the other open requests on that listing are declined and their buyers notified.
- **Robustness:** if the data file cannot be read, it is kept as `marketplace-data.ser.unreadable` and the app starts fresh. Any unexpected error shows a "your data is safe" message instead of crashing.
- **Look and feel:** one shared stylesheet (`ui/style.css`) gives all three portals the same "market stall" theme: warm paper backgrounds, ink text, coral actions, teal for prices and selection, and mustard for ratings and offers. The Manrope typeface is bundled in `ui/fonts` (SIL Open Font License), so the app looks the same on every computer.

## Testing

`mvn verify` runs **132 JUnit 5 tests**, all passing:

| Area | Test classes |
|---|---|
| Buyer (Task 1) | `AuthServiceTest`, `ListingServiceTest`, `DealServiceTest`, `ChatServiceTest`, `NotificationServiceTest`, `ProfileServiceTest`, `ReviewServiceTest`, `DataStoreTest`, `ModelTest`, `CredentialValidatorTest`, `PasswordHasherTest` |
| Seller (Task 2) | `SellerPortalServiceTest` |
| Admin (Task 3) | `AdminIntegrationTest` |

JaCoCo reports **90.5 % branch coverage** (requirement: above 65 %) and 98.6 % line coverage. The `ui` package, `Launcher` and `MainApp` are excluded from coverage because they need a display. The screens were tested by running the app end to end; see the screenshots.

## Project layout

```
src/main/java/hkust/marketplace/
  Launcher, MainApp       entry point, data loading, 1-second timer
  model/                  entities and enums shared by all three portals
  data/                   DataStore (serialized file), SeedData (demo and clean data)
  service/                all business rules, no JavaFX
  util/                   validation, password hashing, ids, exceptions
  ui/                     Buyer, Seller and Admin screens, style.css, fonts/
src/test/java/            132 JUnit 5 tests
docs/
  final/                  combined test report, coverage report, JavaDoc, screenshots
  screenshots/            Task 1 screenshots (53)
  task2-screenshots/      Task 2 screenshots by subtask
  task3/                  Task 3 notes and screenshots
  Task1_Supplementary_Notes.pdf, Task2_Supplementary_Notes.md
SUBMISSION.md             index of everything the assessment asks for
```

## Team

| Member | Responsibility |
|---|---|
| Saumik Vij | Task 1 Buyer Portal; integration of the three portals, combined reports and documentation |
| Aryan Batra | Task 2 Seller Portal; group leader |
| Hasnain Mohammed | Task 3 Admin Portal |
