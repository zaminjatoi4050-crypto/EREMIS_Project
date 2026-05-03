package com.eremis.service;

import com.eremis.config.AppConfig;
import com.eremis.dao.UserDAO;
import com.eremis.model.User;
import com.eremis.utils.PasswordUtil;
import com.eremis.utils.SessionManager;
import com.eremis.utils.ValidationUtil;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Authentication service.
 * Handles login, logout, attempt-limiting, and lockout logic.
 */
public class AuthService {

    private static final Logger  LOGGER   = Logger.getLogger(AuthService.class.getName());
    private final UserDAO        userDAO  = new UserDAO();
    private final AppConfig      config   = AppConfig.getInstance();
    private final SessionManager session  = SessionManager.getInstance();
    private final LoggingService logSvc   = new LoggingService();
    private final EmailService   emailSvc = new EmailService();

    private static final SecureRandom OTP_RANDOM = new SecureRandom();
    private static final Map<String, PasswordResetOtp> RESET_OTPS = new ConcurrentHashMap<>();
    private static final Map<String, EmailVerificationOtp> EMAIL_VERIFICATION_OTPS = new ConcurrentHashMap<>();

    public enum LoginResult {
        SUCCESS, INVALID_CREDENTIALS, ACCOUNT_LOCKED, ACCOUNT_INACTIVE,
        TOO_MANY_ATTEMPTS, ERROR
    }

    public enum PasswordResetResult {
        SUCCESS, INVALID_INPUT, USER_NOT_FOUND, EMAIL_MISMATCH,
        WEAK_PASSWORD, OTP_INVALID, OTP_EXPIRED, ERROR
    }

    public enum OtpResult {
        SUCCESS, INVALID_INPUT, USER_NOT_FOUND, EMAIL_MISMATCH,
        MAIL_NOT_CONFIGURED, MAIL_SEND_FAILED, ERROR
    }

    public enum RegistrationOtpResult {
        SUCCESS, INVALID_INPUT, EMAIL_ALREADY_REGISTERED,
        MAIL_NOT_CONFIGURED, MAIL_SEND_FAILED, ERROR
    }

    public enum EmailVerificationResult {
        SUCCESS, INVALID_INPUT, OTP_INVALID, OTP_EXPIRED
    }

    /**
     * Attempt to authenticate the user.
     *
     * @param username plaintext username
     * @param password plaintext password
     * @return LoginResult describing the outcome
     */
    public LoginResult login(String username, String password) {
        try {
            Optional<User> opt = findByUsernameOrEmail(username.trim());
            if (opt.isEmpty()) {
                LOGGER.warning("Login failed — unknown username: " + username);
                return LoginResult.INVALID_CREDENTIALS;
            }

            User user = opt.get();

            // ── Check account active ──────────────────────────────────────
            if (!user.isActive()) {
                logSvc.log(null, "LOGIN_FAILED_INACTIVE", "USER", user.getId(),
                           "Inactive account: " + username);
                return LoginResult.ACCOUNT_INACTIVE;
            }

            // ── Check lockout ─────────────────────────────────────────────
            if (user.getLockedUntil() != null
                    && LocalDateTime.now().isBefore(user.getLockedUntil())) {
                logSvc.log(null, "LOGIN_BLOCKED_LOCKED", "USER", user.getId(),
                           "Account locked until " + user.getLockedUntil());
                return LoginResult.ACCOUNT_LOCKED;
            }

            // ── Verify password ───────────────────────────────────────────
            if (!PasswordUtil.verify(password, user.getPasswordHash())) {
                int attempts = user.getLoginAttempts() + 1;
                int maxAttempts = config.getMaxLoginAttempts();

                LocalDateTime lockUntil = null;
                if (attempts >= maxAttempts) {
                    lockUntil = LocalDateTime.now()
                                            .plusMinutes(config.getLockoutMinutes());
                    LOGGER.warning("Account locked for user: " + username);
                }

                userDAO.updateLoginAttempts(user.getId(), attempts, lockUntil);
                logSvc.log(null, "LOGIN_FAILED", "USER", user.getId(),
                           "Attempt " + attempts + "/" + maxAttempts);

                return attempts >= maxAttempts
                       ? LoginResult.TOO_MANY_ATTEMPTS
                       : LoginResult.INVALID_CREDENTIALS;
            }

            // ── Successful login ──────────────────────────────────────────
            userDAO.resetLoginAttempts(user.getId());
            session.startSession(user);
            logSvc.log(user.getId(), "USER_LOGIN", "USER", user.getId(),
                       user.getUsername() + " logged in successfully");

            LOGGER.info("Authenticated: " + username + " [" + user.getRole() + "]");
            return LoginResult.SUCCESS;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Login SQL error", e);
            return LoginResult.ERROR;
        }
    }

