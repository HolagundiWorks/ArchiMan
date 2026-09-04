# Architecture Consultancy App
## Company Profile & Tax Structure Module

---

# 1. Purpose

Add a **Company Profile** module that stores the consultancy's business, professional and tax configuration.

Tax configuration must be established at the **company/practice level**, not individually for every project.

The company profile determines the default tax treatment for:

- Consultancy fees
- Project agreements
- Invoices
- Payment records
- Financial reports

---

# 2. Company Profile

Create:

```text
COMPANY_PROFILE
```

The company profile should be created during initial application setup.

It should also be editable later through:

```text
Settings
   ↓
Company Profile
```

---

# 3. Company Information

Fields:

| Field | Type | Required |
|---|---|---:|
| `company_id` | UUID | Yes |
| `company_name` | Text | Yes |
| `trade_name` | Text | No |
| `company_type` | Select | Yes |
| `address` | Text | Yes |
| `city` | Text | Yes |
| `state` | Text | Yes |
| `country` | Text | Yes |
| `pincode` | Text | No |
| `email` | Email | Yes |
| `phone` | Text | No |
| `website` | Text | No |
| `logo` | File | No |

---

# 4. Professional Information

Add professional/practice information:

```text
Professional Registration No.
Architect / Principal Name
Professional Qualification
Practice Registration Details
PAN
```

Additional fields can be added later depending on the practice structure.

---

# 5. Tax Configuration

Add a dedicated section:

```text
TAX CONFIGURATION
```

The company must select its tax treatment.

### Tax Options

```text
○ No GST
○ Composition
○ Regular GST
```

Do not allow the user to select multiple regimes simultaneously as the active company tax regime.

---

# 6. No GST

If:

```text
Tax Regime = NO_GST
```

then:

```text
GST Rate = 0%
```

The application must not automatically add GST to consultancy fees or invoices.

Example:

```text
Professional Fee       ₹5,00,000
GST                    ₹0
Invoice Total          ₹5,00,000
```

UI:

```text
Tax Status:
No GST
```

---

# 7. Composition

If:

```text
Tax Regime = COMPOSITION
```

the default tax rate should be:

```text
6%
```

Example:

```text
Professional Fee       ₹5,00,000
Tax @ 6%               ₹30,000
Invoice Total          ₹5,30,000
```

Display:

```text
Tax Regime:
Composition

Tax Rate:
6%
```

The rate should remain configurable at the company level rather than being permanently hard-coded.

---

# 8. Regular GST

If:

```text
Tax Regime = REGULAR_GST
```

the default GST rate should be:

```text
8%
```

Example:

```text
Professional Fee       ₹5,00,000
GST @ 8%               ₹40,000
Invoice Total          ₹5,40,000
```

Display:

```text
Tax Regime:
Regular GST

GST Rate:
8%
```

The rate must remain configurable because applicable tax rates may change.

---

# 9. Tax Configuration Entity

Create:

```text
COMPANY_TAX_PROFILE
```

Fields:

| Field | Type |
|---|---|
| `id` | UUID |
| `company_id` | UUID |
| `tax_regime` | Enum |
| `tax_rate` | Decimal |
| `tax_registration_number` | Text |
| `tax_effective_from` | Date |
| `tax_effective_to` | Date / Nullable |
| `is_active` | Boolean |
| `created_at` | Timestamp |
| `updated_at` | Timestamp |

---

# 10. Tax Regime Enum

Use internal values:

```text
NO_GST
COMPOSITION
REGULAR_GST
```

UI labels:

```text
No GST
Composition
Regular GST
```

---

# 11. Company Setup Workflow

During initial company creation:

```text
CREATE COMPANY
      ↓
Company Information
      ↓
Professional Information
      ↓
Tax Configuration
      ↓
Invoice Configuration
      ↓
Create Company
```

---

# 12. Tax Setup UI

Example:

