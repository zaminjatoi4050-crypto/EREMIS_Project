package com.eremis.service;

import com.eremis.dao.InquiryDAO;
import com.eremis.model.Inquiry;
import com.eremis.model.enums.InquiryStatus;
import com.eremis.utils.SessionManager;
import com.eremis.utils.ValidationUtil;
import java.sql.SQLException;
import java.util.List;

public class InquiryService {
    private final InquiryDAO     inquiryDAO = new InquiryDAO();
    private final LoggingService log        = new LoggingService();
    private final NotificationService notif = new NotificationService();

    public Inquiry createInquiry(Inquiry inq) {
        if (ValidationUtil.isNullOrBlank(inq.getSubject()))
            throw new IllegalArgumentException("Subject is required.");
        if (ValidationUtil.isNullOrBlank(inq.getMessage()))
            throw new IllegalArgumentException("Message is required.");
        try {
            inq = inquiryDAO.create(inq);
            log.log(inq.getUserId(), "INQUIRY_CREATE", "INQUIRY", inq.getId(),
                    "New inquiry on property #" + inq.getPropertyId());
            notif.notifyAdmins("New Inquiry: " + inq.getSubject(),
                               "A new inquiry has been submitted for review.");
            return inq;
        } catch (SQLException e) { throw new RuntimeException("Cannot save inquiry.", e); }
    }

    public Inquiry updateInquiry(Inquiry inq) {
        try {
            inquiryDAO.update(inq);
            log.log(SessionManager.getInstance().getCurrentUserId(),
                    "INQUIRY_UPDATE", "INQUIRY", inq.getId(), "Status → " + inq.getStatus());
            return inq;
        } catch (SQLException e) { throw new RuntimeException("Cannot update inquiry.", e); }
    }

    public List<Inquiry> getAllInquiries() {
        try { return inquiryDAO.findAll(); }
        catch (SQLException e) { throw new RuntimeException("Cannot load inquiries.", e); }
    }

    public List<Inquiry> getByStatus(InquiryStatus status) {
        try { return inquiryDAO.findByStatus(status); }
        catch (SQLException e) { throw new RuntimeException("Cannot load inquiries.", e); }
    }

    public List<Inquiry> getByUser(int userId) {
        try { return inquiryDAO.findByUserId(userId); }
        catch (SQLException e) { throw new RuntimeException("Cannot load inquiries.", e); }
    }

    public int countPending() {
        try { return inquiryDAO.countByStatus(InquiryStatus.PENDING); }
        catch (SQLException e) { return 0; }
    }
}
