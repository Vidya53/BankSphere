// Marketing content for every public InfoPage. The InfoPage component renders
// these entries; the mega-menu in PublicLayout reads the same map to build its
// dropdowns. Adding a new entry here makes it routable at `/products/:slug`
// or `/segments/:slug` automatically — no extra code needed.

import {
  PiggyBank, Wallet, ShieldCheck, Sparkles, TrendingUp, Banknote,
  CreditCard, Landmark, GraduationCap, Smartphone, Briefcase, Globe2,
  Users, Building2, Plane,
} from 'lucide-react';

// ── Products ──────────────────────────────────────────────────────────────────
export const PRODUCTS = {
  accounts: {
    icon: Wallet,
    eyebrow: 'Savings & current',
    title: 'Accounts',
    heading: 'A bank account that fits the way you live',
    subhead:
      'Open a fully digital savings or current account in minutes — paperless, secure, and ready when you are.',
    cta: { label: 'Open an account', to: '/signup' },
    highlights: [
      { icon: Sparkles, title: 'Zero balance digital savings' },
      { icon: ShieldCheck, title: 'ISO 27001-certified security' },
      { icon: Smartphone, title: 'Fully mobile-first banking' },
    ],
    sections: [
      {
        icon: PiggyBank,
        title: 'Premium Savings',
        body:
          'A relationship-banking account for customers who want priority service, higher transaction limits, and concierge support.',
        bullets: [
          'Unlimited free ATM withdrawals',
          'Priority customer-service queue',
          'Free demand drafts and cheque books',
          'Complimentary airport lounge access',
        ],
      },
      {
        icon: Wallet,
        title: 'Regular Savings',
        body:
          'The everyday account for everyday banking — no minimum balance for digital salaried customers.',
        bullets: [
          'Virtual debit card on day one',
          'UPI, IMPS, NEFT, RTGS — all included',
          'In-app spend analytics',
          'Bill payments and standing instructions',
        ],
      },
      {
        icon: Sparkles,
        title: "Women's Account",
        body:
          'Tailored rewards, savings goals, and dedicated insurance benefits designed around the financial needs of women.',
        bullets: [
          'Higher interest on savings balance',
          'Locker rental discount',
          'Specialised insurance bundle',
          'Goal-based savings buckets',
        ],
      },
      {
        icon: ShieldCheck,
        title: 'Senior Citizens',
        body:
          'Higher interest, doorstep service, and a simpler interface designed for our most-valued customers.',
        bullets: [
          '0.5% additional interest on balance',
          'Doorstep banking — cash, cheques and KYC',
          'Free health check-ups annually',
          'Dedicated relationship manager',
        ],
      },
    ],
  },

  deposits: {
    icon: Landmark,
    eyebrow: 'Grow your money',
    title: 'Deposits',
    heading: 'Fixed and recurring deposits with guaranteed returns',
    subhead:
      'Lock in attractive interest rates with the safety of a regulated bank — flexible tenures, premature withdrawal, and tax-saver options.',
    cta: { label: 'Open a deposit', to: '/signup' },
    highlights: [
      { icon: TrendingUp, title: 'Up to 7.25% interest' },
      { icon: ShieldCheck, title: 'DICGC-insured up to ₹5L' },
      { icon: Sparkles, title: 'Auto-renew on maturity' },
    ],
    sections: [
      {
        icon: Banknote,
        title: 'Fixed Deposit',
        body:
          'Lock in a one-time lump sum for a fixed period and earn predictable, guaranteed returns.',
        bullets: [
          'Tenures from 7 days to 10 years',
          'Monthly, quarterly or maturity payout',
          'Premature withdrawal with simple penalty',
          'Loan against deposit up to 90% of value',
        ],
      },
      {
        icon: TrendingUp,
        title: 'Recurring Deposit',
        body:
          'Save a fixed amount every month and build a disciplined savings habit with FD-level returns.',
        bullets: [
          'Start with as little as ₹500/month',
          'Tenures from 6 months to 10 years',
          'Auto-debit from your savings account',
          'Bonus interest on completing tenure',
        ],
      },
      {
        icon: ShieldCheck,
        title: 'Tax-Saver FD',
        body:
          'A 5-year fixed deposit that qualifies for deduction under Section 80C — saves tax while earning interest.',
        bullets: [
          'Section 80C eligible up to ₹1.5L',
          'Lock-in of 5 years',
          'Quarterly compounding of interest',
          'Senior citizens get extra 0.5%',
        ],
      },
    ],
  },

  cards: {
    icon: CreditCard,
    eyebrow: 'Spend smarter',
    title: 'Cards',
    heading: 'Debit and credit cards that reward every swipe',
    subhead:
      'From cashback to travel miles to airport lounges — pick a card that matches your spending and lifestyle.',
    cta: { label: 'Apply for a card', to: '/signup' },
    highlights: [
      { icon: Sparkles, title: 'Up to 5% cashback' },
      { icon: Plane, title: 'Domestic & international lounges' },
      { icon: ShieldCheck, title: 'Fraud-protected with 2FA' },
    ],
    sections: [
      {
        icon: CreditCard,
        title: 'Cashback Credit Card',
        body:
          'Earn flat cashback on every spend — no categories, no caps, no confusing reward charts.',
        bullets: [
          '5% on groceries and bill payments',
          '2% on every other spend',
          'No annual fee on ₹3L+ annual spend',
          'Instant credit on statement',
        ],
      },
      {
        icon: Plane,
        title: 'Travel Credit Card',
        body:
          'Built for frequent flyers — earn miles on every spend, redeem on partner airlines worldwide.',
        bullets: [
          '4 reward miles per ₹100 spent',
          '8 complimentary domestic lounge visits',
          'Free golf rounds at premier courses',
          'Travel insurance bundled in',
        ],
      },
      {
        icon: Wallet,
        title: 'Debit Card',
        body:
          'Linked to your savings account with contactless tap-to-pay and global acceptance.',
        bullets: [
          'Contactless and chip-enabled',
          'Visa / RuPay acceptance worldwide',
          'Customisable spend limits in-app',
          'Instant lock/unlock from the app',
        ],
      },
    ],
  },

  loans: {
    icon: Landmark,
    eyebrow: 'Borrow responsibly',
    title: 'Loans',
    heading: 'Loans for every milestone in your life',
    subhead:
      'Pre-approved offers for existing customers, transparent EMIs, and zero hidden fees. Apply online and get decisioned in minutes.',
    cta: { label: 'Apply for a loan', to: '/signup' },
    highlights: [
      { icon: Sparkles, title: 'Decision in minutes' },
      { icon: ShieldCheck, title: 'No hidden charges' },
      { icon: TrendingUp, title: 'Competitive rates' },
    ],
    sections: [
      {
        icon: Wallet,
        title: 'Personal Loan',
        body:
          'Unsecured, flexible loans up to ₹40 lakh for any planned expense — weddings, travel, medical, or home improvements.',
        bullets: [
          'Loan amounts from ₹50,000 to ₹40,00,000',
          'Tenures from 12 to 60 months',
          'Rates starting 10.5% p.a.',
          'Disbursed to account within 24 hours',
        ],
      },
      {
        icon: Landmark,
        title: 'Home Loan',
        body:
          'Make your dream home a reality with India-leading home loan products — long tenures, low rates, and step-up options.',
        bullets: [
          'Loan amounts up to ₹15 crore',
          'Tenures up to 30 years',
          'Rates starting 8.5% p.a.',
          'Free legal and valuation services',
        ],
      },
      {
        icon: GraduationCap,
        title: 'Education Loan',
        body:
          'Fund higher education in India or abroad with moratorium during your course and a simple online application.',
        bullets: [
          'Loans up to ₹1.5 crore for overseas study',
          'Tax benefits under Section 80E',
          'Moratorium until course completion',
          'Doorstep document collection',
        ],
      },
      {
        icon: CreditCard,
        title: 'Vehicle Loan',
        body:
          'Drive home your new car or bike with quick approvals, on-road price financing, and flexible repayment.',
        bullets: [
          'Up to 100% on-road price financed',
          'Tenures up to 7 years',
          'Rates starting 9.25% p.a.',
          'Pre-approved offers for existing customers',
        ],
      },
    ],
  },

  investments: {
    icon: TrendingUp,
    eyebrow: 'Build wealth',
    title: 'Investments',
    heading: 'Smart investing, simplified',
    subhead:
      'Mutual funds, SIPs, government bonds, and gold — discover, track and rebalance your portfolio without leaving the app.',
    cta: { label: 'Start investing', to: '/signup' },
    highlights: [
      { icon: Sparkles, title: '3,000+ mutual fund schemes' },
      { icon: ShieldCheck, title: 'Goal-based portfolios' },
      { icon: TrendingUp, title: 'Live portfolio insights' },
    ],
    sections: [
      {
        icon: TrendingUp,
        title: 'Mutual Funds & SIPs',
        body:
          'Pick from 3,000+ schemes, set up systematic investment plans, and watch your wealth compound over time.',
        bullets: [
          'Start a SIP from ₹500/month',
          'No commission — direct schemes',
          'Pause, modify or cancel anytime',
          'Tax-saver ELSS funds with 3-year lock-in',
        ],
      },
      {
        icon: ShieldCheck,
        title: 'Government Bonds',
        body:
          'Sovereign gold bonds, RBI floating-rate bonds, and tax-free PSU bonds — the safest investments backed by the Government of India.',
        bullets: [
          'Sovereign-backed safety',
          'Tax-free interest on select bonds',
          'Long tenures with assured returns',
          'No mark-up, no hidden fees',
        ],
      },
      {
        icon: Sparkles,
        title: 'Digital Gold',
        body:
          'Buy and sell 24-karat gold instantly — securely stored in insured vaults, redeemable as physical gold any time.',
        bullets: [
          'Buy from as little as ₹10',
          '24-karat 99.99% pure',
          'Insured storage included',
          'Convert to coins or jewellery',
        ],
      },
    ],
  },

  payments: {
    icon: Banknote,
    eyebrow: 'Move money',
    title: 'Payments',
    heading: 'Every payment, in one place',
    subhead:
      'UPI, IMPS, NEFT, RTGS, bill payments, and instant transfers — all from a single intuitive interface.',
    cta: { label: 'Send your first payment', to: '/signup' },
    highlights: [
      { icon: Smartphone, title: 'UPI handle in your name' },
      { icon: ShieldCheck, title: '2FA on every transfer' },
      { icon: Sparkles, title: 'Save beneficiaries instantly' },
    ],
    sections: [
      {
        icon: Smartphone,
        title: 'UPI Payments',
        body:
          'Send and receive money instantly with your @banksphere UPI handle — works at every merchant, every bank, every time.',
        bullets: [
          '@banksphere UPI handle on signup',
          'Pay with QR or VPA or mobile number',
          'UPI Lite for small payments',
          'Scheduled payments and reminders',
        ],
      },
      {
        icon: Banknote,
        title: 'Bank Transfers',
        body:
          'Move money to any bank account in India using IMPS (instant), NEFT, or RTGS for large-value transfers.',
        bullets: [
          'IMPS up to ₹5L — instant, 24×7',
          'NEFT batches every 30 minutes',
          'RTGS for high-value transfers',
          'Two-factor approval on staff-routed transfers',
        ],
      },
      {
        icon: Wallet,
        title: 'Bill Payments & Recharges',
        body:
          'Pay electricity, gas, broadband, DTH, mobile, and credit card bills — one place, one tap, automatic reminders.',
        bullets: [
          '600+ billers integrated',
          'Auto-pay with spend caps',
          'Reminder notifications before due dates',
          'Detailed bill history in one tab',
        ],
      },
    ],
  },

  'learning-hub': {
    icon: GraduationCap,
    eyebrow: 'Free banking education',
    title: 'Learning Hub',
    heading: 'Banking that teaches as you bank',
    subhead:
      'Bite-sized articles, video explainers and interactive guides covering everything from your first salary account to retirement planning.',
    cta: { label: 'Explore the hub', to: '/' },
    highlights: [
      { icon: Sparkles, title: '200+ guides and videos' },
      { icon: TrendingUp, title: 'Tracks for every life stage' },
      { icon: ShieldCheck, title: 'Curated by banking experts' },
    ],
    sections: [
      {
        icon: GraduationCap,
        title: 'Money basics',
        body:
          'Start with the fundamentals: budgeting, saving, understanding interest, and your first credit score.',
        bullets: [
          'Build a 50-30-20 monthly budget',
          'Set up your first emergency fund',
          'Understand credit scores in 10 minutes',
          'Decode your bank statement',
        ],
      },
      {
        icon: TrendingUp,
        title: 'Investing 101',
        body:
          'Demystify mutual funds, SIPs, equity, debt, and gold — with worked examples in Indian rupees.',
        bullets: [
          'Pick your first mutual fund',
          'SIP vs lumpsum — which suits you',
          'Asset allocation by life stage',
          'How taxes work on capital gains',
        ],
      },
      {
        icon: Landmark,
        title: 'Borrowing wisely',
        body:
          'Loans are powerful but easy to mis-use. Learn the difference between good debt and bad debt.',
        bullets: [
          'Read an EMI schedule like a pro',
          'When to prepay vs invest the difference',
          'Comparing flat-rate vs reducing-balance',
          'Credit card hygiene that protects your score',
        ],
      },
    ],
  },
};

