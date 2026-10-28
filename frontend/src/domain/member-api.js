/**
 * packageName    : static.js.domain
 * fileName       : Member-api
 * author         : MiracleCat
 * date           : 2025-09-26
 * description    :
 */
// Utils
import {ajax} from '@core/core.js';

const BASE_URL = '/api/member';
const memberApi = {

	/**
	 * 회원 등록
	 * POST /api/member
	 */
	async createMember(data, isLoading) {
		return await ajax.postAjax(data, `${BASE_URL}`, isLoading);
	},

	/**
	 * 내 정보
	 * GET /api/member/me
	 */
	async getMe(isLoading) {
		return await ajax.getAjax(null, `${BASE_URL}/me`, isLoading);
	},

	/**
	 * 이름 / 전화번호 수정
	 * PUT /api/member/me
	 */
	async updateMe(data, isLoading) {
		return await ajax.putAjax(data, `${BASE_URL}/me`, isLoading);
	},

	/**
	 * 비밀번호 변경
	 * PUT /api/member/me/password
	 */
	async changeMyPassword(data, isLoading) {
		return await ajax.putAjax(data, `${BASE_URL}/me/password`, isLoading);
	},

	/**
	 * 회원 목록 (관리자)
	 * GET /api/admin/member
	 */
	async getAdminMembers(params = {}, isLoading) {
		return await ajax.getAjax(params, `/api/admin/member`, isLoading);
	},

	/**
	 * 회원 상세 (관리자)
	 * GET /api/admin/member/${member-id}
	 */
	async getAdminMemberDetail(id, isLoading) {
		return await ajax.getAjax({}, `/api/admin/member/${id}`, isLoading);
	},

	/**
	 * 회원 정지 / 해제 (관리자, status: active / suspended)
	 * PUT /api/admin/member/${member-id}/status
	 */
	async changeMemberStatus(id, data, isLoading) {
		return await ajax.putAjax(data, `/api/admin/member/${id}/status`, isLoading);
	},

};

export default memberApi;