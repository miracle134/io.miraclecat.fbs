/**
 * packageName    : static.js.core
 * fileName       : date-util
 * author         : MiracleCat
 * date           : 2025-07-03
 * description    : 날짜 처리
 */

import dayjs from "dayjs";
import flatpickr from "flatpickr";
import customParseFormat from "dayjs/plugin/customParseFormat";
import {Korean} from "flatpickr/dist/l10n/ko.js";

dayjs.extend(customParseFormat);

/**
 * DateUtil 클래스
 * - 날짜 비교 및 변환을 위한 유틸리티입니다.
 *
 * 주요 메서드:
 * - compareDates(date1, date2): 두 Date 객체를 비교하여 동일 여부와 순서를 판단합니다.
 * - compareAndFormatDateString(dateString1, format1, dateString2, format2): 두 날짜 문자열을 비교하고 포맷에 맞게 변환합니다.
 * - parseStringToDate(dateString, format): 날짜 문자열을 Date 객체로 변환합니다.
 * - formatDate(dateInput, format): Date 객체 또는 날짜 문자열을 주어진 포맷으로 변환하여 문자열로 반환합니다.
 * [dayjs 기반]
 * - d(input): 입력값을 dayjs 인스턴스로 변환
 * - parseStrict(str, parseFormat): 지정 포맷으로 엄격 파싱
 * - isValid(input, parseFormat): 날짜 유효성 검사(엄격 모드 지원)
 * - formatDay(input, outFormat, parseFormat): 날짜 포맷 변환
 * - now(outFormat): 현재 시각 포맷 반환
 * - startOf / endOf / add / subtract / diff: 날짜 계산 관련 메서드
 * - isSameDay / isSameWeek / isSameMonth / isWeekend: 날짜 비교 유틸
 * - clamp(input, min, max): 날짜를 범위 내로 제한
 * - range(start, end, unit, step): 연속 날짜 배열 생성
 * - parseRange(value, separator, parseFormat): 범위 문자열 파싱
 *
 * [flatpickr 래퍼]
 * - initDatePicker(selectorOrEl, options): 단일 날짜/시간 선택기 초기화
 * - initRangePicker(selectorOrEl, options): 범위 선택기 초기화
 * - bindPickerOutput(fp, outputSelector, fmt): 선택된 날짜를 화면에 출력
 * - bindPickerHiddenISO(fp, hiddenSelectorOrEl): 선택된 날짜를 hidden input에 ISO로 저장
 *
 */
class DateUtil {
	constructor() {
	}
	
	/**
	 * 날짜 비교
	 * @param {Date} date1 - Date 날짜
	 * @param {Date} date2 - Date 비교 날짜
	 * @return {number} - 0 : 동일, -1 : date1이 더 이전, 1 : date1이 더 이후
	 */
	compareDates(date1, date2) {
		if (!(date1 instanceof Date) || !(date2 instanceof Date)) {
			throw new Error("Both arguments must be Date objects.");
		}
		if (date1.getTime() === date2.getTime()) {
			return 0; // 동일한 날짜
		}
		return date1 < date2 ? -1 : 1; // -1: date1이 더 이전, 1: date1이 더 이후
	}
	
	/**
	 * 문자열 날짜 비교
	 * @param {string} dateString1 - Date 날짜 문자열
	 * @param {string} format1 - Date 날짜 포멧 yyyy:년 MM:월 dd:일 HH:시간 mm:분 ss:초 sss:밀리초
	 * @param {string} dateString2 - Date 비교 날짜 문자열
	 * @param {string} format2 - Date 비교 날짜 포멧 yyyy:년 MM:월 dd:일 HH:시간 mm:분 ss:초 sss:밀리초
	 * @return {number} - 0 : 동일, -1 : date1이 더 이전, 1 : date1이 더 이후
	 */
	compareAndFormatDateString(dateString1, format1, dateString2, format2) {
		const date1 = this.parseStringToDate(dateString1, format1);
		const date2 = this.parseStringToDate(dateString2, format2);
		
		return this.compareDates(date1, date2);
	}
	
