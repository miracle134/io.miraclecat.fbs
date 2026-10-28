package io.miraclecat.fbs.core.utils.common;

import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * packageName    : io.miraclecat.fbs.utils.support
 * fileName       : StringUtil
 * author         : MiracleCat
 * date           : 2024-05-02
 * description    : 문자열 유틸

 * 주요 메서드:
 * - isEmpty(String str): 문자열이 비어있는지 확인 (null 또는 빈 문자열)
 * - isEmptyOrWhitespace(String str): 문자열이 공백만 포함되어 있는지 확인
 * - nullToEmptyString(Object object): null 객체를 빈 문자열로 변환
 * - randomCodeUpper(int size): 지정된 길이의 랜덤한 알파벳(대문자) 숫자 문자열 생성
 * - randomCodeLower(int size): 지정된 길이의 랜덤한 알파벳(소문자) 숫자 문자열 생성
 * - randomCodeMix(int size): 지정된 길이의 랜덤한 알파벳(Mix) 숫자 문자열 생성
 * - randomNumberCode(int size): 지정된 길이의 랜덤한 숫자 문자열 생성
 * - joiner(String orgStr, String addStr, String delimiter): 문자열을 구분자로 연결하고 비어있는 문자열은 건너뜀
 * - convertSeasonDate(int startMonth, int endMonth): 제공된 월을 기반으로 시즌 날짜 범위를 변환
 * - getOrderNumber(): 현재 날짜와 랜덤 숫자를 기반으로 주문 번호 생성
 * - formatWithThousandComma(String str): 천 단위 구분자로 숫자 문자열 포맷
 */
public class StringUtil {
	
    /** 문자열이 비어있지 않고 공백이 아닌지 확인 */
    public static boolean isNotBlank(String value) {
        return StringUtils.hasText(value);
    }
    
    public static boolean isBlank(String value) {
        return !isNotBlank(value);
    }
    
	/**
	 * 문자열이 비어있는지 확인 (null 또는 빈 문자열)
	 *
	 * @param str 확인할 문자열
	 * @return 문자열이 null이거나 비어있으면 true
	 */
	public static boolean isEmpty(String str) {
		return str == null || str.isEmpty();
	}
	
	/**
	 * 문자열 내용이 있는지 확인
	 *
	 * @param str 확인할 문자열
	 * @return 문자열이 공백없이 존재해야 true
	 */
	public static boolean hasText(String str) {
		return (str != null && !str.isBlank());
	}
	
	/**
	 * 문자열이 비어있거나 공백만 포함되어 있는지 확인
	 *
	 * @param str 확인할 문자열
	 * @return 문자열이 null, 비어있거나 공백만 포함된 경우 true
	 */
	public static boolean isEmptyOrWhitespace(String str) {
		return isEmpty(str) || str.trim().isEmpty();
	}
	
	/**
	 * null 객체를 빈 문자열로 변환
	 *
	 * @param object 원본 객체
	 * @return 문자열 표현 또는 null인 경우 빈 문자열
	 */
	public static String nullToEmptyString(Object object) {
		return object != null ? object.toString().trim() : "";
	}
	
	/**
	 * 지정된 길이의 랜덤한 알파벳(대문자) 숫자 문자열 생성
	 *
	 * @param size 문자열 길이 (기본값 10)
	 * @return 랜덤 알파벳 숫자 문자열
	 */
	public static String randomCodeUpper(int size) {
		if (size <= 0) size = 10;
		
		Random rnd = new Random();
		StringBuilder buf = new StringBuilder(size);
		for (int i = 0; i < size; i++) {
			if (rnd.nextBoolean()) {
				buf.append((char) (rnd.nextInt(26) + 'A')); // 대문자
			} else {
				buf.append(rnd.nextInt(10)); // 숫자
			}
		}
		return buf.toString();
	}
	
	/**
	 * 지정된 길이의 랜덤한 알파벳(대문자) 숫자 문자열 생성
	 *
	 * @param size 문자열 길이 (기본값 10)
	 * @return 랜덤 알파벳 숫자 문자열
	 */
	public static String randomCodeLower(int size) {
		if (size <= 0) size = 10;
	
		Random rnd = new Random();
		StringBuilder buf = new StringBuilder(size);
		for (int i = 0; i < size; i++) {
			if (rnd.nextBoolean()) {
				buf.append((char) (rnd.nextInt(26) + 'a')); // 소문자
			} else {
				buf.append(rnd.nextInt(10)); // 숫자
			}
		}
		return buf.toString();
	}
	
	/**
	 * 지정된 길이의 랜덤한 알파벳(대문자) 숫자 문자열 생성
	 *
	 * @param size 문자열 길이 (기본값 10)
	 * @return 랜덤 알파벳 숫자 문자열
	 */
	public static String randomCodeMix(int size) {
		if (size <= 0) size = 10;
	
		Random rnd = new Random();
		StringBuilder buf = new StringBuilder(size);
		for (int i = 0; i < size; i++) {
			int type = rnd.nextInt(3); // 0: 숫자, 1: 소문자, 2: 대문자
			switch (type) {
				case 0:
					buf.append(rnd.nextInt(10)); break; // 숫자
				case 1:
					buf.append((char) (rnd.nextInt(26) + 'a')); break; // 소문자
				case 2:
					buf.append((char) (rnd.nextInt(26) + 'A')); break; // 대문자
			}
		}
		return buf.toString();
	}
	
