/**

 * packageName    : static.js.core
 * fileName       : validation
 * author         : MiracleCat 
 * date           : 2025-07-08
 * description    :
 */

import stringUtil from '@core/string-util.js';

class ValidationUtil {
	
	
	/**
	 * 단순 입력값 체크 (1차 검증)
	 * data: { username, password }
	 * requiredFields: { username: 'validation.required.username', ... }
	 * returns: { username: [ 'validation.required.username' ] }
	 */
	validateRequired(data, required) {
		const errors = {};
		
		for (const field in required) {
			if (stringUtil.isBlank(data[field])) {
				errors[field] = [required[field]];
			}
		}
		
		return errors;
	}
	
	/**
	 * 규칙 체크 (2차 검증)
	 * @param data {Object}
	 * @param rules {Object} - { field: [validators] }
	 * validator: v => true | 'validation.key'
	 * @returns {Object} - { field: [messageKey] }
	 */
	validate(data, rules) {
		const errors = {};
		
		for (const field in rules) {
			const validators = Array.isArray(rules[field]) ? rules[field] : [rules[field]];
			const value = data[field];
			
			const messages = validators
				.map(fn => fn(value))
				.filter(r => r !== true);
			
			if (messages.length > 0) {
				errors[field] = messages;
			}
		}
		
		return errors;
	}
	
	/**
	 * 1차검증 Error 출력
	 * data: { username, password }
	 * requiredFields: { username: 'validation.required.username', ... }
	 * return: message or false(통과)
	 */
	requiredError(data, required) {
		let requiredErrors = this.validateRequired(data, required);
		if (Object.keys(requiredErrors).length > 0) {
            // i18 사용시
			// for (const field in requiredErrors) {
			// 	requiredErrors[field] = requiredErrors[field].map(messageUtil.getMessage);
			// }
			
			// 에러 메시지
			return this.getFirstErrorMessage(requiredErrors);
		}
		
		return false;
	}
	
	/**
	 * 2차검증 Error 출력
	 * @param data {Object}
	 *
	 * @param rules {Object} - { field: [validators] }
	 * validator: v => true | 'validation.key'
	 * return: message or false(통과)
	 */
	rulesError(data, rules) {
		let rulesErrors = this.validate(data, rules);
		if (Object.keys(rulesErrors).length > 0) {
            // i18 사용시
			// for (const field in rulesErrors) {
			// 	rulesErrors[field] = rulesErrors[field].map(messageUtil.getMessage);
			// }
			
			// 에러 메시지
			return this.getFirstErrorMessage(rulesErrors);
		}
		
		return false;
	}
	
	/**
	 * validation 수행 후 첫번째 에러 메세지만 출력
	 * @param errors
	 * @return {*|null}
	 */
	getFirstErrorMessage(errors) {
		const fields = Object.keys(errors);
		if (fields.length === 0) return null;
		
		const firstField = fields[0];
		const messages = errors[firstField];

        // i18 사용시
		// return messages && messages.length > 0 && messages[0] !== "" ? messages[0] : messageUtil.getMessage("error.invalid");

        // i18 미사용시
		return messages && messages.length > 0 && messages[0] !== "" ? messages[0] : "처리 중 오류가 발생했습니다.";
	}
}

const validationUtil = new ValidationUtil();
export default validationUtil;