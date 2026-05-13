package com.staffone.compliance;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class PayrollCalculator {

    public record Line(String label, BigDecimal amount, String type) {}

    public record Result(
        BigDecimal grossSalary,
        BigDecimal cnssEmployee,
        BigDecimal incomeTax,
        BigDecimal netSalary,
        List<Line> deductions) {}

    public Result calculate(BigDecimal gross, CountryConfig cfg) {
        List<Line> deductions = new ArrayList<>();
        BigDecimal cnss = BigDecimal.ZERO;
        BigDecimal tax  = BigDecimal.ZERO;

        CountryConfig.PayrollConfig pc = cfg.getPayroll();
        if (pc == null)
            return new Result(gross, BigDecimal.ZERO, BigDecimal.ZERO, gross, deductions);

        // CNSS / CNPS employé
        if (pc.getCnssEmployeeRate() != null
                && pc.getCnssEmployeeRate().compareTo(BigDecimal.ZERO) > 0) {
            cnss = gross.multiply(pc.getCnssEmployeeRate()).setScale(2, RoundingMode.HALF_UP);
            deductions.add(new Line("CNSS/CNPS Employé", cnss, "social"));
        }

        // Impôt sur le revenu (barème progressif annualisé)
        if (pc.getIncomeTax() != null && !pc.getIncomeTax().getBrackets().isEmpty()) {
            BigDecimal annualGross = gross.multiply(BigDecimal.valueOf(12));
            BigDecimal abatement  = pc.getIncomeTax().getAbatementProfessional() != null
                ? annualGross.multiply(pc.getIncomeTax().getAbatementProfessional())
                : BigDecimal.ZERO;
            BigDecimal taxable = annualGross
                .subtract(cnss.multiply(BigDecimal.valueOf(12)))
                .subtract(abatement);

            BigDecimal annualTax = BigDecimal.ZERO;
            for (CountryConfig.TaxBracket b : pc.getIncomeTax().getBrackets()) {
                if (taxable.compareTo(b.getMin()) > 0) {
                    BigDecimal upper = b.getMax() != null
                        ? b.getMax().min(taxable) : taxable;
                    annualTax = annualTax.add(
                        upper.subtract(b.getMin()).multiply(b.getRate()));
                }
            }
            tax = annualTax.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            if (tax.compareTo(BigDecimal.ZERO) > 0)
                deductions.add(new Line("Impôt sur revenu", tax, "tax"));
        }

        BigDecimal net = gross.subtract(cnss).subtract(tax)
            .setScale(2, RoundingMode.HALF_UP);
        return new Result(gross, cnss, tax, net, deductions);
    }
}
