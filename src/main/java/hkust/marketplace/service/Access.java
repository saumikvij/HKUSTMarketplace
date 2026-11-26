package hkust.marketplace.service;

import hkust.marketplace.data.DataStore;
import hkust.marketplace.model.Buyer;
import hkust.marketplace.model.Seller;
import hkust.marketplace.model.User;
import hkust.marketplace.util.MarketplaceException;

/**
 * Role-based access checks done in the service layer (not only in the screens), so a buyer session can never
 * call seller or admin operations.
 */
final class Access {
    private Access() { }

    /**
     * @param store data store
     * @param buyerId id claimed to be an active buyer
     * @return the buyer
     * @throws MarketplaceException if the id is not an active buyer account
     */
    static Buyer requireBuyer(DataStore store, String buyerId) {
        User user = store.findUser(buyerId).orElse(null);
        if (!(user instanceof Buyer)) {
            throw new MarketplaceException("This action is only available to buyers.");
        }
        if (!user.isActive()) {
            throw new MarketplaceException("This account is deactivated. Please contact the admin.");
        }
        return (Buyer) user;
    }

    /**
     * @param store data store
     * @param sellerId id claimed to be an active seller
     * @return the seller
     * @throws MarketplaceException if the id is not an active seller account
     */
    static Seller requireSeller(DataStore store, String sellerId) {
        User user = store.findUser(sellerId).orElse(null);
        if (!(user instanceof Seller)) {
            throw new MarketplaceException("This action is only available to sellers.");
        }
        if (!user.isActive()) {
            throw new MarketplaceException("This account is deactivated. Please contact the admin.");
        }
        return (Seller) user;
    }
}
