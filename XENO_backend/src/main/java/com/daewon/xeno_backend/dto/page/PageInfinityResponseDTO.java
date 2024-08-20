package com.daewon.xeno_backend.dto.page;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
public class PageInfinityResponseDTO<E> {

    private int pageIndex;
    private int size;
    private int totalPageIndex;
    private int totalIndex;
    private int viewStart;
    private int viewEnd;
    private boolean prev;
    private boolean next;
    private List<E> dtoList;

    @Builder(builderMethodName = "withAll")
    public PageInfinityResponseDTO(PageRequestDTO pageRequestDTO, List<E> dtoList, int totalIndex) {
        if (totalIndex <= 0) {
            return;
        }
        this.pageIndex = pageRequestDTO.getPageIndex();
        this.size = pageRequestDTO.getSize();
        this.totalIndex = totalIndex;
        this.totalPageIndex = (int) (Math.ceil((double) totalIndex / size)); // 전체 페이지 수 계산

        this.dtoList = dtoList;

        this.viewEnd = Math.min((int) (Math.ceil((double) (pageIndex + 1) / 10.0)) * 10, totalPageIndex); // 화면에 표시할 페이지번호 갯수...
        this.viewStart = Math.max(this.viewEnd - 9, 1); // 화면에서 시작번호 (1 이상으로 보장)
        this.prev = this.viewStart > 1;
        this.next = totalPageIndex > this.pageIndex;
    }
}