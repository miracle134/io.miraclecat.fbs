/**
 * packageName    : static.js.core
 * fileName       : file-util
 * author         : MiracleCat 
 * date           : 2025-07-03
 * description    : 파일 처리
 */

/**
 * FileUtil 클래스
 * - 파일 관련 유틸리티 클래스.
 *
 * 주요 메서드:
 * - setFileName(): 파일 업로드 시 파일명을 표시하는 이벤트 등록
 *
 */
class FileUtil {
	constructor() {
	}

	/**
	 * 파일 기본 init (파일 선택창, 파일명 표시)
	 */
	init() {
        document.querySelectorAll('.file-wrap').forEach(item => {
            const fileInput = item.querySelector('input[type="file"]');
            const fileNameInput = item.querySelector('.form-box');
            const button = item.querySelector('.file-btn');

            // 버튼 클릭 시 파일 선택 창 열기
            if (button && fileInput) {
                button.addEventListener('click', () => {
                    fileInput.click();
                });
            }

            // 파일 선택 시 파일명 표시
            if (fileInput && fileNameInput) {
                fileInput.addEventListener('change', function () {
                    const files = Array.from(fileInput.files).map(f => f.name);
                    const fileNameStr = files.join(', ');
                    fileNameInput.value = files.length ? fileNameStr : "";
                    fileNameInput.style.color = '#595959';
                });
            }
        });

		// document.querySelectorAll('.filebox .upload-hidden').forEach((input) => {
		// 	input.addEventListener('change', () => {
		//         const files = Array.from(input.files).map(f => f.name);
		//         const fileNameStr = files.join(', ');
		//         const uploadName = input.closest('.filebox').querySelector('.upload-name');
		//         uploadName.value = files.length ? fileNameStr : "";
		//         uploadName.style.color = '#595959';
		// 	});
		// });
	}

    /**
     * 단일 파일 가져오기
     * @param {string} selector - input[type="file"] 셀렉터
     * @returns {File|null} 첫 번째 파일 또는 null
     */
    getFile(selector) {
        const fileInput = document.querySelector(selector);
        if (!fileInput || !fileInput.files.length) {
            return null;
        }
        return fileInput.files[0];
    }

    /**
     * 다중 파일 가져오기
     * @param {string} selector - input[type="file"] 셀렉터
     * @returns {File[]} 파일 배열
     */
    getFiles(selector) {
        const fileInput = document.querySelector(selector);
        if (!fileInput || !fileInput.files.length) {
            return [];
        }
        return Array.from(fileInput.files);
    }

    /**
     * FormData에 파일 추가
     * @param {FormData} formData - FormData 객체
     * @param {string} selector - input[type="file"] 셀렉터
     * @param {string} fieldName - 필드명
     * @returns {boolean} 파일 추가 성공 여부
     */
    appendFilesToFormData(formData, selector, fieldName = 'files') {
        const fileInput = document.querySelector(selector);
        if (!fileInput || !fileInput.files.length) {
            return false;
        }

        const files = Array.from(fileInput.files);

        if (fileInput.multiple) {
            // 다중 파일
            files.forEach(file => formData.append(fieldName, file));
        } else {
            // 단일 파일
            formData.append(fieldName, files[0]);
        }

        return true;
    }

    /**
     * 파일 입력 초기화
     * @param {string} selector - input[type="file"] 셀렉터
     */
    clearFile(selector) {
        const fileInput = document.querySelector(selector);
        if (fileInput) {
            fileInput.value = '';
            const fileNameInput = fileInput.closest('.file-wrap')?.querySelector('.form-box');
            if (fileNameInput) {
                fileNameInput.value = '';
            }
        }
    }

    /**
     * 파일 검증
     * @param {string} selector - input[type="file"] 셀렉터
     * @param {Object} options - { maxSize: MB, allowedTypes: ['.jpg', '.png', '.pdf'] }
     * @returns {Object} { valid: boolean, message: string }
     */
    validateFile(selector, options = {}) {
        const { maxSize = 10, allowedTypes = ['.jpg', '.jpeg', '.gif', '.png', '.pdf', '.hwp', '.xls', '.xlsx', '.doc', '.docx', '.ppt', '.pptx', '.txt']} = options;
        const fileInput = document.querySelector(selector);

        if (!fileInput || !fileInput.files.length) {
            return { valid: false, message: '파일을 선택해주세요.' };
        }

        const files = Array.from(fileInput.files);

        // 크기 검증
        for (const file of files) {
            if (file.size > maxSize * 1024 * 1024) {
                return {
                    valid: false,
                    message: `파일 크기는 ${maxSize}MB를 초과할 수 없습니다. (${file.name})`
                };
            }
        }

        // 타입 검증
        if (allowedTypes.length > 0) {
            for (const file of files) {
                const ext = '.' + file.name.split('.').pop().toLowerCase();
                if (!allowedTypes.includes(ext)) {
                    return {
                        valid: false,
                        message: `허용된 파일 형식이 아닙니다. (${file.name})\n허용: ${allowedTypes.join(', ')}`
                    };
                }
            }
        }

        return { valid: true, message: '' };
    }

    /**
     * 파일 존재 여부 확인
     * @param {string} selector - input[type="file"] 셀렉터
     * @returns {boolean} 파일 존재 여부
     */
    hasFile(selector) {
        const fileInput = document.querySelector(selector);
        return fileInput && fileInput.files && fileInput.files.length > 0;
    }

}

const fileUtil = new FileUtil();
export default fileUtil;