// ── Customer segments ────────────────────────────────────────────────────────
export const SEGMENTS = {
  personal: {
    icon: Users,
    eyebrow: 'For individuals',
    title: 'Personal Banking',
    heading: 'Everyday banking, beautifully done',
    subhead:
      'Salary accounts, savings, cards, loans and investments — everything an individual needs, in one paperless app.',
    cta: { label: 'Open your account', to: '/signup' },
    highlights: [
      { icon: Sparkles, title: 'Paperless onboarding' },
      { icon: Smartphone, title: 'Mobile-first design' },
      { icon: ShieldCheck, title: '2FA and biometric login' },
    ],
    sections: [
      {
        icon: Wallet,
        title: 'A single dashboard for your money',
        body:
          'See balances, recent transactions, upcoming bills and your investments in one glance — built to remove banking friction.',
        bullets: [
          'Aggregated balance across accounts',
          'Smart categorisation of spends',
          'Goal-based savings buckets',
          'Customisable home screen',
        ],
      },
      {
        icon: TrendingUp,
        title: 'Wealth, simplified',
        body:
          'Personal banking with built-in wealth — invest in mutual funds, gold or bonds without leaving the app.',
        bullets: [
          'Start a SIP from ₹500/month',
          'Buy digital gold from ₹10',
          'Sovereign bonds at par',
          'Portfolio tracking on home screen',
        ],
      },
    ],
  },

  business: {
    icon: Briefcase,
    eyebrow: 'For SMEs',
    title: 'Business Banking',
    heading: 'Banking that runs at the speed of your business',
    subhead:
      'Current accounts, business loans, GST-ready invoicing and payment collection — designed for India’s SMEs.',
    cta: { label: 'Talk to a relationship manager', to: '/branches' },
    highlights: [
      { icon: Briefcase, title: 'Multi-signatory approvals' },
      { icon: Sparkles, title: 'Bulk payment uploads' },
      { icon: TrendingUp, title: 'Working capital up to ₹5 Cr' },
    ],
    sections: [
      {
        icon: Wallet,
        title: 'Current Account',
        body:
          'High-volume current accounts with no transaction caps and multi-user roles built for finance teams.',
        bullets: [
          'Unlimited transactions',
          'Multiple authorised signatories',
          'Bulk transfer file upload',
          'Free statement APIs for accounting tools',
        ],
      },
      {
        icon: Landmark,
        title: 'Working Capital & MSME Loans',
        body:
          'Overdrafts, cash-credit lines and term loans for working capital — sized to your invoicing and GST returns.',
        bullets: [
          'OD limits from ₹5L to ₹5 Cr',
          'GST-return based underwriting',
          'Collateral-free up to ₹2 Cr',
          'Dedicated SME relationship manager',
        ],
      },
      {
        icon: CreditCard,
        title: 'Payment Collection',
        body:
          'Collect payments through UPI, cards, net-banking, and EMI — settle directly to your current account next day.',
        bullets: [
          'Payment links and QR codes',
          'Recurring billing and subscriptions',
          'Same-day settlement for select plans',
          'Reconciliation built-in',
        ],
      },
    ],
  },

  corporate: {
    icon: Building2,
    eyebrow: 'For enterprises',
    title: 'Corporate Banking',
    heading: 'Treasury, trade and capital markets, end-to-end',
    subhead:
      'A full-spectrum corporate banking suite — cash management, trade finance, FX, supply-chain finance and capital markets advisory.',
    cta: { label: 'Connect with us', to: '/branches' },
    highlights: [
      { icon: Globe2, title: 'Cross-border presence' },
      { icon: Briefcase, title: 'Dedicated coverage team' },
      { icon: ShieldCheck, title: 'Bespoke security controls' },
    ],
    sections: [
      {
        icon: Banknote,
        title: 'Cash Management',
        body:
          'Single-window visibility across collections, payments, payroll and liquidity — with custom workflows and approvals.',
        bullets: [
          'Virtual account architecture',
          'Real-time liquidity dashboards',
          'ERP and host-to-host integrations',
          'Multi-currency pooling',
        ],
      },
      {
        icon: Globe2,
        title: 'Trade Finance',
        body:
          'Letters of credit, bank guarantees, factoring, and supply-chain financing for importers and exporters.',
        bullets: [
          'Issuance of LCs and BGs',
          'Pre and post-shipment finance',
          'Anchor-led supply chain programs',
          'In-house FX desk support',
        ],
      },
      {
        icon: TrendingUp,
        title: 'Capital Markets',
        body:
          'Debt syndication, public-issue advisory, mergers & acquisitions, and structured finance for listed corporates.',
        bullets: [
          'Loan syndication',
          'Debt capital markets origination',
          'M&A advisory',
          'Structured solutions team',
        ],
      },
    ],
  },

  priority: {
    icon: Sparkles,
    eyebrow: 'For high-net-worth customers',
    title: 'Priority Banking',
    heading: 'Banking, the way it should be at scale',
    subhead:
      'Dedicated relationship managers, exclusive lounges, premium product access and curated wealth advisory.',
    cta: { label: 'Become a priority client', to: '/signup' },
    highlights: [
      { icon: Briefcase, title: 'Personal RM on speed-dial' },
      { icon: Plane, title: 'Airport lounge unlimited' },
      { icon: TrendingUp, title: 'Curated investment ideas' },
    ],
    sections: [
      {
        icon: Briefcase,
        title: 'Dedicated Relationship Manager',
        body:
          'Your RM understands you, your family and your wealth — and is reachable across phone, email and WhatsApp.',
        bullets: [
          '8 AM to 10 PM availability',
          'Doorstep service across India',
          'Quarterly portfolio reviews',
          'Concierge for travel and lifestyle',
        ],
      },
      {
        icon: TrendingUp,
        title: 'Wealth Advisory',
        body:
          'Discretionary portfolio management, alternatives and private equity — only the right ideas, never product pushing.',
        bullets: [
          'Goal-based wealth plans',
          'Private equity and AIF access',
          'Curated equity recommendations',
          'Tax-efficient structuring',
        ],
      },
      {
        icon: ShieldCheck,
        title: 'Family Office Lite',
        body:
          'Wills, succession, trusts and family-business advisory — with our partner law and tax firms.',
        bullets: [
          'Estate and succession planning',
          'Trust and family-office set-up',
          'Tax and audit coordination',
          'Inter-generational education programs',
        ],
      },
    ],
  },

  nri: {
    icon: Globe2,
    eyebrow: 'For Non-Resident Indians',
    title: 'NRI Banking',
    heading: 'Bank in India, from anywhere in the world',
    subhead:
      'NRE, NRO and FCNR accounts, low-cost remittances, and Indian investments — opened entirely online from outside India.',
    cta: { label: 'Open an NRI account', to: '/signup' },
    highlights: [
      { icon: Globe2, title: 'Operate from 100+ countries' },
      { icon: Sparkles, title: 'Tax-free interest on NRE' },
      { icon: TrendingUp, title: 'Repatriation-ready accounts' },
    ],
    sections: [
      {
        icon: Wallet,
        title: 'NRE / NRO / FCNR Accounts',
        body:
          'Repatriable savings (NRE), Indian-rupee earnings (NRO), and foreign-currency deposits (FCNR) — pick what fits.',
        bullets: [
          'NRE interest is tax-free in India',
          'NRO for rental and pension income',
          'FCNR deposits in USD, GBP, EUR, AUD, SGD',
          'Joint accounts allowed with other NRIs',
        ],
      },
      {
        icon: Banknote,
        title: 'Remittances & Money2India',
        body:
          'Send money home from across 20+ corridors with live FX rates and zero hidden fees.',
        bullets: [
          'Same-day credit to NRE / NRO',
          'Live exchange rates in-app',
          'Recurring remittance schedules',
          'PAN-linked tax statements',
        ],
      },
      {
        icon: TrendingUp,
        title: 'Indian Investments',
        body:
          'Mutual funds, sovereign gold bonds, PMS, and direct equity — all accessible to NRIs through our app.',
        bullets: [
          'PMS and AIF access for HNI NRIs',
          'Tax-saver ELSS mutual funds',
          'Sovereign gold bonds at par',
          'Form 15CA/CB support in-app',
        ],
      },
    ],
  },
};

