-- ════════════════════════════════════════════════════════════════
-- V2__seed_data.sql — StaffOne seed multi-tenant
-- Compatible PostgreSQL + H2 (MODE=PostgreSQL)
-- ════════════════════════════════════════════════════════════════

-- ── TENANTS ──────────────────────────────────────────────────────

INSERT INTO tenants (id, name, slug, country_code, sector, plan, mode,
                     locale, timezone, currency, status, trial_ends_at)
VALUES
  ('a0000000-0000-0000-0000-000000000001',
   'École Al-Nour', 'ecole-al-nour', 'SN', 'school',
   'PRO', 'RH_ONLY', 'fr', 'Africa/Dakar', 'XOF', 'ACTIVE', NULL),

  ('a0000000-0000-0000-0000-000000000002',
   'École Avenir Abidjan', 'ecole-avenir-abi', 'CI', 'school',
   'PRO', 'BUNDLE', 'fr', 'Africa/Abidjan', 'XOF', 'ACTIVE', NULL),

  ('a0000000-0000-0000-0000-000000000003',
   'Institut Technique Yaoundé', 'it-yaounde', 'CM', 'school',
   'STARTER', 'RH_ONLY', 'fr', 'Africa/Douala', 'XAF',
   'TRIAL', NOW() + INTERVAL '30 days');

-- ── MODULES ──────────────────────────────────────────────────────

INSERT INTO tenant_modules (tenant_id, module) VALUES
  ('a0000000-0000-0000-0000-000000000001', 'employees'),
  ('a0000000-0000-0000-0000-000000000001', 'attendance'),
  ('a0000000-0000-0000-0000-000000000001', 'payroll'),
  ('a0000000-0000-0000-0000-000000000001', 'leaves'),
  ('a0000000-0000-0000-0000-000000000001', 'documents'),

  ('a0000000-0000-0000-0000-000000000002', 'employees'),
  ('a0000000-0000-0000-0000-000000000002', 'attendance'),
  ('a0000000-0000-0000-0000-000000000002', 'payroll'),
  ('a0000000-0000-0000-0000-000000000002', 'leaves'),
  ('a0000000-0000-0000-0000-000000000002', 'documents'),
  ('a0000000-0000-0000-0000-000000000002', 'edukira_sync'),

  ('a0000000-0000-0000-0000-000000000003', 'employees'),
  ('a0000000-0000-0000-0000-000000000003', 'attendance'),
  ('a0000000-0000-0000-0000-000000000003', 'leaves');

-- ── EMPLOYEES — Tenant 1 (École Al-Nour, Sénégal) ────────────────

INSERT INTO employees (id, tenant_id, first_name, last_name, email, phone,
                       role, department, position, hire_date, status)
VALUES
  ('e1000000-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000001',
   'Fatou', 'Diop', 'fatou.diop@al-nour.sn', '+221 77 100 0001',
   'ADMIN', 'Direction', 'Directrice RH', '2022-01-15', 'ACTIVE'),

  ('e1000000-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000001',
   'Aminata', 'Fall', 'a.fall@al-nour.sn', '+221 77 100 0002',
   'TEACHER', 'Mathématiques', 'Professeure Maths', '2023-09-01', 'ACTIVE'),

  ('e1000000-0000-0000-0000-000000000003',
   'a0000000-0000-0000-0000-000000000001',
   'Ibrahima', 'Ndiaye', 'i.ndiaye@al-nour.sn', '+221 77 100 0003',
   'TEACHER', 'Sciences', 'Professeur SVT', '2021-09-01', 'ACTIVE'),

  ('e1000000-0000-0000-0000-000000000004',
   'a0000000-0000-0000-0000-000000000001',
   'Mariama', 'Ba', 'm.ba@al-nour.sn', '+221 77 100 0004',
   'STAFF', 'Administration', 'Secrétaire', '2020-03-01', 'ACTIVE'),

  ('e1000000-0000-0000-0000-000000000005',
   'a0000000-0000-0000-0000-000000000001',
   'Moussa', 'Sarr', 'm.sarr@al-nour.sn', '+221 77 100 0005',
   'MANAGER', 'Direction', 'Directeur Adjoint', '2019-09-01', 'ACTIVE');

-- ── EMPLOYEES — Tenant 2 (École Avenir, CI, Bundle) ──────────────

INSERT INTO employees (id, tenant_id, first_name, last_name, email, phone,
                       role, department, position, hire_date, status,
                       edukira_teacher_id, edukira_tenant_id, last_edukira_sync)
