package com.daewon.xeno_backend.dto.page;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageRequestDTO {

    @Builder.Default
    private int pageIndex = 1;

    @Builder.Default
    private int size = 10;

    public Pageable getPageable(String...props) {
        return PageRequest.of(this.pageIndex - 1, this.size, Sort.by(props).descending());
    }

    private String link;

    public String getLink() {

        if(link == null) {
            StringBuilder builder = new StringBuilder();
            builder.append("page=" + this.pageIndex);
            builder.append("&size=" + this.size);

            link = builder.toString();
        }
        return link;
    }

}