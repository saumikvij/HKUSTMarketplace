# Admin Portal · Task 3

HKUST COMP3111 • Group 48 • Task owner: Hasnain Mohammed

This contribution integrates the Admin portal with Saumik Vij’s Buyer implementation from the supplied GitHub main-branch archive `f8f3384bda3c8c1c4b634b9bd51033ed32f9a971`. Both roles use the same JavaFX application, object model and `marketplace-data.ser`. The Seller dashboard is the placeholder present in that archive. A future Seller screen can call `AdminService.submitForReview` to place an item in the queue and notify Admin.

## Run and demonstrate

Open the folder containing `pom.xml` as a Maven project in IntelliJ IDEA. Select JDK 21 and run `hkust.marketplace.Launcher`, or run `mvn javafx:run`. From a fresh dataset:

1. Select **Admin portal**. Sign in as **admin** with **Admin1234**. The review queue contains Alice Wong’s 24-inch monitor.
2. Select the monitor. Open **View details & photos**, close the preview, then choose **Approve** and confirm. The queue no longer contains the item.
3. Choose **Log out**, then **Buyer login**. Sign in as **demo_buyer** with **Buyer123**. Search for **Monitor**. The result shows Alice’s monitor available for HK$600.
4. To demonstrate moderation in the other direction, make an offer on that item. Log back in as Admin, find the monitor under **All listings**, choose **Take down** and enter a reason. In the Buyer portal, the offer appears cancelled and the buyer receives a notification.
5. In **Seller ratings**, hide the seeded review and inspect the corresponding seller’s public average from Buyer. Restore the review to return it to the average.

Run these steps in one application instance and switch roles using **Log out**. Reopening the app from the same working folder reloads its data. For a fresh demo without affecting another dataset, launch from a different writable working directory.

## Implemented features

| Task | Screen | Behaviour |
| --- | --- | --- |
| 3.1 | Admin login | Uses the seeded, role-checked account. A Buyer or Seller login is rejected. |
| 3.2 | Review queue | View submission details, photos and seller identity. Approve or reject with a reason. The seller is notified and the review time is recorded. |
| 3.3 | Users | Search usernames or names, filter by role, add and edit accounts, deactivate, reactivate and confirm deletion. Status changes leave timestamped audit notifications. |
| 3.4 | My profile | Confirm the current password for any edit. A password change requires confirmation and another login. |
| 3.5 | All deals | View buyer, seller, item, type, dates and status. Filter and export the displayed rows, including quoted item names, to CSV. Loans show their due date until an actual return is recorded. |
| 3.6 | Notifications | Filter by category and inclusive date range, see unread counts, open, archive and restore notices. Existing pending listings acquire a queue alert on first admin login. |
| 3.7 | All listings and Listing reports | Add a live listing on behalf of an active seller, edit listing details, review buyer reports, dismiss reports or remove a live listing with a reason. |
| 3.8 | Seller ratings | Flagged ratings appear first. Filter by buyer or seller, score, dates or flag. Hide and restore ratings. Public lists and seller averages reflect each change. |

## Data decisions

- Deactivating an account prevents login and hides that seller’s listings. Reactivating restores eligible listings. Deletion erases login credentials and profile details and retains a placeholder user ID for historical transactions, chats and reviews. A current loan blocks deletion. Other active commitments are cancelled and counterparties receive notices. A released giveaway reservation becomes available when its buyer is deleted.
- Taking down a live listing closes its pending and countered requests, notifies their buyers and the seller, and resolves open reports on that listing. An edit made against an outdated listing status is rejected. The listing type stays fixed once a transaction references it.
- Added listings accept up to six photos and the type-appropriate terms. The editor uses the Buyer project’s local photo paths. Bundled demo images work on other computers; newly selected photos must remain accessible at their saved paths.
- Admin account and moderation audit entries are retained on the notification board. Archiving a notice preserves its timestamp and contents.

## Verification

| Check | Result |
| --- | --- |
| Full JUnit Jupiter suite | **120 passed out of 120**, including all 105 existing cases and 15 new Admin integration cases. Zero skipped or failed. |
| JaCoCo branch coverage | **756 of 810 branches (93.33%)** across data, model, service and utility classes. This follows the project POM’s coverage scope, which excludes JavaFX UI and launcher classes. |
| JavaFX workflow | Verified wrong-role login, admin login, opening all management screens, approving through the confirmation dialog, Buyer login and visibility of the approved monitor. |

The sources and tests were compiled with the installed Java 21 and JavaFX runtime. JUnit and JaCoCo ran directly with local dependencies. Maven Central was inaccessible from the verification environment, so the Maven lifecycle could not be verified here.

[Full JUnit results](junit-results.txt) · [JaCoCo HTML report](coverage/index.html) · [JaCoCo XML](coverage/jacoco.xml) · [Generated service and UI JavaDoc](javadoc/index.html)

### Execution screenshots

| Admin input | Observed output |
| --- | --- |
| Select the pending monitor | [Review queue with approval controls](01-admin-queue.png) |
| Open the cross-platform deal register | [Sales, loans and giveaways with dates and statuses](02-admin-deals.png) |
| Open the seller ratings register | [Review, score, buyer, seller and visibility](03-admin-ratings.png) |
| Approve the monitor, log in as Buyer and search for Monitor | [One available listing at HK$600](04-buyer-after-approval.png) |
