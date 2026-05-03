package com.eremis.controller;

import com.eremis.service.AuthService;
import com.eremis.service.AuthService.EmailVerificationResult;
import com.eremis.service.AuthService.LoginResult;
import com.eremis.service.AuthService.OtpResult;
import com.eremis.service.AuthService.PasswordResetResult;
import com.eremis.service.AuthService.RegistrationOtpResult;
import com.eremis.utils.SessionManager;

/**
 * AuthController — mediates between LoginFrame (UI) and AuthService.
 */
public class AuthController {

    private final AuthService authService = new AuthService();

    /**
     * Attempt login and return a LoginResult.
     */
    public LoginResult login(String username, String password) {
        if (username == null || username.isBlank())
            return LoginResult.INVALID_CREDENTIALS;
        if (password == null || password.isEmpty())
            return LoginResult.INVALID_CREDENTIALS;
        return authService.login(username, password);
    }

    public void logout() {
        authService.logout();
    }

    public String getLoginMessage(LoginResult result) {
        return authService.getLoginMessage(result);
    }

    public PasswordResetResult resetPassword(String username, String email, String newPassword) {
        return authService.resetPassword(username, email, newPassword);
    }

    public OtpResult sendPasswordResetOtp(String username, String email) {
        return authService.sendPasswordResetOtp(username, email);
    }

    public PasswordResetResult resetPasswordWithOtp(String username, String email, String otp, String newPassword) {
        return authService.resetPasswordWithOtp(username, email, otp, newPassword);
    }

    public String getPasswordResetMessage(PasswordResetResult result) {
        return authService.getPasswordResetMessage(result);
    }

    public String getOtpMessage(OtpResult result) {
        return authService.getOtpMessage(result);
    }

    public RegistrationOtpResult sendRegistrationEmailOtp(String email) {
        return authService.sendRegistrationEmailOtp(email);
    }

    public EmailVerificationResult verifyRegistrationEmailOtp(String email, String otp) {
        return authService.verifyRegistrationEmailOtp(email, otp);
    }

    public String getRegistrationOtpMessage(RegistrationOtpResult result) {
        return authService.getRegistrationOtpMessage(result);
    }

    public String getEmailVerificationMessage(EmailVerificationResult result) {
        return authService.getEmailVerificationMessage(result);
    }

    public boolean isAdmin() {
        return SessionManager.getInstance().isAdmin();
    }
}
