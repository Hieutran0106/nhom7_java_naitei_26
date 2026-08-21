package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.config.AppOtpProperties;
import com.nhom7.coworkingspace.entity.OtpToken;
import com.nhom7.coworkingspace.entity.User;
import com.nhom7.coworkingspace.enums.OtpPurpose;
import com.nhom7.coworkingspace.enums.UserStatus;
import com.nhom7.coworkingspace.repository.OtpTokenRepository;
import com.nhom7.coworkingspace.repository.UserRepository;
import com.nhom7.coworkingspace.service.impl.OtpServiceImpl;
import com.nhom7.coworkingspace.util.OtpCodeGenerator;
import com.nhom7.coworkingspace.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

        private static final Instant NOW = Instant.parse("2026-08-20T10:00:00Z");

        @Mock
        private UserRepository userRepository;

        @Mock
        private OtpTokenRepository otpTokenRepository;

        @Mock
        private EmailService emailService;

        @Mock
        private EmailTemplateService emailTemplateService;

        @Mock
        private OtpCodeGenerator otpCodeGenerator;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private MessageSource messageSource;

        @Mock
        private TokenBlacklistService tokenBlacklistService;

        private OtpService otpService;
        private AppOtpProperties properties;

        @BeforeEach
        void setUp() {
                properties = new AppOtpProperties();
                properties.setExpirationMinutes(5);
                properties.setResendCooldownSeconds(60);
                properties.setMaxFailedAttempts(5);
                Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
                otpService = new OtpServiceImpl(
                                userRepository,
                                otpTokenRepository,
                                emailService,
                                emailTemplateService,
                                otpCodeGenerator,
                                passwordEncoder,
                                properties,
                                clock,
                                messageSource,
                                tokenBlacklistService);
        }

        @Test
        void sendConfirmationOtpShouldCreateTokenAndSendEmailWhenNoPreviousToken() {
                User user = User.builder()
                                .id(1L)
                                .name("Test User")
                                .email("user@coworking.test")
                                .language("vi")
                                .status(UserStatus.INACTIVE)
                                .build();
                given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
                given(otpTokenRepository.findByUserAndPurpose(user, OtpPurpose.ACCOUNT_CONFIRMATION))
                                .willReturn(Optional.empty());
                given(otpCodeGenerator.generateCode()).willReturn("123456");
                given(passwordEncoder.encode("123456")).willReturn("hashed-code");
                given(messageSource.getMessage("email.confirmation.subject", null, Locale.forLanguageTag("vi")))
                                .willReturn("Xác nhận tài khoản của bạn");
                given(emailTemplateService.renderAccountConfirmation("123456", Locale.forLanguageTag("vi")))
                                .willReturn("<p>Confirmation template: 123456</p>");

                otpService.sendConfirmationOtp(user.getEmail());

                ArgumentCaptor<OtpToken> tokenCaptor = ArgumentCaptor.forClass(OtpToken.class);
                verify(otpTokenRepository).saveAndFlush(tokenCaptor.capture());
                OtpToken savedToken = tokenCaptor.getValue();
                assertThat(savedToken.getUser()).isSameAs(user);
                assertThat(savedToken.getCodeHash()).isEqualTo("hashed-code");
                assertThat(savedToken.getPurpose()).isEqualTo(OtpPurpose.ACCOUNT_CONFIRMATION);
                assertThat(savedToken.getCreatedAt()).isEqualTo(NOW);
                assertThat(savedToken.getExpiresAt()).isEqualTo(NOW.plusSeconds(300));
                assertThat(savedToken.getFailedAttempts()).isEqualTo(0);
                verify(emailService).sendHtmlEmail(
                                user.getEmail(),
                                "Xác nhận tài khoản của bạn",
                                "<p>Confirmation template: 123456</p>");
        }

        @Test
        void sendConfirmationOtpShouldThrowCooldownExceptionWhenRequestedTooSoon() {
                User user = User.builder()
                                .id(1L)
                                .email("user@coworking.test")
                                .status(UserStatus.INACTIVE)
                                .build();
                OtpToken recentToken = OtpToken.builder()
                                .id(10L)
                                .user(user)
                                .createdAt(NOW.minusSeconds(30)) // 30s ago, cooldown is 60s
                                .build();

                given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
                given(otpTokenRepository.findByUserAndPurpose(user, OtpPurpose.ACCOUNT_CONFIRMATION))
                                .willReturn(Optional.of(recentToken));

                assertThatThrownBy(() -> otpService.sendConfirmationOtp(user.getEmail()))
                                .isInstanceOf(AppException.class)
                                .satisfies(ex -> {
                                        AppException appEx = (AppException) ex;
                                        assertThat(appEx.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                                        assertThat(appEx.getMessage()).isEqualTo("auth.otp.cooldown");
                                });

                verify(otpTokenRepository, never()).delete(recentToken);
                verifyNoInteractions(emailService);
        }

        @Test
        void sendConfirmationOtpShouldAllowResendAfterCooldownPeriod() {
                User user = User.builder()
                                .id(1L)
                                .email("user@coworking.test")
                                .status(UserStatus.INACTIVE)
                                .language("en")
                                .build();
                OtpToken oldToken = OtpToken.builder()
                                .id(10L)
                                .user(user)
                                .createdAt(NOW.minusSeconds(65)) // 65s ago -> cooldown passed
                                .build();

                given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
                given(otpTokenRepository.findByUserAndPurpose(user, OtpPurpose.ACCOUNT_CONFIRMATION))
                                .willReturn(Optional.of(oldToken));
                given(otpCodeGenerator.generateCode()).willReturn("123456");
                given(passwordEncoder.encode("123456")).willReturn("hashed-code");
                given(messageSource.getMessage(eq("email.confirmation.subject"), any(), eq(Locale.ENGLISH)))
                                .willReturn("Confirm Account");
                given(emailTemplateService.renderAccountConfirmation(eq("123456"), eq(Locale.ENGLISH)))
                                .willReturn("<html>Confirm</html>");

                otpService.sendConfirmationOtp(user.getEmail());

                verify(otpTokenRepository).delete(oldToken);
                verify(otpTokenRepository).saveAndFlush(any(OtpToken.class));
        }

        @Test
        void sendConfirmationOtpShouldIgnoreBlankEmail() {
                otpService.sendConfirmationOtp(" ");

                verify(userRepository, never()).findByEmail(" ");
        }

        @Test
        void sendConfirmationOtpShouldNotRevealUnknownEmail() {
                String email = "unknown@coworking.test";
                given(userRepository.findByEmail(email)).willReturn(Optional.empty());

                otpService.sendConfirmationOtp(email);

                verifyNoInteractions(otpTokenRepository, emailService);
        }

        @Test
        void sendConfirmationOtpShouldIgnoreActiveUser() {
                User user = User.builder()
                                .id(1L)
                                .email("active@coworking.test")
                                .language("en")
                                .status(UserStatus.ACTIVE)
                                .build();
                given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));

                otpService.sendConfirmationOtp(user.getEmail());

                verifyNoInteractions(otpTokenRepository, emailService);
        }

        @Test
        void sendPasswordResetOtpShouldCreateTokenAndSendEmailForActiveUser() {
                User user = User.builder()
                                .id(2L)
                                .name("Active User")
                                .email("active@coworking.test")
                                .status(UserStatus.ACTIVE)
                                .build();
                given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
                given(otpTokenRepository.findByUserAndPurpose(user, OtpPurpose.PASSWORD_RESET))
                                .willReturn(Optional.empty());
                given(otpCodeGenerator.generateCode()).willReturn("654321");
                given(passwordEncoder.encode("654321")).willReturn("hashed-reset-code");
                given(messageSource.getMessage("email.password.reset.subject", null, Locale.ENGLISH))
                                .willReturn("Reset your password");
                given(emailTemplateService.renderPasswordReset("654321", Locale.ENGLISH))
                                .willReturn("<p>Reset template: 654321</p>");

                otpService.sendPasswordResetOtp(user.getEmail());

                ArgumentCaptor<OtpToken> tokenCaptor = ArgumentCaptor.forClass(OtpToken.class);
                verify(otpTokenRepository).saveAndFlush(tokenCaptor.capture());
                assertThat(tokenCaptor.getValue().getPurpose()).isEqualTo(OtpPurpose.PASSWORD_RESET);
                assertThat(tokenCaptor.getValue().getCodeHash()).isEqualTo("hashed-reset-code");
                assertThat(tokenCaptor.getValue().getFailedAttempts()).isEqualTo(0);
                verify(emailService).sendHtmlEmail(
                                user.getEmail(),
                                "Reset your password",
                                "<p>Reset template: 654321</p>");
        }

        @Test
        void sendPasswordResetOtpShouldThrowCooldownExceptionWhenRequestedTooSoon() {
                User user = User.builder()
                                .id(2L)
                                .email("active@coworking.test")
                                .status(UserStatus.ACTIVE)
                                .build();
                OtpToken recentToken = OtpToken.builder()
                                .id(20L)
                                .user(user)
                                .createdAt(NOW.minusSeconds(15))
                                .build();

                given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
                given(otpTokenRepository.findByUserAndPurpose(user, OtpPurpose.PASSWORD_RESET))
                                .willReturn(Optional.of(recentToken));

                assertThatThrownBy(() -> otpService.sendPasswordResetOtp(user.getEmail()))
                                .isInstanceOf(AppException.class)
                                .satisfies(ex -> {
                                        AppException appEx = (AppException) ex;
                                        assertThat(appEx.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                                        assertThat(appEx.getMessage()).isEqualTo("auth.otp.cooldown");
                                });

                verify(otpTokenRepository, never()).delete(recentToken);
        }

        @Test
        void sendPasswordResetOtpShouldNormalizeEmailBeforeLookup() {
                User user = User.builder()
                                .id(3L)
                                .email("active@coworking.test")
                                .status(UserStatus.ACTIVE)
                                .build();
                given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));
                given(otpTokenRepository.findByUserAndPurpose(user, OtpPurpose.PASSWORD_RESET))
                                .willReturn(Optional.empty());
                given(otpCodeGenerator.generateCode()).willReturn("111222");
                given(passwordEncoder.encode("111222")).willReturn("hashed-code");
                given(messageSource.getMessage("email.password.reset.subject", null, Locale.ENGLISH))
                                .willReturn("Reset your password");
                given(emailTemplateService.renderPasswordReset("111222", Locale.ENGLISH))
                                .willReturn("reset-body");

                otpService.sendPasswordResetOtp(" Active@Coworking.Test ");

                verify(userRepository).findByEmail("active@coworking.test");
                verify(emailService).sendHtmlEmail(user.getEmail(), "Reset your password", "reset-body");
        }

        @Test
        void sendPasswordResetOtpShouldNotRevealUnknownEmail() {
                String email = "unknown@coworking.test";
                given(userRepository.findByEmail(email)).willReturn(Optional.empty());

                otpService.sendPasswordResetOtp(email);

                verifyNoInteractions(otpTokenRepository, emailTemplateService, emailService);
        }

        @Test
        void confirmAccountShouldActivateUserAndRemoveTokenWhenOtpIsValid() {
                User user = User.builder()
                                .id(1L)
                                .email("user@coworking.test")
                                .status(UserStatus.INACTIVE)
                                .build();
                OtpToken token = OtpToken.builder()
                                .id(10L)
                                .user(user)
                                .codeHash("hashed-otp")
                                .purpose(OtpPurpose.ACCOUNT_CONFIRMATION)
                                .expiresAt(NOW.plusSeconds(100))
                                .failedAttempts(0)
                                .build();

                given(userRepository.findByEmail("user@coworking.test")).willReturn(Optional.of(user));
                given(otpTokenRepository.findByUserAndPurpose(user, OtpPurpose.ACCOUNT_CONFIRMATION))
                                .willReturn(Optional.of(token));
                given(passwordEncoder.matches("123456", "hashed-otp")).willReturn(true);

                otpService.confirmAccount("user@coworking.test", "123456");

                assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
                verify(userRepository).save(user);
                verify(otpTokenRepository).delete(token);
        }

        @Test
        void confirmAccountShouldThrowExceptionWhenOtpIsExpired() {
                User user = User.builder()
                                .id(1L)
                                .email("user@coworking.test")
                                .status(UserStatus.INACTIVE)
                                .build();
                OtpToken token = OtpToken.builder()
                                .id(10L)
                                .user(user)
                                .codeHash("hashed-otp")
                                .purpose(OtpPurpose.ACCOUNT_CONFIRMATION)
                                .expiresAt(NOW.minusSeconds(10))
                                .build();

                given(userRepository.findByEmail("user@coworking.test")).willReturn(Optional.of(user));
                given(otpTokenRepository.findByUserAndPurpose(user, OtpPurpose.ACCOUNT_CONFIRMATION))
                                .willReturn(Optional.of(token));

                assertThatThrownBy(() -> otpService.confirmAccount("user@coworking.test", "123456"))
                                .isInstanceOf(AppException.class);

                verify(otpTokenRepository).delete(token);
                verify(userRepository, never()).save(user);
        }

        @Test
        void confirmAccountShouldIncrementFailedAttemptsWhenOtpDoesNotMatch() {
                User user = User.builder()
                                .id(1L)
                                .email("user@coworking.test")
                                .status(UserStatus.INACTIVE)
                                .build();
                OtpToken token = OtpToken.builder()
                                .id(10L)
                                .user(user)
                                .codeHash("hashed-otp")
                                .purpose(OtpPurpose.ACCOUNT_CONFIRMATION)
                                .expiresAt(NOW.plusSeconds(100))
                                .failedAttempts(1)
                                .build();

                given(userRepository.findByEmail("user@coworking.test")).willReturn(Optional.of(user));
                given(otpTokenRepository.findByUserAndPurpose(user, OtpPurpose.ACCOUNT_CONFIRMATION))
                                .willReturn(Optional.of(token));
                given(passwordEncoder.matches("000000", "hashed-otp")).willReturn(false);

                assertThatThrownBy(() -> otpService.confirmAccount("user@coworking.test", "000000"))
                                .isInstanceOf(AppException.class)
                                .hasMessage("auth.otp.invalid");

                assertThat(token.getFailedAttempts()).isEqualTo(2);
                verify(otpTokenRepository).save(token);
                verify(otpTokenRepository, never()).delete(token);
                verify(userRepository, never()).save(user);
        }

        @Test
        void confirmAccountShouldDeleteTokenWhenMaxFailedAttemptsReached() {
                User user = User.builder()
                                .id(1L)
                                .email("user@coworking.test")
                                .status(UserStatus.INACTIVE)
                                .build();
                OtpToken token = OtpToken.builder()
                                .id(10L)
                                .user(user)
                                .codeHash("hashed-otp")
                                .purpose(OtpPurpose.ACCOUNT_CONFIRMATION)
                                .expiresAt(NOW.plusSeconds(100))
                                .failedAttempts(4) // 4 + 1 = 5 (max)
                                .build();

                given(userRepository.findByEmail("user@coworking.test")).willReturn(Optional.of(user));
                given(otpTokenRepository.findByUserAndPurpose(user, OtpPurpose.ACCOUNT_CONFIRMATION))
                                .willReturn(Optional.of(token));
                given(passwordEncoder.matches("000000", "hashed-otp")).willReturn(false);

                assertThatThrownBy(() -> otpService.confirmAccount("user@coworking.test", "000000"))
                                .isInstanceOf(AppException.class)
                                .hasMessage("auth.otp.max_attempts_exceeded");

                assertThat(token.getFailedAttempts()).isEqualTo(5);
                verify(otpTokenRepository).delete(token);
                verify(userRepository, never()).save(user);
        }

        @Test
        void resetPasswordShouldUpdatePasswordAndRevokeTokensWhenOtpIsValid() {
                User user = User.builder()
                                .id(2L)
                                .email("user@coworking.test")
                                .status(UserStatus.ACTIVE)
                                .password("old-hashed-password")
                                .build();
                OtpToken token = OtpToken.builder()
                                .id(20L)
                                .user(user)
                                .codeHash("hashed-reset-otp")
                                .purpose(OtpPurpose.PASSWORD_RESET)
                                .expiresAt(NOW.plusSeconds(100))
                                .failedAttempts(0)
                                .build();

                given(userRepository.findByEmail("user@coworking.test")).willReturn(Optional.of(user));
                given(otpTokenRepository.findByUserAndPurpose(user, OtpPurpose.PASSWORD_RESET))
                                .willReturn(Optional.of(token));
                given(passwordEncoder.matches("123456", "hashed-reset-otp")).willReturn(true);
                given(passwordEncoder.encode("NewSecret123@")).willReturn("new-hashed-password");

                otpService.resetPassword("user@coworking.test", "123456", "NewSecret123@");

                assertThat(user.getPassword()).isEqualTo("new-hashed-password");
                assertThat(user.getPasswordChangedAt()).isEqualTo(NOW);
                verify(userRepository).save(user);
                verify(tokenBlacklistService).blacklistUserTokens("user@coworking.test", Date.from(NOW));
                verify(otpTokenRepository).delete(token);
        }

        @Test
        void resetPasswordShouldThrowExceptionWhenOtpIsExpired() {
                User user = User.builder()
                                .id(2L)
                                .email("user@coworking.test")
                                .status(UserStatus.ACTIVE)
                                .build();
                OtpToken token = OtpToken.builder()
                                .id(20L)
                                .user(user)
                                .codeHash("hashed-reset-otp")
                                .purpose(OtpPurpose.PASSWORD_RESET)
                                .expiresAt(NOW.minusSeconds(10))
                                .build();

                given(userRepository.findByEmail("user@coworking.test")).willReturn(Optional.of(user));
                given(otpTokenRepository.findByUserAndPurpose(user, OtpPurpose.PASSWORD_RESET))
                                .willReturn(Optional.of(token));

                assertThatThrownBy(() -> otpService.resetPassword("user@coworking.test", "123456", "NewSecret123@"))
                                .isInstanceOf(AppException.class);

                verify(otpTokenRepository).delete(token);
                verify(userRepository, never()).save(user);
        }

        @Test
        void resetPasswordShouldIncrementFailedAttemptsWhenOtpDoesNotMatch() {
                User user = User.builder()
                                .id(2L)
                                .email("user@coworking.test")
                                .status(UserStatus.ACTIVE)
                                .build();
                OtpToken token = OtpToken.builder()
                                .id(20L)
                                .user(user)
                                .codeHash("hashed-reset-otp")
                                .purpose(OtpPurpose.PASSWORD_RESET)
                                .expiresAt(NOW.plusSeconds(100))
                                .failedAttempts(2)
                                .build();

                given(userRepository.findByEmail("user@coworking.test")).willReturn(Optional.of(user));
                given(otpTokenRepository.findByUserAndPurpose(user, OtpPurpose.PASSWORD_RESET))
                                .willReturn(Optional.of(token));
                given(passwordEncoder.matches("000000", "hashed-reset-otp")).willReturn(false);

                assertThatThrownBy(() -> otpService.resetPassword("user@coworking.test", "000000", "NewSecret123@"))
                                .isInstanceOf(AppException.class)
                                .hasMessage("auth.otp.invalid");

                assertThat(token.getFailedAttempts()).isEqualTo(3);
                verify(otpTokenRepository).save(token);
                verify(otpTokenRepository, never()).delete(token);
                verify(userRepository, never()).save(user);
        }

        @Test
        void resetPasswordShouldDeleteTokenWhenMaxFailedAttemptsReached() {
                User user = User.builder()
                                .id(2L)
                                .email("user@coworking.test")
                                .status(UserStatus.ACTIVE)
                                .build();
                OtpToken token = OtpToken.builder()
                                .id(20L)
                                .user(user)
                                .codeHash("hashed-reset-otp")
                                .purpose(OtpPurpose.PASSWORD_RESET)
                                .expiresAt(NOW.plusSeconds(100))
                                .failedAttempts(4)
                                .build();

                given(userRepository.findByEmail("user@coworking.test")).willReturn(Optional.of(user));
                given(otpTokenRepository.findByUserAndPurpose(user, OtpPurpose.PASSWORD_RESET))
                                .willReturn(Optional.of(token));
                given(passwordEncoder.matches("000000", "hashed-reset-otp")).willReturn(false);

                assertThatThrownBy(() -> otpService.resetPassword("user@coworking.test", "000000", "NewSecret123@"))
                                .isInstanceOf(AppException.class)
                                .hasMessage("auth.otp.max_attempts_exceeded");

                assertThat(token.getFailedAttempts()).isEqualTo(5);
                verify(otpTokenRepository).delete(token);
                verify(userRepository, never()).save(user);
        }

        @Test
        void resetPasswordShouldThrowExceptionWhenUserIsInactive() {
                User user = User.builder()
                                .id(2L)
                                .email("user@coworking.test")
                                .status(UserStatus.INACTIVE)
                                .build();

                given(userRepository.findByEmail("user@coworking.test")).willReturn(Optional.of(user));

                assertThatThrownBy(() -> otpService.resetPassword("user@coworking.test", "123456", "NewSecret123@"))
                                .isInstanceOf(AppException.class);

                verifyNoInteractions(otpTokenRepository);
                verify(userRepository, never()).save(user);
        }

        @Test
        void resetPasswordShouldThrowExceptionWhenUserDoesNotExist() {
                given(userRepository.findByEmail("unknown@coworking.test")).willReturn(Optional.empty());

                assertThatThrownBy(() -> otpService.resetPassword("unknown@coworking.test", "123456", "NewSecret123@"))
                                .isInstanceOf(AppException.class);

                verifyNoInteractions(otpTokenRepository);
                verify(userRepository, never()).save(any(User.class));
        }

        @Test
        void confirmAccountShouldThrowExceptionWhenUserIsAlreadyActive() {
                User user = User.builder()
                                .id(1L)
                                .email("active@coworking.test")
                                .status(UserStatus.ACTIVE)
                                .build();

                given(userRepository.findByEmail("active@coworking.test")).willReturn(Optional.of(user));

                assertThatThrownBy(() -> otpService.confirmAccount("active@coworking.test", "123456"))
                                .isInstanceOf(AppException.class);

                verifyNoInteractions(otpTokenRepository);
                verify(userRepository, never()).save(user);
        }

        @Test
        void confirmAccountShouldThrowExceptionWhenUserDoesNotExist() {
                given(userRepository.findByEmail("unknown@coworking.test")).willReturn(Optional.empty());

                assertThatThrownBy(() -> otpService.confirmAccount("unknown@coworking.test", "123456"))
                                .isInstanceOf(AppException.class);

                verifyNoInteractions(otpTokenRepository);
                verify(userRepository, never()).save(any(User.class));
        }
}