	/**
	 * 문자열 날짜 Date 변환
	 * @param {string} dateString - Date 날짜 문자열
	 * @param {string} format - Date 날짜 포멧 yyyy:년 MM:월 dd:일 HH:시간 mm:분 ss:초 sss:밀리초
	 * @return {Date} - 포멧 형식 Date 반환
	 */
	parseStringToDate(dateString, format = 'yyyy-MM-dd HH:mm:ss') {
		if (!dateString) return null;
		
		// format이 없거나 LocalDateTime(T포함) 형태일 경우 처리
		if (!format || dateString.includes('T')) {
			const trimmed = dateString.length > 23 ? dateString.substring(0, 23) : dateString;
			return new Date(trimmed.replace('T', ' '));
		}
		
		const year = format.includes('yyyy') ? dateString.substring(format.indexOf('yyyy'), format.indexOf('yyyy') + 4) : '';
		const month = format.includes('MM') ? dateString.substring(format.indexOf('MM'), format.indexOf('MM') + 2) : '01';
		const day = format.includes('dd') ? dateString.substring(format.indexOf('dd'), format.indexOf('dd') + 2) : '01';
		const hour = format.includes('HH') ? dateString.substring(format.indexOf('HH'), format.indexOf('HH') + 2) : '00';
		const minute = format.includes('mm') ? dateString.substring(format.indexOf('mm'), format.indexOf('mm') + 2) : '00';
		const second = format.includes('ss') ? dateString.substring(format.indexOf('ss'), format.indexOf('ss') + 2) : '00';
		const milliseconds = format.includes('sss') ? dateString.substring(format.indexOf('sss'), format.indexOf('sss') + 3) : '000';
		
		return new Date(`${year}-${month}-${day}T${hour}:${minute}:${second}.${milliseconds}`);
	}
	
	/**
	 * 문자열&Date 포멧 변환
	 * @param {string} dateInput - Date 날짜 문자열
	 * @param {string} format - Date 날짜 포멧 yyyy:년 MM:월 dd:일 HH:시간 mm:분 ss:초 sss:밀리초
	 * @return {string} - 포멧 형식 String 반환
	 */
	formatDate(dateInput, format = 'yyyy-MM-dd HH:mm:ss') {
        if(!dateInput) return "";
		const date = (typeof dateInput === 'string') ? this.parseStringToDate(dateInput) : dateInput;
		if (!(date instanceof Date) || isNaN(date.getTime())) {
			throw new Error("Invalid date input.");
		}
		
		const yyyy = date.getFullYear().toString();
		const MM = (date.getMonth() + 1).toString().padStart(2, '0');
		const dd = date.getDate().toString().padStart(2, '0');
		const HH = date.getHours().toString().padStart(2, '0');
		const mm = date.getMinutes().toString().padStart(2, '0');
		const ss = date.getSeconds().toString().padStart(2, '0');
		const sss = date.getMilliseconds().toString().padStart(3, '0');
		
		return format
			.replace('yyyy', yyyy)
			.replace('MM', MM)
			.replace('dd', dd)
			.replace('HH', HH)
			.replace('mm', mm)
			.replace('ss', ss)
			.replace('sss', sss);
	}
	
	/**
	 * dayjs 인스턴스 반환
	 * @param {any} input - Date | string | number | dayjs
	 * @returns {dayjs.Dayjs}
	 */
	d(input) {
		return dayjs(input);
	}
	
	/**
	 * 엄격 포맷 파싱(customParseFormat 플러그인 기반)
	 * @param {string} str - 입력 문자열
	 * @param {string} parseFormat - 예: 'YYYY-MM-DD HH:mm:ss'
	 * @returns {dayjs.Dayjs}
	 */
	parseStrict(str, parseFormat) {
		return dayjs(str, parseFormat, true);
	}
	
	/**
	 * 유효성 검사
	 * @param {any} input
	 * @param {string} [parseFormat] - 지정 시 엄격 검사
	 */
	isValid(input, parseFormat) {
		if (parseFormat) {
			return this.parseStrict(String(input), parseFormat).isValid();
		}
		return dayjs(input).isValid();
	}
	