// ── Mega-menu structure for the TopNav ───────────────────────────────────────
// Each product link in the navbar expands into 3-4 cross-promoted entries.
export const MEGA_MENUS = {
  Accounts: {
    to: '/products/accounts',
    blurb: 'Open a paperless account in minutes — choose what fits your stage of life.',
    items: [
      { title: 'Premium Savings',  desc: 'Priority service, higher limits',          to: '/products/accounts' },
      { title: 'Regular Savings',  desc: 'Everyday digital banking',                 to: '/products/accounts' },
      { title: "Women's Account",  desc: 'Tailored rewards and benefits',            to: '/products/accounts' },
      { title: 'Senior Citizens',  desc: 'Higher interest, doorstep service',        to: '/products/accounts' },
    ],
  },
  Deposits: {
    to: '/products/deposits',
    blurb: 'Lock in guaranteed returns with regulated, DICGC-insured deposits.',
    items: [
      { title: 'Fixed Deposit',    desc: '7 days to 10 years',                        to: '/products/deposits' },
      { title: 'Recurring Deposit',desc: 'Save monthly, build the habit',             to: '/products/deposits' },
      { title: 'Tax-Saver FD',     desc: 'Section 80C eligible',                      to: '/products/deposits' },
    ],
  },
  Cards: {
    to: '/products/cards',
    blurb: 'Debit and credit cards that reward every swipe — cashback, miles and lounges.',
    items: [
      { title: 'Cashback Card',    desc: 'Up to 5% on every spend',                   to: '/products/cards' },
      { title: 'Travel Card',      desc: 'Miles, lounges and travel insurance',       to: '/products/cards' },
      { title: 'Debit Card',       desc: 'Contactless, global acceptance',            to: '/products/cards' },
    ],
  },
  Loans: {
    to: '/products/loans',
    blurb: 'Pre-approved offers, transparent EMIs and instant decisioning for every milestone.',
    items: [
      { title: 'Personal Loan',    desc: 'Up to ₹40L, unsecured',                     to: '/products/loans' },
      { title: 'Home Loan',        desc: 'Tenures up to 30 years',                    to: '/products/loans' },
      { title: 'Education Loan',   desc: 'India and overseas study',                  to: '/products/loans' },
      { title: 'Vehicle Loan',     desc: 'Up to 100% on-road financing',              to: '/products/loans' },
    ],
  },
  Investments: {
    to: '/products/investments',
    blurb: 'Mutual funds, SIPs, bonds and digital gold — invest without leaving the app.',
    items: [
      { title: 'Mutual Funds & SIPs', desc: '3,000+ schemes',                         to: '/products/investments' },
      { title: 'Government Bonds',    desc: 'Sovereign-backed safety',                to: '/products/investments' },
      { title: 'Digital Gold',        desc: 'Buy from ₹10, insured',                  to: '/products/investments' },
    ],
  },
  Payments: {
    to: '/products/payments',
    blurb: 'UPI, IMPS, NEFT, RTGS and bill payments — every payment, in one place.',
    items: [
      { title: 'UPI Payments',     desc: '@banksphere handle on signup',              to: '/products/payments' },
      { title: 'Bank Transfers',   desc: 'IMPS, NEFT, RTGS, instant',                 to: '/products/payments' },
      { title: 'Bill Payments',    desc: '600+ billers, auto-pay',                    to: '/products/payments' },
    ],
  },
  'Learning Hub': {
    to: '/products/learning-hub',
    blurb: 'Bite-sized banking education for every stage of life.',
    items: [
      { title: 'Money basics',     desc: 'Budgeting, saving, interest',               to: '/products/learning-hub' },
      { title: 'Investing 101',    desc: 'Mutual funds, SIPs, gold',                  to: '/products/learning-hub' },
      { title: 'Borrowing wisely', desc: 'Loans, EMIs, credit scores',                to: '/products/learning-hub' },
    ],
  },
};

