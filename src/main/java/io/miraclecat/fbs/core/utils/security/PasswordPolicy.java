package io.miraclecat.fbs.core.utils.security;

import java.util.regex.Pattern;

/**
 * packageName    : io.miraclecat.fbs.core.utils.security
 * fileName       : PasswordPolicy
 * author         : MiracleCat
 * date           : 2026-10-27
 * description    : 비밀번호 규칙 (8~64자, 영문·숫자·특수문자 포함) — 프론트 regexUtil.isValidPassword와 동일
 */
public final class PasswordPolicy {

    public static final String MESSAGE = "비밀번호는 8자리 이상 영문, 숫자, 특수문자가 포함되어야 합니다.";

    private static final Pattern LETTER = Pattern.compile("[A-Za-z]");
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Pattern SPECIAL = Pattern.compile("[^A-Za-z0-9\\s]");

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        return password != null
                && password.length() >= 8
                && password.length() <= 64
                && !password.contains(" ")
                && LETTER.matcher(password).find()
                && DIGIT.matcher(password).find()
                && SPECIAL.matcher(password).find();
    }
}