	/**
	 * 지정된 길이의 랜덤한 숫자 문자열 생성
	 *
	 * @param size 문자열 길이 (기본값 10)
	 * @return 랜덤 숫자 문자열
	 */
	public static String randomNumberCode(int size) {
		if (size <= 0) size = 10;
		
		Random rnd = new Random();
		StringBuilder buf = new StringBuilder(size);
		for (int i = 0; i < size; i++) {
			buf.append(rnd.nextInt(10)); // 숫자
		}
		return buf.toString();
	}
	
	/**
	 * 문자열을 구분자로 연결하고 비어있는 문자열은 건너뜀
	 *
	 * @param orgStr 원본 문자열
	 * @param addStr 추가할 문자열
	 * @param delimiter 연결 구분자
	 * @return 연결된 문자열
	 */
	public static String joiner(String orgStr, String addStr, String delimiter) {
		if (isEmpty(delimiter)) {
			return "";
		}
		
		String orgTmp = nullToEmptyString(orgStr);
		String addTmp = nullToEmptyString(addStr);
		
		if (!isEmpty(orgTmp) && isEmpty(addTmp)) {
			return orgTmp;
		} else if (!isEmpty(orgTmp)) {
			return orgTmp + delimiter + addTmp;
		}
		return "";
	}
	
	/**
	 * 제공된 월을 기반으로 시즌 날짜 범위를 변환
	 *
	 * @param startMonth 시작 월
	 * @param endMonth 종료 월
	 * @return 시작일과 종료일이 포함된 맵
	 */
	public static Map<String, String> convertSeasonDate(int startMonth, int endMonth) {
		Map<String, String> map = new HashMap<>();
		int currentYear = LocalDate.now().getYear();
		
		LocalDate startDate = LocalDate.of(currentYear, startMonth, 1);
		LocalDate endDate = (startMonth <= endMonth)
			? YearMonth.of(currentYear, endMonth).atEndOfMonth()
			: YearMonth.of(currentYear + 1, endMonth).atEndOfMonth();
		
		map.put("startDate", startDate.toString());
		map.put("endDate", endDate.toString());
		
		return map;
	}
	
	/**
	 * 현재 날짜와 랜덤 숫자를 기반으로 주문 번호 생성
	 *
	 * @return 생성된 주문 번호
	 */
	public static String getOrderNumber() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
		String timestamp = sdf.format(new Date());
		
		Random random = new Random();
		int randomNum = 1000 + random.nextInt(9000);
		
		return "SERVICE" + timestamp + randomNum;
	}
	
	/**
	 * 천 단위 구분자로 숫자 문자열 포맷
	 *
	 * @param str 숫자 문자열
	 * @return 천 단위 구분자가 포함된 포맷된 숫자 문자열
	 */
	public static String formatWithThousandComma(String str) {
		BigDecimal number = new BigDecimal(str);
		DecimalFormat formatter = new DecimalFormat("#,###");
		return formatter.format(number);
	}
	
	/**
	 * 앞자리 소문자 변환
	 * @param str 문자열
	 * @return 앞자리 소문자 변환
	 */
	public static String toLowerCaseFirstChar(String str) {
		if (str == null || str.isEmpty()) return str;
		return Character.toLowerCase(str.charAt(0)) + str.substring(1);
	}
	
	/**
	 * 앞자리 대문자 변환
	 * @param str 문자열
	 * @return 앞자리 대문자 변환
	 */
	public static String toUpperCaseFirstChar(String str) {
		if (str == null || str.isEmpty()) return str;
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}
	
	/**
	 * CamelCase & PascalCase
	 * @param input 문자열
	 * @param upperFirst ture : PascalCase false : CamelCase
	 * @return ex) ITEM-A  or ITEM_A or ITEM A > ItemA or itemA
	 */
    public static String toCamelCase(String input, boolean upperFirst) {
        StringBuilder sb = new StringBuilder();
        boolean nextUpper = upperFirst;

        for (char c : input.toCharArray()) {
            if (c == '_' || c == ' ' || c == '-') {
                nextUpper = true;
            } else {
                if (nextUpper) {
                    sb.append(Character.toUpperCase(c));
                    nextUpper = false;
                } else {
                    sb.append(Character.toLowerCase(c));
                }
            }
        }
        return sb.toString();
    }
	
    /**
     * PascalCase / camelCase -> kebab-case
     *
     * @param input 변환할 문자열
     * @return kebab-case 문자열
     */
    public static String toKebabCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        // 정규식: 대문자 앞에 하이픈 추가
        String kebab = input
                .replaceAll("([a-z])([A-Z])", "$1-$2") // camelCase
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1-$2"); // ABCDef -> ABC-Def

        return kebab.toLowerCase();
    }

    /**
     * 문자열의 마지막 N글자를 마스킹 처리
     *
     * @param str 원본 문자열
     * @param maskLength 마스킹할 글자 수
     * @return 마스킹 처리된 문자열
     */
    public static String maskLastChars(String str, int maskLength) {
        if (str == null || str.length() <= maskLength) {
            return "*".repeat(Math.max(0, str != null ? str.length() : 0));
        }
        
        String visible = str.substring(0, str.length() - maskLength);
        String masked = "*".repeat(maskLength);
        return visible + masked;
    }

    /**
     * 이메일 주소를 마스킹 처리
     *
     * @param email 원본 이메일
     * @return 마스킹된 이메일 (예: test***@example.com)
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 2) {
            return localPart.charAt(0) + "*@" + domain;
        } else {
            return localPart.substring(0, 2) + "***@" + domain;
        }
    }

    /**
     * 메시지 길이 제한
     */
    public static String truncateMessage(String message, int maxLength) {
        if (message == null) return null;
        if (message.length() <= maxLength) return message;

        return message.substring(0, maxLength - 3) + "...";
    }

}
