package com.eremis.service;

import com.eremis.config.AppConfig;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Minimal SMTP sender for Gmail app-password accounts.
 * Supports implicit SSL (465) and STARTTLS (587) without JavaMail.
 * Avoids adding an external JavaMail dependency to this desktop project.
 */
public class EmailService {

    private final AppConfig config = AppConfig.getInstance();

    public boolean isConfigured() {
        return config.isMailEnabled()
            && !config.getMailUsername().isBlank()
            && !config.getMailPassword().isBlank();
    }

    public void sendPasswordResetOtp(String toEmail, String otp, int validMinutes) throws Exception {
        String subject = "Your EREMIS password reset OTP";
        String body = "Your EREMIS password reset OTP is: " + otp + "\r\n\r\n"
            + "This code expires in " + validMinutes + " minutes.\r\n"
            + "If you did not request this, ignore this email.";
        send(toEmail, subject, body);
    }

    public void sendEmailVerificationOtp(String toEmail, String otp, int validMinutes) throws Exception {
        String subject = "Verify your EREMIS email";
        String body = "Your EREMIS email verification OTP is: " + otp + "\r\n\r\n"
            + "This code expires in " + validMinutes + " minutes.\r\n"
            + "If you did not create an EREMIS account, ignore this email.";
        send(toEmail, subject, body);
    }

    private void send(String toEmail, String subject, String body) throws Exception {
        String host = config.getMailHost();
        int port = config.getMailPort();
        boolean implicitSsl = config.isMailSslEnabled();
        Socket socket = implicitSsl ? openSslSocket(host, port) : openPlainSocket(host, port);

        try {
            SmtpSession smtp = new SmtpSession(socket);
            smtp.expect(220);
            smtp.command("EHLO eremis.local");
            smtp.expect(250);

            if (!implicitSsl && config.isMailStartTlsEnabled()) {
                smtp.command("STARTTLS");
                smtp.expect(220);
                socket = upgradeToTls(socket, host, port);
                smtp = new SmtpSession(socket);
                smtp.command("EHLO eremis.local");
                smtp.expect(250);
            }

            smtp.command("AUTH LOGIN");
            smtp.expect(334);
            smtp.command(base64(config.getMailUsername()));
            smtp.expect(334);
            smtp.command(base64(config.getMailPassword()));
            smtp.expect(235);
            smtp.command("MAIL FROM:<" + sanitizeAddress(config.getMailUsername()) + ">");
            smtp.expect(250);
            smtp.command("RCPT TO:<" + sanitizeAddress(toEmail) + ">");
            smtp.expect(250);
            smtp.command("DATA");
            smtp.expect(354);

            smtp.write("From: " + sanitizeHeader(config.getMailFromName()) + " <"
                + sanitizeAddress(config.getMailUsername()) + ">\r\n");
            smtp.write("To: <" + sanitizeAddress(toEmail) + ">\r\n");
            smtp.write("Subject: " + sanitizeHeader(subject) + "\r\n");
            smtp.write("MIME-Version: 1.0\r\n");
            smtp.write("Content-Type: text/plain; charset=UTF-8\r\n");
            smtp.write("\r\n");
            smtp.write(escapeSmtpBody(body));
            smtp.write("\r\n.\r\n");
            smtp.flush();
            smtp.expect(250);
            smtp.command("QUIT");
        } finally {
            socket.close();
        }
    }

    private String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private Socket openPlainSocket(String host, int port) throws Exception {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), config.getMailTimeoutMillis());
        socket.setSoTimeout(config.getMailTimeoutMillis());
        return socket;
    }

    private Socket openSslSocket(String host, int port) throws Exception {
        SSLSocket socket = (SSLSocket) SSLSocketFactory.getDefault().createSocket();
        socket.connect(new InetSocketAddress(host, port), config.getMailTimeoutMillis());
        socket.setSoTimeout(config.getMailTimeoutMillis());
        socket.startHandshake();
        return socket;
    }

    private Socket upgradeToTls(Socket socket, String host, int port) throws Exception {
        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket) factory.createSocket(socket, host, port, true);
        sslSocket.setSoTimeout(config.getMailTimeoutMillis());
        sslSocket.startHandshake();
        return sslSocket;
    }

    private String escapeSmtpBody(String body) {
        String normalized = body.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        StringBuilder escaped = new StringBuilder(normalized.length() + lines.length * 2);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith(".")) {
                escaped.append('.');
            }
            escaped.append(lines[i]);
            if (i < lines.length - 1) {
                escaped.append("\r\n");
            }
        }
        return escaped.toString();
    }

    private String sanitizeHeader(String value) {
        return value == null ? "" : value.replace("\r", "").replace("\n", "").trim();
    }

    private String sanitizeAddress(String value) {
        return sanitizeHeader(value);
    }

    private static final class SmtpSession {
        private final BufferedReader in;
        private final BufferedWriter out;

        private SmtpSession(Socket socket) throws Exception {
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        }

        private void command(String command) throws Exception {
            write(command);
            write("\r\n");
            flush();
        }

        private void write(String value) throws Exception {
            out.write(value);
        }

        private void flush() throws Exception {
            out.flush();
        }

        private void expect(int expectedCode) throws Exception {
            String line = in.readLine();
            if (line == null) {
                throw new IllegalStateException("SMTP server closed the connection.");
            }
            String last = line;
            while (line.length() >= 4 && line.charAt(3) == '-') {
                line = in.readLine();
                if (line == null) {
                    break;
                }
                last = line;
            }
            if (!last.startsWith(String.valueOf(expectedCode))) {
                throw new IllegalStateException("SMTP error: " + last);
            }
        }
    }
}