VALUES
  ('e2000000-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000002',
   'Adjoua', 'Kouassi', 'a.kouassi@avenir-abi.ci', '+225 07 200 0001',
   'ADMIN', 'Direction', 'Directrice', '2021-09-01', 'ACTIVE',
   NULL, NULL, NULL),

  ('e2000000-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000002',
   'Kofi', 'Asante', 'k.asante@avenir-abi.ci', '+225 07 200 0002',
   'TEACHER', 'Physique', 'Professeur Physique', '2022-09-01', 'ACTIVE',
   'edk_teacher_kofi001', 'edk_tenant_avenir', NOW() - INTERVAL '1 day'),

  ('e2000000-0000-0000-0000-000000000003',
   'a0000000-0000-0000-0000-000000000002',
   'Awa', 'Traore', 'a.traore@avenir-abi.ci', '+225 07 200 0003',
   'TEACHER', 'Français', 'Professeure Français', '2020-09-01', 'ACTIVE',
   'edk_teacher_awa002', 'edk_tenant_avenir', NOW() - INTERVAL '1 day'),

  ('e2000000-0000-0000-0000-000000000004',
   'a0000000-0000-0000-0000-000000000002',
   'Yao', 'Kouame', 'y.kouame@avenir-abi.ci', '+225 07 200 0004',
   'TEACHER', 'Histoire', 'Professeur Histoire', '2023-09-01', 'ACTIVE',
   'edk_teacher_yao003', 'edk_tenant_avenir', NOW() - INTERVAL '2 days'),

  ('e2000000-0000-0000-0000-000000000005',
   'a0000000-0000-0000-0000-000000000002',
   'Mariam', 'Coulibaly', 'm.coulibaly@avenir-abi.ci', '+225 07 200 0005',
   'STAFF', 'Administration', 'Comptable', '2021-01-15', 'ACTIVE',
   NULL, NULL, NULL);

-- ── EMPLOYEES — Tenant 3 (Institut Yaoundé, CM) ───────────────────

INSERT INTO employees (id, tenant_id, first_name, last_name, email, phone,
                       role, department, position, hire_date, status)
VALUES
  ('e3000000-0000-0000-0000-000000000001',
   'a0000000-0000-0000-0000-000000000003',
   'Paul', 'Nkomo', 'p.nkomo@it-yaounde.cm', '+237 6 90 000 001',
   'ADMIN', 'Direction', 'Directeur', '2020-01-01', 'ACTIVE'),

  ('e3000000-0000-0000-0000-000000000002',
   'a0000000-0000-0000-0000-000000000003',
   'Celestine', 'Mvondo', 'c.mvondo@it-yaounde.cm', '+237 6 90 000 002',
   'TEACHER', 'Informatique', 'Professeure Informatique', '2022-09-01', 'ACTIVE');

-- ── TIMESHEETS — 5 derniers jours ouvrables ──────────────────────

INSERT INTO timesheets (tenant_id, employee_id, date, check_in, check_out,
                        source, offline_queued, synced_at)
SELECT
  'a0000000-0000-0000-0000-000000000001',
  e.id,
  d.day::DATE,
  '07:45:00'::TIME,
  '16:30:00'::TIME,
  'MOBILE',
  FALSE,
  NOW()
FROM employees e
CROSS JOIN (
  SELECT (CURRENT_DATE - (n || ' days')::INTERVAL)::DATE AS day
  FROM generate_series(1, 7) AS n
) d
WHERE e.tenant_id = 'a0000000-0000-0000-0000-000000000001'
  AND EXTRACT(DOW FROM d.day) NOT IN (0, 6)
ON CONFLICT (tenant_id, employee_id, date) DO NOTHING;

INSERT INTO timesheets (tenant_id, employee_id, date, check_in, check_out,
                        source, offline_queued, synced_at)
SELECT
  'a0000000-0000-0000-0000-000000000002',
  e.id,
  d.day::DATE,
  '08:00:00'::TIME,
  '16:00:00'::TIME,
  CASE WHEN e.edukira_teacher_id IS NOT NULL THEN 'EDUKIRA_SYNC' ELSE 'MOBILE' END,
  FALSE,
  NOW()
FROM employees e
CROSS JOIN (
  SELECT (CURRENT_DATE - (n || ' days')::INTERVAL)::DATE AS day
  FROM generate_series(1, 5) AS n
) d
WHERE e.tenant_id = 'a0000000-0000-0000-0000-000000000002'
  AND EXTRACT(DOW FROM d.day) NOT IN (0, 6)
ON CONFLICT (tenant_id, employee_id, date) DO NOTHING;

-- ── LEAVE REQUESTS ────────────────────────────────────────────────

INSERT INTO leave_requests (id, tenant_id, employee_id, leave_type,
                             start_date, end_date, days_count,
                             reason, status, reviewed_by, reviewed_at)
