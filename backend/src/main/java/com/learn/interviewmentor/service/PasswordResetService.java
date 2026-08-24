package com.learn.interviewmentor.service;

import com.learn.interviewmentor.dto.auth.ForgotPasswordDto;
import com.learn.interviewmentor.dto.auth.ResetPasswordDto;

/**
 * Forgotten passwords.
 *
 * <h2>The rule that shapes the whole flow</h2>
 * {@link #requestReset} answers <b>identically</b> whether the address belongs
 * to an account or not - same message, same status, and as close to the same
 * timing as is reasonable.
 *
 * This is not politeness. An endpoint that says "no account with that email" is
 * a free membership oracle: feed it a list of addresses and it tells you which
 * ones are registered here. For this app that leaks who is job-hunting, which
 * is exactly the thing a candidate would not want their employer to learn. The
 * cost of hiding it is one confusing case - a typo produces a confident "check
 * your inbox" and no email - and that is a good trade.
 */
public interface PasswordResetService {

    /**
     * Send a reset link, if that address has an account.
     *
     * Returns nothing on purpose: there is no outcome the caller is allowed to
     * distinguish. Never throws for an unknown address.
     */
    void requestReset(ForgotPasswordDto dto);

    /**
     * Redeem a token and set the new password.
     *
     * @throws com.learn.interviewmentor.exception.BadRequestException if the
     *         token is unknown, expired, already used, or superseded. All four
     *         produce the same message - see the implementation for why.
     */
    void resetPassword(ResetPasswordDto dto);

    /** Housekeeping: drop rows that can never be used again. */
    int purgeExpired();
}
