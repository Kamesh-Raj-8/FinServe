package com.smelend.smelendbackend.entity.enums;
/**
 * Predefined fee categories for FeeConfig.
 * PROCESSING: deducted at disbursement (net amount = sanctioned - processing fees)
 * LEGAL:      legal/documentation charge at disbursement
 * INSURANCE:  loan insurance premium
 * PENAL:      overdue/late payment fine — applied by PenalSchedulingService
 * TECH:       tech/platform fee at disbursement
 * OTHER:      any other defined fee
 */
public enum FeeType { PROCESSING, LEGAL, INSURANCE, PENAL, TECH, OTHER }