```text
┌──────────────────────────────────────────────┐
│ TAX CONFIGURATION                            │
├──────────────────────────────────────────────┤
│                                              │
│ Tax Regime                                   │
│                                              │
│ ○ No GST                                     │
│                                              │
│ ○ Composition                                │
│   Default Rate: 6%                           │
│                                              │
│ ○ Regular GST                                │
│   Default Rate: 8%                           │
│                                              │
│ GST / Tax Registration No.                   │
│ [____________________________]               │
│                                              │
└──────────────────────────────────────────────┘
```

If **No GST** is selected:

```text
GST / Tax Registration No.
```

may be hidden or marked not applicable.

---

# 13. Tax Calculation Engine

Tax calculation should be centralized.

Do not implement separate tax calculations in:

```text
Projects
Invoices
Agreements
Payments
Reports
```

Create one tax calculation service.

Conceptually:

```text
calculateTax(
    taxableAmount,
    companyTaxProfile
)
```

---

# 14. Calculation

```text
Tax Amount =
Taxable Amount × Tax Rate / 100
```

Example:

```text
Fee = ₹5,00,000
Rate = 8%

Tax =
5,00,000 × 8 / 100
= ₹40,000
```

Total:

```text
₹5,00,000 + ₹40,000
= ₹5,40,000
```

---

# 15. Project Fee Integration

Project fee should show:

```text
Professional Fee
Tax Regime
Tax Rate
Tax Amount
Total Payable
```

Example:

```text
Professional Fee       ₹8,00,000
Tax Regime             Regular GST
GST @ 8%               ₹64,000
────────────────────────────────
Total                  ₹8,64,000
```

---

# 16. Agreement Integration

When an agreement is generated from a consultancy template, the company tax profile should be applied.

Example:

```text
Professional Consultancy Fee:
₹8,00,000

Applicable Tax:
Regular GST @ 8%

Tax:
₹64,000

Total:
₹8,64,000
```

The agreement should clearly distinguish:

```text
Professional Fee
Tax
Total Contract Value
```

---

# 17. Agreement Snapshot

When an agreement is finalized, store the tax information used at that time.

Example:

```text
Agreement Tax Snapshot

Tax Regime:
REGULAR_GST

Tax Rate:
8%

Tax Amount:
₹64,000

Tax Profile Version:
3
```

This is important because the company tax configuration may change later.

A historical agreement must **not change automatically** when the company changes its tax profile.

---

# 18. Invoice Integration

Every invoice should inherit the applicable tax profile.

Example:

```text
INVOICE

Professional Consultancy Fee     ₹2,00,000
GST @ 8%                          ₹16,000
──────────────────────────────────────────
Total Invoice                    ₹2,16,000
```

For No GST:

```text
Professional Consultancy Fee     ₹2,00,000
GST                               ₹0
──────────────────────────────────────────
Total Invoice                    ₹2,00,000
```

---

# 19. Tax History

Do not overwrite historical tax configuration.

Example:

```text
COMPANY TAX HISTORY

2026
Regular GST
8%
Active

2025
Composition
6%
Inactive

2024
No GST
0%
Inactive
```

This allows historical invoices and agreements to remain accurate.

---

# 20. Project-Level Override

By default:

```text
Company Tax Profile
        ↓
Project
        ↓
Agreement
        ↓
Invoice
```

The project should **not normally override** the company tax regime.

If a future requirement requires project-specific tax treatment, implement it explicitly as:

```text
Tax Treatment:
○ Use Company Default
○ Project Override
```

and restrict this feature to authorized users.

For MVP, use the company default.

---

# 21. Consultancy Template Integration

Tax should **not be permanently embedded into consultancy templates**.

For example, the template should define:

```text
Full Architectural Consultancy
Fee Basis: Percentage
Default Fee: X%
```

The company profile determines:

```text
Tax Regime: Regular GST
Tax Rate: 8%
```

Therefore:

```text
CONSULTANCY TEMPLATE
        ↓
PROFESSIONAL FEE
        ↓
COMPANY TAX PROFILE
        ↓
TAX
        ↓
TOTAL
```

