package com.eremis.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Curated worldwide bank registry used by the payment UI.
 */
public final class BankRegistry {

    private static final List<String> BANKS;

    static {
        List<String> banks = new ArrayList<>();
        Collections.addAll(banks,
            "HBL", "UBL", "MCB", "Allied Bank", "Bank Alfalah", "Meezan Bank", "JS Bank", "Faysal Bank", "Standard Chartered Pakistan", "Askari Bank", "Sindh Bank", "Bank of Punjab", "National Bank of Pakistan", "BankIslami", "Soneri Bank", "Dubai Islamic Bank Pakistan",
            "Chase", "Bank of America", "Wells Fargo", "Citibank", "U.S. Bank", "PNC Bank", "Capital One", "TD Bank USA", "Truist", "Goldman Sachs", "Morgan Stanley", "BofA Securities", "Ally Bank", "Charles Schwab Bank", "Regions Bank", "Fifth Third Bank",
            "HSBC", "Barclays", "Lloyds Bank", "NatWest", "Santander UK", "Standard Chartered", "Nationwide", "Monzo", "Starling Bank", "Halifax", "RBS", "TSB",
            "Emirates NBD", "First Abu Dhabi Bank", "Dubai Islamic Bank", "Abu Dhabi Islamic Bank", "Mashreq Bank", "RAKBANK", "Commercial Bank of Dubai", "Abu Dhabi Commercial Bank", "Sharjah Islamic Bank",
            "HDFC Bank", "ICICI Bank", "State Bank of India", "Axis Bank", "Kotak Mahindra Bank", "IndusInd Bank", "Punjab National Bank", "Bank of Baroda", "Canara Bank", "Yes Bank", "IDFC FIRST Bank", "Federal Bank",
            "ING", "Deutsche Bank", "BNP Paribas", "Société Générale", "Crédit Agricole", "Rabobank", "ABN AMRO", "UniCredit", "Intesa Sanpaolo", "Commerzbank",
            "DBS", "OCBC", "UOB", "Maybank", "CIMB", "RHB", "Bank Negara Indonesia", "Bank Mandiri", "BCA", "KBank", "Krungsri", "BPI", "BDO Unibank"
        );
        BANKS = Collections.unmodifiableList(banks);
    }

    private BankRegistry() {}

    public static List<String> getBanks() {
        return BANKS;
    }

    public static List<String> search(String query) {
        if (query == null || query.isBlank()) return BANKS;
        String needle = query.toLowerCase(Locale.ROOT);
        return BANKS.stream()
            .filter(bank -> bank.toLowerCase(Locale.ROOT).contains(needle))
            .collect(Collectors.toList());
    }

    public static boolean contains(String bankName) {
        if (bankName == null) return false;
        String needle = bankName.trim().toLowerCase(Locale.ROOT);
        return BANKS.stream().anyMatch(bank -> bank.toLowerCase(Locale.ROOT).equals(needle));
    }
}