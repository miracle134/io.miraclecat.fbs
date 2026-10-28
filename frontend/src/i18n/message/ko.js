/**
 
 * packageName    : static.js.i18n.message
 * fileName       : ko.js
 * author         : MiracleCat 
 * date           : 2025-07-08
 * description    :
 */
export default {
	/**
	 * 검증 문구
	 */
	"validation": {
		// 로그인
		"login": {
			"loginId": "아이디를 입력해 주세요.",
			"loginPassword": "비밀번호를 입력해 주세요."
		},
		// 회원
		"member": {
			"loginId": "아이디를 입력해 주세요.",
			"loginPassword": "비밀번호를 입력해 주세요.",
			"name": "이름을 입력해 주세요.",
			"role": "권한을 선택해 주세요.",
			"rules": {
				"password": "비밀번호는 특수문자, 대문자 포함\n8자 이상 입력해 주세요.",
				"email": "이메일 형식을 확인해 주세요."
			}
		}
	},
	
	/**
	 * 일반 문구
	 */
	"message": {
		// 로그인
		"login": {
			"unauthorized": "아이디와 비밀번호를 확인해주세요."
		},
		// 회원
		"member": {
			"update-complete": "수정 되었습니다.",
			"delete": "정말 삭제하시겠습니까?",
			"delete-complete": "삭제 되었습니다.",
			"error": {
				"duplicate": "중복된 아이디입니다.",
				"join": "회원 등록 처리 중 오류가 발생했습니다.",
				"update": "회원 수정 처리 중 오류가 발생했습니다.",
				"delete": "삭제에 실패했습니다."
			}
		}
	},
	
	/**
	 * 공통 문구
	 */
	"error": {
		"default": "관리자에게 문의해주세요."
	},
	"button": {
		"save": "저장",
		"cancel": "취소"
	}
}
