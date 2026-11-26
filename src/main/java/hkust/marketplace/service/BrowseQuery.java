package hkust.marketplace.service;

import hkust.marketplace.model.Category;
import hkust.marketplace.model.Condition;
import hkust.marketplace.model.ListingType;

/**
 * Search, filter and sort settings for Browse Marketplace (Task 1.3). Unset fields (null or blank) are ignored.
 * Prices are kept as typed text so the service can report invalid input.
 */
public class BrowseQuery {
    private String keyword;
    private Category category;
    private ListingType type;
    private Condition condition;
    private String minPrice;
    private String maxPrice;
    private SortOrder sort = SortOrder.NEWEST;

    /** @return keyword matched against title and description */
    public String getKeyword() { return keyword; }
    /** @param keyword keyword, or null
     *  @return this query */
    public BrowseQuery keyword(String keyword) { this.keyword = keyword; return this; }
    /** @return category filter */
    public Category getCategory() { return category; }
    /** @param category category, or null
     *  @return this query */
    public BrowseQuery category(Category category) { this.category = category; return this; }
    /** @return listing type filter */
    public ListingType getType() { return type; }
    /** @param type listing type, or null
     *  @return this query */
    public BrowseQuery type(ListingType type) { this.type = type; return this; }
    /** @return condition filter */
    public Condition getCondition() { return condition; }
    /** @param condition condition, or null
     *  @return this query */
    public BrowseQuery condition(Condition condition) { this.condition = condition; return this; }
    /** @return minimum price text */
    public String getMinPrice() { return minPrice; }
    /** @param minPrice minimum price in HK$ as typed, or null
     *  @return this query */
    public BrowseQuery minPrice(String minPrice) { this.minPrice = minPrice; return this; }
    /** @return maximum price text */
    public String getMaxPrice() { return maxPrice; }
    /** @param maxPrice maximum price in HK$ as typed, or null
     *  @return this query */
    public BrowseQuery maxPrice(String maxPrice) { this.maxPrice = maxPrice; return this; }
    /** @return sort order */
    public SortOrder getSort() { return sort; }
    /** @param sort sort order (null means newest first)
     *  @return this query */
    public BrowseQuery sort(SortOrder sort) { this.sort = sort == null ? SortOrder.NEWEST : sort; return this; }
}
