package hkust.marketplace.service;

import hkust.marketplace.model.Category;
import hkust.marketplace.model.DealType;
import java.time.LocalDate;

/** Search and filter settings for Deal History (Task 1.9). Unset fields are ignored. */
public class HistoryQuery {
    private String keyword;
    private LocalDate from;
    private LocalDate to;
    private Category category;
    private DealType dealType;

    /** @return keyword matched against item title and seller name */
    public String getKeyword() { return keyword; }
    /** @param keyword keyword or null
     *  @return this query */
    public HistoryQuery keyword(String keyword) { this.keyword = keyword; return this; }
    /** @return first deal date included */
    public LocalDate getFrom() { return from; }
    /** @param from first date or null
     *  @return this query */
    public HistoryQuery from(LocalDate from) { this.from = from; return this; }
    /** @return last deal date included */
    public LocalDate getTo() { return to; }
    /** @param to last date or null
     *  @return this query */
    public HistoryQuery to(LocalDate to) { this.to = to; return this; }
    /** @return category filter */
    public Category getCategory() { return category; }
    /** @param category category or null
     *  @return this query */
    public HistoryQuery category(Category category) { this.category = category; return this; }
    /** @return deal type filter */
    public DealType getDealType() { return dealType; }
    /** @param dealType deal type or null
     *  @return this query */
    public HistoryQuery dealType(DealType dealType) { this.dealType = dealType; return this; }
}
