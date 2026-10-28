/**
 
 * packageName    :
 * fileName       : paging-util
 * author         : MiracleCat
 * date           : 2025-08-08
 * description    :
 */

// Utils

/**
 * PagingUtil 클래스
 * - 페이징 생성 및 조작을 위한 유틸리티 클래스.
 *
 * 주요 메서드:
 *
 */
class PagingUtil {
	constructor() {
	}
	
	/**
	 * 전역 페이징 클릭 이벤트 핸들러 등록
	 *
	 * @param {function} fetchCallback - 페이지 클릭 시 실행할 함수 (pageNum => void)
	 * @param {string} containerId - 페이징 영역 ID (기본값: 'pagination')
	 */
	initPaginationListener(fetchCallback, containerId = "pagination") {
	    const container = document.getElementById(containerId);
	    if (!container) return;
	
	    container.addEventListener("click", (e) => {
	        const target = e.target.closest("li[data-page]");
	        if (target) {
	            e.preventDefault();
				
				// active 갱신 안 함
			    if (target.classList.contains("active")) {
			        return;
			    }
				
	            const page = Number(target.dataset.page);
	            if (Number.isInteger(page)) {
	                fetchCallback(page);
	            }
	        }
	    });
	}

	/**
	 * 전역 페이징 클릭 이벤트 핸들러 등록
	 */
	setPageSizeSelectBox(id = "pageSize") {
	    const container = document.getElementById(id);
	    if (!container) return;

        container.innerHTML = `<option value="10">10</option>
            <option value="20">20</option>
            <option value="50">50</option>
            <option value="100">100</option>`;
	}

	/**
	 * 페이지 생성
	 * @param {function} onPageClick - 선택된 행에 대해 실행할 함수
	 * @param currentPage - 현재 페이지 번호
	 * @param totalItemCount - 전체 항목 수
	 * @param pageSize - 페이지 당 항목 수 (페이지 row 수)
	 * @param pageBlock - 한번에 표시할 페이지 번호 수
	 * @return {string}
	 */
    setPaging(currentPage = 1, totalItemCount, pageSize = 10, pageBlock = 10) {
		currentPage = Math.max(1, Number(currentPage) || 0);
		pageSize    = Math.max(10, Number(pageSize) || 0);
        const totalPages = Math.ceil(totalItemCount / pageSize); // 총 페이지 수
        const totalBlocks = Math.ceil(totalPages / pageBlock); // 총 블록 수
        const currentBlock = Math.ceil(currentPage / pageBlock); // 현재 페이지가 속한 블록 번호
        const startPage = ((currentBlock - 1) * pageBlock) + 1; // 현재 블록의 시작 페이지 번호
        const endPage = startPage + pageBlock - 1; // 현재 블록의 끝 페이지 번호
        let html = "";

        if (totalPages > 1) {
            // ◀◀ 이전 블록
            if (currentBlock > 1) {
				const prevBlockPage = (currentBlock - 2) * pageBlock + 1;
				html += `<li class="pagination-item double-prev" data-page="${prevBlockPage}">
							<img src="/images/ic-double-prev.png" alt="이전 블럭" class="pagination-icon">
						</li>`;
            } else {
                // html += `<li class="page-item page-prev disabled"><a href="javascript:void(0);" class="page-link">&#60;&#60;</a></li>`;
            }
            // ◀ 이전 페이지
            if (currentPage > 1) {
				html += `<li class="pagination-item prev" data-page="${currentPage - 1}">
							<img src="/images/ic-prev.png" alt="이전 페이지" class="pagination-icon">
						</li>`;
            } else {
                // html += `<li class="page-item page-prev disabled"><a href="javascript:void(0);" class="page-link">&#60;</a></li>`;
            }
            // 페이지 번호들
            for (let i = startPage; i <= endPage; i++) {
                if (i > totalPages) break;
                if (i === currentPage) {
					html += `<li class="pagination-item active" data-page="${i}">${i}</li>`;
                } else {
					html += `<li class="pagination-item" data-page="${i}">${i}</li>`;
                }
            }
            // ▶ 다음 페이지
            if (currentPage < totalPages) {
				html += `<li class="pagination-item next" data-page="${currentPage + 1}">
							<img src="/images/ic-next.png" alt="다음 페이지" class="pagination-icon">
						</li>`;
            } else {
                // html += `<li class="page-item page-next disabled"><a href="javascript:void(0);" class="page-link">&#62;</a></li>`;
            }
            // ▶▶ 다음 블록
            if (currentBlock < totalBlocks) {
				html += `<li class="pagination-item double-next" data-page="${(currentBlock * pageBlock) + 1}">
							<img src="/images/ic-double-next.png" alt="다음 블럭" class="pagination-icon">
						</li>`;
            } else {
                // html += `<li class="page-item page-next disabled"><a href="javascript:void(0);" class="page-link">&#62;&#62;</a></li>`;
            }
        }else{
			html = `<li class="pagination-item active">1</li>`;
        }

        return html;
    }
	
	/**
	 * 부트스트랩 pagination 형식 페이지 생성 (page-item / page-link, 이전·다음)
	 * initPaginationListener와 함께 사용 (li[data-page] 클릭)
	 * @param currentPage - 현재 페이지 번호 (1부터)
	 * @param totalItemCount - 전체 항목 수
	 * @param pageSize - 페이지 당 항목 수
	 * @param pageBlock - 한번에 표시할 페이지 번호 수
	 * @return {string}
	 */
	setBootstrapPaging(currentPage = 1, totalItemCount = 0, pageSize = 10, pageBlock = 5) {
		currentPage = Math.max(1, Number(currentPage) || 1);
		pageSize = Math.max(1, Number(pageSize) || 10);
		const totalPages = Math.max(1, Math.ceil(totalItemCount / pageSize));
		const currentBlock = Math.ceil(currentPage / pageBlock);
		const startPage = (currentBlock - 1) * pageBlock + 1;
		const endPage = Math.min(startPage + pageBlock - 1, totalPages);

		const item = (label, page, {active = false, disabled = false} = {}) => {
			const cls = ['page-item', active ? 'active' : '', disabled ? 'disabled' : ''].join(' ').trim();
			const dataPage = disabled ? '' : ` data-page="${page}"`;
			return `<li class="${cls}"${dataPage}><a class="page-link" href="#">${label}</a></li>`;
		};

		let html = item('이전', currentPage - 1, {disabled: currentPage <= 1});
		for (let i = startPage; i <= endPage; i++) {
			html += item(i, i, {active: i === currentPage});
		}
		html += item('다음', currentPage + 1, {disabled: currentPage >= totalPages});
		return html;
	}

	/**
	 * 페이지 인덱스 계산
	 * @param index
	 * @param pageNum
	 * @param pageSize
	 * @return {*}
	 */
	pageIndex(index, pageNum, pageSize) {
		return index + 1 + (pageNum - 1) * pageSize;
	}
	
	/**
	 * 기본 row
	 * @param span
	 * @return
	 */
	getDefaultRow(span= 0) {
		return `<tr><td colspan="${span}">조회된 데이터가 없습니다.</td></tr>`;
	}

	/**
	 * 기본 div row
	 * @param span
	 * @return
	 */
	getDivRow() {
		return `<div class="card flight-card mb-3">
                        <div class="card-body">
                            <div class="row align-items-center">
                                <div class="col-md-6 mb-0 mb-md-0">
                                    <div class="row align-items-center">
                                        <div class="col-7 text-start">
                                            <div class="">조회된 데이터가 없습니다.</div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>`;
	}

}

const pagingUtil = new PagingUtil();
export default pagingUtil;