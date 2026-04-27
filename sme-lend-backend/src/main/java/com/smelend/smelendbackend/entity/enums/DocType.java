package com.smelend.smelendbackend.entity.enums;

public enum DocType {
    // ── KYC / Identity documents (Agent-reviewed) ──────────────────
    PAN,                    // PAN card (individual or business)
    AADHAAR,                // Aadhaar card (individual identity + address)
    BUSINESS_REG_CERT,      // Business Registration Certificate (Udyam / incorporation)
    GST_CERTIFICATE,        // GST registration certificate
    PROMOTER_PHOTO,         // Recent passport-size photograph of promoter
    SHOP_LICENSE,           // Shop & Establishment licence

    // ── Financial / Underwriting documents (Underwriter-reviewed) ──
    BANK_STATEMENT,         // Last 6 months bank statements
    ITR,                    // Income Tax Returns (last 2 years)
    BALANCE_SHEET,          // Audited Balance Sheet
    PROFIT_LOSS,            // Audited P&L Statement
    GST_RETURNS,            // GSTR-3B (quarterly / monthly returns)
    AUDIT_REPORT,           // Chartered Accountant Audit Report

    // ── General ─────────────────────────────────────────────────────
    GST,                    // Legacy alias (kept for backwards compat)
    OTHER                   // Miscellaneous
}
