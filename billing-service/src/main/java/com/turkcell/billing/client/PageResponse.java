package com.turkcell.billing.client;

import java.util.List;

/**
 * Spring Data Page<T>'nin varsayilan JSON serilestirmesini (content/last/totalPages alanlari)
 * Feign uzerinden tuketmek icin - Page arayuzunun kendisi Jackson ile dogrudan deserialize
 * edilemez, bu yuzden ihtiyac duyulan alanlari tasiyan sade bir DTO kullanilir.
 */
public class PageResponse<T> {

    private List<T> content;
    private boolean last;
    private int totalPages;

    public List<T> getContent() { return content; }
    public void setContent(List<T> content) { this.content = content; }

    public boolean isLast() { return last; }
    public void setLast(boolean last) { this.last = last; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
}
