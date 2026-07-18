package p2p.service;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;

public class EmailService {

    public static void sendOtpEmail(String toEmail, String otp) throws Exception {

        String apiKey = System.getenv("RESEND_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("RESEND_API_KEY not found.");
        }

        Resend resend = new Resend(apiKey);

        String html =
            "<h2>PeerLink Verification</h2>" +
            "<p>Your OTP code is:</p>" +
            "<h1 style=''letter-spacing:5px''>" + otp + "</h1>" +
            "<p>This code is valid for 5 minutes.</p>";

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("PeerLink <onboarding@resend.dev>")
                .to(toEmail)
                .subject("Your PeerLink Verification Code")
                .html(html)
                .build();

        resend.emails().send(params);

        System.out.println("OTP email sent successfully to: " + toEmail);
    }
}
