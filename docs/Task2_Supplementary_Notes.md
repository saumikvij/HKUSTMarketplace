# HKUST Marketplace · Task 2 Seller Portal

COMP3111 Group 48 · Aryan Batra · Activity 2

## Build and run

Open the project folder in IntelliJ IDEA as a Maven project and select JDK 21 or newer.
Run `hkust.marketplace.Launcher`, or use the following commands from the folder containing `pom.xml`.

```sh
mvn javafx:run
mvn test
mvn javadoc:javadoc
```

The test command writes Surefire results to `target/surefire-reports/` and JaCoCo
coverage to `target/site/jacoco/index.html`. The JavaDoc command writes to
`target/site/apidocs/index.html`. [Generated JavaDoc](task2-javadoc/index.html)
is also included.

## Features and demo inputs

Choose **Seller portal** on the welcome screen. Use either seeded seller
`alice_w` or `bob_c`, with password `Seller123`. The seeded Buyer Portal account
`demo_buyer` uses `Buyer123`.

| Subtask | Implementation | Demo input and expected result |
|---|---|---|
| 2.1 Seller registration | `SellerRegistrationView`, `AuthService.registerSeller` | Register a fresh seller with a full name, a valid password and Library as a preferred location. Try `DEMO_BUYER` as the username to see the cross-role duplicate check. |
| 2.2 Seller login | Shared `BuyerLoginView` in seller mode, `AuthService.login` | Sign in as `alice_w`. Try a buyer’s credentials in the seller form to see the role check. |
| 2.3 Create listing | `SellerListingForm`, `SellerListingService.create` | Select Sell, add a price of HK$75.50 and choose Library. Submit for review. Verify that My Listings shows Pending Review and buyers cannot see it. Test Lend with a duration of 5 seconds and Giveaway with no price. |
| 2.4 My Listings | `SellerListingsView`, `SellerListingService` | Open listing details, edit an eligible listing and confirm its resubmission. Try editing a listing after a buyer has submitted an open request. Remove an eligible listing and verify its history is preserved. |
| 2.5 Offers and Requests | `SellerRequestsView`, shared `DealService` and `ChatWindow` | From Buyer Portal, make an offer on Alice’s calculus textbook. As Alice, counter it and send a message. Check the counter and chat on the buyer side, then accept from My Deals. Verify the seller sees the Reserved listing. |
| 2.6 Manage Profile | `SellerProfileView`, `ProfileService` | Change the seller name, bio, locations and password. Log out and sign in with the updated password. |
| 2.7 Notification Board | `SellerNotificationsView`, shared `NotificationService` | Submit an offer from Buyer Portal. As the seller, filter by Requests, open the notification and confirm it is marked read and selects the request. |
| 2.8 Seller Stats | `SellerStatsView`, `Marketplace.sellerStats` | Open Alice’s statistics. Verify requests are grouped by Sell, Lend and Giveaway, and current listings by status. Countering a request does not add another request. |
| 2.9 Reviews | `SellerReviewsView`, `ReviewService.reply` and `flag` | Complete an eligible deal as a buyer and leave a review. As the seller, reply and flag. Check the buyer’s notification and review reply. The flag is sent to the admin while the review remains visible. |

Imported photos are resized to fit within 1200 × 1200 pixels and retained inside
the shared data file. The first photo is the thumbnail. A seller submission or
resubmission creates a Pending Review listing and an admin notification. Seller
actions use the same persistent models and notifications as Buyer Portal.

## Task 3 integration

The supplied project includes a Task 1 implementation and a placeholder for the
Admin Portal. When the team's Admin Portal is merged, it should use the same
`Marketplace` and `DataStore` instances.

- Review `ListingStatus.PENDING_REVIEW` records. On approval, set `LIVE` and record
  the review time. On rejection, set `REJECTED` and store the reason and review
  time. Use `NotificationCategory.LISTING_UPDATES` and the listing ID for the
  seller notification. The seller’s notification board opens that listing.
- The seller's flag is stored in `Review.isFlagged()` and sends an admin
  `MODERATION` notification linked to the review ID. Moderation uses
  `Review.setHidden(...)`. Hidden reviews disappear from the seller’s review list
  and are excluded from rating statistics.
- Demo approval, rejection and review moderation through the admin UI once Task 3
  is integrated. The existing seed data supports seller and buyer demos independently.

## Verification

The full Maven test suite passed in IntelliJ IDEA on 26 November 2026.
All 116 tests passed, including 11 Seller Portal tests. JaCoCo reported
91.2% branch coverage and 98.1% line coverage. The Maven configuration
excludes JavaFX screens and the launch classes from the coverage calculation.
See the [test report](task2-test-report/README.md) and
[JaCoCo report](task2-coverage/index.html).

The full JavaFX project compiled in the Maven test run. JavaFX screen
screenshots and Task 3 integration checks are pending.
AI assistance was used to implement and review Task 2, write regression tests,
and draft these notes.
