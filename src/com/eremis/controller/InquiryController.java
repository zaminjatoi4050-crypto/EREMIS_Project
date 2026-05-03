package com.eremis.controller;

import com.eremis.model.Inquiry;
import com.eremis.model.enums.InquiryStatus;
import com.eremis.service.InquiryService;

import java.util.List;

public class InquiryController {
    private final InquiryService inquiryService = new InquiryService();
    public Inquiry createInquiry(Inquiry inq)                { return inquiryService.createInquiry(inq); }
    public Inquiry updateInquiry(Inquiry inq)                { return inquiryService.updateInquiry(inq); }
    public List<Inquiry> getAllInquiries()                    { return inquiryService.getAllInquiries(); }
    public List<Inquiry> getByStatus(InquiryStatus status)   { return inquiryService.getByStatus(status); }
    public List<Inquiry> getMyInquiries(int userId)          { return inquiryService.getByUser(userId); }
    public int countPending()                                 { return inquiryService.countPending(); }
}