	/**
	 * 포맷팅(입력 포맷 지정 시 엄격 파싱 → 출력 포맷)
	 * @param {any} input
	 * @param {string} outFormat - 기본 'YYYY-MM-DD HH:mm:ss'
	 * @param {string} [parseFormat] - 입력 포맷
	 * @returns {string}
	 */
	formatDay(input, outFormat = "YYYY-MM-DD HH:mm:ss", parseFormat) {
		if (parseFormat) {
			const d = this.parseStrict(String(input), parseFormat);
			return d.isValid() ? d.format(outFormat) : "";
		}
		return dayjs(input).format(outFormat);
	}
	
	now(outFormat = "YYYY-MM-DD HH:mm:ss") {
		return dayjs().format(outFormat);
	}
	
	startOf(input, unit = "day") {
		return dayjs(input).startOf(unit);
	}
	
	endOf(input, unit = "day") {
		return dayjs(input).endOf(unit);
	}
	
	add(input, amount, unit = "day") {
		return dayjs(input).add(amount, unit);
	}
	
	subtract(input, amount, unit = "day") {
		return dayjs(input).subtract(amount, unit);
	}
	
	diff(a, b, unit = "millisecond", asFloat = false) {
		return dayjs(a).diff(dayjs(b), unit, asFloat);
	}
	
	isSameDay(a, b) {
		return dayjs(a).isSame(dayjs(b), "day");
	}
	
	isSameWeek(a, b) {
		return dayjs(a).isSame(dayjs(b), "week");
	}
	
	isSameMonth(a, b) {
		return dayjs(a).isSame(dayjs(b), "month");
	}
	
	isWeekend(input) {
		const dow = dayjs(input).day(); // 0=일, 6=토
		return dow === 0 || dow === 6;
	}
	
	clamp(input, min, max) {
		const x = dayjs(input);
		if (min && x.isBefore(dayjs(min))) return dayjs(min);
		if (max && x.isAfter(dayjs(max))) return dayjs(max);
		return x;
	}
	
	range(start, end, unit = "day", step = 1) {
		const out = [];
		let cur = dayjs(start).startOf(unit);
		const last = dayjs(end).startOf(unit);
		while (cur.isBefore(last) || cur.isSame(last)) {
			out.push(cur);
			cur = cur.add(step, unit);
		}
		return out;
	}
	
	parseRange(value, separator = " ~ ", parseFormat = "YYYY-MM-DD") {
		if (!value) return {start: null, end: null};
		const [s, e] = String(value).split(separator).map((v) => v?.trim());
		const left = s ? this.parseStrict(s, parseFormat) : null;
		const right = e ? this.parseStrict(e, parseFormat) : null;
		return {
			start: left && left.isValid() ? left : null,
			end: right && right.isValid() ? right : null,
		};
	}
	
	/**
	 * 단일 날짜/시간 피커 초기화 (기본 한국어 로케일)
	 * @param {string|HTMLElement} selectorOrEl
	 * @param {Object} options - flatpickr 옵션
	 * @returns {FlatpickrInstance}
	 */
	initDatePicker(selectorOrEl, options = {}) {
		const el = typeof selectorOrEl === "string" ? document.querySelector(selectorOrEl) : selectorOrEl;
		if (!el) throw new Error("initDatePicker: 대상 요소를 찾을 수 없습니다.");
		
		const defaults = {
			locale: Korean,
			allowInput: true,
			dateFormat: "Y-m-d",
			// enableTime: true,
			// time_24hr: true,
			// altInput: true,
			// altFormat: "Y-m-d H:i",
		};
		return flatpickr(el, {...defaults, ...options});
	}
	
	/**
	 * 범위 선택 피커 초기화 (기본 한국어 로케일)
	 * @param {string|HTMLElement} selectorOrEl
	 * @param {Object} options
	 * @returns {FlatpickrInstance}
	 */
	initRangePicker(selectorOrEl, options = {}) {
		return this.initDatePicker(selectorOrEl, {mode: "range", ...options});
	}
	
	/**
	 * 변경 시 텍스트 노드에 포맷 반영
	 * @param {FlatpickrInstance} fp
	 * @param {string} outputSelector
	 * @param {string} fmt - 'YYYY-MM-DD HH:mm:ss'
	 */
	bindPickerOutput(fp, outputSelector, fmt = "YYYY-MM-DD HH:mm:ss") {
		const out = document.querySelector(outputSelector);
		if (!out) return;
		const onChange = (dates) => {
			if (!dates || dates.length === 0) {
				out.textContent = "";
				return;
			}
			out.textContent = dayjs(dates[0]).format(fmt);
		};
		fp.set("onChange", [...(fp.config.onChange || []), onChange]);
	}
	