    /**
     * Log out the current session.
     */
    public void logout() {
        if (session.isLoggedIn()) {
            logSvc.log(session.getCurrentUserId(), "USER_LOGOUT", "USER",
                       session.getCurrentUserId(),
                       session.getCurrentUser().getUsername() + " logged out");
        }
        session.endSession();
    }

    /**
     * Reset password after verifying username and email match.
     */
    public PasswordResetResult resetPassword(String username, String email, String newPassword) {
        return resetPasswordWithOtp(username, email, null, newPassword);
    }

    public OtpResult sendPasswordResetOtp(String username, String email) {
        try {
            if (ValidationUtil.isNullOrBlank(username) || ValidationUtil.isNullOrBlank(email)) {
                return OtpResult.INVALID_INPUT;
            }

            String cleanUsername = ValidationUtil.sanitize(username);
            String cleanEmail = ValidationUtil.sanitize(email);
            if (!ValidationUtil.isValidEmail(cleanEmail)) {
                return OtpResult.INVALID_INPUT;
            }

            Optional<User> opt = findByUsernameOrEmail(cleanUsername);
            if (opt.isEmpty()) {
                return OtpResult.USER_NOT_FOUND;
            }

            User user = opt.get();
            if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(cleanEmail)) {
                return OtpResult.EMAIL_MISMATCH;
            }
            if (!emailSvc.isConfigured()) {
                return OtpResult.MAIL_NOT_CONFIGURED;
            }

            String otp = generateOtp();
            int validMinutes = config.getPasswordResetOtpMinutes();
            RESET_OTPS.put(resetOtpKey(cleanUsername, cleanEmail),
                new PasswordResetOtp(otp, LocalDateTime.now().plusMinutes(validMinutes)));
            emailSvc.sendPasswordResetOtp(cleanEmail, otp, validMinutes);
            logSvc.log(user.getId(), "PASSWORD_RESET_OTP_SENT", "USER", user.getId(),
                "Password reset OTP sent");
            return OtpResult.SUCCESS;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Password reset OTP SQL error", e);
            return OtpResult.ERROR;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Password reset OTP email error", e);
            return OtpResult.MAIL_SEND_FAILED;
        }
    }

    public RegistrationOtpResult sendRegistrationEmailOtp(String email) {
        try {
            if (ValidationUtil.isNullOrBlank(email)) {
                return RegistrationOtpResult.INVALID_INPUT;
            }

            String cleanEmail = ValidationUtil.sanitize(email);
            if (!ValidationUtil.isValidEmail(cleanEmail)) {
                return RegistrationOtpResult.INVALID_INPUT;
            }
            if (userDAO.isEmailExists(cleanEmail)) {
                return RegistrationOtpResult.EMAIL_ALREADY_REGISTERED;
            }
            if (!emailSvc.isConfigured()) {
                return RegistrationOtpResult.MAIL_NOT_CONFIGURED;
            }

            String otp = generateOtp();
            int validMinutes = config.getPasswordResetOtpMinutes();
            EMAIL_VERIFICATION_OTPS.put(emailOtpKey(cleanEmail),
                new EmailVerificationOtp(otp, LocalDateTime.now().plusMinutes(validMinutes)));
            emailSvc.sendEmailVerificationOtp(cleanEmail, otp, validMinutes);
            LOGGER.info("Registration email OTP sent to " + cleanEmail);
            return RegistrationOtpResult.SUCCESS;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Registration OTP SQL error", e);
            return RegistrationOtpResult.ERROR;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Registration OTP email error", e);
            return RegistrationOtpResult.MAIL_SEND_FAILED;
        }
    }

    public EmailVerificationResult verifyRegistrationEmailOtp(String email, String otp) {
        if (ValidationUtil.isNullOrBlank(email) || ValidationUtil.isNullOrBlank(otp)) {
            return EmailVerificationResult.INVALID_INPUT;
        }

        String cleanEmail = ValidationUtil.sanitize(email);
        if (!ValidationUtil.isValidEmail(cleanEmail)) {
            return EmailVerificationResult.INVALID_INPUT;
        }

        String key = emailOtpKey(cleanEmail);
        EmailVerificationOtp savedOtp = EMAIL_VERIFICATION_OTPS.get(key);
        if (savedOtp == null || !savedOtp.otp.equals(otp.trim())) {
            return EmailVerificationResult.OTP_INVALID;
        }
        if (LocalDateTime.now().isAfter(savedOtp.expiresAt)) {
            EMAIL_VERIFICATION_OTPS.remove(key);
            return EmailVerificationResult.OTP_EXPIRED;
        }

        EMAIL_VERIFICATION_OTPS.remove(key);
        return EmailVerificationResult.SUCCESS;
    }

    public PasswordResetResult resetPasswordWithOtp(String username, String email, String otp, String newPassword) {
        try {
            if (ValidationUtil.isNullOrBlank(username)
                    || ValidationUtil.isNullOrBlank(email)
                    || ValidationUtil.isNullOrBlank(otp)
                    || ValidationUtil.isNullOrBlank(newPassword)) {
                return PasswordResetResult.INVALID_INPUT;
            }

            String cleanUsername = ValidationUtil.sanitize(username);
            String cleanEmail = ValidationUtil.sanitize(email);

            if (!ValidationUtil.isValidEmail(cleanEmail)) {
                return PasswordResetResult.INVALID_INPUT;
            }
            if (!ValidationUtil.isValidPassword(newPassword)) {
                return PasswordResetResult.WEAK_PASSWORD;
            }

            String key = resetOtpKey(cleanUsername, cleanEmail);
            PasswordResetOtp savedOtp = RESET_OTPS.get(key);
            if (savedOtp == null || !savedOtp.otp.equals(otp.trim())) {
                return PasswordResetResult.OTP_INVALID;
            }
            if (LocalDateTime.now().isAfter(savedOtp.expiresAt)) {
                RESET_OTPS.remove(key);
                return PasswordResetResult.OTP_EXPIRED;
            }

            Optional<User> opt = findByUsernameOrEmail(cleanUsername);
            if (opt.isEmpty()) {
                return PasswordResetResult.USER_NOT_FOUND;
            }

            User user = opt.get();
            if (user.getEmail() == null || !user.getEmail().equalsIgnoreCase(cleanEmail)) {
                return PasswordResetResult.EMAIL_MISMATCH;
            }

            boolean updated = userDAO.updatePasswordHash(user.getId(), PasswordUtil.hash(newPassword));
            if (!updated) {
                return PasswordResetResult.ERROR;
            }

            logSvc.log(user.getId(), "PASSWORD_RESET", "USER", user.getId(),
                "Password reset via login screen");
            RESET_OTPS.remove(key);
            return PasswordResetResult.SUCCESS;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Password reset SQL error", e);
            return PasswordResetResult.ERROR;
        }
    }

    /**
     * Returns human-readable text for a LoginResult (shown in the UI).
     */
    public String getLoginMessage(LoginResult result) {
        switch (result) {
            case SUCCESS:             return "Login successful.";
            case INVALID_CREDENTIALS: return "Invalid username or password.";
            case ACCOUNT_LOCKED:      return "Account locked. Try again later.";
            case ACCOUNT_INACTIVE:    return "Account is inactive. Contact admin.";
            case TOO_MANY_ATTEMPTS:   return "Too many failed attempts. Account locked for "
                                            + config.getLockoutMinutes() + " minutes.";
            default:                  return "A system error occurred. Please try again.";
        }
    }

    public String getPasswordResetMessage(PasswordResetResult result) {
        switch (result) {
            case SUCCESS:       return "Password reset successful. Please sign in.";
            case INVALID_INPUT: return "Please enter valid username, email, and password.";
            case USER_NOT_FOUND:return "No account found with that username.";
            case EMAIL_MISMATCH:return "Email does not match this username.";
            case WEAK_PASSWORD: return "Password must be at least 8 characters and contain a digit.";
            case OTP_INVALID:    return "Invalid OTP. Please check your email.";
            case OTP_EXPIRED:    return "OTP expired. Please request a new code.";
            default:            return "Could not reset password. Please try again.";
        }
    }

    public String getOtpMessage(OtpResult result) {
        switch (result) {
            case SUCCESS:             return "OTP sent to your email.";
            case INVALID_INPUT:       return "Enter a valid username and email first.";
            case USER_NOT_FOUND:      return "No account found with that username.";
            case EMAIL_MISMATCH:      return "Email does not match this username.";
            case MAIL_NOT_CONFIGURED: return "Gmail OTP is not configured yet.";
            case MAIL_SEND_FAILED:    return "Could not send OTP email. Check Gmail settings.";
            default:                  return "Could not send OTP. Please try again.";
        }
    }

    public String getRegistrationOtpMessage(RegistrationOtpResult result) {
        switch (result) {
            case SUCCESS:                  return "OTP sent to your email.";
            case INVALID_INPUT:            return "Enter a valid email address first.";
            case EMAIL_ALREADY_REGISTERED: return "Email is already registered.";
            case MAIL_NOT_CONFIGURED:      return "Email OTP is not configured yet.";
            case MAIL_SEND_FAILED:         return "Could not send OTP email. Check SMTP settings.";
            default:                       return "Could not send OTP. Please try again.";
        }
    }

    public String getEmailVerificationMessage(EmailVerificationResult result) {
        switch (result) {
            case SUCCESS:       return "Email verified.";
            case INVALID_INPUT: return "Enter your email and OTP first.";
            case OTP_INVALID:   return "Invalid OTP. Please check your email.";
            case OTP_EXPIRED:   return "OTP expired. Please request a new code.";
            default:            return "Could not verify OTP. Please try again.";
        }
    }

    private Optional<User> findByUsernameOrEmail(String loginIdentifier) throws SQLException {
        Optional<User> user = userDAO.findByUsername(loginIdentifier);
        if (user.isPresent()) {
            return user;
        }
        if (ValidationUtil.isValidEmail(loginIdentifier)) {
            return userDAO.findByEmail(loginIdentifier);
        }
        return Optional.empty();
    }

    private String resetOtpKey(String username, String email) {
        return username.trim().toLowerCase() + "|" + email.trim().toLowerCase();
    }

    private String emailOtpKey(String email) {
        return email.trim().toLowerCase();
    }

    private String generateOtp() {
        return String.format("%06d", OTP_RANDOM.nextInt(1_000_000));
    }

    private static final class PasswordResetOtp {
        private final String otp;
        private final LocalDateTime expiresAt;

        private PasswordResetOtp(String otp, LocalDateTime expiresAt) {
            this.otp = otp;
            this.expiresAt = expiresAt;
        }
    }

    private static final class EmailVerificationOtp {
        private final String otp;
        private final LocalDateTime expiresAt;

        private EmailVerificationOtp(String otp, LocalDateTime expiresAt) {
            this.otp = otp;
            this.expiresAt = expiresAt;
        }
    }
}