export const SEGMENT_LINKS = [
  { label: 'Personal',  to: '/segments/personal' },
  { label: 'Business',  to: '/segments/business' },
  { label: 'Corporate', to: '/segments/corporate' },
  { label: 'Priority',  to: '/segments/priority' },
  { label: 'NRI',       to: '/segments/nri' },
];

export const SECONDARY_LINKS = [
  { label: 'About Us',       to: '/about' },
  { label: 'Branch Locator', to: '/branches' },
];

export const FOOTER_COLUMNS = [
  {
    title: 'Banking',
    items: [
      { label: 'Accounts',    to: '/products/accounts' },
      { label: 'Deposits',    to: '/products/deposits' },
      { label: 'Cards',       to: '/products/cards' },
      { label: 'Loans',       to: '/products/loans' },
      { label: 'Investments', to: '/products/investments' },
    ],
  },
  {
    title: 'Company',
    items: [
      { label: 'About',          to: '/about' },
      { label: 'Careers',        to: '/about#careers' },
      { label: 'Press',          to: '/about#press' },
      { label: 'Contact',        to: '/about#contact' },
      { label: 'Branch Locator', to: '/branches' },
    ],
  },
  {
    title: 'Legal',
    items: [
      { label: 'Privacy Policy', to: '/about#privacy' },
      { label: 'Terms of Use',   to: '/about#terms' },
      { label: 'Cookie Notice',  to: '/about#cookies' },
      { label: 'Disclosure',     to: '/about#disclosure' },
      { label: 'Grievance',      to: '/about#grievance' },
    ],
  },
];
