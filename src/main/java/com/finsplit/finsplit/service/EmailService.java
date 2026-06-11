package com.finsplit.finsplit.service;

//import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
//@RequiredArgsConstructor
@Slf4j
public class EmailService {

//    private final JavaMailSender mailSender;

    @Async
    public void sendBudgetAlert(String toEmail, String userName,
                                String categoryName, double percentageUsed,
                                double spent, double limit, String monthYear) {
        // Email sending commented out — uncomment when mail is configured
        log.info("Budget alert skipped (mail not configured) for {} — {}% used on {}",
                toEmail, String.format("%.0f", percentageUsed), categoryName);

        /*
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("⚠️ Budget Alert — " + categoryName + " (" + monthYear + ")");
            message.setText(
                    "Hi " + userName + ",\n\n" +
                            "You have used " + String.format("%.0f", percentageUsed) + "% of your " +
                            categoryName + " budget for " + monthYear + ".\n\n" +
                            "  Spent  : ₹" + String.format("%.2f", spent) + "\n" +
                            "  Budget : ₹" + String.format("%.2f", limit) + "\n\n" +
                            "Consider reviewing your spending in this category.\n\n" +
                            "— FinSplit"
            );
            mailSender.send(message);
            log.info("Budget alert sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send budget alert email to {}: {}", toEmail, e.getMessage());
        }
        */
    }
}
