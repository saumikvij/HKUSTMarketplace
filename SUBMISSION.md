# HKUST Marketplace — Group 48 Submission

COMP3111 group project · **Buyer Portal** (Task 1, Saumik Vij) · **Seller Portal** (Task 2, Aryan Batra) · **Admin Portal** (Task 3, Hasnain Mohammed)

Java 21 · JavaFX 21 · Maven · JUnit 5 · JaCoCo. Everything below was produced from the final integrated code on `main`.

| Required item | Where |
|---|---|
| Screenshots of the application with sample inputs and outputs | [Section 2](#2-screenshots-sample-inputs-and-outputs) below |
| Unit test report (100 % pass) | [docs/final/test-report/index.html](docs/final/test-report/index.html): **132 / 132 passed** |
| Coverage report (> 65 % branch) | [docs/final/coverage/index.html](docs/final/coverage/index.html): **90.5 % branch**, 98.6 % line |
| JavaDoc for the implemented tasks | [docs/final/javadoc/index.html](docs/final/javadoc/index.html) |
| Supplementary notes | [Task 1](docs/Task1_Supplementary_Notes.pdf) · [Task 2](docs/Task2_Supplementary_Notes.md) · [Task 3](docs/task3/README.md) · [section 4](#4-supplementary-notes-integrated-system) below |

## 1. Run it

1. Open the folder in IntelliJ IDEA as a Maven project, with JDK 21 or newer.
2. Start the app: Maven tool window → **Plugins → javafx → javafx:run**. From a terminal, run `mvn javafx:run`.
3. Run the tests and coverage: `mvn verify`. The test results go to `target/surefire-reports`, and the coverage report to `target/site/jacoco/index.html`.
4. Build the JavaDoc: `mvn javadoc:javadoc`, which writes to `target/site/apidocs/index.html`.

The data is kept in `marketplace-data.ser` in the project folder. **Delete that file to reset the marketplace.**

The app can start in one of two ways when no data file exists:

| Start mode | Command | What you get |
|---|---|---|
| Demo data (default) | `mvn javafx:run` | The accounts below, plus sample listings, deals, chats and reviews |
| Clean | IntelliJ: Maven → **Profiles → tick `clean-start`**, then `javafx:run` (or `mvn javafx:run -Pclean-start`) | Only the admin account. Buyers and sellers register themselves. |

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `Admin1234` |
| Seller | `alice_w`, `bob_c` | `Seller123` |
| Buyer | `demo_buyer`, `chris_l` | `Buyer123` |

The `admin` account exists in both modes.

## 2. Screenshots (sample inputs and outputs)

### Task 1 · Buyer Portal (Saumik)
There are 53 annotated screenshots in [Task1_Supplementary_Notes.pdf](docs/Task1_Supplementary_Notes.pdf), section 6, with the image files in [docs/screenshots](docs/screenshots). They cover:
- registering and logging in, including validation errors;
- browsing with search, filters and sorting, and an invalid price range;
- listing details;
- offers, borrow requests and giveaway claims, including invalid input;
- chat with offer cards and message search, and the Messages tab that lists every conversation;
- My Deals: counter-offers, mark complete, and the live borrow countdown and auto-return;
- the notification board, deal history and filters, rate-and-review, and profile changes.

### Task 2 · Seller Portal (Aryan)
Also see Aryan's screenshots for each subtask, 2.1 to 2.9, in [docs/task2-screenshots](docs/task2-screenshots). They are listed in his [supplementary notes](docs/Task2_Supplementary_Notes.md#seller-portal-screenshots).

| Screen | Sample input → output |
|---|---|
| ![](docs/final/screenshots/task2-seller/s02_seller_register_validation.png) | Register with every field empty → every problem is listed |
| ![](docs/final/screenshots/task2-seller/s03_seller_register_filled.png) | `sam_seller`, Sam Seller, `Secret123`, Academic Building, a bio → ![](docs/final/screenshots/task2-seller/s04_seller_register_result.png) the account is created |
| ![](docs/final/screenshots/task2-seller/s05_seller_my_listings.png) | My Listings for `alice_w`, with each listing's status |
| ![](docs/final/screenshots/task2-seller/s07_create_listing_validation.png) | Create a Sell listing with no details → the validation messages |
| ![](docs/final/screenshots/task2-seller/s08_create_listing_filled.png) | Mechanical keyboard, Electronics, Like New, Atrium, HK$320 → ![](docs/final/screenshots/task2-seller/s10_my_listings_pending.png) the listing is Pending review |
| ![](docs/final/screenshots/task2-seller/s11_offers_and_requests.png) | Offers and Requests: a pending offer, a borrow request and a claim |
| ![](docs/final/screenshots/task2-seller/s12_counter_offer_dialog.png) | Counter Chris's HK$150 offer with HK$170 → ![](docs/final/screenshots/task2-seller/s13_counter_sent.png) |
| ![](docs/final/screenshots/task2-seller/s14_accept_borrow_confirm.png) | Accept a borrow request → ![](docs/final/screenshots/task2-seller/s15_borrow_accepted.png) the item is lent and the buyer notified |
| ![](docs/final/screenshots/task2-seller/s16_seller_stats.png) | Seller statistics: views, requests, completed deals, average rating and charts |
| ![](docs/final/screenshots/task2-seller/s17_reply_to_review.png) | Reply "Thanks Demo, glad it fit well!" → ![](docs/final/screenshots/task2-seller/s18_review_replied.png) |
| ![](docs/final/screenshots/task2-seller/s21_seller_chat_with_offer_cards.png) | The seller's view of the chat, with the offer and counter-offer cards |
| ![](docs/final/screenshots/task2-seller/s22_seller_messages_tab.png) | Messages tab: every conversation with buyers, newest first → ![](docs/final/screenshots/task2-seller/s23_seller_chat_from_messages_tab.png) double-click opens the chat |
| ![](docs/final/screenshots/task2-seller/s19_seller_notifications.png) | Seller notifications · ![](docs/final/screenshots/task2-seller/s20_seller_profile.png) Seller profile |

### Task 3 · Admin Portal (Hasnain)
These screens were captured from the running JavaFX app with the shared theme from commit [15e22d7](https://github.com/saumikvij/HKUSTMarketplace/commit/15e22d77f4ccc5531722b648437f69733cf72d89). The [Task 3 guide](docs/task3/README.md) includes a second approval walkthrough.

| Screen | Sample input → output |
|---|---|
| ![](docs/final/screenshots/task3-admin/a01_admin_login.png) | Enter the seeded administrator credentials on the Admin login screen. |
| ![](docs/final/screenshots/task3-admin/a02_review_queue.png) | The review queue with two pending listings |
| ![](docs/final/screenshots/task3-admin/a03_approve_confirm.png) | Approve the keyboard → ![](docs/final/screenshots/task3-admin/a04_listing_approved.png) it goes live for buyers |
| ![](docs/final/screenshots/task3-admin/a05_reject_with_reason.png) | Reject the monitor with the reason "Please add a real photo of the monitor." → ![](docs/final/screenshots/task3-admin/a06_listing_rejected.png) |
| ![](docs/final/screenshots/task3-admin/a07_all_listings.png) | All listings with their statuses · ![](docs/final/screenshots/task3-admin/a08_listing_reports.png) a buyer's report |
| ![](docs/final/screenshots/task3-admin/a09_users.png) | User management · ![](docs/final/screenshots/task3-admin/a10_add_account_dialog.png) adding an account |
| ![](docs/final/screenshots/task3-admin/a11_all_deals.png) | Every deal in the system · ![](docs/final/screenshots/task3-admin/a13_seller_ratings.png) seller ratings |
| ![](docs/final/screenshots/task3-admin/a12_admin_notifications.png) | Admin notifications: submissions, reports and new accounts |
| ![](docs/final/screenshots/task3-admin/a14_admin_profile.png) | Open My profile to inspect the current name and password-change fields. |

### All three portals working together
All three portals share one stylesheet (`src/main/resources/hkust/marketplace/ui/style.css`): warm paper backgrounds, ink text, coral actions, teal for prices and selection, and the bundled Manrope typeface. Buyers and sellers both have a **Messages** tab listing all their conversations (Task 1 screenshots 52–53, Task 2 screenshots s22–s23).

| Screen | What it shows |
|---|---|
| ![](docs/final/screenshots/integration/b01_buyer_sees_approved_listing.png) | The keyboard the seller submitted and the admin approved now appears in the buyer's Browse page |
| ![](docs/final/screenshots/integration/b02_buyer_notifications_after_seller_actions.png) | The buyer is notified of the seller's counter-offer, the accepted borrow request and the seller's reply |
| ![](docs/final/screenshots/integration/b03_buyer_my_deals_after_seller_actions.png) | Those seller decisions show up in the buyer's My Deals |

## 3. Testing and coverage

- **Unit tests:** [docs/final/test-report/index.html](docs/final/test-report/index.html) has 132 JUnit 5 tests, all passing:
  - Task 1 and the shared core: 106
  - Task 2 Seller: 11
  - Task 3 Admin: 15

  The raw Surefire XML files are in the same folder.
- **Coverage:** [docs/final/coverage/index.html](docs/final/coverage/index.html) shows **90.5 % branch coverage** (835 of 923 branches) and 98.6 % line coverage. The JavaFX screens and launcher classes are excluded in `pom.xml`, because those need a display. They were tested by running the application, as the screenshots show.
- **Each member's report from before integration:**
  - Task 1: [test report](docs/test-report/index.html), [coverage](docs/coverage/index.html)
  - Task 2: [test report](docs/task2-test-report/README.md), [coverage](docs/task2-coverage/index.html)
  - Task 3: [JUnit results](docs/task3/junit-results.txt), [coverage](docs/task3/coverage/index.html)

## 4. Supplementary notes (integrated system)

- **Architecture:** every screen calls a JavaFX-free service layer (`hkust.marketplace.service`) through the `Marketplace` facade. All three portals share the same model, data store and services. For example, a seller's counter-offer is the same `Deal` record the buyer accepts in My Deals, and that the admin sees under All deals.
- **Saving and recovery:**
  - All data is saved to one serialized file, written atomically (to a temporary file first, then moved into place).
  - Seller operations run as transactions, so the data is restored if a save fails.
  - If the data file can't be read, it's kept as `marketplace-data.ser.unreadable` and the app starts fresh instead of failing to open.
  - Unexpected errors are shown in a dialog, and the app keeps running.
- **Rules enforced in the services, not just the screens:**
  - role checks on every operation;
  - atomic giveaway claims, so only one buyer can win;
  - borrow periods to the second (minimum 5 s), with automatic reminders and auto-return;
  - only one open request per buyer per listing;
  - a deactivated seller's listings disappear from Browse;
  - an admin take-down cancels open offers and notifies the buyers;
  - an account with a borrowed item can't be deleted.
- **Standout features:**
  - offers and counter-offers appear as live cards in chat, and messages can be searched across chats;
  - borrowing shows a live countdown and returns the item automatically;
  - the seller dashboard has charts;
  - the admin can review listings with their photos and export deals to CSV;
  - all three portals were tested end to end with the same data.
- **Assumptions and limitations:**
  - It's a single-machine desktop app with local file storage, not a server.
  - Photos are stored inside the data file.
  - Demo-mode photos are generated placeholders.
- Each member's design notes are in the linked Task 1, Task 2 and Task 3 documents.
