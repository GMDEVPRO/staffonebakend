package com.staffone.compliance;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class CountryConfig {
    private String country, currency, zone;
    private boolean rtl;
    private LeaveConfig leave;
    private PayrollConfig payroll;

    @Data
    public static class LeaveConfig {
        private int annualDays = 24;
        private int maternityDays = 98;
        private int paternityDays = 3;
    }

    @Data
    public static class PayrollConfig {
        private BigDecimal smig;
        private BigDecimal cnssEmployeeRate;
        private BigDecimal cnssEmployerRate;
        private IncomeTax incomeTax;
    }

    @Data
    public static class IncomeTax {
        private BigDecimal abatementProfessional;
        private List<TaxBracket> brackets = new ArrayList<>();
    }

    @Data
    public static class TaxBracket {
        private BigDecimal min, max, rate;
    }
}