VALUES
  (gen_random_uuid(),
   'a0000000-0000-0000-0000-000000000001',
   'e1000000-0000-0000-0000-000000000003',
   'ANNUAL', CURRENT_DATE + 10, CURRENT_DATE + 20,
   8, 'Conge familial', 'APPROVED',
   'e1000000-0000-0000-0000-000000000005', NOW() - INTERVAL '2 days'),

  (gen_random_uuid(),
   'a0000000-0000-0000-0000-000000000001',
   'e1000000-0000-0000-0000-000000000002',
   'ANNUAL', CURRENT_DATE + 30, CURRENT_DATE + 37,
   5, 'Vacances scolaires', 'PENDING', NULL, NULL),

  (gen_random_uuid(),
   'a0000000-0000-0000-0000-000000000001',
   'e1000000-0000-0000-0000-000000000004',
   'SICK', CURRENT_DATE - 3, CURRENT_DATE - 1,
   2, 'Certificat medical fourni', 'APPROVED',
   'e1000000-0000-0000-0000-000000000001', NOW() - INTERVAL '3 days'),

  (gen_random_uuid(),
   'a0000000-0000-0000-0000-000000000002',
   'e2000000-0000-0000-0000-000000000002',
   'ANNUAL', CURRENT_DATE + 14, CURRENT_DATE + 21,
   5, 'Conge annuel', 'APPROVED',
   'e2000000-0000-0000-0000-000000000001', NOW() - INTERVAL '1 day'),

  (gen_random_uuid(),
   'a0000000-0000-0000-0000-000000000002',
   'e2000000-0000-0000-0000-000000000003',
   'ANNUAL', CURRENT_DATE + 45, CURRENT_DATE + 55,
   7, NULL, 'PENDING', NULL, NULL);

-- ── PAYROLLS — Avril 2026 ─────────────────────────────────────────

INSERT INTO payrolls (id, tenant_id, employee_id, period_year, period_month,
                      gross_salary, net_salary, total_deductions,
                      deductions_detail, country_config_version,
                      status, generated_at)
VALUES
  (gen_random_uuid(),
   'a0000000-0000-0000-0000-000000000001',
   'e1000000-0000-0000-0000-000000000001',
   2026, 4, 350000.00, 298450.00, 51550.00,
   '[{"label":"CNSS Employe","amount":19530.0,"type":"social"},{"label":"Impot sur revenu","amount":32020.0,"type":"tax"}]',
   'SN-v1.0', 'GENERATED', NOW() - INTERVAL '5 days'),

  (gen_random_uuid(),
   'a0000000-0000-0000-0000-000000000001',
   'e1000000-0000-0000-0000-000000000002',
   2026, 4, 280000.00, 242630.00, 37370.00,
   '[{"label":"CNSS Employe","amount":15624.0,"type":"social"},{"label":"Impot sur revenu","amount":21746.0,"type":"tax"}]',
   'SN-v1.0', 'GENERATED', NOW() - INTERVAL '5 days'),

  (gen_random_uuid(),
   'a0000000-0000-0000-0000-000000000001',
   'e1000000-0000-0000-0000-000000000003',
   2026, 4, 310000.00, 267820.00, 42180.00,
   '[{"label":"CNSS Employe","amount":17298.0,"type":"social"},{"label":"Impot sur revenu","amount":24882.0,"type":"tax"}]',
   'SN-v1.0', 'GENERATED', NOW() - INTERVAL '5 days'),

  (gen_random_uuid(),
   'a0000000-0000-0000-0000-000000000002',
   'e2000000-0000-0000-0000-000000000002',
   2026, 4, 290000.00, 252940.00, 37060.00,
   '[{"label":"CNPS Employe","amount":9744.0,"type":"social"},{"label":"Impot sur revenu","amount":27316.0,"type":"tax"}]',
   'CI-v1.0', 'GENERATED', NOW() - INTERVAL '4 days'),

  (gen_random_uuid(),
   'a0000000-0000-0000-0000-000000000002',
   'e2000000-0000-0000-0000-000000000003',
   2026, 4, 320000.00, 278560.00, 41440.00,
   '[{"label":"CNPS Employe","amount":10752.0,"type":"social"},{"label":"Impot sur revenu","amount":30688.0,"type":"tax"}]',
   'CI-v1.0', 'GENERATED', NOW() - INTERVAL '4 days'),

  (gen_random_uuid(),
   'a0000000-0000-0000-0000-000000000003',
   'e3000000-0000-0000-0000-000000000001',
   2026, 4, 250000.00, 218050.00, 31950.00,
   '[{"label":"CNPS Employe","amount":10500.0,"type":"social"},{"label":"Impot sur revenu","amount":21450.0,"type":"tax"}]',
   'CM-v1.0', 'GENERATED', NOW() - INTERVAL '3 days');