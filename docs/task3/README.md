# Admin Portal supplementary notes (Task 3)

HKUST COMP3111 • Group 48 • Task owner: Hasnain Mohammed

The Admin, Seller and Buyer portals share one JavaFX application and one Marketplace data store. A seller submits a listing for review. An administrator can approve it for Buyer search or reject it with a reason that the seller can read.

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

The integrated project reports record **132 of 132 tests passed** and **90.5% branch coverage** (835 of 923 branches). See the [team test report](../final/test-report/index.html), [coverage report](../final/coverage/index.html) and [JavaDoc](../final/javadoc/index.html).

The table below records the earlier verification of the Buyer and Admin implementation before the Seller portal was integrated.

| Check | Result |
| --- | --- |
| Full JUnit Jupiter suite | **120 passed out of 120**, including all 105 existing cases and 15 new Admin integration cases. Zero skipped or failed. |
| JaCoCo branch coverage | **756 of 810 branches (93.33%)** across data, model, service and utility classes. This follows the project POM’s coverage scope, which excludes JavaFX UI and launcher classes. |
| JavaFX workflow | Verified wrong-role login, admin login, opening all management screens, approving through the confirmation dialog, Buyer login and visibility of the approved monitor. |

The sources and tests were compiled with the installed Java 21 and JavaFX runtime. JUnit and JaCoCo ran directly with local dependencies. Maven Central was inaccessible from the verification environment, so the Maven lifecycle could not be verified here.

[Full JUnit results](junit-results.txt) · [JaCoCo HTML report](coverage/index.html) · [JaCoCo XML](coverage/jacoco.xml) · [Generated service and UI JavaDoc](javadoc/index.html)

## Execution screenshots

The screenshots below were captured from the running JavaFX app using the shared theme introduced in [15e22d7](https://github.com/saumikvij/HKUSTMarketplace/commit/15e22d77f4ccc5531722b648437f69733cf72d89). The first three sections follow one session with two pending listings. The final section begins with fresh demo data.

### Admin login and listing review

**Figure 1.** Enter the seeded administrator credentials on the Admin login form.

![Enter the seeded administrator credentials on the Admin login form.](../final/screenshots/task3-admin/a01_admin_login.png)

**Figure 2.** Select the mechanical keyboard in the queue. The Dell monitor is also awaiting review.

![Select the mechanical keyboard in the queue. The Dell monitor is also awaiting review.](../final/screenshots/task3-admin/a02_review_queue.png)

**Figure 3.** Choose Approve. The confirmation explains that approval will make the keyboard visible to buyers.

![Choose Approve. The confirmation explains that approval will make the keyboard visible to buyers.](../final/screenshots/task3-admin/a03_approve_confirm.png)

**Figure 4.** Confirm the approval. The keyboard leaves the review queue and the monitor remains.

![Confirm the approval. The keyboard leaves the review queue and the monitor remains.](../final/screenshots/task3-admin/a04_listing_approved.png)

**Figure 5.** Select the monitor, choose Reject and enter “Please add a real photo of the monitor.”

![Select the monitor, choose Reject and enter “Please add a real photo of the monitor.”](../final/screenshots/task3-admin/a05_reject_with_reason.png)

**Figure 6.** Confirm the rejection. The queue is empty and the application acknowledges the decision.

![Confirm the rejection. The queue is empty and the application acknowledges the decision.](../final/screenshots/task3-admin/a06_listing_rejected.png)

**Figure 7.** Sign in as a Buyer and search for Mechanical keyboard. One approved listing appears at HK$320.

![Sign in as a Buyer and search for Mechanical keyboard. One approved listing appears at HK$320.](../final/screenshots/integration/b01_buyer_sees_approved_listing.png)

### Listings, reports and user accounts

**Figure 8.** Open All listings to inspect the status and seller for each item.

![Open All listings to inspect the status and seller for each item.](../final/screenshots/task3-admin/a07_all_listings.png)

**Figure 9.** Open Listing reports to inspect the buyer’s report about the headphones.

![Open Listing reports to inspect the buyer’s report about the headphones.](../final/screenshots/task3-admin/a08_listing_reports.png)

**Figure 10.** Open Users to inspect account roles, statuses and registration dates.

![Open Users to inspect account roles, statuses and registration dates.](../final/screenshots/task3-admin/a09_users.png)

**Figure 11.** Choose Add account to open the form for creating a Buyer or Seller account.

![Choose Add account to open the form for creating a Buyer or Seller account.](../final/screenshots/task3-admin/a10_add_account_dialog.png)

### Transactions, notifications, ratings and profile

**Figure 12.** Open All deals to inspect the item, parties, dates and status for each transaction.

![Open All deals to inspect the item, parties, dates and status for each transaction.](../final/screenshots/task3-admin/a11_all_deals.png)

**Figure 13.** Open Notifications to inspect unread notices and their categories.

![Open Notifications to inspect unread notices and their categories.](../final/screenshots/task3-admin/a12_admin_notifications.png)

**Figure 14.** Open Seller ratings to see the review score, comment and public visibility.

![Open Seller ratings to see the review score, comment and public visibility.](../final/screenshots/task3-admin/a13_seller_ratings.png)

**Figure 15.** Open My profile to inspect the administrator’s name and password-change fields.

![Open My profile to inspect the administrator’s name and password-change fields.](../final/screenshots/task3-admin/a14_admin_profile.png)

### Approve the seeded monitor in a fresh demo

**Figure 16.** In a fresh demo, select the pending Dell monitor in the review queue and approve it.

![In a fresh demo, select the pending Dell monitor in the review queue and approve it.](01-admin-queue.png)

**Figure 17.** Open All deals to view the transaction register in the same session.

![Open All deals to view the transaction register in the same session.](02-admin-deals.png)

**Figure 18.** Open Seller ratings to inspect the seeded review.

![Open Seller ratings to inspect the seeded review.](03-admin-ratings.png)

**Figure 19.** Log out, sign in as demo_buyer and search for Monitor. One available Dell monitor appears at HK$600.

![Log out, sign in as demo_buyer and search for Monitor. One available Dell monitor appears at HK$600.](04-buyer-after-approval.png)