	/**
	 * 변경 시 hidden input에 ISO 저장(서버 전송용)
	 * @param {FlatpickrInstance} fp
	 * @param {string|HTMLElement} hiddenSelectorOrEl
	 */
	bindPickerHiddenISO(fp, hiddenSelectorOrEl) {
		const hidden = typeof hiddenSelectorOrEl === "string" ? document.querySelector(hiddenSelectorOrEl) : hiddenSelectorOrEl;
		if (!hidden) return;
		const onChange = (dates) => {
			if (!dates || dates.length === 0) {
				hidden.value = "";
				return;
			}
			if (fp.config.mode === "range" && dates.length === 2) {
				const [s, e] = dates;
				hidden.value = `${dayjs(s).toISOString()}~${dayjs(e).toISOString()}`;
			} else {
				hidden.value = dayjs(dates[0]).toISOString();
			}
		};
		fp.set("onChange", [...(fp.config.onChange || []), onChange]);
	}
	
	/**
	 * 두 개의 input을 날짜 범위로 초기화하고 기본값을 세팅한다.
	 * - 기본: start=1개월 전, end=오늘
	 * - linkMinMax=true면 시작 변경 시 end.minDate, 종료 변경 시 start.maxDate를 자동 반영
	 *
	 * @param {string|HTMLElement} startSelectorOrEl
	 * @param {string|HTMLElement} endSelectorOrEl
	 * @param {Object} opts
	 *   - dateFormat: flatpickr 입력 포맷(기본 "Y-m-d")
	 *   - startDefault: 기본 시작값(dayjs/Date/string) (기본: 오늘-1개월)
	 *   - endDefault: 기본 종료값(dayjs/Date/string) (기본: 오늘)
	 *   - linkMinMax: 시작/종료 상호 제약 연동 여부(기본 true)
	 *   - startOptions: 시작 피커에만 줄 flatpickr 옵션
	 *   - endOptions: 종료 피커에만 줄 flatpickr 옵션
	 *   - triggerChange: setDate 시 onChange 트리거(기본 true)
	 * @returns {{startPicker: any, endPicker: any, setRange: Function, getRange: Function}}
	 */
	initDateRange(startSelectorOrEl, endSelectorOrEl, opts = {}) {
		const {
			dateFormat = "Y-m-d",
			startDefault = this.d().subtract(1, "month"),
			endDefault = this.d(),
			linkMinMax = true,
			startOptions = {},
			endOptions = {},
            autoSetDefault = true,
			triggerChange = true,
		} = opts;
		
		// 피커 초기화
		const startPicker = this.initDatePicker(startSelectorOrEl, {
			dateFormat,
			...startOptions,
		});
		const endPicker = this.initDatePicker(endSelectorOrEl, {
			dateFormat,
			...endOptions,
		});
		
		// 기본값 세팅
        if (autoSetDefault) {
            const toDate = v => this.d(v).toDate();
            startPicker.setDate(toDate(startDefault), triggerChange);
            endPicker.setDate(toDate(endDefault), triggerChange);
        }
		
		// 상호 제약(최소/최대) 연동
		if (linkMinMax) {
			const startDate = startPicker.selectedDates?.[0];
			const endDate = endPicker.selectedDates?.[0];
			if (startDate) endPicker.set("minDate", startDate);
			if (endDate) startPicker.set("maxDate", endDate);
			
			startPicker.set("onChange", [
				...(startPicker.config.onChange || []),
				(dates) => {
					const s = dates?.[0];
					if (s) endPicker.set("minDate", s);
				},
			]);
			endPicker.set("onChange", [
				...(endPicker.config.onChange || []),
				(dates) => {
					const e = dates?.[0];
					if (e) startPicker.set("maxDate", e);
				},
			]);
		}
		
		// start, end 한 번에 세팅
		const setRange = (start, end, fire = triggerChange) => {
			const s = this.d(start), e = this.d(end);
			// 순서 보정
			const [s1, e1] = s.isAfter(e) ? [e, s] : [s, e];
			startPicker.setDate(s1.toDate(), fire);
			endPicker.setDate(e1.toDate(), fire);
			if (linkMinMax) {
				endPicker.set("minDate", s1.toDate());
				startPicker.set("maxDate", e1.toDate());
			}
		}

        // 초기화
        const clearRange = (fire = triggerChange) => {
            startPicker.clear();
            endPicker.clear();
            if (linkMinMax) {
                startPicker.set("maxDate", null);
                endPicker.set("minDate", null);
            }
            if (fire) {
                startPicker.config.onChange?.forEach(cb => cb([]));
                endPicker.config.onChange?.forEach(cb => cb([]));
            }
        };


		// 현재 선택값 조회
		const getRange = () => {
			const s = startPicker.selectedDates?.[0] ? this.d(startPicker.selectedDates[0]) : null;
			const e = endPicker.selectedDates?.[0] ? this.d(endPicker.selectedDates[0]) : null;
			return {start: s, end: e}; // dayjs 인스턴스 반환
		};
		
		// 포맷 지정하여 문자열로 받기
		const normalizeDayjsFormat = (fmt) => {
			// 사용자가 'yyyy-MM-dd'처럼 소문자 포맷을 넣어도 dayjs 형식으로 보정
			return (fmt || 'YYYY-MM-DD')
				.replace(/yyyy/g, 'YYYY')
				.replace(/dd/g, 'DD')
				.replace(/hh/g, 'HH');
		};
		
		/**
		 * 범위를 포맷 문자열로 반환
		 * - 단일 문자열: start/end 동일 포맷
		 * - 객체: { start: 'YYYY-MM-DD', end: 'YYYY-MM-DD' } 개별 포맷
		 * - 날짜가 없으면 빈 문자열 반환
		 *
		 * @param {string|{start?: string, end?: string}} fmtOrObj
		 * @returns {{start: string, end: string}}
		 */
		const getRangeFormatted = (fmtOrObj) => {
			const {start, end} = getRange(); // 기존 dayjs 반환 함수 재사용
			
			// 포맷 해석
			let fmtStart, fmtEnd;
			if (typeof fmtOrObj === 'string') {
				fmtStart = fmtEnd = normalizeDayjsFormat(fmtOrObj);
			} else {
				fmtStart = normalizeDayjsFormat(fmtOrObj?.start || 'YYYY-MM-DD');
				fmtEnd = normalizeDayjsFormat(fmtOrObj?.end || fmtStart);
			}
			
			return {
				start: start ? start.format(fmtStart) : '',
				end: end ? end.format(fmtEnd) : ''
			};
		};
		
		/**
		 * 선택된 날짜 범위를 하루 기준으로 확장
		 * - 시작: 해당 날짜의 00:00:00.000
		 * - 종료: 해당 날짜의 23:59:59.999
		 * - 시간 선택 옵션을 쓰더라도 범위를 하루 단위로 확장 가능
		 * @returns {{start: dayjs.Dayjs|null, end: dayjs.Dayjs|null}}
		 */
		const getRangeDayBounds = () => {
			const {start, end} = getRange(); // 기존 메서드 재사용
			return {
				start: start ? start.startOf("day") : null,
				end: end ? end.endOf("day") : null
			};
		};
		
		/**
		 * LocalDateTime 형식(yyyy-MM-dd'T'HH:mm:ss)으로 범위 반환
		 * - Java Spring @RequestParam LocalDateTime에 바로 매핑 가능
		 * - 시간 선택이 없으면 start=00:00:00.000, end=23:59:59.999로 보정
		 *
		 * @param {boolean} fullDayBounds - true면 하루 시작/끝으로 보정
		 * @returns {{start: string, end: string}}
		 */
		const getRangeLocalDateTime = (fullDayBounds = true, outFormat = "YYYY-MM-DDTHH:mm:ss.SSS") => {
			const {start, end} = fullDayBounds ? getRangeDayBounds() : getRange();
			
			const fmt = outFormat
			return {
				start: start ? start.format(fmt) : "",
				end: end ? end.format(fmt) : ""
			};
		};

		return {startPicker, endPicker, setRange, clearRange, getRange, getRangeFormatted, getRangeDayBounds, getRangeLocalDateTime};
	}
	
}

const dateUtil = new DateUtil();
export default dateUtil;