This prevents the same consultancy template from containing outdated tax information.

---

# 22. Company Profile Structure

Final company setup should contain:

```text
COMPANY PROFILE
│
├── General
│   ├── Company Name
│   ├── Trade Name
│   ├── Address
│   ├── Contact
│   └── Logo
│
├── Professional
│   ├── Principal / Architect
│   ├── Registration
│   └── Professional Details
│
├── Tax
│   ├── Tax Regime
│   ├── Tax Rate
│   └── Registration Number
│
├── Invoice
│   ├── Invoice Prefix
│   ├── Invoice Numbering
│   └── Payment Details
│
└── Agreement
    ├── Agreement Numbering
    ├── Default Terms
    └── Signature Details
```

---

# 23. Important Developer Rules

### Rule 1 — Tax Is Company-Level Configuration

Do not ask for tax regime every time a project is created.

The company profile determines the default.

---

### Rule 2 — Never Hard-Code Rates in Business Logic

Although the initial configuration is:

```text
No GST       = 0%
Composition  = 6%
Regular GST  = 8%
```

store the rate in the database.

Example:

```text
tax_regime = REGULAR_GST
tax_rate = 8.00
```

This allows the company to update the rate without modifying application code.

---

### Rule 3 — Preserve Historical Tax Data

Invoices and agreements must store the actual tax regime and rate used when they were issued/finalized.

Do not recalculate old invoices from the current company profile.

---

### Rule 4 — Tax Must Be Centralized

Use a common tax service:

```text
TaxService
```

All financial modules should use it.

---

# 24. Complete Commercial Flow

The complete system should now work as:

```text
COMPANY PROFILE
       │
       ├── Consultancy Templates
       │
       ├── Tax Configuration
       │
       └── Agreement Templates
                │
                ↓
             PROJECT
                │
                ├── Consultancy Type
                ├── Scope of Work
                ├── Fee Structure
                │
                ↓
          PROFESSIONAL FEE
                │
                ↓
          COMPANY TAX PROFILE
                │
                ↓
              TAX
                │
                ↓
          TOTAL CONTRACT VALUE
                │
                ↓
             AGREEMENT
                │
                ↓
             INVOICE
                │
                ↓
             PAYMENT
```

---

# 25. Example End-to-End

Company profile:

```text
Company:
ABC Architects

Tax Regime:
Regular GST

Tax Rate:
8%
```

New project:

```text
Project:
Residential Villa

Consultancy:
Full Architectural Consultancy

Area:
10,000 Sq.ft

Fee Basis:
Sq.ft

Rate:
₹50 / Sq.ft
```

Fee:

```text
10,000 × ₹50
= ₹5,00,000
```

Tax:

```text
₹5,00,000 × 8%
= ₹40,000
```

Agreement:

```text
Professional Fee       ₹5,00,000
GST @ 8%               ₹40,000
────────────────────────────────
Total                  ₹5,40,000
```

The agreement stores a snapshot:

```text
Tax Regime: Regular GST
Tax Rate: 8%
Tax Amount: ₹40,000
```

If the company's tax configuration changes later, this historical agreement remains unchanged.

---

# 26. Final Module Relationship

```text
COMPANY
│
├── COMPANY PROFILE
│
├── TAX PROFILE
│      ├── No GST
│      ├── Composition @ 6%
│      └── Regular GST @ 8%
│
├── CONSULTANCY TEMPLATES
│      ├── Scope
│      ├── Deliverables
│      ├── Fees
│      ├── Payment Milestones
│      └── Agreement Template
│
└── PROJECTS
       │
       ├── Consultancy
       ├── Scope
       ├── Fee
       ├── Tax Snapshot
       ├── Agreement
       ├── Invoices
       └── Payments
```

> **Tax rates and GST treatment should be treated as configurable business settings and verified against the firm's applicable tax/legal requirements before production use.**