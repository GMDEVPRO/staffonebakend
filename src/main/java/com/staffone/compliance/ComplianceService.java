package com.staffone.compliance;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ComplianceService {

    private final ResourceLoader loader;
    private final Map<String, CountryConfig> cache = new ConcurrentHashMap<>();

    private static final List<String> COUNTRIES =
        List.of("sn", "ci", "ma", "eg", "ao", "cm", "ga", "cg");

    @PostConstruct
    public void loadAll() {
        Yaml yaml = new Yaml();
        for (String code : COUNTRIES) {
            try {
                var res = loader.getResource("classpath:compliance/" + code + ".yml");
                if (res.exists()) {
                    try (InputStream is = res.getInputStream()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> raw = yaml.load(is);
                        cache.put(code.toUpperCase(), map(raw, code.toUpperCase()));
                    }
                }
            } catch (Exception e) {
                System.err.println("WARN: compliance/" + code + ".yml — " + e.getMessage());
            }
        }
        System.out.println("ComplianceService loaded: " + cache.keySet());
    }

    public CountryConfig get(String code) {
        CountryConfig c = cache.get(code.toUpperCase());
        if (c == null) throw new CountryNotSupportedException(
            "Unsupported country: " + code + ". Available: " + cache.keySet());
        return c;
    }

    public boolean supports(String code)  { return cache.containsKey(code.toUpperCase()); }
    public Set<String> supported()        { return Collections.unmodifiableSet(cache.keySet()); }

    @SuppressWarnings("unchecked")
    private CountryConfig map(Map<String, Object> raw, String code) {
        CountryConfig cfg = new CountryConfig();
        cfg.setCountry(code);
        cfg.setCurrency((String) raw.getOrDefault("currency", "XOF"));
        cfg.setRtl(Boolean.TRUE.equals(raw.get("rtl")));
        cfg.setZone((String) raw.getOrDefault("zone", "CEDEAO"));

        Map<String, Object> lv = (Map<String, Object>) raw.get("leave");
        if (lv != null) {
            CountryConfig.LeaveConfig lc = new CountryConfig.LeaveConfig();
            lc.setAnnualDays(n(lv.get("annual_days"), 24));
            lc.setMaternityDays(n(lv.get("maternity_days"), 98));
            lc.setPaternityDays(n(lv.get("paternity_days"), 3));
            cfg.setLeave(lc);
        }

        Map<String, Object> pv = (Map<String, Object>) raw.get("payroll");
        if (pv != null) {
            CountryConfig.PayrollConfig pc = new CountryConfig.PayrollConfig();
            pc.setSmig(bd(pv.get("smig")));
            pc.setCnssEmployeeRate(bd(pv.get("cnss_employee_rate")));
            pc.setCnssEmployerRate(bd(pv.get("cnss_employer_rate")));

            Map<String, Object> it = (Map<String, Object>) pv.get("income_tax");
            if (it != null) {
                CountryConfig.IncomeTax itc = new CountryConfig.IncomeTax();
                itc.setAbatementProfessional(bd(it.get("abatement_professional")));
                List<Map<String, Object>> bs =
                    (List<Map<String, Object>>) it.getOrDefault("brackets", List.of());
                for (Map<String, Object> b : bs) {
                    CountryConfig.TaxBracket tb = new CountryConfig.TaxBracket();
                    tb.setMin(bd(b.get("min")));
                    tb.setMax(b.get("max") != null ? bd(b.get("max")) : null);
                    tb.setRate(bd(b.get("rate")));
                    itc.getBrackets().add(tb);
                }
                pc.setIncomeTax(itc);
            }
            cfg.setPayroll(pc);
        }
        return cfg;
    }

    private BigDecimal bd(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof Number num) return BigDecimal.valueOf(num.doubleValue());
        return new BigDecimal(v.toString());
    }

    private int n(Object v, int def) {
        return v instanceof Number num ? num.intValue() : def;
    }

    public static class CountryNotSupportedException extends RuntimeException {
        public CountryNotSupportedException(String m) { super(m); }
    }
}
