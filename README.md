# HKUST Marketplace — Buyer, Seller and Admin Portals

**Task 3 · Admin Portal (Hasnain Mohammed, Group 48).** Choose **Admin portal** on the welcome screen and sign in as `admin` / `Admin1234` on a fresh dataset. See [Admin implementation, demonstration and assessment evidence](docs/task3/README.md).

COMP3111 group project, Group 48. Task 1 (Buyer Portal, subtasks 1.1–1.10) by **Saumik Vij** (21010852, svij@connect.ust.hk).

Java 21 · JavaFX 21 · Maven · JUnit 5 · JaCoCo

Task 2 adds the Seller Portal (2.1–2.9) to the Task 1 project. See
[Task 2 supplementary notes](docs/Task2_Supplementary_Notes.md) for the subtask map,
demo inputs, verification status, and Admin Portal integration points.

## Run it

**IntelliJ IDEA**
1. File → Open → choose this folder (the one with `pom.xml`) → open as a Maven project.
2. Project Structure → SDK: JDK 21 or newer.
3. Run `hkust.marketplace.Launcher` (green arrow next to `main`). You can also open the Maven tool window and run **Plugins → javafx → javafx:run**.

**Command line**
```bash
mvn javafx:run          # start the app
mvn test                # run the unit tests + JaCoCo coverage → target/site/jacoco/index.html
mvn javadoc:javadoc     # JavaDoc → target/site/apidocs/index.html
```

On the first start the app creates `marketplace-data.ser` in the working folder and fills it with demo data. **Delete that file to reset everything** (the short demo loan only counts down right after a fresh start).

## Demo accounts

| Role | Username | Password |
|---|---|---|
| Buyer (has demo deals) | `demo_buyer` | `Buyer123` |
| Buyer | `chris_l` | `Buyer123` |
| Seller (Task 2) | `alice_w`, `bob_c` | `Seller123` |
| Admin (Task 3) | `admin` | `Admin1234` |

`demo_buyer` starts with:
- a **countered offer** on the headphones (accept or decline it in My Deals);
- an **accepted giveaway claim** on the hoodie (Mark complete, then rate the seller);
- a **4-minute loan** of the calculator, which shows a live countdown, sends a "Due soon" reminder halfway through and is auto-returned at the due time;
- a finished, **unrated purchase** in Deal History.

## What is implemented (Task 1)

| Subtask | Screen | Main classes |
|---|---|---|
| 1.1 Register | Register as a buyer | `AuthService.registerBuyer`, `BuyerRegisterView` |
| 1.2 Login | Buyer login | `AuthService.login`, `BuyerLoginView` |
| 1.3 Browse | Browse Marketplace (search, filters, price range, sort) | `ListingService.browse`, `BrowseQuery`, `BrowseView` |
| 1.4 Listing detail | Detail window, photo viewer, seller rating, report | `ListingService.openListing/reportListing`, `ListingDetailDialog` |
| 1.5 Offer / borrow / claim + chat | Offer, borrow and claim dialogs, chat window | `DealService.makeOffer/requestBorrow/claim`, `ChatService`, `DealDialogs`, `ChatWindow` |
| 1.6 My Deals | Active / closed tabs, counter-offers, withdraw, return, complete, countdown | `DealService`, `MyDealsView` |
| 1.7 Manage Profile | Edit name and hall, change password | `ProfileService`, `ProfileView` |
| 1.8 Notification Board | Category filter, unread only, mark read, open related item | `NotificationService`, `NotificationsView` |
| 1.9 Deal History | Keyword, date range, category and type filters | `DealService.history`, `HistoryQuery`, `HistoryView` |
| 1.10 Rate & review | Review after a finished deal, My Reviews | `ReviewService`, `ReviewDialog`, `MyReviewsView` |

The Seller Portal uses the shared `DealService` for accept, decline, and counter actions. Buyer My Deals reads the same records.

## Project layout

```
src/main/java/hkust/marketplace/
  Launcher, MainApp       entry point, 1-second timer for reminders and auto-return
  model/                  entities and enums (shared with Tasks 2 and 3)
  data/                   DataStore (serialized file), SeedData (demo data)
  service/                all business rules, no JavaFX → fully unit-testable
  util/                   validation, PBKDF2 password hashing, ids, exceptions
  ui/                     JavaFX screens + style.css
src/test/java/            131 JUnit 5 tests (Buyer 105, Seller 11, Admin 15)
docs/                     screenshots, test report, coverage report, JavaDoc, supplementary notes
```

## Buyer portal assessment evidence (Task 1)

The figures below are from Saumik’s original Buyer submission.

- `docs/Task1_Supplementary_Notes.pdf`: screenshots with sample inputs and outputs, test and coverage summary, design notes, assumptions.
- `docs/test-report/index.html`: **105 / 105 tests passed**.
- `docs/coverage/index.html`: JaCoCo, **95.3 % branch coverage** and 98.3 % line coverage. The `ui` package, `Launcher` and `MainApp` are excluded because they need a display.
- `docs/javadoc/index.html`: JavaDoc for every class, including private members.
- `docs/screenshots/`: 51 screenshots of the running app.
