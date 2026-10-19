import { describe, expect, it } from 'vitest';
import { createCard, updateCard } from '../src/domain/cards';
import { accountBalance } from '../src/domain/core';
import { checkIntegrity, validateState } from '../src/domain/persistence';
import { chargeDuePlanFees, planAllowsStyle, PLAN_INFO, subscribePlan } from '../src/domain/plans';
import { ctxAt, freshState, unwrap } from './helpers';

const D = '2026-01-15';
const CARD = { name: 'Test', accountId: 'acc_main', color: '#7656F6', accent: '#38BDF8', avatar: null, monthlyLimit: null, locked: false } as const;

describe('abonnements fictifs', () => {
  it('souscription : prélèvement, formule, échéance, persistance', () => {
    const s0 = freshState(D, 10_000);
    const r = unwrap(subscribePlan(s0, ctxAt(D), 'orchidee'));
    expect(accountBalance(r.state, 'acc_main')).toBe(10_000 - PLAN_INFO.orchidee.price);
    expect(r.state.profile.plan).toBe('orchidee');
    expect(r.state.profile.planRenewsOn).toBe('2026-02-15');
    expect(r.state.transactions[0]?.type).toBe('plan_fee');
    const reloaded = validateState(JSON.parse(JSON.stringify(r.state)));
    expect(reloaded.ok).toBe(true);
    expect(checkIntegrity(r.state)).toBeNull();
  });

  it('solde insuffisant ou formule identique : refus sans modification', () => {
    const s0 = freshState(D, 100);
    const r = subscribePlan(s0, ctxAt(D), 'nuit');
    expect(r.ok).toBe(false);
    expect(subscribePlan(s0, ctxAt(D), 'essentiel').ok).toBe(false);
  });

  it('designs premium réservés à la formule correspondante', () => {
    const s0 = freshState(D, 10_000);
    expect(createCard(s0, ctxAt(D), { ...CARD, style: 'holo' }).ok).toBe(false);
    expect(createCard(s0, ctxAt(D), { ...CARD, style: 'gradient' }).ok).toBe(true);
    const sub = unwrap(subscribePlan(s0, ctxAt(D), 'amethyste')).state;
    expect(createCard(sub, ctxAt(D), { ...CARD, style: 'holo' }).ok).toBe(true);
    expect(createCard(sub, ctxAt(D), { ...CARD, style: 'royal' }).ok).toBe(false);
    expect(planAllowsStyle('nuit', 'royal')).toBe(true);
  });

  it('résiliation : la carte garde son design premium mais ne peut pas en changer pour un autre', () => {
    const s0 = freshState(D, 10_000);
    let s = unwrap(subscribePlan(s0, ctxAt(D), 'orchidee')).state;
    const card = unwrap(createCard(s, ctxAt(D), { ...CARD, style: 'neon' }));
    s = unwrap(subscribePlan(card.state, ctxAt(D), 'essentiel')).state;
    expect(s.profile.planRenewsOn).toBeNull();
    expect(updateCard(s, ctxAt(D), card.data.id, { name: 'Renommée' }).ok).toBe(true);
    expect(updateCard(s, ctxAt(D), card.data.id, { style: 'galaxy' }).ok).toBe(false);
  });

  it('renouvellement mensuel puis suspension faute de solde', () => {
    const price = PLAN_INFO.amethyste.price;
    const s0 = freshState(D, price * 2);
    const s1 = unwrap(subscribePlan(s0, ctxAt(D), 'amethyste')).state;
    expect(chargeDuePlanFees(s1, ctxAt('2026-02-01'))).toBe(s1);
    const s2 = chargeDuePlanFees(s1, ctxAt('2026-02-15'));
    expect(accountBalance(s2, 'acc_main')).toBe(0);
    expect(s2.profile.planRenewsOn).toBe('2026-03-15');
    const s3 = chargeDuePlanFees(s2, ctxAt('2026-03-20'));
    expect(s3.profile.plan).toBe('essentiel');
    expect(accountBalance(s3, 'acc_main')).toBe(0);
  });
});
